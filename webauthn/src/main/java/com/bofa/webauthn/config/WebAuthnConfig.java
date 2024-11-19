package com.bofa.webauthn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "authn")
@Data
public class WebAuthnConfig {
  private String hostname;
  private String display;
  private String origin;
  private String relyingParty;
  private String relyingPartyId;
  private String timeout;
  private boolean userVerificationRequired;
  private boolean userPresenceRequired;
}
