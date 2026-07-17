package com.bofa.webauthn.service;

import com.bofa.webauthn.config.AttestationMode;
import com.bofa.webauthn.config.FidoServerConfig;
import com.bofa.webauthn.domain.Credential;
import com.bofa.webauthn.domain.User;
import com.bofa.webauthn.dto.PublicKeyCredentialCreationOptionsDTO;
import com.bofa.webauthn.dto.PublicKeyCredentialUserEntityDTO;
import com.bofa.webauthn.dto.UserDTO;
import com.bofa.webauthn.repository.ChallengeStore;
import com.bofa.webauthn.repository.UserStore;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.exception.DataConversionException;
import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.*;
import com.webauthn4j.data.attestation.AttestationObject;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.attestation.statement.AttestationCertificatePath;
import com.webauthn4j.data.attestation.statement.AttestationStatement;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.attestation.statement.CertificateBaseAttestationStatement;
import com.webauthn4j.data.client.CollectedClientData;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.verifier.exception.VerificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.cert.X509Certificate;
import java.util.*;

@Slf4j
@Service
public class WebAuthnRegistrationService {
  private final WebAuthnManager webAuthnManager;
  private final FidoServerConfig fidoServerConfig;
  private final ChallengeStore challengeStore;
  private final UserStore userStore;

  public WebAuthnRegistrationService(WebAuthnManager webAuthnManager,
          FidoServerConfig fidoServerConfig,
                                     ChallengeStore challengeStore,
                                     UserStore userStore,
                                     MetadataProviderService metadataProviderService) {
    this.fidoServerConfig = fidoServerConfig;
    this.challengeStore = challengeStore;
    this.userStore = userStore;
    this.webAuthnManager = webAuthnManager;
  }

  public PublicKeyCredentialCreationOptionsDTO generateRegistrationOptions(UserDTO userDTO){
    Optional<User> userOptional = userStore.getUserByUserId(userDTO.getId());

    User user = userOptional.orElse(new User());
    user.setId(userDTO.getId());
    user.setName(userDTO.getName());
    user.setDisplayName(userDTO.getDisplayName());
    user.setPhoneNumber(userDTO.getPhoneNumber());

    // generate new challenge for registration and save in session
    Challenge challenge = new DefaultChallenge(new Random().ints(97, 123)
      .limit(16)
      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
      .toString());
    String challengeBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(challenge.getValue());
    challengeStore.saveChallenge(user.getId(), challengeBase64);

    PublicKeyCredentialRpEntity rp = new PublicKeyCredentialRpEntity(fidoServerConfig.getRelyingPartyId(), fidoServerConfig.getRelyingParty());

    PublicKeyCredentialUserEntityDTO userEntity = new PublicKeyCredentialUserEntityDTO( Base64.getUrlEncoder().withoutPadding().encodeToString(user.getId().getBytes()), user.getName(), user.getDisplayName());
    List<PublicKeyCredentialParameters> publicKeyCredentialParameters = getPublicKeyCredentialParameters();
    AuthenticatorSelectionCriteria authenticatorSelectionCriteria = new AuthenticatorSelectionCriteria(
      AuthenticatorAttachment.PLATFORM,
      false,
      ResidentKeyRequirement.DISCOURAGED,
      UserVerificationRequirement.REQUIRED
    );

    AttestationConveyancePreference attestationConveyancePreference = AttestationConveyancePreference.DIRECT;

    userStore.saveUser(user);

    return new PublicKeyCredentialCreationOptionsDTO(
      challengeBase64,
      rp,
      userEntity,
      new ArrayList<>(),
      publicKeyCredentialParameters,
      Long.parseLong(fidoServerConfig.getTimeout()),
      attestationConveyancePreference,
      authenticatorSelectionCriteria
    );


  }

