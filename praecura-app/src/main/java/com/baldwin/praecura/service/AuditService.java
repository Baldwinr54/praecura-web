package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.AuditLog;
import com.baldwin.praecura.repository.AuditLogRepository;
import com.baldwin.praecura.config.RequestContext;
import com.baldwin.praecura.config.RequestContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

  private final AuditLogRepository auditLogRepository;

  public AuditService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  /**
   * Uses the authenticated principal username when available; otherwise uses "system".
   */
  public void log(String action, String entity, Long entityId, String detail) {
    String username = null;
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
      username = auth.getName();
    }
    logAs(username, action, entity, entityId, detail);
  }

  /**
   * Forces a username in the audit log. Useful for unauthenticated contexts (e.g., failed logins).
   */
  public void logAs(String username, String action, String entity, Long entityId, String detail) {
    AuditLog a = new AuditLog();
    a.setAction(action);
    a.setEntity(entity);
    a.setEntityId(entityId);
    a.setDetail(detail);

    // Contexto de request (si existe)
    RequestContext ctx = RequestContextHolder.get();
    if (ctx != null) {
      a.setRequestId(ctx.getRequestId());
      a.setIpAddress(ctx.getIpAddress());
      a.setUserAgent(ctx.getUserAgent());
    }

    if (username != null && !username.isBlank()) {
      a.setUsername(username);
    } else {
      a.setUsername("system");
    }

    auditLogRepository.save(a);
  }
}
