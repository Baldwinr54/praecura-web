package com.baldwin.praecura.security;

import com.baldwin.praecura.entity.AppUser;
import com.baldwin.praecura.repository.UserRepository;
import com.baldwin.praecura.service.AuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LoginPolicyFailureHandler implements AuthenticationFailureHandler {

    // Policy: 5 failed attempts => 15 minutes lockout (ADMIN exempt)
    public static final int MAX_ATTEMPTS = 5;
    public static final int LOCK_MINUTES = 15;

    private final UserRepository userRepository;
    private final AuditService auditService;

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String usernameParam = request.getParameter("username");
        String unameForAudit = (usernameParam == null || usernameParam.isBlank()) ? "unknown" : usernameParam;

        if (exception instanceof LockedException) {
            auditService.logAs(unameForAudit, "LOGIN_FAIL", "Auth", null, "Usuario bloqueado");
            redirectStrategy.sendRedirect(request, response, "/login?locked");
            return;
        }

        if (exception instanceof DisabledException) {
            auditService.logAs(unameForAudit, "LOGIN_FAIL", "Auth", null, "Usuario deshabilitado");
            redirectStrategy.sendRedirect(request, response, "/login?disabled");
            return;
        }

        String target = "/login?error";

        String username = usernameParam;
        if (username != null && !username.isBlank()) {
            AppUser user = userRepository.findByUsername(username).orElse(null);

            // Auditoría del intento fallido
            if (user == null) {
                auditService.logAs(username, "LOGIN_FAIL", "Auth", null, "Credenciales inválidas");
            } else if (user.isLockedNow()) {
                auditService.logAs(username, "LOGIN_FAIL", "Auth", user.getId(), "Usuario bloqueado");
            } else if (!user.isEnabled()) {
                auditService.logAs(username, "LOGIN_FAIL", "Auth", user.getId(), "Usuario deshabilitado");
            } else {
                auditService.logAs(username, "LOGIN_FAIL", "Auth", user.getId(), "Autenticación fallida");
            }

            if (user != null && !user.isSuperAdmin()) {
                if (user.isLockedNow()) {
                    target = "/login?locked";
                } else {
                    int next = user.getFailedLoginAttempts() + 1;
                    user.setFailedLoginAttempts(next);

                    if (next >= MAX_ATTEMPTS) {
                        user.setFailedLoginAttempts(MAX_ATTEMPTS);
                        user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                        target = "/login?locked";

                        auditService.logAs(username, "LOCK", "User", user.getId(),
                                "Bloqueó temporalmente el usuario '" + username + "' por " + LOCK_MINUTES + " minutos (" + MAX_ATTEMPTS + " intentos fallidos).");
                    }

                    userRepository.save(user);
                }
            }
        }

        redirectStrategy.sendRedirect(request, response, target);
    }
}
