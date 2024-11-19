package com.bofa.webauthn.dto;

import lombok.Data;

@Data
public class VerifyRegistrationDTO {
  private String userId;
  private String registrationResponseJSON;
}