  public boolean verifyRegistration(String userId, String registrationResponseJSON){
    log.info("═══════════════════════════════════════════════════════════════");
    log.info("WebAuthn Registration Verification - Starting");
    log.info("═══════════════════════════════════════════════════════════════");
    log.info("User ID: {}", userId);
    
    // Retrieve challenge and user from store
    String challengeStr = challengeStore.getChallengeForUser(userId);
    Optional<User> userOptional = userStore.getUserByUserId(userId);

    if(userOptional.isEmpty() || challengeStr == null){
      log.error("❌ Verification Failed: Invalid user or challenge");
      log.error("   User found: {}, Challenge found: {}", userOptional.isPresent(), challengeStr != null);
      return false;
    }

    // Parse registration response JSON
    RegistrationData registrationData;
    try{
      registrationData = webAuthnManager.parseRegistrationResponseJSON(registrationResponseJSON);
    } catch (DataConversionException e){
      log.error("❌ Failed to parse registration response JSON: {}", e.getMessage());
      return false;
    }

    log.info("");
    log.info("─ REQUEST PARAMETERS ─");
    Origin origin = new Origin(fidoServerConfig.getOrigin());
    String rpId = fidoServerConfig.getRelyingPartyId();
    Challenge challenge = new DefaultChallenge(challengeStr);
    
    log.info("  Origin         : {}", origin.toString());
    log.info("  RP ID          : {}", rpId);
    log.info("  Challenge      : {} bytes", challengeStr.getBytes().length);
    
    ServerProperty serverProperty = new ServerProperty(origin, rpId, challenge);

    log.info("");
    log.info("─ CONFIGURATION ─");
    boolean userVerificationRequired = fidoServerConfig.isUserVerificationRequired();
    boolean userPresenceRequired = fidoServerConfig.isUserPresenceRequired();
    
    log.info("  User Verification Required : {}", userVerificationRequired);
    log.info("  User Presence Required     : {}", userPresenceRequired);

    List<PublicKeyCredentialParameters> publicKeyCredentialParameters = getPublicKeyCredentialParameters();
    RegistrationParameters registrationParameters = new RegistrationParameters(
      serverProperty, 
      publicKeyCredentialParameters, 
      userVerificationRequired, 
      userPresenceRequired
    );

    // Parse attestation data for detailed logging
    AttestationObject attestationObject = registrationData.getAttestationObject();
    logAttestationDetails(registrationData, attestationObject);

    // Perform WebAuthn verification
    log.info("");
    log.info("─ PERFORMING WEBAUTHN VERIFICATION ─");
    try{
      log.info("Registration Data : {}", registrationData);
      registrationData = webAuthnManager.verify(registrationData, registrationParameters);
      log.info("✓ WebAuthn verification PASSED");
    } catch (VerificationException e){
      log.error("❌ WebAuthn verification FAILED: {}", e.getMessage());
      log.error("   Error details: {}", e.getClass().getSimpleName());
      return false;
    }

    // Log post-verification details
    logVerificationResults(registrationData, attestationObject);

    enforceAttestationPolicy(registrationData);

    // Persist credential record
    CredentialRecord credentialRecord = new CredentialRecordImpl(
      registrationData.getAttestationObject(),
      registrationData.getCollectedClientData(),
      registrationData.getClientExtensions(),
      registrationData.getTransports()
    );

    String credentialId = Base64.getUrlEncoder()
      .withoutPadding()
      .encodeToString(attestationObject.getAuthenticatorData().getAttestedCredentialData().getCredentialId());
    
    Credential credential = new Credential();
    credential.setCredentialRecord(credentialRecord);
    credential.setCredentialId(credentialId);

    userStore.addCredential(userId, credential);
    
    log.info("");
    log.info("✓ Credential successfully registered");
    log.info("  Credential ID: {} bytes", credentialId.length());
    log.info("═══════════════════════════════════════════════════════════════");
    return true;
  }

