package com.wego.config;

import com.wego.security.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Spring Security Configuration.
 *
 * Configures OAuth2 login with Google, session management, CSRF protection,
 * and security headers.
 *
 * @contract
 *   - pre: Spring Security dependencies are available
 *   - post: SecurityFilterChain is configured with OAuth2, CSRF, headers, and session settings
 *   - calledBy: Spring Security auto-configuration
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    /**
     * Configures the security filter chain.
     *
     * @contract
     *   - pre: HttpSecurity is injected by Spring
     *   - post: OAuth2 login enabled, public endpoints accessible
     *   - post: CSRF protection enabled with cookie-based token repository
     *   - post: Security headers configured (CSP, X-Frame-Options, etc.)
     *   - calls: HttpSecurity methods, CustomOAuth2UserService
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF Protection - use cookie-based token for Thymeleaf integration
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // Disable CSRF for public API endpoints
                .ignoringRequestMatchers("/api/health", "/api/test/auth/**")
            )
            // Security Headers
            .headers(headers -> {
                // Prevent clickjacking
                headers.frameOptions(frame -> frame.sameOrigin());
                // Content Security Policy
                headers.contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' https://unpkg.com https://cdn.jsdelivr.net; " +
                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.jsdelivr.net; " +
                        "font-src 'self' https://fonts.gstatic.com data:; " +
                        "img-src 'self' https: data: blob:; " +
                        "connect-src 'self' https://unpkg.com https://lottie.host https://cdn.jsdelivr.net; " +
                        "frame-src 'self' https://www.google.com https://maps.google.com https://*.supabase.co; " +
                        "frame-ancestors 'self'"
                    )
                );
                // Referrer Policy
                headers.referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                );
                // Permissions Policy (restrict browser features)
                headers.permissionsPolicy(permissions -> permissions
                    .policy("geolocation=(self), camera=(), microphone=()")
                );
                // HSTS - force HTTPS
                headers.httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                );
            })
            // Authorization rules
            .authorizeHttpRequests(authorize -> authorize
                // Public endpoints
                .requestMatchers(
                    "/",
                    "/login",
                    "/error",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico",
                    "/favicon.svg",
                    "/.well-known/**",
                    "/api/health",
                    "/api/weather/**",
                    "/api/test/auth/**"  // Test auth endpoint (only available in test/e2e profile)
                ).permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            // OAuth2 Login
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard")
                .failureUrl("/login?error=true")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
            )
            // Logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            // Session management
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.changeSessionId())
                .maximumSessions(1)
                .expiredUrl("/login?expired=true")
            );

        return http.build();
    }
}
