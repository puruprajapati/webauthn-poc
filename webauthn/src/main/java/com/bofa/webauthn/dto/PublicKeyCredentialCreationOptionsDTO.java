package com.bofa.webauthn.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.webauthn4j.data.*;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class PublicKeyCredentialCreationOptionsDTO {
  private final String challenge;
  private final PublicKeyCredentialRpEntity rp;
  private final PublicKeyCredentialUserEntityDTO user;
  private final List<PublicKeyCredentialDescriptorDTO> allowCredentials;
  @JsonIgnore
  private final List<PublicKeyCredentialParameters> pubKeyCredParamsInternal;
  private final List<PublicKeyCredentialParametersDTO> pubKeyCredParams;
  private final long timeout;
  private final AttestationConveyancePreference attestation;
  private final AuthenticatorSelectionCriteria authenticatorSelection;

  public PublicKeyCredentialCreationOptionsDTO(
          String challenge,
          PublicKeyCredentialRpEntity rp,
          PublicKeyCredentialUserEntityDTO user,
          List<PublicKeyCredentialDescriptorDTO> allowCredentials, List<PublicKeyCredentialParameters> pubKeyCredParamsInternal,
          long timeout,
          AttestationConveyancePreference attestation,
          AuthenticatorSelectionCriteria authenticatorSelection) {
    this.challenge = challenge;
    this.rp = rp;
    this.user = user;
    this.allowCredentials = allowCredentials;
      this.pubKeyCredParamsInternal = pubKeyCredParamsInternal;
      this.pubKeyCredParams = pubKeyCredParamsInternal.stream().map(PublicKeyCredentialParametersDTO::from).collect(Collectors.toList());
    this.timeout = timeout;
    this.attestation = attestation;
    this.authenticatorSelection = authenticatorSelection;
  }

  public PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions() {
    return new PublicKeyCredentialCreationOptions(
            rp,
            user.toPublicKeyCredentialUserEntity(),
            new DefaultChallenge(challenge),
            pubKeyCredParamsInternal,
            timeout,
            List.of(),
            authenticatorSelection,
            null,
            null,
            null
    );
  }

}
