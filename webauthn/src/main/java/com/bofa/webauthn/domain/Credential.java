package com.bofa.webauthn.domain;

import com.webauthn4j.credential.CredentialRecord;
import lombok.Data;

@Data
public class Credential {
  private String credentialId;
  private CredentialRecord credentialRecord;
  private int signCount;
}
