package com.bofa.webauthn.dto;

import lombok.Data;

@Data
public class VerifyAuthenticationDTO {
  private String userId; // Optional - for backward compatibility with userID-based auth
  private String authenticateResponseJSON;
}
