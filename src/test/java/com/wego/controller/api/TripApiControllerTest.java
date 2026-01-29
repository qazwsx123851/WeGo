package com.wego.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wego.dto.request.ChangeMemberRoleRequest;
import com.wego.dto.request.CreateInviteLinkRequest;
import com.wego.dto.request.CreateTripRequest;
import com.wego.dto.request.UpdateTripRequest;
import com.wego.dto.response.InviteLinkResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.Role;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.exception.ValidationException;
import com.wego.security.UserPrincipal;
import com.wego.service.InviteLinkService;
import com.wego.service.TripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for TripApiController.
 *
 * Uses @WebMvcTest for focused controller testing with mocked services.
 *
 * @contract
 *   - pre: MockMvc is configured with security
 *   - post: All endpoints are tested with various scenarios
 */
@WebMvcTest(TripApiController.class)
@Import(TestSecurityConfig.class)
class TripApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TripService tripService;

    @MockBean
    private InviteLinkService inviteLinkService;

    private ObjectMapper objectMapper;
    private User testUser;
    private UserPrincipal userPrincipal;
    private UUID tripId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        userId = UUID.randomUUID();
        tripId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .provider("google")
                .providerId("google-123")
                .build();

        userPrincipal = new UserPrincipal(testUser);
    }

    /**
     * Creates an OAuth2User for testing with our custom UserPrincipal.
     */
    private UserPrincipal createTestPrincipal() {
        return userPrincipal;
    }

    // ========== POST /api/trips - Create Trip ==========

    @Nested
    @DisplayName("POST /api/trips - Create Trip")
    class CreateTripTests {

        @Test
        @DisplayName("should create trip with valid request")
        void createTrip_withValidRequest_shouldReturnCreatedTrip() throws Exception {
            CreateTripRequest request = CreateTripRequest.builder()
                    .title("Tokyo Trip")
                    .description("A wonderful trip to Tokyo")
                    .startDate(LocalDate.now().plusDays(30))
                    .endDate(LocalDate.now().plusDays(37))
                    .baseCurrency("JPY")
                    .build();

            TripResponse response = TripResponse.builder()
                    .id(tripId)
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .baseCurrency(request.getBaseCurrency())
                    .ownerId(userId)
                    .memberCount(1)
                    .currentUserRole(Role.OWNER)
                    .createdAt(Instant.now())
                    .build();

            when(tripService.createTrip(any(CreateTripRequest.class), any(User.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/trips")
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(tripId.toString()))
                    .andExpect(jsonPath("$.data.title").value("Tokyo Trip"))
                    .andExpect(jsonPath("$.data.currentUserRole").value("OWNER"));

            verify(tripService).createTrip(any(CreateTripRequest.class), any(User.class));
        }

        @Test
        @DisplayName("should return 400 when title is blank")
        void createTrip_withBlankTitle_shouldReturn400() throws Exception {
            CreateTripRequest request = CreateTripRequest.builder()
                    .title("")
                    .startDate(LocalDate.now().plusDays(30))
                    .endDate(LocalDate.now().plusDays(37))
                    .build();

            mockMvc.perform(post("/api/trips")
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when dates are missing")
        void createTrip_withMissingDates_shouldReturn400() throws Exception {
            CreateTripRequest request = CreateTripRequest.builder()
                    .title("Tokyo Trip")
                    .build();

            mockMvc.perform(post("/api/trips")
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should return 400 when end date is before start date")
        void createTrip_withInvalidDateRange_shouldReturn400() throws Exception {
            CreateTripRequest request = CreateTripRequest.builder()
                    .title("Tokyo Trip")
                    .startDate(LocalDate.now().plusDays(37))
                    .endDate(LocalDate.now().plusDays(30))
                    .build();

            when(tripService.createTrip(any(), any()))
                    .thenThrow(new ValidationException("INVALID_DATE_RANGE", "結束日期不可早於開始日期"));

            mockMvc.perform(post("/api/trips")
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_DATE_RANGE"));
        }

        @Test
        @DisplayName("should return error when not authenticated")
        void createTrip_withoutAuthentication_shouldReturnError() throws Exception {
            CreateTripRequest request = CreateTripRequest.builder()
                    .title("Tokyo Trip")
                    .startDate(LocalDate.now().plusDays(30))
                    .endDate(LocalDate.now().plusDays(37))
                    .build();

            // Spring Security returns 401 for unauthenticated requests (may be 403 due to CSRF)
            mockMvc.perform(post("/api/trips")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ========== GET /api/trips - Get User Trips ==========

    @Nested
    @DisplayName("GET /api/trips - Get User Trips")
    class GetUserTripsTests {

        @Test
        @DisplayName("should return paginated trips")
        void getUserTrips_shouldReturnPaginatedTrips() throws Exception {
            TripResponse trip1 = TripResponse.builder()
                    .id(UUID.randomUUID())
                    .title("Trip 1")
                    .startDate(LocalDate.now().plusDays(10))
                    .endDate(LocalDate.now().plusDays(15))
                    .memberCount(3)
                    .currentUserRole(Role.OWNER)
                    .build();

            TripResponse trip2 = TripResponse.builder()
                    .id(UUID.randomUUID())
                    .title("Trip 2")
                    .startDate(LocalDate.now().plusDays(20))
                    .endDate(LocalDate.now().plusDays(25))
                    .memberCount(2)
                    .currentUserRole(Role.EDITOR)
                    .build();

            Page<TripResponse> page = new PageImpl<>(List.of(trip1, trip2), PageRequest.of(0, 10), 2);

            when(tripService.getUserTrips(eq(userId), any())).thenReturn(page);

            mockMvc.perform(get("/api/trips")
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.totalElements").value(2));
        }

        @Test
        @DisplayName("should return empty page when no trips")
        void getUserTrips_withNoTrips_shouldReturnEmptyPage() throws Exception {
            Page<TripResponse> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

            when(tripService.getUserTrips(eq(userId), any())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/trips")
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    // ========== GET /api/trips/{tripId} - Get Single Trip ==========

    @Nested
    @DisplayName("GET /api/trips/{tripId} - Get Single Trip")
    class GetTripTests {

        @Test
        @DisplayName("should return trip when user is member")
        void getTrip_whenUserIsMember_shouldReturnTrip() throws Exception {
            TripResponse response = TripResponse.builder()
                    .id(tripId)
                    .title("Tokyo Trip")
                    .description("A wonderful trip")
                    .startDate(LocalDate.now().plusDays(30))
                    .endDate(LocalDate.now().plusDays(37))
                    .memberCount(3)
                    .currentUserRole(Role.EDITOR)
                    .members(List.of(
                            TripResponse.MemberSummary.builder()
                                    .userId(userId)
                                    .nickname("Test User")
                                    .role(Role.EDITOR)
                                    .build()
                    ))
                    .build();

            when(tripService.getTrip(tripId, userId)).thenReturn(response);

            mockMvc.perform(get("/api/trips/{tripId}", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(tripId.toString()))
                    .andExpect(jsonPath("$.data.title").value("Tokyo Trip"))
                    .andExpect(jsonPath("$.data.members").isArray());
        }

        @Test
        @DisplayName("should return 404 when trip not found")
        void getTrip_whenTripNotFound_shouldReturn404() throws Exception {
            when(tripService.getTrip(tripId, userId))
                    .thenThrow(ResourceNotFoundException.withCode("TRIP_NOT_FOUND", "行程不存在"));

            mockMvc.perform(get("/api/trips/{tripId}", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("TRIP_NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 403 when user is not member")
        void getTrip_whenUserNotMember_shouldReturn403() throws Exception {
            when(tripService.getTrip(tripId, userId))
                    .thenThrow(new ForbiddenException("您沒有權限查看此行程"));

            mockMvc.perform(get("/api/trips/{tripId}", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        }
    }

    // ========== PUT /api/trips/{tripId} - Update Trip ==========

    @Nested
    @DisplayName("PUT /api/trips/{tripId} - Update Trip")
    class UpdateTripTests {

        @Test
        @DisplayName("should update trip when user is owner")
        void updateTrip_whenUserIsOwner_shouldReturnUpdatedTrip() throws Exception {
            UpdateTripRequest request = UpdateTripRequest.builder()
                    .title("Updated Tokyo Trip")
                    .description("Updated description")
                    .build();

            TripResponse response = TripResponse.builder()
                    .id(tripId)
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .startDate(LocalDate.now().plusDays(30))
                    .endDate(LocalDate.now().plusDays(37))
                    .currentUserRole(Role.OWNER)
                    .build();

            when(tripService.updateTrip(eq(tripId), any(UpdateTripRequest.class), eq(userId)))
                    .thenReturn(response);

            mockMvc.perform(put("/api/trips/{tripId}", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("Updated Tokyo Trip"));

            verify(tripService).updateTrip(eq(tripId), any(UpdateTripRequest.class), eq(userId));
        }

        @Test
        @DisplayName("should return 403 when user is not owner")
        void updateTrip_whenUserNotOwner_shouldReturn403() throws Exception {
            UpdateTripRequest request = UpdateTripRequest.builder()
                    .title("Updated Title")
                    .build();

            when(tripService.updateTrip(eq(tripId), any(), eq(userId)))
                    .thenThrow(new ForbiddenException("只有行程建立者可以修改基本資訊"));

            mockMvc.perform(put("/api/trips/{tripId}", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("should return 400 when title exceeds max length")
        void updateTrip_withTitleTooLong_shouldReturn400() throws Exception {
            UpdateTripRequest request = UpdateTripRequest.builder()
                    .title("a".repeat(101))
                    .build();

            mockMvc.perform(put("/api/trips/{tripId}", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }
    }

    // ========== DELETE /api/trips/{tripId} - Delete Trip ==========

    @Nested
    @DisplayName("DELETE /api/trips/{tripId} - Delete Trip")
    class DeleteTripTests {

        @Test
        @DisplayName("should delete trip when user is owner")
        void deleteTrip_whenUserIsOwner_shouldReturn204() throws Exception {
            doNothing().when(tripService).deleteTrip(tripId, userId);

            mockMvc.perform(delete("/api/trips/{tripId}", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(tripService).deleteTrip(tripId, userId);
        }

        @Test
        @DisplayName("should return 403 when user is not owner")
        void deleteTrip_whenUserNotOwner_shouldReturn403() throws Exception {
            doThrow(new ForbiddenException("只有行程建立者可以刪除行程"))
                    .when(tripService).deleteTrip(tripId, userId);

            mockMvc.perform(delete("/api/trips/{tripId}", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("should return 404 when trip not found")
        void deleteTrip_whenTripNotFound_shouldReturn404() throws Exception {
            doThrow(ResourceNotFoundException.withCode("TRIP_NOT_FOUND", "行程不存在"))
                    .when(tripService).deleteTrip(tripId, userId);

            mockMvc.perform(delete("/api/trips/{tripId}", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("TRIP_NOT_FOUND"));
        }
    }

    // ========== GET /api/trips/{tripId}/members - Get Members ==========

    @Nested
    @DisplayName("GET /api/trips/{tripId}/members - Get Members")
    class GetMembersTests {

        @Test
        @DisplayName("should return members list when user can view")
        void getMembers_whenUserCanView_shouldReturnMembers() throws Exception {
            List<TripResponse.MemberSummary> members = List.of(
                    TripResponse.MemberSummary.builder()
                            .userId(userId)
                            .nickname("Owner")
                            .role(Role.OWNER)
                            .build(),
                    TripResponse.MemberSummary.builder()
                            .userId(UUID.randomUUID())
                            .nickname("Editor")
                            .role(Role.EDITOR)
                            .build()
            );

            when(tripService.getTripMembers(tripId, userId)).thenReturn(members);

            mockMvc.perform(get("/api/trips/{tripId}/members", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].role").value("OWNER"));
        }

        @Test
        @DisplayName("should return 403 when user cannot view")
        void getMembers_whenUserCannotView_shouldReturn403() throws Exception {
            when(tripService.getTripMembers(tripId, userId))
                    .thenThrow(new ForbiddenException("您沒有權限查看此行程"));

            mockMvc.perform(get("/api/trips/{tripId}/members", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ========== DELETE /api/trips/{tripId}/members/{userId} - Remove Member ==========

    @Nested
    @DisplayName("DELETE /api/trips/{tripId}/members/{userId} - Remove Member")
    class RemoveMemberTests {

        @Test
        @DisplayName("should remove member when user is owner")
        void removeMember_whenUserIsOwner_shouldReturn204() throws Exception {
            UUID targetUserId = UUID.randomUUID();
            doNothing().when(tripService).removeMember(tripId, targetUserId, userId);

            mockMvc.perform(delete("/api/trips/{tripId}/members/{userId}", tripId, targetUserId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(tripService).removeMember(tripId, targetUserId, userId);
        }

        @Test
        @DisplayName("should return 403 when user is not owner")
        void removeMember_whenUserNotOwner_shouldReturn403() throws Exception {
            UUID targetUserId = UUID.randomUUID();
            doThrow(new ForbiddenException("只有行程建立者可以移除成員"))
                    .when(tripService).removeMember(tripId, targetUserId, userId);

            mockMvc.perform(delete("/api/trips/{tripId}/members/{userId}", tripId, targetUserId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should return 403 when trying to remove owner")
        void removeMember_whenTargetIsOwner_shouldReturn403() throws Exception {
            UUID ownerUserId = UUID.randomUUID();
            doThrow(new ForbiddenException("無法移除行程建立者"))
                    .when(tripService).removeMember(tripId, ownerUserId, userId);

            mockMvc.perform(delete("/api/trips/{tripId}/members/{userId}", tripId, ownerUserId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when member not found")
        void removeMember_whenMemberNotFound_shouldReturn404() throws Exception {
            UUID targetUserId = UUID.randomUUID();
            doThrow(ResourceNotFoundException.withCode("MEMBER_NOT_FOUND", "找不到此成員"))
                    .when(tripService).removeMember(tripId, targetUserId, userId);

            mockMvc.perform(delete("/api/trips/{tripId}/members/{userId}", tripId, targetUserId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
        }
    }

    // ========== PUT /api/trips/{tripId}/members/{userId}/role - Change Member Role ==========

    @Nested
    @DisplayName("PUT /api/trips/{tripId}/members/{userId}/role - Change Member Role")
    class ChangeMemberRoleTests {

        @Test
        @DisplayName("should change role when user is owner")
        void changeMemberRole_whenUserIsOwner_shouldReturn200() throws Exception {
            UUID targetUserId = UUID.randomUUID();
            ChangeMemberRoleRequest request = ChangeMemberRoleRequest.builder()
                    .role(Role.VIEWER)
                    .build();

            doNothing().when(tripService).changeMemberRole(tripId, targetUserId, Role.VIEWER, userId);

            mockMvc.perform(put("/api/trips/{tripId}/members/{userId}/role", tripId, targetUserId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").exists());

            verify(tripService).changeMemberRole(tripId, targetUserId, Role.VIEWER, userId);
        }

        @Test
        @DisplayName("should return 403 when user is not owner")
        void changeMemberRole_whenUserNotOwner_shouldReturn403() throws Exception {
            UUID targetUserId = UUID.randomUUID();
            ChangeMemberRoleRequest request = ChangeMemberRoleRequest.builder()
                    .role(Role.EDITOR)
                    .build();

            doThrow(new ForbiddenException("只有行程建立者可以變更成員角色"))
                    .when(tripService).changeMemberRole(any(), any(), any(), any());

            mockMvc.perform(put("/api/trips/{tripId}/members/{userId}/role", tripId, targetUserId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 when trying to assign OWNER role")
        void changeMemberRole_toOwner_shouldReturn400() throws Exception {
            UUID targetUserId = UUID.randomUUID();
            ChangeMemberRoleRequest request = ChangeMemberRoleRequest.builder()
                    .role(Role.OWNER)
                    .build();

            doThrow(new ValidationException("INVALID_ROLE_CHANGE", "無法將角色變更為 OWNER"))
                    .when(tripService).changeMemberRole(any(), any(), any(), any());

            mockMvc.perform(put("/api/trips/{tripId}/members/{userId}/role", tripId, targetUserId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_ROLE_CHANGE"));
        }

        @Test
        @DisplayName("should return 400 when role is null")
        void changeMemberRole_withNullRole_shouldReturn400() throws Exception {
            UUID targetUserId = UUID.randomUUID();
            ChangeMemberRoleRequest request = new ChangeMemberRoleRequest();

            mockMvc.perform(put("/api/trips/{tripId}/members/{userId}/role", tripId, targetUserId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ========== POST /api/trips/{tripId}/invites - Create Invite Link ==========

    @Nested
    @DisplayName("POST /api/trips/{tripId}/invites - Create Invite Link")
    class CreateInviteLinkTests {

        @Test
        @DisplayName("should create invite link when user can invite")
        void createInviteLink_whenUserCanInvite_shouldReturnLink() throws Exception {
            CreateInviteLinkRequest request = CreateInviteLinkRequest.builder()
                    .role(Role.EDITOR)
                    .expiryDays(7)
                    .build();

            InviteLinkResponse response = InviteLinkResponse.builder()
                    .id(UUID.randomUUID())
                    .token("abc123")
                    .tripId(tripId)
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().plusSeconds(7 * 24 * 60 * 60))
                    .useCount(0)
                    .inviteUrl("http://localhost:8080/invite/abc123")
                    .build();

            when(inviteLinkService.createInviteLink(eq(tripId), any(CreateInviteLinkRequest.class), eq(userId)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/trips/{tripId}/invites", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.token").value("abc123"))
                    .andExpect(jsonPath("$.data.role").value("EDITOR"))
                    .andExpect(jsonPath("$.data.inviteUrl").exists());

            verify(inviteLinkService).createInviteLink(eq(tripId), any(CreateInviteLinkRequest.class), eq(userId));
        }

        @Test
        @DisplayName("should return 403 when user cannot invite")
        void createInviteLink_whenUserCannotInvite_shouldReturn403() throws Exception {
            CreateInviteLinkRequest request = CreateInviteLinkRequest.builder()
                    .role(Role.VIEWER)
                    .expiryDays(7)
                    .build();

            when(inviteLinkService.createInviteLink(any(), any(), any()))
                    .thenThrow(new ForbiddenException("您沒有權限建立邀請連結"));

            mockMvc.perform(post("/api/trips/{tripId}/invites", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 when trying to invite as OWNER")
        void createInviteLink_asOwner_shouldReturn400() throws Exception {
            CreateInviteLinkRequest request = CreateInviteLinkRequest.builder()
                    .role(Role.OWNER)
                    .expiryDays(7)
                    .build();

            when(inviteLinkService.createInviteLink(any(), any(), any()))
                    .thenThrow(new ValidationException("INVALID_ROLE", "無法邀請成為 OWNER"));

            mockMvc.perform(post("/api/trips/{tripId}/invites", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_ROLE"));
        }

        @Test
        @DisplayName("should return 400 when expiry days out of range")
        void createInviteLink_withInvalidExpiryDays_shouldReturn400() throws Exception {
            CreateInviteLinkRequest request = CreateInviteLinkRequest.builder()
                    .role(Role.EDITOR)
                    .expiryDays(31) // Max is 30
                    .build();

            mockMvc.perform(post("/api/trips/{tripId}/invites", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ========== GET /api/trips/{tripId}/invites - Get Active Invite Links ==========

    @Nested
    @DisplayName("GET /api/trips/{tripId}/invites - Get Active Invite Links")
    class GetInviteLinksTests {

        @Test
        @DisplayName("should return active invite links")
        void getInviteLinks_shouldReturnActiveLinks() throws Exception {
            List<InviteLinkResponse> links = List.of(
                    InviteLinkResponse.builder()
                            .id(UUID.randomUUID())
                            .token("abc123")
                            .role(Role.EDITOR)
                            .expiresAt(Instant.now().plusSeconds(3600))
                            .useCount(2)
                            .build(),
                    InviteLinkResponse.builder()
                            .id(UUID.randomUUID())
                            .token("xyz789")
                            .role(Role.VIEWER)
                            .expiresAt(Instant.now().plusSeconds(7200))
                            .useCount(0)
                            .build()
            );

            when(inviteLinkService.getActiveInviteLinks(tripId, userId)).thenReturn(links);

            mockMvc.perform(get("/api/trips/{tripId}/invites", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("should return empty list when no active links")
        void getInviteLinks_withNoActiveLinks_shouldReturnEmptyList() throws Exception {
            when(inviteLinkService.getActiveInviteLinks(tripId, userId))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/trips/{tripId}/invites", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("should return 403 when user cannot view trip")
        void getInviteLinks_whenUserCannotView_shouldReturn403() throws Exception {
            when(inviteLinkService.getActiveInviteLinks(tripId, userId))
                    .thenThrow(new ForbiddenException("您沒有權限查看此行程"));

            mockMvc.perform(get("/api/trips/{tripId}/invites", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== POST /api/invites/{token}/accept - Accept Invite ==========

    @Nested
    @DisplayName("POST /api/invites/{token}/accept - Accept Invite")
    class AcceptInviteTests {

        @Test
        @DisplayName("should accept invite and return trip ID")
        void acceptInvite_withValidToken_shouldReturnTripId() throws Exception {
            String token = "valid-token";
            when(inviteLinkService.acceptInvite(token, userId)).thenReturn(tripId);

            mockMvc.perform(post("/api/invites/{token}/accept", token)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.tripId").value(tripId.toString()))
                    .andExpect(jsonPath("$.message").exists());

            verify(inviteLinkService).acceptInvite(token, userId);
        }

        @Test
        @DisplayName("should return 400 when token is invalid or expired")
        void acceptInvite_withInvalidToken_shouldReturn400() throws Exception {
            String token = "invalid-token";
            when(inviteLinkService.acceptInvite(token, userId))
                    .thenThrow(new ValidationException("INVALID_INVITE_LINK", "邀請連結無效或已過期"));

            mockMvc.perform(post("/api/invites/{token}/accept", token)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INVITE_LINK"));
        }

        @Test
        @DisplayName("should return 400 when user is already a member")
        void acceptInvite_whenAlreadyMember_shouldReturn400() throws Exception {
            String token = "valid-token";
            when(inviteLinkService.acceptInvite(token, userId))
                    .thenThrow(new ValidationException("DUPLICATE_MEMBER", "已是行程成員"));

            mockMvc.perform(post("/api/invites/{token}/accept", token)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("DUPLICATE_MEMBER"));
        }

        @Test
        @DisplayName("should return 400 when member limit exceeded")
        void acceptInvite_whenMemberLimitExceeded_shouldReturn400() throws Exception {
            String token = "valid-token";
            when(inviteLinkService.acceptInvite(token, userId))
                    .thenThrow(new ValidationException("MEMBER_LIMIT_EXCEEDED", "行程成員已達上限"));

            mockMvc.perform(post("/api/invites/{token}/accept", token)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("MEMBER_LIMIT_EXCEEDED"));
        }
    }

    // ========== Security Tests ==========

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("should require CSRF token for POST requests")
        void postRequest_withoutCsrf_shouldReturn403() throws Exception {
            CreateTripRequest request = CreateTripRequest.builder()
                    .title("Tokyo Trip")
                    .startDate(LocalDate.now().plusDays(30))
                    .endDate(LocalDate.now().plusDays(37))
                    .build();

            mockMvc.perform(post("/api/trips")
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should require authentication for all endpoints except health")
        void protectedEndpoint_withoutAuth_shouldReturnError() throws Exception {
            // Spring Security returns either 401 or 403 for unauthenticated requests
            mockMvc.perform(get("/api/trips"))
                    .andExpect(status().is4xxClientError());
        }
    }
}
