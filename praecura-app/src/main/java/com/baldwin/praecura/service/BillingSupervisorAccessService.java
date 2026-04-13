package com.baldwin.praecura.service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingSupervisorAccessService {

  private final Set<String> supervisorUsernames;

  public BillingSupervisorAccessService(
      @Value("${praecura.billing.supervisor-usernames:admin}") String configuredSupervisorUsernames) {
    this.supervisorUsernames = Arrays.stream(configuredSupervisorUsernames.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .map(String::toLowerCase)
        .collect(Collectors.toSet());
  }

  public boolean isSupervisor(String username) {
    if (username == null || username.isBlank()) return false;
    return supervisorUsernames.contains(username.trim().toLowerCase());
  }
}
