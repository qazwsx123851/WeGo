package com.wego.controller.web;

import com.wego.dto.response.InviteLinkResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.Role;
import com.wego.entity.User;
import com.wego.security.UserPrincipal;
import com.wego.service.InviteLinkService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for MemberWebController.
 *
 * @contract
 *   - Tests member list page rendering
 *   - Verifies model attributes, invite link info, and permission flags
 *   - Tests authentication, trip access, and edge cases
 */
@WebMvcTest(MemberWebController.class)
@ActiveProfiles("test")
class MemberWebControllerTest {

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
    private TripResponse testTrip;
    private TripResponse.MemberSummary ownerMember;

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

        ownerMember = TripResponse.MemberSummary.builder()
                .userId(userId)
                .nickname("Test User")
                .role(Role.OWNER)
                .build();

        testTrip = TripResponse.builder()
                .id(tripId)
                .title("Tokyo Trip")
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(15))
                .baseCurrency("TWD")
                .members(List.of(ownerMember))
                .build();
    }

    private SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor oidcLogin() {
        return SecurityMockMvcRequestPostProcessors.oauth2Login().oauth2User(testPrincipal);
    }

    @Test
    @DisplayName("GET /trips/{id}/members - should return member list view with invite link")
    void showMembersPage_authenticated_shouldReturnView() throws Exception {
        when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
        when(tripService.getTripMembers(tripId, userId)).thenReturn(List.of(ownerMember));

        InviteLinkResponse inviteLink = InviteLinkResponse.builder()
                .token("abc123")
                .inviteUrl("https://example.com/invite/abc123")
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 3600))
                .build();
        when(inviteLinkService.getActiveInviteLinks(tripId, userId))
                .thenReturn(List.of(inviteLink));

        mockMvc.perform(get("/trips/{tripId}/members", tripId).with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(view().name("trip/members"))
                .andExpect(model().attributeExists("trip", "members", "currentMember",
                        "currentUserId", "isOwner", "canEdit", "memberCount",
                        "name", "picture", "canInvite", "inviteLink", "inviteLinkExpiry"))
                .andExpect(model().attribute("isOwner", true))
                .andExpect(model().attribute("canEdit", true))
                .andExpect(model().attribute("canInvite", true))
                .andExpect(model().attribute("memberCount", 1));
    }

    @Test
    @DisplayName("GET /trips/{id}/members - should handle no active invite links")
    void showMembersPage_noInviteLinks_shouldReturnNullLink() throws Exception {
        when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
        when(tripService.getTripMembers(tripId, userId)).thenReturn(List.of(ownerMember));
        when(inviteLinkService.getActiveInviteLinks(tripId, userId))
                .thenReturn(List.of());

        mockMvc.perform(get("/trips/{tripId}/members", tripId).with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(view().name("trip/members"))
                .andExpect(model().attributeDoesNotExist())
                .andExpect(model().attribute("inviteLink", (Object) null))
                .andExpect(model().attribute("inviteLinkExpiry", (Object) null));
    }

    @Test
    @DisplayName("GET /trips/{id}/members - should handle invite link service exception")
    void showMembersPage_inviteLinkError_shouldReturnNullLink() throws Exception {
        when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
        when(tripService.getTripMembers(tripId, userId)).thenReturn(List.of(ownerMember));
        when(inviteLinkService.getActiveInviteLinks(tripId, userId))
                .thenThrow(new RuntimeException("Service unavailable"));

        mockMvc.perform(get("/trips/{tripId}/members", tripId).with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(view().name("trip/members"))
                .andExpect(model().attribute("inviteLink", (Object) null))
                .andExpect(model().attribute("inviteLinkExpiry", (Object) null));
    }

    @Test
    @DisplayName("GET /trips/{id}/members - should redirect when trip not found")
    void showMembersPage_tripNotFound_shouldRedirect() throws Exception {
        when(tripService.getTrip(tripId, userId)).thenReturn(null);

        mockMvc.perform(get("/trips/{tripId}/members", tripId).with(oidcLogin()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?error=trip_not_found"));
    }

    @Test
    @DisplayName("GET /trips/{id}/members - should redirect when not authenticated")
    void showMembersPage_notAuthenticated_shouldRedirect() throws Exception {
        mockMvc.perform(get("/trips/{tripId}/members", tripId))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("GET /trips/{id}/members - viewer should not have edit/invite permission")
    void showMembersPage_viewer_shouldNotCanEdit() throws Exception {
        UUID viewerId = UUID.randomUUID();
        User viewerUser = User.builder()
                .id(viewerId)
                .email("viewer@example.com")
                .nickname("Viewer")
                .avatarUrl("https://example.com/viewer.jpg")
                .provider("google")
                .providerId("viewer-id")
                .build();
        UserPrincipal viewerPrincipal = new UserPrincipal(viewerUser);

        TripResponse.MemberSummary viewerMember = TripResponse.MemberSummary.builder()
                .userId(viewerId)
                .nickname("Viewer")
                .role(Role.VIEWER)
                .build();

        TripResponse viewerTrip = TripResponse.builder()
                .id(tripId)
                .title("Tokyo Trip")
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(15))
                .baseCurrency("TWD")
                .members(List.of(ownerMember, viewerMember))
                .build();

        when(tripService.getTrip(tripId, viewerId)).thenReturn(viewerTrip);
        when(tripService.getTripMembers(tripId, viewerId)).thenReturn(List.of(ownerMember, viewerMember));
        when(inviteLinkService.getActiveInviteLinks(tripId, viewerId)).thenReturn(List.of());

        mockMvc.perform(get("/trips/{tripId}/members", tripId)
                        .with(SecurityMockMvcRequestPostProcessors.oauth2Login().oauth2User(viewerPrincipal)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("canEdit", false))
                .andExpect(model().attribute("isOwner", false))
                .andExpect(model().attribute("canInvite", false));
    }
}
