package com.wego.controller.web;

import com.wego.dto.response.ActivityResponse;
import com.wego.dto.response.ExpenseResponse;
import com.wego.dto.response.PlaceResponse;
import com.wego.dto.response.RecalculationResult;
import com.wego.dto.response.TripResponse;
import com.wego.entity.Role;
import com.wego.entity.TransportMode;
import com.wego.entity.User;
import com.wego.security.UserPrincipal;
import com.wego.service.ActivityService;
import com.wego.service.ActivityViewHelper;
import com.wego.service.ExpenseService;
import com.wego.service.PlaceService;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.service.TripService;
import com.wego.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests for ActivityWebController.
 *
 * @contract
 *   - Tests activity CRUD endpoints within a trip
 *   - Verifies model attributes and view names
 *   - Tests authentication and permission checks (OWNER vs VIEWER)
 */
@WebMvcTest(ActivityWebController.class)
@Import(ActivityViewHelper.class)
@ActiveProfiles("test")
class ActivityWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private TripService tripService;

    @MockBean
    private ActivityService activityService;

    @MockBean
    private PlaceService placeService;

    @MockBean
    private ExpenseService expenseService;

    private UUID userId;
    private UUID tripId;
    private UUID activityId;
    private User testUser;
    private UserPrincipal testPrincipal;
    private TripResponse testTrip;
    private TripResponse viewerTrip;
    private ActivityResponse testActivity;
    private UUID testPlaceId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tripId = UUID.randomUUID();
        activityId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .provider("google")
                .providerId("google-id")
                .build();

        TripResponse.MemberSummary ownerMember = TripResponse.MemberSummary.builder()
                .userId(userId)
                .nickname("Test User")
                .role(Role.OWNER)
                .build();

        testTrip = TripResponse.builder()
                .id(tripId)
                .title("Tokyo Trip")
                .description("Summer vacation")
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 3, 5))
                .baseCurrency("TWD")
                .members(List.of(ownerMember))
                .build();

        TripResponse.MemberSummary viewerMember = TripResponse.MemberSummary.builder()
                .userId(userId)
                .nickname("Test User")
                .role(Role.VIEWER)
                .build();

        viewerTrip = TripResponse.builder()
                .id(tripId)
                .title("Tokyo Trip")
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 3, 5))
                .baseCurrency("TWD")
                .members(List.of(viewerMember))
                .build();

        PlaceResponse placeResponse = PlaceResponse.builder()
                .id(UUID.randomUUID())
                .name("Tokyo Tower")
                .address("4 Chome-2-8 Shibakoen")
                .latitude(35.6586)
                .longitude(139.7454)
                .build();

        testActivity = ActivityResponse.builder()
                .id(activityId)
                .tripId(tripId)
                .place(placeResponse)
                .day(1)
                .sortOrder(0)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .durationMinutes(120)
                .note("Visit observation deck")
                .transportMode(TransportMode.WALKING)
                .build();

        testPlaceId = placeResponse.getId();

        testPrincipal = new UserPrincipal(testUser);
    }

    private RequestPostProcessor oauth2Login() {
        return SecurityMockMvcRequestPostProcessors.oauth2Login().oauth2User(testPrincipal);
    }

    @Nested
    @DisplayName("GET /trips/{tripId}/activities")
    class ListActivitiesTests {

        @Test
        @DisplayName("should return activity list view with model attributes")
        void listActivities_authenticated_shouldReturnListView() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
            when(activityService.getActivitiesByTrip(tripId, userId))
                    .thenReturn(List.of(testActivity));

            mockMvc.perform(get("/trips/{tripId}/activities", tripId).with(oauth2Login()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("activity/list"))
                    .andExpect(model().attributeExists(
                            "trip", "dates", "activitiesByDate", "allActivities",
                            "totalActivityCount", "currentDay", "currentMember",
                            "canEdit", "isOwner", "name", "picture"))
                    .andExpect(model().attribute("canEdit", true))
                    .andExpect(model().attribute("isOwner", true))
                    .andExpect(model().attribute("totalActivityCount", 1))
                    .andExpect(model().attribute("name", "Test User"));
        }

        @Test
        @DisplayName("should redirect when trip not found")
        void listActivities_tripNotFound_shouldRedirect() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(null);

            mockMvc.perform(get("/trips/{tripId}/activities", tripId).with(oauth2Login()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard?error=trip_not_found"));
        }
    }

    @Nested
    @DisplayName("GET /trips/{tripId}/activities/{activityId}")
    class ShowActivityDetailTests {

        @Test
        @DisplayName("should return activity detail view with model attributes")
        void showDetail_shouldReturnViewWithAttributes() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
            when(activityService.getActivity(activityId, userId)).thenReturn(testActivity);
            when(activityService.getActivitiesByTrip(tripId, userId))
                    .thenReturn(List.of(testActivity));
            when(expenseService.getExpensesByActivity(tripId, activityId, userId))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/trips/{tripId}/activities/{activityId}", tripId, activityId)
                            .with(oauth2Login()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("activity/detail"))
                    .andExpect(model().attributeExists(
                            "trip", "activity", "currentMember", "canEdit",
                            "isOwner", "name", "picture", "relatedExpenses"))
                    .andExpect(model().attribute("canEdit", true));
        }

        @Test
        @DisplayName("should redirect when activity not found")
        void showDetail_activityNotFound_shouldRedirect() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
            when(activityService.getActivity(activityId, userId))
                    .thenThrow(new ResourceNotFoundException("Activity", activityId.toString()));

            mockMvc.perform(get("/trips/{tripId}/activities/{activityId}", tripId, activityId)
                            .with(oauth2Login()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "/activities?error=activity_not_found"));
        }
    }

    @Nested
    @DisplayName("GET /trips/{tripId}/activities/new")
    class ShowCreateFormTests {

        @Test
        @DisplayName("should return create form for owner")
        void showCreateForm_owner_shouldReturnForm() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
            when(activityService.getActivitiesByTrip(tripId, userId))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/trips/{tripId}/activities/new", tripId).with(oauth2Login()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("activity/create"))
                    .andExpect(model().attributeExists(
                            "trip", "tripId", "dates", "selectedDay",
                            "canEdit", "name", "picture",
                            "searchLat", "searchLng", "searchRadius"))
                    .andExpect(model().attribute("canEdit", true))
                    .andExpect(model().attribute("selectedDay", 1));
        }

        @Test
        @DisplayName("should redirect when viewer tries to access create form")
        void showCreateForm_viewer_shouldRedirect() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(viewerTrip);

            mockMvc.perform(get("/trips/{tripId}/activities/new", tripId).with(oauth2Login()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "?error=access_denied"));
        }
    }

    @Nested
    @DisplayName("POST /trips/{tripId}/activities")
    class CreateActivityTests {

        @Test
        @DisplayName("should create activity and redirect to list")
        void createActivity_validData_shouldRedirect() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
            when(placeService.findOrCreate(anyString(), anyString(), anyString(),
                    any(Double.class), any(Double.class), anyString()))
                    .thenReturn(testPlaceId);
            when(activityService.createActivity(eq(tripId), any(), eq(userId)))
                    .thenReturn(testActivity);

            mockMvc.perform(post("/trips/{tripId}/activities", tripId)
                            .param("placeName", "Tokyo Tower")
                            .param("address", "4 Chome-2-8 Shibakoen")
                            .param("placeId", "ChIJ123")
                            .param("latitude", "35.6586")
                            .param("longitude", "139.7454")
                            .param("activityDate", "2026-03-01")
                            .param("startTime", "09:00")
                            .param("durationMinutes", "120")
                            .param("notes", "Visit observation deck")
                            .param("type", "ATTRACTION")
                            .param("transportMode", "WALKING")
                            .with(oauth2Login())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "/activities"));

            verify(activityService).createActivity(eq(tripId), any(), eq(userId));
        }

        @Test
        @DisplayName("should redirect with error when creation fails")
        void createActivity_serviceFails_shouldRedirectWithError() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
            when(placeService.findOrCreate(anyString(), anyString(), anyString(),
                    any(Double.class), any(Double.class), anyString()))
                    .thenThrow(new RuntimeException("Place creation failed"));

            mockMvc.perform(post("/trips/{tripId}/activities", tripId)
                            .param("placeName", "Tokyo Tower")
                            .param("address", "4 Chome-2-8 Shibakoen")
                            .param("placeId", "ChIJ123")
                            .param("latitude", "35.6586")
                            .param("longitude", "139.7454")
                            .param("activityDate", "2026-03-01")
                            .param("type", "ATTRACTION")
                            .param("transportMode", "WALKING")
                            .with(oauth2Login())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/trips/" + tripId + "/activities/new?error=*"));
        }

        @Test
        @DisplayName("should not succeed when required placeName is missing")
        void createActivity_missingPlaceName_shouldNotSucceed() throws Exception {
            mockMvc.perform(post("/trips/{tripId}/activities", tripId)
                            .param("activityDate", "2026-03-01")
                            .with(oauth2Login())
                            .with(csrf()))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("GET /trips/{tripId}/activities/{activityId}/edit")
    class ShowEditFormTests {

        @Test
        @DisplayName("should return edit form for owner")
        void showEditForm_owner_shouldReturnForm() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
            when(activityService.getActivity(activityId, userId)).thenReturn(testActivity);
            when(activityService.getActivitiesByTrip(tripId, userId))
                    .thenReturn(List.of(testActivity));

            mockMvc.perform(get("/trips/{tripId}/activities/{activityId}/edit", tripId, activityId)
                            .with(oauth2Login()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("activity/create"))
                    .andExpect(model().attributeExists(
                            "trip", "tripId", "activity", "dates",
                            "selectedDay", "selectedDate", "isEdit", "canEdit",
                            "name", "picture", "searchLat", "searchLng", "searchRadius"))
                    .andExpect(model().attribute("isEdit", true))
                    .andExpect(model().attribute("canEdit", true));
        }

        @Test
        @DisplayName("should redirect when viewer tries to edit")
        void showEditForm_viewer_shouldRedirect() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(viewerTrip);

            mockMvc.perform(get("/trips/{tripId}/activities/{activityId}/edit", tripId, activityId)
                            .with(oauth2Login()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "?error=access_denied"));
        }

        @Test
        @DisplayName("should redirect when activity not found")
        void showEditForm_activityNotFound_shouldRedirect() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
            when(activityService.getActivity(activityId, userId))
                    .thenThrow(new ResourceNotFoundException("Activity", activityId.toString()));

            mockMvc.perform(get("/trips/{tripId}/activities/{activityId}/edit", tripId, activityId)
                            .with(oauth2Login()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "/activities?error=activity_not_found"));
        }
    }

    @Nested
    @DisplayName("POST /trips/{tripId}/activities/{activityId}")
    class UpdateActivityTests {

        @Test
        @DisplayName("should update activity and redirect to list")
        void updateActivity_validData_shouldRedirect() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
            when(placeService.findOrCreate(anyString(), anyString(), anyString(),
                    any(Double.class), any(Double.class), anyString()))
                    .thenReturn(testPlaceId);
            when(activityService.updateActivity(eq(activityId), any(), eq(userId)))
                    .thenReturn(testActivity);

            mockMvc.perform(post("/trips/{tripId}/activities/{activityId}", tripId, activityId)
                            .param("placeName", "Tokyo Skytree")
                            .param("address", "1 Chome-1-2 Oshiage")
                            .param("placeId", "ChIJ456")
                            .param("latitude", "35.7101")
                            .param("longitude", "139.8107")
                            .param("activityDate", "2026-03-02")
                            .param("startTime", "10:00")
                            .param("durationMinutes", "90")
                            .param("notes", "Updated notes")
                            .param("type", "ATTRACTION")
                            .param("transportMode", "WALKING")
                            .with(oauth2Login())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "/activities"));

            verify(activityService).updateActivity(eq(activityId), any(), eq(userId));
        }

        @Test
        @DisplayName("should redirect with error when update fails")
        void updateActivity_serviceFails_shouldRedirectWithError() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
            when(placeService.findOrCreate(anyString(), anyString(), anyString(),
                    any(Double.class), any(Double.class), anyString()))
                    .thenReturn(testPlaceId);
            when(activityService.updateActivity(eq(activityId), any(), eq(userId)))
                    .thenThrow(new RuntimeException("Update failed"));

            mockMvc.perform(post("/trips/{tripId}/activities/{activityId}", tripId, activityId)
                            .param("placeName", "Tokyo Skytree")
                            .param("address", "1 Chome-1-2 Oshiage")
                            .param("placeId", "ChIJ456")
                            .param("latitude", "35.7101")
                            .param("longitude", "139.8107")
                            .param("activityDate", "2026-03-02")
                            .param("type", "ATTRACTION")
                            .param("transportMode", "WALKING")
                            .with(oauth2Login())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/trips/" + tripId + "/activities/" + activityId + "/edit?error=*"));
        }
    }

    @Nested
    @DisplayName("POST /trips/{tripId}/activities/{activityId}/delete")
    class DeleteActivityTests {

        @Test
        @DisplayName("should delete activity and redirect to list")
        void deleteActivity_shouldRedirectToList() throws Exception {
            doNothing().when(activityService).deleteActivity(activityId, userId);

            mockMvc.perform(post("/trips/{tripId}/activities/{activityId}/delete", tripId, activityId)
                            .with(oauth2Login())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "/activities"));

            verify(activityService).deleteActivity(activityId, userId);
        }

        @Test
        @DisplayName("should redirect with error when delete fails")
        void deleteActivity_serviceFails_shouldRedirectWithError() throws Exception {
            doThrow(new RuntimeException("Delete failed"))
                    .when(activityService).deleteActivity(activityId, userId);

            mockMvc.perform(post("/trips/{tripId}/activities/{activityId}/delete", tripId, activityId)
                            .with(oauth2Login())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "/activities"));
        }
    }

    @Nested
    @DisplayName("POST /trips/{tripId}/recalculate-transport")
    class RecalculateTransportTests {

        private RecalculationResult buildResult() {
            return RecalculationResult.builder()
                    .totalActivities(5)
                    .recalculatedCount(3)
                    .apiSuccessCount(2)
                    .fallbackCount(1)
                    .skippedCount(1)
                    .manualCount(1)
                    .rateLimitReached(false)
                    .build();
        }

        @Test
        @DisplayName("regular POST should redirect with success flash")
        void regularPost_shouldRedirectWithSuccessFlash() throws Exception {
            when(activityService.recalculateAllTransport(eq(tripId), eq(userId), eq(50)))
                    .thenReturn(buildResult());

            mockMvc.perform(post("/trips/{tripId}/recalculate-transport", tripId)
                            .with(oauth2Login())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "/activities"))
                    .andExpect(flash().attributeExists("success"));
        }

        @Test
        @DisplayName("AJAX POST should return 202 Accepted with async message")
        void ajaxPost_shouldReturnJsonSuccess() throws Exception {
            // Permission check passes (no throw)
            // Async method is fire-and-forget

            mockMvc.perform(post("/trips/{tripId}/recalculate-transport", tripId)
                            .with(oauth2Login())
                            .with(csrf())
                            .header("X-Requested-With", "XMLHttpRequest")
                            .header("Accept", "application/json"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.async").value(true));
        }

        @Test
        @DisplayName("AJAX POST should return 403 JSON when forbidden")
        void ajaxPost_forbidden_shouldReturnForbiddenJson() throws Exception {
            doThrow(new ForbiddenException("activity", "recalculate transport"))
                    .when(activityService).checkTransportRecalculatePermission(any(), any());

            mockMvc.perform(post("/trips/{tripId}/recalculate-transport", tripId)
                            .with(oauth2Login())
                            .with(csrf())
                            .header("X-Requested-With", "XMLHttpRequest")
                            .header("Accept", "application/json"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("regular POST should redirect with error when forbidden")
        void regularPost_forbidden_shouldRedirectWithError() throws Exception {
            doThrow(new ForbiddenException("activity", "recalculate transport"))
                    .when(activityService).checkTransportRecalculatePermission(any(), any());

            mockMvc.perform(post("/trips/{tripId}/recalculate-transport", tripId)
                            .with(oauth2Login())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "/activities"))
                    .andExpect(flash().attributeExists("error"));
        }
    }
}