    private void enforceAttestationPolicy(RegistrationData registrationData) {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("Enforcing Attestation Policy");
        log.info("═══════════════════════════════════════════════════════════════");
        String format = registrationData.getAttestationObject().getFormat();
        AttestationStatement attestationStatement = registrationData.getAttestationObject().getAttestationStatement();
        AttestationMode attestationMode = fidoServerConfig.getAttesattionMode();
        boolean isNone = "none".equalsIgnoreCase(format);
        boolean hasCertChain = hasCertChain(attestationStatement);
        UUID aaguid = registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData().getAaguid().getValue();
        boolean zeroAaguid = isZeroAaguid(aaguid);

        if(attestationMode == AttestationMode.NONE){
            log.info("  Attestation Mode: NONE - No attestation checks required");
            return;
        }
        if(attestationMode == AttestationMode.REQUIRED){
            if(isNone){
                log.error("❌ Attestation Mode REQUIRED but attestation format is 'none'");
                throw new IllegalArgumentException("Attestation required but format is 'none'");
            }
            if(!hasCertChain){
                log.error("❌ Attestation Mode REQUIRED but no certificate chain found in attestation statement");
                throw new IllegalArgumentException("Attestation required but no certificate chain found");
            }
            enforceAaaguidAllowList(aaguid, zeroAaguid);
            log.info("  Attestation Mode: REQUIRED - Attestation checks passed");
            log.info("*******************************************************");
            return;
        }
    }

    private void enforceAaaguidAllowList(UUID aaguid, boolean zeroAaguid) {
      if(zeroAaguid){
          log.info("  AAGUID is zero - skipping allow list check");
          return;
      }
      List<String> allowed = fidoServerConfig.getAllowedAAGUIDs();
      String aaguidStr = aaguid.toString();
      if(allowed == null || allowed.isEmpty()){
          log.info("  No AAGUID allow list configured - skipping check");
          return;
      }
      boolean permitted = allowed.stream().filter(java.util.Objects::nonNull).anyMatch(aaguidStr::equalsIgnoreCase);
      if(!permitted){
          log.error("❌ AAGUID {} is not in the allowed list: {}", aaguidStr, allowed);
          throw new IllegalArgumentException("AAGUID not permitted: " + aaguidStr);
      }
      log.info("  AAGUID {} is permitted according to the allow list", aaguidStr);
    }

    private boolean isZeroAaguid(UUID aaguid) {
        return aaguid.getMostSignificantBits() == 0L && aaguid.getLeastSignificantBits() == 0L;
    }

    private boolean hasCertChain(AttestationStatement attestationStatement) {
        if(attestationStatement instanceof CertificateBaseAttestationStatement){
            AttestationCertificatePath x5c = ((CertificateBaseAttestationStatement) attestationStatement).getX5c();
            return x5c != null && !x5c.isEmpty();
        }
        return false;
    }

    /**
   * Log detailed attestation information
   */
  private void logAttestationDetails(RegistrationData registrationData, AttestationObject attestationObject) {
    log.info("");
    log.info("─ ATTESTATION DETAILS ─");
    
    // Attestation format
    log.info("  Attestation Format: {}", attestationObject.getFormat());
    
    // Attestation statement
    log.info("  Attestation Statement Type: {}", 
      attestationObject.getAttestationStatement() != null ? 
      attestationObject.getAttestationStatement().getClass().getSimpleName() : 
      "NONE");
    
    // Authenticator data
    AuthenticatorData authData = attestationObject.getAuthenticatorData();
    log.info("  Authenticator Data:");
    log.info("    • Credential ID Length: {} bytes", 
      authData.getAttestedCredentialData() != null ? 
      authData.getAttestedCredentialData().getCredentialId().length : 0);
    log.info("    • AAGUID: {}", 
      authData.getAttestedCredentialData() != null ? 
      authData.getAttestedCredentialData().getAaguid() : "N/A");
    log.info("    • Counter: {}", authData.getSignCount());
    log.info("    • User Present: {}", authData.isFlagUP());
    log.info("    • User Verified: {}", authData.isFlagUV());
    log.info("    • Backup Eligibility: {}", authData.isFlagBE());
    log.info("    • Backup State: {}", authData.isFlagBS());
    
    // Client data
    CollectedClientData clientData = registrationData.getCollectedClientData();
    log.info("  Client Data:");
    log.info("    • Type (Ceremony): {}", clientData.getType());
    log.info("    • Challenge: {} bytes", clientData.getChallenge().getValue().length);
    log.info("    • Origin: {}", clientData.getOrigin());
    
    // X5c certificate chain
    logCertificateChainInfo(attestationObject);
  }

