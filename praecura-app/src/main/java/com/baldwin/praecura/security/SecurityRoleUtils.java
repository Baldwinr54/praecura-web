package com.baldwin.praecura.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public final class SecurityRoleUtils {

  private SecurityRoleUtils() {
  }

  public static boolean isAdminAuthority(String authority) {
    if (authority == null) return false;
    String normalized = authority.trim().toUpperCase();
    return "ADMIN".equals(normalized)
        || "ROLE_ADMIN".equals(normalized)
        || "ROLE_ROLE_ADMIN".equals(normalized);
  }

  public static boolean hasAdminAuthority(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) return false;
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(SecurityRoleUtils::isAdminAuthority);
  }
}
