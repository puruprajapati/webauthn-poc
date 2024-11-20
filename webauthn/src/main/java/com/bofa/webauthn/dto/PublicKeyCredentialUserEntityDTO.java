package com.bofa.webauthn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PublicKeyCredentialUserEntityDTO {
  private final String id;
  private final String name;
  private final String displayName;
}