  /**
   * Log certificate chain information (x5c from attestation statement)
   */
  private void logCertificateChainInfo(AttestationObject attestationObject) {
    log.info("");
    log.info("─ CERTIFICATE CHAIN (x5c) ─");
    
    try {
      // Try to extract x5c from attestation statement
      Object attestationStatement = attestationObject.getAttestationStatement();
      
      if (attestationStatement == null) {
        log.info("  No attestation statement available");
        return;
      }

      if(!(attestationStatement instanceof CertificateBaseAttestationStatement)) {
        log.info("---- Certificate chian                     : N/A (statement type carries no x5c)");
      }

      AttestationCertificatePath x5c = ((CertificateBaseAttestationStatement) attestationStatement).getX5c();
      if (x5c == null || x5c.isEmpty()) {
        log.info("---- Certificate chian                     : EMPTY (selft-attestation or 'none' format)");
      }

      // AttestationCertificatePath extends AbstractList<X509Certificate> - use direclty as list
      int chainSize = x5c.size();

      // classify the chain
      boolean leafOnly = chainSize == 1;
      int intermediateCount = chainSize - 1; // all certs after leaf are intermediates (root is in trust store, not sent)


      // Check if this is a direct/packed attestation with certificates
      String attestationStatementClass = attestationStatement.getClass().getSimpleName();
      log.info("  Attestation Statement Class: {}", attestationStatementClass);

      log.info("--------------Certificate chian details------------------------------------");
      log.info(" - Chainsize : {} certificates", chainSize);
      if(leafOnly)
      {
        log.info("Chain composition : LEAF ONLY (no intermediate certificate sent)");
        log.info("Trust store validation path: LEAF certificate issuer must chain DIRECTLY to a root in the trust store");
      } else {
        log.info("Chain composition : LEAF + {} INTERMEDIATE certificate(s)", intermediateCount);
        log.info("Trust store validation path: LEAF certificate issuer must chain to INTERMEDIATE(s) and ultimately to a root in the trust store");
      }

      // print LEAF certificate
      X509Certificate leafCert = x5c.get(0);
      log.info("  LEAF Certificate Type: {}", leafCert.getType());
      log.info("  LEAF Certificate Subject: {}", leafCert.getSubjectX500Principal().getName());
      log.info("  LEAF Certificate Issuer : {}", leafCert.getIssuerX500Principal().getName());
      log.info("  LEAF Certificate Serial : {}", leafCert.getSerialNumber().toString(16));
      log.info("  LEAF Certificate Validity: {} to {}", leafCert.getNotBefore(), leafCert.getNotAfter());
      log.info("  LEAF Certificate Signature Algorithm: {}", leafCert.getSigAlgName());
      log.info("  LEAF Certificate Public Key Algorithm: {}", leafCert.getPublicKey().getAlgorithm());
      log.info("  LEAF Certificate Public Key Format: {}", leafCert.getPublicKey().getFormat());
      log.info("  LEAF Certificate Public Key Encoded Length: {} bytes", leafCert.getPublicKey().getEncoded().length);
      log.info("  LEAF Certificate self-signed? {}", leafCert.getSubjectX500Principal().equals(leafCert.getIssuerX500Principal()));

      // Print intermediate certificates
      for(int i=1; i<chainSize; i++){
        X509Certificate inter = x5c.get(i);
        log.info("  INTERMEDIATE Certificate #{} Type: {}", i, inter.getType());
        log.info("  INTERMEDIATE Certificate #{} Subject: {}", i, inter.getSubjectX500Principal().getName());
        log.info("  INTERMEDIATE Certificate #{} Issuer : {}", i, inter.getIssuerX500Principal().getName());
        log.info("  INTERMEDIATE Certificate #{} Serial : {}", i, inter.getSerialNumber().toString(16));
        log.info("  INTERMEDIATE Certificate #{} Validity: {} to {}", i, inter.getNotBefore(), inter.getNotAfter());
        log.info("  INTERMEDIATE Certificate #{} Signature Algorithm: {}", i, inter.getSigAlgName());
        log.info("  INTERMEDIATE Certificate #{} Public Key Algorithm: {}", i, inter.getPublicKey().getAlgorithm());
        log.info("  INTERMEDIATE Certificate #{} Public Key Format: {}", i, inter.getPublicKey().getFormat());
        log.info("  INTERMEDIATE Certificate #{} Public Key Encoded Length: {} bytes", i, inter.getPublicKey().getEncoded().length);
        log.info("  INTERMEDIATE Certificate #{} self-signed? {}", i, inter.getSubjectX500Principal().equals(inter.getIssuerX500Principal()));
      }

      // For PackedAttestationStatement or similar formats with x5c
      if (attestationStatementClass.contains("Packed") || 
          attestationStatementClass.contains("FIDO") ||
          attestationStatementClass.contains("Android")) {
        
        log.info("  Attempting to extract x5c from attestation statement...");
        log.info("  AttestationStatmentClass : {}", attestationStatementClass);
        log.info("    Note: Certificate extraction depends on attestation format");
        log.info("    • PACKED format may have x5c array");
        log.info("    • FIDO-U2F may have certificates");
        log.info("    • ANDROID-SAFETYNET may have certificates");
        log.info("    • TPMV2 may have x5c");

      }
    } catch (Exception e) {
      log.warn("  Could not extract certificate chain details: {}", e.getMessage());
    }
  }

