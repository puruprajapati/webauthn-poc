package com.bofa.webauthn.dto;

import lombok.Data;

@Data
public class UserDTO {
  private String id;
  private String name;
  private String displayName;
  private String phoneNumber;
}
