package com.baldwin.praecura.security;

import com.baldwin.praecura.entity.AppUser;
import com.baldwin.praecura.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DbUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser u = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        if (!u.isEnabled()) {
            throw new DisabledException("Usuario deshabilitado");
        }


        // Auto-unlock after lock expiry for non-ADMIN users.
        if (!u.isSuperAdmin() && u.getLockedUntil() != null && !u.getLockedUntil().isAfter(LocalDateTime.now())) {
            u.setLockedUntil(null);
            u.setFailedLoginAttempts(0);
            userRepository.save(u);
        }

        // Login lockout policy does not apply to ADMIN users.
        if (!u.isSuperAdmin() && u.isLockedNow()) {
            throw new LockedException("Usuario bloqueado temporalmente");
        }

        String roleName = normalizeRoleName(u.getRole() != null ? u.getRole().getName() : null);
        return User.withUsername(u.getUsername())
                .password(u.getPasswordHash())
                .roles(roleName)
                .build();
    }

    private String normalizeRoleName(String roleName) {
        if (roleName == null) return "USER";
        String normalized = roleName.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        return normalized.isBlank() ? "USER" : normalized;
    }
}
