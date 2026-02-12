package com.wego.controller.web;

import com.wego.dto.response.InvitePageData;
import com.wego.entity.User;
import com.wego.exception.ValidationException;
import com.wego.security.UserPrincipal;
import com.wego.service.InviteLinkService;
import com.wego.service.TripService;
import com.wego.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for InviteController.
 *
 * @contract
 *   - Tests invite page display and acceptance flow
 *   - Verifies token validation, error handling, and redirects
 *   - Tests already-member redirect and acceptance errors
 */
@WebMvcTest(InviteController.class)
@ActiveProfiles("test")
class InviteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private TripService tripService;

    @MockBean
    private InviteLinkService inviteLinkService;

    private UUID userId;
    private UUID tripId;
    private User testUser;
    private UserPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tripId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .provider("google")
                .providerId("google-id")
                .build();
        testPrincipal = new UserPrincipal(testUser);
    }

    private SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor oidcLogin() {
        return SecurityMockMvcRequestPostProcessors.oauth2Login().oauth2User(testPrincipal);
    }

    @Nested
    @DisplayName("GET /invite/{token}")
    class ShowInvitePageTests {

        @Test
        @DisplayName("should show invite page with trip info")
        void showInvitePage_validToken_shouldReturnView() throws Exception {
            InvitePageData pageData = InvitePageData.builder()
                    .token("valid-token-123")
                    .tripTitle("Tokyo Trip")
                    .tripId(tripId)
                    .tripStartDate(LocalDate.now().plusDays(10))
                    .tripEndDate(LocalDate.now().plusDays(15))
                    .inviteRole("EDITOR")
                    .expiresAt(ZonedDateTime.now().plusDays(3))
                    .memberCount(3)
                    .expiresWithin24h(false)
                    .alreadyMember(false)
                    .build();

            when(inviteLinkService.getInvitePageData("valid-token-123", userId))
                    .thenReturn(pageData);

            mockMvc.perform(get("/invite/{token}", "valid-token-123").with(oidcLogin()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("trip/invite"))
                    .andExpect(model().attribute("tripTitle", "Tokyo Trip"))
                    .andExpect(model().attribute("inviteRole", "EDITOR"));
        }

        @Test
        @DisplayName("should show error for invalid token format")
        void showInvitePage_invalidFormat_shouldShowError() throws Exception {
            mockMvc.perform(get("/invite/{token}", "invalid!@#token").with(oidcLogin()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("trip/invite"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @DisplayName("should redirect when already a member")
        void showInvitePage_alreadyMember_shouldRedirect() throws Exception {
            InvitePageData pageData = InvitePageData.builder()
                    .tripId(tripId)
                    .alreadyMember(true)
                    .build();

            when(inviteLinkService.getInvitePageData("member-token", userId))
                    .thenReturn(pageData);

            mockMvc.perform(get("/invite/{token}", "member-token").with(oidcLogin()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId));
        }

        @Test
        @DisplayName("should show error when token expired")
        void showInvitePage_expiredToken_shouldShowError() throws Exception {
            InvitePageData pageData = InvitePageData.builder()
                    .error("邀請連結已過期")
                    .build();

            when(inviteLinkService.getInvitePageData("expired-token", userId))
                    .thenReturn(pageData);

            mockMvc.perform(get("/invite/{token}", "expired-token").with(oidcLogin()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("trip/invite"))
                    .andExpect(model().attributeExists("error"));
        }
    }

    @Nested
    @DisplayName("POST /invite/{token}/accept")
    class AcceptInviteTests {

        @Test
        @DisplayName("should accept invite and redirect to trip")
        void acceptInvite_valid_shouldRedirectToTrip() throws Exception {
            when(inviteLinkService.acceptInvite("valid-token-123", userId))
                    .thenReturn(tripId);

            mockMvc.perform(post("/invite/{token}/accept", "valid-token-123")
                            .with(oidcLogin())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId));
        }

        @Test
        @DisplayName("should redirect with error when invite is invalid")
        void acceptInvite_invalidInvite_shouldRedirectWithError() throws Exception {
            when(inviteLinkService.acceptInvite("bad-token", userId))
                    .thenThrow(new ValidationException("INVALID_INVITE_LINK", "邀請連結無效"));

            mockMvc.perform(post("/invite/{token}/accept", "bad-token")
                            .with(oidcLogin())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/invite/bad-token"));
        }

        @Test
        @DisplayName("should redirect to dashboard for invalid token format")
        void acceptInvite_invalidFormat_shouldRedirectDashboard() throws Exception {
            mockMvc.perform(post("/invite/{token}/accept", "bad!@#format")
                            .with(oidcLogin())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard"));
        }
    }
}
