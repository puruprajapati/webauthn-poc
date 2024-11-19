package com.bofa.webauthn.controller;

import com.bofa.webauthn.dto.PublicKeyCredentialRequestOptionsDTO;
import com.bofa.webauthn.dto.UserDTO;
import com.bofa.webauthn.dto.VerifyAuthenticationDTO;
import com.bofa.webauthn.dto.VerifyRegistrationDTO;
import com.bofa.webauthn.service.WebAuthnAuthenticationService;
import com.bofa.webauthn.service.WebAuthnRegistrationService;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/")
public class WebAuthnController {

  private final WebAuthnAuthenticationService webAuthnAuthenticationService;
  private final WebAuthnRegistrationService webAuthnRegistrationService;


  public WebAuthnController(WebAuthnAuthenticationService webAuthnAuthenticationService, WebAuthnRegistrationService webAuthnRegistrationService) {
    this.webAuthnAuthenticationService = webAuthnAuthenticationService;
    this.webAuthnRegistrationService = webAuthnRegistrationService;
  }

  @PostMapping("register/options")
  public PublicKeyCredentialCreationOptions getRegistrationOptions(@RequestBody UserDTO user){
    return webAuthnRegistrationService.generateRegistrationOptions(user);
  }

  @PostMapping("register/verify")
  public boolean verifyRegistration(@RequestBody VerifyRegistrationDTO verifyRegistrationDTO){
    return webAuthnRegistrationService.verifyRegistration(verifyRegistrationDTO.getUserId(), verifyRegistrationDTO.getRegistrationResponseJSON());
  }

  @PostMapping("authenticate/options")
  public PublicKeyCredentialRequestOptionsDTO getAuthenticationOPtions(@RequestBody UserDTO user){
    PublicKeyCredentialRequestOptionsDTO result = webAuthnAuthenticationService.generateAuthenticationOptions(user.getId());
    return result;
  }

  @PostMapping("authenticate/verify")
  public boolean verifyAuthentication(@RequestBody VerifyAuthenticationDTO verifyAuthenticationDTO){
    return webAuthnAuthenticationService.verifyAuthentication(verifyAuthenticationDTO.getUserId(), verifyAuthenticationDTO.getAuthenticateResponseJSON());
  }


}
