package com.bofa.webauthn.service;

import com.bofa.webauthn.config.FidoServerConfig;
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

@Service
public class WebAuthnAuthenticationService {
  private final WebAuthnManager webAuthnManager;
  private final FidoServerConfig fidoServerConfig;
  private final ChallengeStore challengeStore;
  private final UserStore userStore;

  public WebAuthnAuthenticationService(FidoServerConfig fidoServerConfig, ChallengeStore challengeStore, UserStore userStore) {
    this.webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();
    this.fidoServerConfig = fidoServerConfig;
    this.challengeStore = challengeStore;
    this.userStore = userStore;
  }

  public PublicKeyCredentialRequestOptionsDTO generateAuthenticationOptions() {
    // For true passwordless authentication, we don't require userID upfront
    // The resident key will provide the userID during verification

    // Generate a new challenge for authentication
    Challenge challenge = new DefaultChallenge(new Random().ints(97, 123)
      .limit(16)
      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
      .toString());
    String challengeBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(challenge.getValue());

    // Store challenge without user-specific mapping (will use generic key or later mapping)
    challengeStore.saveChallenge("general", challengeBase64);

    // For passwordless authentication with resident keys, we don't send allowCredentials
    // The client will use resident keys and the authenticator will determine which user
    List<PublicKeyCredentialDescriptorDTO> allowCredentialsDTO = new ArrayList<>();


    // Build the options
    PublicKeyCredentialRequestOptionsDTO result = new PublicKeyCredentialRequestOptionsDTO(
      challengeBase64,
      Long.parseLong(fidoServerConfig.getTimeout()),
      fidoServerConfig.getRelyingPartyId(),
      allowCredentialsDTO,
      UserVerificationRequirement.PREFERRED,
      null
    );
    return result;
  }

  public boolean verifyAuthentication(String userId, String authenticationResponseJSON) {
    // For passwordless authentication with resident keys, userId may be null
    // In that case, we'll extract it from the credential

    AuthenticationData authenticationData;
    try {
      authenticationData = webAuthnManager.parseAuthenticationResponseJSON(authenticationResponseJSON);
    } catch (DataConversionException e) {
      throw new IllegalArgumentException("Invalid authentication response JSON", e);
    }

    // If userId is not provided, extract from resident key
    String resolvedUserId = (userId == null || userId.isEmpty()) ?
      extractUserIdFromResidentKey(authenticationData) : userId;

    String challengeStr = Optional.ofNullable(challengeStore.getChallengeForUser("general"))
      .orElseThrow(() -> new IllegalArgumentException("No challenge found"));

    User user = userStore.getUserByUserId(resolvedUserId).orElseThrow(() ->
      new IllegalArgumentException("User not found for ID: " + resolvedUserId)
    );

    // Prepare Server Properties
    ServerProperty serverProperty = new ServerProperty(new Origin(fidoServerConfig.getOrigin()), fidoServerConfig.getRelyingPartyId(), new DefaultChallenge(challengeStr));

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
      fidoServerConfig.isUserVerificationRequired(),
      fidoServerConfig.isUserPresenceRequired()
    );

    // Verify authentication
    try {
      webAuthnManager.verify(authenticationData, authenticationParameters);
    } catch (VerificationException e) {
      throw new IllegalArgumentException("Authentication Failed!!", e);
    }

    // Authentication is successful
    System.out.println("Authentication successful for user: " + resolvedUserId);
    return true;
  }

  private String extractUserIdFromResidentKey(AuthenticationData authenticationData) {
    // Extract userId from the resident key credential
    // The authenticator provides the user handle which contains the userId
    if (authenticationData.getCredentialId() != null) {
      String credentialId = Base64.getUrlEncoder().withoutPadding().encodeToString(authenticationData.getCredentialId());

      // Search through all users to find which one has this credential
      // This is necessary because we don't know the user upfront in passwordless flow
      List<User> allUsers = userStore.getAllUsers();
      for (User user : allUsers) {
        for (Credential credential : user.getCredentials()) {
          if (credential.getCredentialId().equals(credentialId)) {
            return user.getId();
          }
        }
      }
    }
    throw new IllegalArgumentException("Unable to extract userId from resident key");
  }
}
