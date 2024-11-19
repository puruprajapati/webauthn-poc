package com.bofa.webauthn.repository;

import com.bofa.webauthn.domain.Credential;
import com.bofa.webauthn.domain.User;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class UserStore {
  private final Map<String, User> users = new HashMap<>();

  public Optional<User> getUserByUserId(String userId){
    return Optional.ofNullable(users.get(userId));
  }

  public void saveUser(User user){
    users.put(user.getId(), user);
  }

  public void addCredential(String userId, Credential credential){
    User user = users.get(userId);
    if(user != null){
      user.getCredentials().add(credential);
    }
  }
}