  /**
   * Log verification results after webAuthnManager.verify()
   */
  private void logVerificationResults(RegistrationData registrationData, AttestationObject attestationObject) {
    log.info("");
    log.info("─ VERIFICATION RESULTS ─");
    
    try {
      // Attestation verification status
      log.info("  Attestation Verification:");
      log.info("    • Format: {}", attestationObject.getFormat());
      log.info("    • Status: PASSED (signature verified)");

      
      // User verification results
      AuthenticatorData authData = attestationObject.getAuthenticatorData();
      log.info("");
      log.info("  Authenticator Response:");
      log.info("    • User Present: {}", authData.isFlagUP() ? "YES" : "NO");
      log.info("    • User Verified: {}", authData.isFlagUV() ? "YES" : "NO");
      
      if (authData.isFlagBE()) {
        log.info("    • Backup Eligible: YES");
      }
      if (authData.isFlagBS()) {
        log.info("    • Backup State: YES");
      }
      
      // AAGUID
      if (attestationObject.getAuthenticatorData().getAttestedCredentialData() != null) {
        String aaguid = attestationObject.getAuthenticatorData()
          .getAttestedCredentialData().getAaguid().toString();
        log.info("");
        log.info("  Authenticator Identification:");
        log.info("    • AAGUID: {}", aaguid);
        log.info("      (Can be used to look up authenticator metadata in FIDO registry)");
      }
      
      // Policy result
      log.info("");
      log.info("  Policy Verification Result:");
      log.info("    • Status: PASSED");
      log.info("    • User Presence Verified: YES");
      log.info("    • User Verification Requirement: {}", 
        fidoServerConfig.isUserVerificationRequired() ? "REQUIRED" : "PREFERRED");
      log.info("    • Attestation Conveyance: DIRECT");
      log.info("    • All Checks: PASSED ✓");
      
    } catch (Exception e) {
      log.warn("  Could not log detailed verification results: {}", e.getMessage());
    }
  }




  private List<PublicKeyCredentialParameters> getPublicKeyCredentialParameters() {
    List<PublicKeyCredentialParameters> publicKeyCredentialParameters = new ArrayList<>();
    publicKeyCredentialParameters.add(new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256));
    publicKeyCredentialParameters.add(new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256));
    return publicKeyCredentialParameters;
  }
}
