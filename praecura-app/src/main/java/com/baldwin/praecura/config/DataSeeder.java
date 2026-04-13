package com.baldwin.praecura.config;

import com.baldwin.praecura.entity.AppUser;
import com.baldwin.praecura.entity.Role;
import com.baldwin.praecura.repository.RoleRepository;
import com.baldwin.praecura.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the minimum reference data required for a clean first run.
 *
 * Intentionally idempotent: it can run multiple times without duplicating records.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
  private static final String DEFAULT_ADMIN_PASSWORD = "CAMBIAR_PASSWORD_ADMIN";

  private final RoleRepository roleRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${praecura.bootstrap.admin.enabled:true}")
  private boolean bootstrapAdminEnabled;

  @Value("${praecura.bootstrap.admin.username:admin}")
  private String bootstrapAdminUsername;

  @Value("${praecura.bootstrap.admin.password:}")
  private String bootstrapAdminPassword;

  @Override
  @Transactional
  public void run(String... args) {
    // Keep role names WITHOUT the ROLE_ prefix in DB.
    // Spring Security will add ROLE_ internally via DbUserDetailsService normalization.
    Role adminRole = roleRepository.findByName("ADMIN")
        .orElseGet(() -> roleRepository.save(new Role(null, "ADMIN")));

    Role userRole = roleRepository.findByName("USER")
        .orElseGet(() -> roleRepository.save(new Role(null, "USER")));

    if (!bootstrapAdminEnabled) {
      return;
    }

    // Credencial mínima requerida para iniciar el sistema (solo si el usuario no existe).
    String adminUsername = (bootstrapAdminUsername == null || bootstrapAdminUsername.isBlank())
        ? "admin"
        : bootstrapAdminUsername.trim();

    AppUser admin = userRepository.findByUsername(adminUsername).orElse(null);
    if (admin == null) {
      String rawPassword = (bootstrapAdminPassword == null || bootstrapAdminPassword.isBlank())
          ? DEFAULT_ADMIN_PASSWORD
          : bootstrapAdminPassword;

      admin = AppUser.builder().username(adminUsername).build();
      admin.setRole(adminRole);
      admin.setEnabled(true);
      admin.setPasswordHash(passwordEncoder.encode(rawPassword));
      userRepository.save(admin);

      if (DEFAULT_ADMIN_PASSWORD.equals(rawPassword)) {
        log.warn("Bootstrap admin creado con contraseña por defecto. Cambiar inmediatamente.");
      } else {
        log.info("Bootstrap admin creado con contraseña personalizada.");
      }
    }

    // Nota: No sembramos doctores/servicios/pacientes de ejemplo aquí.
    // El objetivo es permitir que el usuario arranque con el sistema limpio,
    // dejando únicamente roles, especialidades (por migración) y el usuario admin.
  }
}
