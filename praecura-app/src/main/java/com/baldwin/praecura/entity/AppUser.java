package com.baldwin.praecura.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El usuario es requerido")
    @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres")
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank(message = "La contraseña es requerida")
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "force_password_change", nullable = false)
    @Builder.Default
    private boolean forcePasswordChange = false;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    
    @Transient
    public boolean isSuperAdmin() {
        return username != null && username.equalsIgnoreCase("admin");
    }

@Transient
    public boolean isAdmin() {
        if (role == null || role.getName() == null) return false;
        String normalized = role.getName().trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        return "ADMIN".equals(normalized);
    }

    @Transient
    public boolean isLockedNow() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }
}
