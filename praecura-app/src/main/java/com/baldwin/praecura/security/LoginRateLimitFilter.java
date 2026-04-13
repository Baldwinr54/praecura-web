package com.baldwin.praecura.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter for login attempts by IP.
 *
 * Goal: reduce brute-force pressure without introducing new dependencies.
 *
 * Notes:
 * - Applies only to POST /login.
 * - Uses a sliding window: maxAttempts within window.
 * - Reset happens automatically as timestamps fall outside the window.
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final int maxAttempts;
    private final Duration window;
    private final boolean trustForwardedFor;

    private final Map<String, Deque<Instant>> attemptsByIp = new ConcurrentHashMap<>();

    public LoginRateLimitFilter(int maxAttempts, Duration window, boolean trustForwardedFor) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.window = window == null ? Duration.ofMinutes(1) : window;
        this.trustForwardedFor = trustForwardedFor;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().equals("/login"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String ip = extractClientIp(request);
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);

        Deque<Instant> deque = attemptsByIp.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                deque.removeFirst();
            }
            if (deque.size() >= maxAttempts) {
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"message\":\"Demasiados intentos. Intenta de nuevo en unos minutos.\"}");
                return;
            }
            deque.addLast(now);
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        if (trustForwardedFor) {
            // If behind a trusted reverse proxy, X-Forwarded-For may be present. We take the first hop.
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                int comma = xff.indexOf(',');
                return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
            }
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
