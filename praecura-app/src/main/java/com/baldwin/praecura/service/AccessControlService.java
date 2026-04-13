package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.AppUser;
import com.baldwin.praecura.entity.PermissionModule;
import com.baldwin.praecura.entity.SystemPermission;
import com.baldwin.praecura.entity.UserPermission;
import com.baldwin.praecura.repository.SystemPermissionRepository;
import com.baldwin.praecura.repository.UserPermissionRepository;
import com.baldwin.praecura.repository.UserRepository;
import com.baldwin.praecura.security.SecurityRoleUtils;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessControlService {

  private final UserRepository userRepository;
  private final SystemPermissionRepository systemPermissionRepository;
  private final UserPermissionRepository userPermissionRepository;
  private final AuditService auditService;

  public AccessControlService(UserRepository userRepository,
                              SystemPermissionRepository systemPermissionRepository,
                              UserPermissionRepository userPermissionRepository,
                              AuditService auditService) {
    this.userRepository = userRepository;
    this.systemPermissionRepository = systemPermissionRepository;
    this.userPermissionRepository = userPermissionRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public boolean hasPermission(Authentication authentication, String code) {
    if (authentication == null || !authentication.isAuthenticated() || code == null || code.isBlank()) {
      return false;
    }
    if (SecurityRoleUtils.hasAdminAuthority(authentication)) {
      return true;
    }
    String username = authentication.getName();
    if (username == null || username.isBlank()) {
      return false;
    }
    Set<String> codes = permissionCodesByUsername(username);
    return codes.contains(code.trim().toUpperCase());
  }

  @Transactional(readOnly = true)
  public Set<String> permissionCodesByAuthentication(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return new HashSet<>();
    }
    if (SecurityRoleUtils.hasAdminAuthority(authentication)) {
      return listPermissions().stream()
          .map(SystemPermission::getCode)
          .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    }
    return permissionCodesByUsername(authentication.getName());
  }

  @Transactional(readOnly = true)
  @Cacheable(
      value = "permissionCodesByUsername",
      key = "#username.trim().toLowerCase()",
      condition = "#username != null && !#username.isBlank()",
      sync = true
  )
  public Set<String> permissionCodesByUsername(String username) {
    if (username == null || username.isBlank()) {
      return new HashSet<>();
    }
    var userOpt = userRepository.findByUsername(username);
    if (userOpt.isEmpty()) {
      return new HashSet<>();
    }
    AppUser user = userOpt.get();
    List<UserPermission> permissions = userPermissionRepository.findByUserIdOrderByPermissionModuleAscPermissionNameAsc(user.getId());
    Set<String> codes = new HashSet<>();
    for (UserPermission up : permissions) {
      if (up.isGranted() && up.getPermission() != null && up.getPermission().isActive()) {
        codes.add(up.getPermission().getCode());
      }
    }
    return codes;
  }

  @Transactional(readOnly = true)
  @Cacheable(
      value = "activePermissions",
      sync = true
  )
  public List<SystemPermission> listPermissions() {
    return systemPermissionRepository.findByActiveTrueOrderByModuleAscNameAsc();
  }

  @Transactional(readOnly = true)
  public List<UserPermission> listUserPermissions(Long userId) {
    if (userId == null) return List.of();
    return userPermissionRepository.findByUserIdOrderByPermissionModuleAscPermissionNameAsc(userId);
  }

  @Transactional(readOnly = true)
  @Cacheable(value = "permissionMatrix", sync = true)
  public Map<PermissionModule, List<SystemPermission>> permissionMatrix() {
    Map<PermissionModule, List<SystemPermission>> grouped = new EnumMap<>(PermissionModule.class);
    for (PermissionModule module : PermissionModule.values()) {
      grouped.put(module, systemPermissionRepository.findByModuleOrderByNameAsc(module));
    }
    return grouped;
  }

  @Transactional
  @CacheEvict(
      value = {"permissionCodesByUsername", "permissionMatrix", "activePermissions"},
      allEntries = true
  )
  public void setPermission(Long userId, String permissionCode, boolean granted) {
    if (userId == null) {
      throw new IllegalArgumentException("Usuario inválido para permisos.");
    }
    if (permissionCode == null || permissionCode.isBlank()) {
      throw new IllegalArgumentException("Código de permiso inválido.");
    }

    AppUser user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("Usuario no existe."));

    String code = permissionCode.trim().toUpperCase();
    SystemPermission permission = systemPermissionRepository.findByCode(code)
        .orElseThrow(() -> new IllegalArgumentException("Permiso no existe."));

    UserPermission up = userPermissionRepository.findByUserIdAndPermissionCode(userId, code)
        .orElseGet(() -> {
          UserPermission created = new UserPermission();
          created.setUser(user);
          created.setPermission(permission);
          created.setCreatedAt(LocalDateTime.now());
          return created;
        });

    up.setGranted(granted);
    up.setUpdatedAt(LocalDateTime.now());
    userPermissionRepository.save(up);

    auditService.log(
        granted ? "PERMISSION_GRANTED" : "PERMISSION_REVOKED",
        "UserPermission",
        up.getId(),
        "userId=" + userId + ", permission=" + code + ", granted=" + granted
    );
  }
}
