package com.bofa.webauthn.repository;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ChallengeStore {
  private Map<String, String> challenges = new HashMap<>();

  public void saveChallenge(String userId, String challenge){
    challenges.put(userId, challenge);
  }

  public String getChallengeForUser(String userId){
    return challenges.get(userId);
  }
}
