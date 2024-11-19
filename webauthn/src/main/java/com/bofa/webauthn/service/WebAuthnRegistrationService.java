package com.bofa.webauthn.service;

import com.bofa.webauthn.config.WebAuthnConfig;
import com.bofa.webauthn.domain.Credential;
import com.bofa.webauthn.domain.User;
import com.bofa.webauthn.dto.UserDTO;
import com.bofa.webauthn.repository.ChallengeStore;
import com.bofa.webauthn.repository.UserStore;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.exception.DataConversionException;
import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.*;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.verifier.exception.VerificationException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WebAuthnRegistrationService {
  private final WebAuthnManager webAuthnManager;
  private final WebAuthnConfig webAuthnConfig;
  private final ChallengeStore challengeStore;
  private final UserStore userStore;


  public WebAuthnRegistrationService(WebAuthnConfig webAuthnConfig, ChallengeStore challengeStore, UserStore userStore) {
    this.webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();
    this.webAuthnConfig = webAuthnConfig;
    this.challengeStore = challengeStore;
    this.userStore = userStore;
  }

  public PublicKeyCredentialCreationOptions generateRegistrationOptions(UserDTO userDTO){
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

    PublicKeyCredentialRpEntity rp = new PublicKeyCredentialRpEntity(webAuthnConfig.getRelyingPartyId(), webAuthnConfig.getRelyingParty());

    PublicKeyCredentialUserEntity userEntity = new PublicKeyCredentialUserEntity(
      Base64.getUrlDecoder().decode(user.getId()),
      user.getName(),
      user.getDisplayName()
    );
    
    List<PublicKeyCredentialParameters> publicKeyCredentialParameters = getPublicKeyCredentialParameters();

    AttestationConveyancePreference attestationConveyancePreference = AttestationConveyancePreference.DIRECT;

    AuthenticatorSelectionCriteria authenticatorSelectionCriteria = new AuthenticatorSelectionCriteria(
      AuthenticatorAttachment.CROSS_PLATFORM,
      true,
      ResidentKeyRequirement.PREFERRED,
      UserVerificationRequirement.PREFERRED
    );

    userStore.saveUser(user);

    return new PublicKeyCredentialCreationOptions(
      rp,
      userEntity,
      challenge,
      publicKeyCredentialParameters,
      Long.parseLong(webAuthnConfig.getTimeout()),
      new ArrayList<>(),
      authenticatorSelectionCriteria,
      attestationConveyancePreference,
      null
    );
  }

  public boolean verifyRegistration(String userId, String registrationResponseJSON){
    String challengeStr = challengeStore.getChallengeForUser(userId);
    Optional<User> userOptional = userStore.getUserByUserId(userId);

    if(userOptional.isEmpty() || challengeStr == null){
      System.out.println("Invalid user or challenge");
      return false;
    }

    RegistrationData registrationData;
    try{
      registrationData = webAuthnManager.parseRegistrationResponseJSON(registrationResponseJSON);
    } catch (DataConversionException e){
      System.out.println(e.getMessage());
      return false;
    }

    Origin origin = new Origin(webAuthnConfig.getOrigin());
    String rpId = webAuthnConfig.getRelyingPartyId();
    Challenge challenge = new DefaultChallenge(challengeStr);
    ServerProperty serverProperty = new ServerProperty(origin, rpId, challenge);

    List<PublicKeyCredentialParameters> publicKeyCredentialParameters = getPublicKeyCredentialParameters();
    boolean userVerificationRequired = webAuthnConfig.isUserVerificationRequired();
    boolean userPresenceRequired = webAuthnConfig.isUserPresenceRequired();

    RegistrationParameters registrationParameters = new RegistrationParameters(serverProperty, publicKeyCredentialParameters, userVerificationRequired, userPresenceRequired);

    try{
      registrationData = webAuthnManager.verify(registrationData, registrationParameters);
    } catch (VerificationException e){
      System.out.println(e.getMessage());
      return false;
    }

    // persist CredentialRecord object, which will be used in authentication process
    CredentialRecord credentialRecord = new CredentialRecordImpl(
      registrationData.getAttestationObject(),
      registrationData.getCollectedClientData(),
      registrationData.getClientExtensions(),
      registrationData.getTransports()
    );

    // save
    String credentialId = Base64.getUrlEncoder().withoutPadding().encodeToString(registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData().getCredentialId());
    Credential credential = new Credential();
    credential.setCredentialRecord(credentialRecord);
    credential.setCredentialId(credentialId);
    userStore.addCredential(userId, credential);

    return true;
  }



  private List<PublicKeyCredentialParameters> getPublicKeyCredentialParameters() {
    List<PublicKeyCredentialParameters> publicKeyCredentialParameters = new ArrayList<>();
    publicKeyCredentialParameters.add(new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256));
    publicKeyCredentialParameters.add(new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256));
    return publicKeyCredentialParameters;
  }
}
