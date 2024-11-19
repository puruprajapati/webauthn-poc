package com.bofa.webauthn.dto;

import lombok.Data;

@Data
public class VerifyAuthenticationDTO {
  private String userId;
  private String authenticationResponseJSON;
}
