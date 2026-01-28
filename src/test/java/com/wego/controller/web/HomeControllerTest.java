package com.wego.controller.web;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Integration tests for HomeController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HomeControllerTest {

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
    @DisplayName("Should return home page for unauthenticated users")
    void home_shouldReturnIndexView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @DisplayName("Should return dashboard page for authenticated users")
    void dashboard_whenAuthenticated_shouldReturnDashboardView() throws Exception {
        mockMvc.perform(get("/dashboard")
                        .with(SecurityMockMvcRequestPostProcessors.oauth2Login()
                                .oauth2User(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("userId", testUser.getId()))
                .andExpect(model().attribute("name", "Test User"))
                .andExpect(model().attribute("email", "test@example.com"))
                .andExpect(model().attribute("picture", "https://example.com/avatar.jpg"));
    }

    @Test
    @DisplayName("Should redirect to login when accessing dashboard unauthenticated")
    void dashboard_whenNotAuthenticated_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }
}
