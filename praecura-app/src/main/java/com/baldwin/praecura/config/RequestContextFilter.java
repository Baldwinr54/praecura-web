package com.baldwin.praecura.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Captures basic request metadata used for auditing/correlation.
 * Adds/propagates X-Request-Id.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component("praecuraRequestContextFilter")
public class RequestContextFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestContextFilter.class);
  public static final String HEADER_REQUEST_ID = "X-Request-Id";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    long startMs = System.currentTimeMillis();

    String requestId = request.getHeader(HEADER_REQUEST_ID);
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
    }

    String ip = extractClientIp(request);
    String ua = request.getHeader("User-Agent");

    RequestContextHolder.set(new RequestContext(requestId, ip, ua));
    // Correlación en logs (Logback/SLF4J) vía MDC
    MDC.put("requestId", requestId);
    response.setHeader(HEADER_REQUEST_ID, requestId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      long elapsedMs = System.currentTimeMillis() - startMs;
      String method = request.getMethod();
      String path = request.getRequestURI();
      int status = response.getStatus();
      log.info("REQ {} {} -> {} ({} ms)", method, path, status, elapsedMs);
      MDC.remove("requestId");
      RequestContextHolder.clear();
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return uri.startsWith("/css/")
        || uri.startsWith("/js/")
        || uri.startsWith("/images/")
        || uri.startsWith("/webjars/")
        || uri.equals("/favicon.ico");
  }

  private String extractClientIp(HttpServletRequest request) {
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      // Take the first entry in the list
      String first = xff.split(",")[0].trim();
      if (!first.isBlank()) return first;
    }
    return request.getRemoteAddr();
  }
}
