package com.baldwin.praecura.service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OwnerAccessService {

  private final Set<String> ownerUsernames;

  public OwnerAccessService(@Value("${praecura.owner.usernames:admin}") String configuredOwnerUsernames) {
    this.ownerUsernames = Arrays.stream(configuredOwnerUsernames.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .map(String::toLowerCase)
        .collect(Collectors.toSet());
  }

  public boolean isOwner(String username) {
    if (username == null || username.isBlank()) return false;
    return ownerUsernames.contains(username.trim().toLowerCase());
  }
}
