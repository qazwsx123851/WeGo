package com.wego.controller.api;

import com.wego.entity.User;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.UUID;

/**
 * Test security configuration for @WebMvcTest.
 *
 * Provides a simplified security setup for controller tests.
 *
 * @contract
 *   - post: Security is configured for testing
 *   - calledBy: @WebMvcTest annotated test classes
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    /**
     * Test security filter chain that requires authentication for all API endpoints.
     *
     * @contract
     *   - post: All /api/** endpoints require authentication
     *   - post: CSRF is enabled
     */
    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/weather/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/health")
                .ignoringRequestMatchers("/api/weather/**")
            );

        return http.build();
    }

    /**
     * Test user details service providing a mock user.
     */
    @Bean
    @Primary
    public UserDetailsService testUserDetailsService() {
        return new InMemoryUserDetailsManager(
            org.springframework.security.core.userdetails.User.builder()
                .username("testuser@example.com")
                .password("{noop}password")
                .roles("USER")
                .build()
        );
    }
}
