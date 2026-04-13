package com.baldwin.praecura.config;

import jakarta.servlet.http.HttpServletRequest;
import com.baldwin.praecura.security.SecurityRoleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @Value("${praecura.ui.show-error-details:false}")
  private boolean showErrorDetails;

  /**
   * Missing static assets (e.g., /favicon.ico, /js/*.js) should be a normal 404,
   * not a full 500 error page.
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<Void> noResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ModelAndView accessDenied(HttpServletRequest request, AccessDeniedException ex) {
    return renderError(HttpStatus.FORBIDDEN, request, ex);
  }

  @ExceptionHandler({
      IllegalArgumentException.class,
      MissingServletRequestParameterException.class,
      MethodArgumentTypeMismatchException.class
  })
  public ModelAndView badRequest(HttpServletRequest request, Exception ex) {
    return renderError(HttpStatus.BAD_REQUEST, request, ex);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ModelAndView methodNotAllowed(HttpServletRequest request, HttpRequestMethodNotSupportedException ex) {
    return renderError(HttpStatus.METHOD_NOT_ALLOWED, request, ex);
  }

  @ExceptionHandler(Exception.class)
  public ModelAndView unhandled(HttpServletRequest request, Exception ex) {
    return renderError(HttpStatus.INTERNAL_SERVER_ERROR, request, ex);
  }

  private ModelAndView renderError(HttpStatus status, HttpServletRequest request, Exception ex) {
    String errorId = java.util.UUID.randomUUID().toString();
    String rootMessage = rootCauseMessage(ex);
    log.error(
        "ErrorId={} status={} path={} exception={} message={} rootCause={}",
        errorId,
        status.value(),
        request.getRequestURI(),
        ex.getClass().getName(),
        ex.getMessage(),
        rootMessage,
        ex
    );

    ModelAndView mv = new ModelAndView("error");
    mv.addObject("status", status.value());
    mv.addObject("error", status.getReasonPhrase());
    mv.addObject("path", request.getRequestURI());
    String realMessage = ex.getMessage();
    if (realMessage == null || realMessage.isBlank()) {
      realMessage = (rootMessage != null && !rootMessage.isBlank()) ? rootMessage : "Ha ocurrido un problema";
    }
    if (!showErrorDetails && status.is5xxServerError()) {
      realMessage = "Ha ocurrido un problema";
    }
    mv.addObject("message", realMessage);
    mv.addObject("errorId", errorId);

    enrichWithSecurityContext(mv);

    return mv;
  }

  private String rootCauseMessage(Throwable ex) {
    Throwable current = ex;
    while (current != null && current.getCause() != null && current.getCause() != current) {
      current = current.getCause();
    }
    if (current == null || current.getMessage() == null || current.getMessage().isBlank()) {
      return null;
    }
    return current.getMessage();
  }

  /**
   * IMPORTANT: Los @ModelAttribute de otros @ControllerAdvice NO se ejecutan
   * automáticamente cuando entramos en un @ExceptionHandler.
   *
   * Para que el layout no pierda datos (menú ADMIN, usuario/rol, etc.) en páginas
   * de error, agregamos explícitamente los atributos globales aquí.
   */
  private void enrichWithSecurityContext(ModelAndView mv) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    String currentUser = "—";
    String currentRole = "—";
    boolean isAdmin = false;

    if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null
        && !"anonymousUser".equals(auth.getPrincipal())) {

      currentUser = auth.getName();

      if (auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
        // Tomamos el primer rol (ordenado) solo para mostrarlo en UI.
        currentRole = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .findFirst()
            .orElse("—");

        if (currentRole != null && currentRole.toUpperCase().startsWith("ROLE_")) {
          currentRole = currentRole.substring("ROLE_".length());
        }

        isAdmin = auth.getAuthorities().stream().anyMatch(a -> {
          String r = a.getAuthority();
          return SecurityRoleUtils.isAdminAuthority(r);
        });
      }
    }

    mv.addObject("currentUser", currentUser);
    mv.addObject("currentRole", currentRole);
    mv.addObject("isAdmin", isAdmin);
  }
}
