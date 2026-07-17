package com.bofa.webauthn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "fidoserver")
@Data
public class FidoServerConfig {
  private String hostname;
  private String display;
  private String origin;
  private String relyingParty;
  private String relyingPartyId;
  private String timeout;
  private boolean userVerificationRequired;
  private boolean userPresenceRequired;


  private boolean certPathVerificationEnabled = true;
  private  boolean certRevocationCheckEnabled = false;

  private AttestationMode attesattionMode = AttestationMode.PREFERRED;

  private List<String> allowedAAGUIDs = new ArrayList<>();

  private PinnedRoots pinnedRoots = new PinnedRoots();


  @Data
  public static class PinnedRoots{
    private boolean enabled = true;
    private String location = "pinned-roots";
    private int expiryWarningDays = 180;
    private boolean logFingerPrints = true;
    private Apple apple = new Apple();

    @Data
    public static class Apple{
      private boolean remoteDriftCheckEnabled = false;
      private String remotePemUrl = "https://www.apple.com/certificateauthority/Apple_WebAuthn_Root_CA.pem";
      private String expectedSha256 = "";
      private boolean failOnFInterprintMismatch = false;
      private int remoteCheckTimeoutMs = 5000;
    }
  }
}
