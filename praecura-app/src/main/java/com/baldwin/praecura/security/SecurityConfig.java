package com.baldwin.praecura.security;

import java.time.Duration;
import java.util.function.Supplier;

import com.baldwin.praecura.service.AccessControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.PermissionsPolicyHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            com.baldwin.praecura.config.RequestContextFilter requestContextFilter,
            LoginPolicySuccessHandler successHandler,
            LoginPolicyFailureHandler failureHandler,
            AuditLogoutSuccessHandler logoutSuccessHandler,
            DbUserDetailsService userDetailsService,
            AccessControlService accessControlService,
            @Value("${praecura.security.remember-me-key:}") String rememberMeKey,
            @Value("${praecura.security.trust-proxy:false}") boolean trustProxy
    ) throws Exception {

	        http
	                // Contexto de request para auditoría/correlación
	                .addFilterBefore(requestContextFilter, UsernamePasswordAuthenticationFilter.class)
	                // Rate-limit defensivo (por IP) en el POST /login
	                // Valor por defecto: 20 solicitudes por minuto por IP para mitigar fuerza bruta.
	                .addFilterBefore(new LoginRateLimitFilter(20, Duration.ofMinutes(1), trustProxy), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Recursos estáticos y endpoints públicos
                        .requestMatchers(
                                "/login",
                                "/system/health",
                                "/error",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**",
                                "/favicon.ico"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_ROLE_ADMIN")
                        .requestMatchers("/owner/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_ROLE_ADMIN")
                        .requestMatchers("/reports/audit/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_ROLE_ADMIN")
                        .requestMatchers("/reports/compliance/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_ROLE_ADMIN")
                        .requestMatchers("/reports/messages/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_ROLE_ADMIN")
                        .requestMatchers("/reports/executive/**").access((authentication, context) -> new AuthorizationDecision(
                            hasAnyPermission(
                                authentication,
                                accessControlService,
                                "REPORT_EXECUTIVE_VIEW",
                                "ALERTS_MANAGE",
                                "DAILY_CLOSING_FINALIZE"
                            )
                        ))
                        .requestMatchers("/reports/alerts/**").access((authentication, context) -> new AuthorizationDecision(
                            hasAnyPermission(authentication, accessControlService, "ALERTS_MANAGE", "REPORT_EXECUTIVE_VIEW")
                        ))
                        .requestMatchers("/reports/closings/**").access((authentication, context) -> new AuthorizationDecision(
                            hasAnyPermission(authentication, accessControlService, "DAILY_CLOSING_FINALIZE", "REPORT_EXECUTIVE_VIEW")
                        ))
                        .requestMatchers("/billing/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_ROLE_ADMIN")
                        .requestMatchers("/insurance/**").access((authentication, context) -> new AuthorizationDecision(
                            hasAnyPermission(authentication, accessControlService, "INSURANCE_MANAGE")
                        ))
                        .requestMatchers("/pharmacy/**").access((authentication, context) -> new AuthorizationDecision(
                            hasAnyPermission(authentication, accessControlService, "PHARMACY_MANAGE", "INVENTORY_MANAGE")
                        ))
                        .requestMatchers("/inpatient/**").access((authentication, context) -> new AuthorizationDecision(
                            hasAnyPermission(authentication, accessControlService, "INPATIENT_MANAGE")
                        ))
                        .requestMatchers("/admin/access/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_ROLE_ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .permitAll()
                )
                .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"));

        http.sessionManagement(sm -> sm
            .sessionFixation(session -> session.migrateSession())
            .maximumSessions(1)
            .maxSessionsPreventsLogin(false)
        );

        if (rememberMeKey != null && !rememberMeKey.isBlank()) {
            http.rememberMe(rm -> rm
                .key(rememberMeKey)
                .rememberMeParameter("remember-me")
                .tokenValiditySeconds((int) Duration.ofDays(7).getSeconds())
                .userDetailsService(userDetailsService)
            );
        } else {
            log.warn("Remember-me desactivado: praecura.security.remember-me-key no configurado.");
        }

	        // Hardening: cabeceras de seguridad (sin romper el UI ni recursos externos)
	        // Nota: evitamos el chaining para mantener compatibilidad con Spring Security 7.x.
	        http.headers(headers -> {
	            headers.contentTypeOptions(c -> {});
	            headers.frameOptions(f -> f.sameOrigin());
	            headers.referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN));
	            // Permissions-Policy (en Spring Security 7.x el DSL está deprec...)
	            headers.addHeaderWriter(new PermissionsPolicyHeaderWriter(
	                    "camera=(), microphone=(), geolocation=(), payment=(), usb=()"
	            ));
	            // CSP moderada (permitimos Bootstrap Icons desde CDN si aplica)
            headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "base-uri 'self'; " +
                    "object-src 'none'; " +
                    "frame-ancestors 'self'; " +
                    "form-action 'self'; " +
                    "img-src 'self' data: https://api.qrserver.com; " +
                    "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                    "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                    "font-src 'self' https://cdn.jsdelivr.net; " +
                    "connect-src 'self'"
            ));
        });

        return http.build();
    }

    private boolean hasAnyPermission(Supplier<? extends Authentication> authSupplier,
                                     AccessControlService accessControlService,
                                     String... permissionCodes) {
        Authentication authentication;
        try {
            authentication = authSupplier != null ? authSupplier.get() : null;
        } catch (Exception ex) {
            return false;
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (SecurityRoleUtils.hasAdminAuthority(authentication)) {
            return true;
        }

        if (permissionCodes == null || permissionCodes.length == 0) {
            return false;
        }

        for (String code : permissionCodes) {
            if (code != null && !code.isBlank() && accessControlService.hasPermission(authentication, code)) {
                return true;
            }
        }
        return false;
    }
}
