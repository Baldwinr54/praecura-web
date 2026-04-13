package com.baldwin.praecura.security;

import com.baldwin.praecura.entity.AppUser;
import com.baldwin.praecura.repository.UserRepository;
import com.baldwin.praecura.service.AuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginPolicySuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final SavedRequestAwareAuthenticationSuccessHandler delegate;

    public LoginPolicySuccessHandler(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        SavedRequestAwareAuthenticationSuccessHandler h = new SavedRequestAwareAuthenticationSuccessHandler();
        // Default landing page after login. If the user was trying to access a protected URL,
        // SavedRequestAwareAuthenticationSuccessHandler will send them back there.
        h.setDefaultTargetUrl("/");
        h.setAlwaysUseDefaultTargetUrl(false);
        this.delegate = h;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        String username = authentication.getName();
        if (username != null && !username.isBlank()) {
            auditService.logAs(username, "LOGIN_SUCCESS", "Auth", null, "Inicio de sesión exitoso");
            AppUser user = userRepository.findByUsername(username).orElse(null);
            if (user != null && !user.isSuperAdmin()) {
                if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
                    user.setFailedLoginAttempts(0);
                    user.setLockedUntil(null);
                    userRepository.save(user);
                }
            }
            if (user != null && user.isForcePasswordChange()) {
                response.sendRedirect("/account/password?force=true");
                return;
            }
        }

        delegate.onAuthenticationSuccess(request, response, authentication);
    }
}
