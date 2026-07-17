package com.bofa.webauthn.dto;

import com.webauthn4j.data.PublicKeyCredentialUserEntity;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PublicKeyCredentialUserEntityDTO {
  private final String id;
  private final String name;
  private final String displayName;

  public PublicKeyCredentialUserEntity toPublicKeyCredentialUserEntity(){
    return new PublicKeyCredentialUserEntity(id.getBytes(), name, displayName);
  }
}
