package com.wego.controller.api;

import com.wego.entity.User;
import com.wego.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for AuthApiController.
 *
 * Test cases:
 * - U-010: Get current user info
 * - U-011: Logout invalidates session
 * - U-012: Unauthenticated access returns 401
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private User testUser;
    private UserPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .nickname("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .provider("google")
                .providerId("google-123456")
                .build();

        testPrincipal = new UserPrincipal(testUser, Collections.emptyMap());
    }

    @Test
    @DisplayName("U-010: Should return current user info when authenticated")
    void getCurrentUser_whenAuthenticated_shouldReturnUserInfo() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .with(SecurityMockMvcRequestPostProcessors.oauth2Login()
                                .oauth2User(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("Test User"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/avatar.jpg"))
                .andExpect(jsonPath("$.data.provider").value("google"));
    }

    @Test
    @DisplayName("U-012: Should redirect to login when not authenticated")
    void getCurrentUser_whenNotAuthenticated_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("U-011: Should logout successfully")
    void logout_whenAuthenticated_shouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(SecurityMockMvcRequestPostProcessors.oauth2Login()
                                .oauth2User(testPrincipal))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    @DisplayName("Should redirect to login for logout when not authenticated")
    void logout_whenNotAuthenticated_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Should return user with all fields populated")
    void getCurrentUser_shouldReturnAllUserFields() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .with(SecurityMockMvcRequestPostProcessors.oauth2Login()
                                .oauth2User(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.email").exists())
                .andExpect(jsonPath("$.data.nickname").exists())
                .andExpect(jsonPath("$.data.provider").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
