package com.bofa.webauthn.dto;

import com.webauthn4j.data.UserVerificationRequirement;
import lombok.Data;

import java.util.List;

@Data
public class PublicKeyCredentialRequestOptionsDTO {
  private final String challenge;
  private final long timeout;
  private final String rpId;
  private final List<PublicKeyCredentialDescriptorDTO> allowCredentials;
  private final String userVerification;
  private final String extensions;

  public PublicKeyCredentialRequestOptionsDTO(String challenge, long timeout, String rpId, List<PublicKeyCredentialDescriptorDTO> allowCredentials, UserVerificationRequirement userVerification, String extensions) {
    this.challenge = challenge;
    this.timeout = timeout;
    this.rpId = rpId;
    this.allowCredentials = allowCredentials;
    this.userVerification = userVerification.toString();
    this.extensions = extensions;
  }
}
