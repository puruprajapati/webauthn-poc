package com.bofa.webauthn.service;

import com.bofa.webauthn.config.WebAuthnConfig;
import com.bofa.webauthn.domain.Credential;
import com.bofa.webauthn.domain.User;
import com.bofa.webauthn.dto.PublicKeyCredentialDescriptorDTO;
import com.bofa.webauthn.dto.PublicKeyCredentialRequestOptionsDTO;
import com.bofa.webauthn.repository.ChallengeStore;
import com.bofa.webauthn.repository.UserStore;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.exception.DataConversionException;
import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.data.*;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.verifier.exception.VerificationException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WebAuthnAuthenticationService {
  private final WebAuthnManager webAuthnManager;
  private final WebAuthnConfig webAuthnConfig;
  private final ChallengeStore challengeStore;
  private final UserStore userStore;

  public WebAuthnAuthenticationService(WebAuthnConfig webAuthnConfig, ChallengeStore challengeStore, UserStore userStore) {
    this.webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();
    this.webAuthnConfig = webAuthnConfig;
    this.challengeStore = challengeStore;
    this.userStore = userStore;
  }

  public PublicKeyCredentialRequestOptionsDTO generateAuthenticationOptions(String userId) {
    Optional<User> userOptional = userStore.getUserByUserId(userId);

    if (userOptional.isEmpty()) {
      throw new IllegalArgumentException("User not found");
    }

    // Generate a new challenge for authentication
    Challenge challenge = new DefaultChallenge(new Random().ints(97, 123)
      .limit(16)
      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
      .toString());
    String challengeBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(challenge.getValue());
    challengeStore.saveChallenge(userId, challengeBase64);

    // Fetch the user's registered credentials
    List<Credential> userCredentials = userOptional.get().getCredentials();
    List<PublicKeyCredentialDescriptor> allowCredentials = new ArrayList<>();

    for (Credential credential : userCredentials) {
      allowCredentials.add(new PublicKeyCredentialDescriptor(
        PublicKeyCredentialType.PUBLIC_KEY,
        Base64.getUrlDecoder().decode(credential.getCredentialId()),
        credential.getCredentialRecord().getTransports()
      ));
    }

    // Convert to DTO for client-side compatibility
    List<PublicKeyCredentialDescriptorDTO> allowCredentialsDTO = allowCredentials.stream()
      .map(PublicKeyCredentialDescriptorDTO::new)
      .collect(Collectors.toList());


    // Build the options
    PublicKeyCredentialRequestOptionsDTO result = new PublicKeyCredentialRequestOptionsDTO(
      challengeBase64,
      Long.parseLong(webAuthnConfig.getTimeout()),
      webAuthnConfig.getRelyingPartyId(),
      allowCredentialsDTO,
      UserVerificationRequirement.PREFERRED,
      null
    );
    return result;
  }

  public boolean verifyAuthentication(String userId, String authenticationResponseJSON) {
    String challengeStr = Optional.ofNullable(challengeStore.getChallengeForUser(userId))
      .orElseThrow(() -> new IllegalArgumentException("No challenge found for user: " + userId));


    User user = userStore.getUserByUserId(userId).orElseThrow(() ->
      new IllegalArgumentException("User not found for ID: " + userId)
    );

    AuthenticationData authenticationData;
    try {
      authenticationData = webAuthnManager.parseAuthenticationResponseJSON(authenticationResponseJSON);
    } catch (DataConversionException e) {
      throw new IllegalArgumentException("Invalid authentication response JSON", e);
    }

    // Prepare Server Properties
    ServerProperty serverProperty = new ServerProperty(new Origin(webAuthnConfig.getOrigin()), webAuthnConfig.getRelyingPartyId(), new DefaultChallenge(challengeStr));

    // Fetch credential data from the userStore
    String credentialId = Base64.getUrlEncoder().withoutPadding().encodeToString(authenticationData.getCredentialId());
    Credential credential = user.getCredentials().stream()
      .filter(c -> c.getCredentialId().equals(credentialId))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Credential not found for ID: " + credentialId));
    CredentialRecord credentialRecord = credential != null ? credential.getCredentialRecord() : null;

    AuthenticationParameters authenticationParameters = new AuthenticationParameters(
      serverProperty,
      credentialRecord,
      null,
      webAuthnConfig.isUserVerificationRequired(),
      webAuthnConfig.isUserPresenceRequired()
    );

    // Verify authentication
    try {
      webAuthnManager.verify(authenticationData, authenticationParameters);
    } catch (VerificationException e) {
      System.out.println(e.getMessage());
      return false;
    }

    // Authentication is successful
    System.out.println("Authentication successful for user: " + userId);
    return true;
  }
}
