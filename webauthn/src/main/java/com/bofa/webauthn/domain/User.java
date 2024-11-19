package com.bofa.webauthn.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class User {
    private String id;
    private String name;
    private String displayName;
    private String phoneNumber;
    private List<Credential> credentials = new ArrayList<>();

    public void addCredential(Credential credential){
      credentials.add(credential);
    }
}
