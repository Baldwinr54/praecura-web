package com.baldwin.praecura.config;

import jakarta.servlet.http.HttpServletRequest;
import com.baldwin.praecura.service.OwnerAccessService;
import com.baldwin.praecura.service.BillingSupervisorAccessService;
import com.baldwin.praecura.service.DashboardService;
import com.baldwin.praecura.service.SystemBrandingService;
import com.baldwin.praecura.service.AccessControlService;
import com.baldwin.praecura.security.SecurityRoleUtils;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global model attributes for all MVC controllers.
 *
 * <p>We provide the current request URI to Thymeleaf templates so the sidebar
 * can mark the active navigation item without relying on servlet request
 * expression objects (which may be disabled depending on Spring Boot/Thymeleaf
 * versions).
 */
@ControllerAdvice
public class GlobalViewModel {

  private final DashboardService dashboardService;
  private final SystemBrandingService systemBrandingService;
  private final OwnerAccessService ownerAccessService;
  private final BillingSupervisorAccessService billingSupervisorAccessService;
  private final AccessControlService accessControlService;

  public GlobalViewModel(DashboardService dashboardService,
                         SystemBrandingService systemBrandingService,
                         OwnerAccessService ownerAccessService,
                         BillingSupervisorAccessService billingSupervisorAccessService,
                         AccessControlService accessControlService) {
    this.dashboardService = dashboardService;
    this.systemBrandingService = systemBrandingService;
    this.ownerAccessService = ownerAccessService;
    this.billingSupervisorAccessService = billingSupervisorAccessService;
    this.accessControlService = accessControlService;
  }

  @ModelAttribute("currentUri")
  public String currentUri(HttpServletRequest request) {
    return request != null ? request.getRequestURI() : "";
  }

  /**
   * Logged-in username for the sidebar (null when unauthenticated).
   */
  @ModelAttribute("currentUser")
  public String currentUser(Authentication authentication) {
    if (authentication == null
        || authentication instanceof AnonymousAuthenticationToken
        || !authentication.isAuthenticated()) {
      return null;
    }
    return authentication.getName();
  }

  @ModelAttribute("appDisplayName")
  public String appDisplayName() {
    try {
      String value = systemBrandingService.load().appDisplayName();
      return (value != null && !value.isBlank()) ? value : "PraeCura";
    } catch (Exception ex) {
      return "PraeCura";
    }
  }

  @ModelAttribute("appTagline")
  public String appTagline() {
    try {
      String value = systemBrandingService.load().appTagline();
      return (value != null && !value.isBlank()) ? value : "Gestión Clínica Integral";
    } catch (Exception ex) {
      return "Gestión Clínica Integral";
    }
  }

  /**
   * Friendly role label for the sidebar and admin-only UI.
   */
  @ModelAttribute("currentRole")
  public String currentRole(Authentication authentication) {
    if (authentication == null
        || authentication instanceof AnonymousAuthenticationToken
        || !authentication.isAuthenticated()) {
      return null;
    }

    Optional<String> role = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(a -> a != null && !a.isBlank())
        .map(String::trim)
        .filter(a -> a.startsWith("ROLE_"))
        .map(a -> a.substring("ROLE_".length()))
        .findFirst();

    return role.orElseGet(() -> authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(a -> a != null && !a.isBlank())
        .map(String::trim)
        .findFirst()
        .orElse(null));
  }

  /**
   * Whether the current user has admin role.
   */
  @ModelAttribute("isAdmin")
  public boolean isAdmin(Authentication authentication) {
    if (authentication == null
        || authentication instanceof AnonymousAuthenticationToken
        || !authentication.isAuthenticated()) {
      return false;
    }
    return SecurityRoleUtils.hasAdminAuthority(authentication);
  }

  @ModelAttribute("isOwner")
  public boolean isOwner(Authentication authentication) {
    if (authentication == null
        || authentication instanceof AnonymousAuthenticationToken
        || !authentication.isAuthenticated()) {
      return false;
    }
    return ownerAccessService.isOwner(authentication.getName());
  }

  @ModelAttribute("isBillingSupervisor")
  public boolean isBillingSupervisor(Authentication authentication) {
    if (authentication == null
        || authentication instanceof AnonymousAuthenticationToken
        || !authentication.isAuthenticated()) {
      return false;
    }
    boolean isAdmin = SecurityRoleUtils.hasAdminAuthority(authentication);
    if (isAdmin) return true;
    return billingSupervisorAccessService.isSupervisor(authentication.getName());
  }

  @ModelAttribute("navBadges")
  public DashboardService.NavBadges navBadges(Authentication authentication) {
    if (authentication == null
        || authentication instanceof AnonymousAuthenticationToken
        || !authentication.isAuthenticated()) {
      return new DashboardService.NavBadges(0, 0, 0);
    }
    try {
      return dashboardService.loadNavBadges(LocalDate.now());
    } catch (Exception ex) {
      return new DashboardService.NavBadges(0, 0, 0);
    }
  }

  @ModelAttribute("permissionCodes")
  public java.util.Set<String> permissionCodes(Authentication authentication) {
    try {
      return accessControlService.permissionCodesByAuthentication(authentication);
    } catch (Exception ex) {
      return new java.util.HashSet<>();
    }
  }
}
