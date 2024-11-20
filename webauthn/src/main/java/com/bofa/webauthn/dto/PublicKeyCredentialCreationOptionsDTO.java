package com.bofa.webauthn.dto;

import com.webauthn4j.data.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PublicKeyCredentialCreationOptionsDTO {
  private final String challenge;
  private final PublicKeyCredentialRpEntity rp;
  private final PublicKeyCredentialUserEntityDTO user;
  private final List<PublicKeyCredentialDescriptorDTO> allowCredentials;
  private final List<PublicKeyCredentialParameters> pubKeyCredParams;
  private final long timeout;
  private final AttestationConveyancePreference attestation;
  private final AuthenticatorSelectionCriteria authenticatorSelection;

}
