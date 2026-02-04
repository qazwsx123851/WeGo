package com.wego.controller;

import com.wego.entity.User;
import com.wego.repository.UserRepository;
import com.wego.security.UserPrincipal;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Test authentication controller for E2E testing.
 *
 * ONLY available in "test" or "e2e" profiles.
 * DO NOT enable in production.
 *
 * This endpoint allows E2E tests to authenticate without going through
 * the real Google OAuth flow.
 *
 * @contract
 *   - pre: Application is running with "test" or "e2e" profile
 *   - post: Creates or uses test user and establishes authenticated session
 *   - calledBy: E2E test fixtures
 */
@RestController
@RequestMapping("/api/test")
@Profile({"test", "e2e"})
@RequiredArgsConstructor
@Slf4j
public class TestAuthController {

    private final UserRepository userRepository;

    /**
     * Authenticates a test user for E2E testing.
     *
     * Creates a test user if not exists and establishes an authenticated session.
     *
     * @contract
     *   - pre: Request contains email (optional, defaults to test user)
     *   - post: Session is authenticated as the specified user
     *   - post: Returns user info including ID
     *
     * @param request Optional test auth request with email
     * @param session HTTP session to store security context
     * @return User info for the authenticated user
     */
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> testLogin(
            @RequestBody(required = false) TestAuthRequest request,
            HttpSession session) {

        String email = (request != null && request.email != null)
                ? request.email
                : "e2e-test@wego.test";
        String name = (request != null && request.name != null)
                ? request.name
                : "E2E Test User";

        log.info("[TEST] Authenticating test user: {}", email);

        // Find or create test user
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .provider("test")
                            .providerId("test-" + UUID.randomUUID())
                            .email(email)
                            .nickname(name)
                            .avatarUrl(null)
                            .build();
                    return userRepository.save(newUser);
                });

        // Create OAuth2 attributes
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", user.getProviderId());
        attributes.put("email", user.getEmail());
        attributes.put("name", user.getNickname());
        attributes.put("picture", user.getAvatarUrl());

        // Create UserPrincipal
        UserPrincipal userPrincipal = new UserPrincipal(user, attributes);

        // Create OAuth2AuthenticationToken
        OAuth2AuthenticationToken authToken = new OAuth2AuthenticationToken(
                userPrincipal,
                userPrincipal.getAuthorities(),
                "test"
        );

        // Set authentication in security context
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // Store security context in session
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        log.info("[TEST] Test user authenticated: id={}, email={}", user.getId(), user.getEmail());

        // Return user info
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("userId", user.getId().toString());
        response.put("email", user.getEmail());
        response.put("name", user.getNickname());
        response.put("sessionId", session.getId());

        return ResponseEntity.ok(response);
    }

    /**
     * Logs out the current test session.
     *
     * @param session HTTP session to invalidate
     * @return Success response
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<Map<String, Object>> testLogout(HttpSession session) {
        log.info("[TEST] Logging out test session: {}", session.getId());

        SecurityContextHolder.clearContext();
        session.invalidate();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Logged out successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Checks if the test auth endpoint is available.
     *
     * @return Health check response
     */
    @GetMapping("/auth/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("endpoint", "test-auth");
        response.put("message", "Test authentication endpoint is available");

        return ResponseEntity.ok(response);
    }

    /**
     * Request body for test authentication.
     */
    public record TestAuthRequest(String email, String name) {}
}
