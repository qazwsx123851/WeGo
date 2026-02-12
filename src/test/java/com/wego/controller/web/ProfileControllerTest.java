package com.wego.controller.web;

import com.wego.dto.response.UserProfileResponse;
import com.wego.entity.User;
import com.wego.security.UserPrincipal;
import com.wego.service.TripService;
import com.wego.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for ProfileController.
 *
 * @contract
 *   - Tests profile view, edit form, and update endpoints
 *   - Verifies validation (blank nickname, too long)
 *   - Tests authentication requirement
 */
@WebMvcTest(ProfileController.class)
@ActiveProfiles("test")
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private TripService tripService;

    private UUID userId;
    private User testUser;
    private UserPrincipal testPrincipal;
    private UserProfileResponse testProfile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .provider("google")
                .providerId("google-id")
                .build();
        testPrincipal = new UserPrincipal(testUser);

        testProfile = UserProfileResponse.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .provider("google")
                .createdAt(Instant.now())
                .tripCount(3)
                .documentsUploaded(10)
                .expensesCreated(20)
                .build();

    }

    private SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor oidcLogin() {
        return SecurityMockMvcRequestPostProcessors.oauth2Login().oauth2User(testPrincipal);
    }

    @Test
    @DisplayName("GET /profile - should return profile view with user data")
    void showProfile_authenticated_shouldReturnProfileView() throws Exception {
        when(userService.getUserProfile(testUser)).thenReturn(testProfile);

        mockMvc.perform(get("/profile").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/index"))
                .andExpect(model().attributeExists("profile", "name", "picture"))
                .andExpect(model().attribute("name", "Test User"));
    }

    @Test
    @DisplayName("GET /profile - should redirect when not authenticated")
    void showProfile_notAuthenticated_shouldRedirect() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("GET /profile/edit - should return edit form")
    void showEditForm_authenticated_shouldReturnEditView() throws Exception {
        mockMvc.perform(get("/profile/edit").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(model().attributeExists("user", "name", "picture"));
    }

    @Test
    @DisplayName("POST /profile/edit - should update nickname and redirect")
    void updateProfile_validNickname_shouldRedirect() throws Exception {
        mockMvc.perform(post("/profile/edit")
                        .param("nickname", "New Name")
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?success=profile_updated"));

        verify(userService).updateNickname(eq(userId), eq("New Name"));
    }

    @Test
    @DisplayName("POST /profile/edit - should show error when nickname is blank")
    void updateProfile_blankNickname_shouldReturnForm() throws Exception {
        mockMvc.perform(post("/profile/edit")
                        .param("nickname", "")
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @DisplayName("POST /profile/edit - should show error when nickname exceeds 50 chars")
    void updateProfile_tooLongNickname_shouldReturnForm() throws Exception {
        String longName = "A".repeat(51);

        mockMvc.perform(post("/profile/edit")
                        .param("nickname", longName)
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(model().attributeExists("error"));
    }
}
