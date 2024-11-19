package com.bofa.webauthn.dto;

import com.webauthn4j.data.PublicKeyCredentialDescriptor;
import lombok.Data;

import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class PublicKeyCredentialDescriptorDTO {
  private final String type;
  private final String id; // Base64 URL-safe encoded string
  private final Set<String> transports;

  public PublicKeyCredentialDescriptorDTO(PublicKeyCredentialDescriptor descriptor) {
    this.type = descriptor.getType().toString();
    this.id = Base64.getUrlEncoder().withoutPadding().encodeToString(descriptor.getId());;
    this.transports = descriptor.getTransports().stream().map(transport -> transport.toString()).collect(Collectors.toSet());;
  }
}
