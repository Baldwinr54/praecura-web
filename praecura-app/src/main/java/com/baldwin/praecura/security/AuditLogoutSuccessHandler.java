package com.baldwin.praecura.security;

import com.baldwin.praecura.service.AuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuditLogoutSuccessHandler implements LogoutSuccessHandler {

  private final AuditService auditService;
  private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

  @Override
  public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                              Authentication authentication) throws IOException, ServletException {
    if (authentication != null && authentication.getName() != null) {
      auditService.logAs(authentication.getName(), "LOGOUT", "Auth", null, "Cierre de sesión");
    } else {
      auditService.logAs("system", "LOGOUT", "Auth", null, "Cierre de sesión");
    }
    redirectStrategy.sendRedirect(request, response, "/login?logout=true");
  }
}
