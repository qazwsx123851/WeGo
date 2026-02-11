package com.wego.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wego.dto.request.ApplyOptimizationRequest;
import com.wego.dto.request.CreateActivityRequest;
import com.wego.dto.request.ReorderActivitiesRequest;
import com.wego.dto.request.UpdateActivityRequest;
import com.wego.dto.response.ActivityResponse;
import com.wego.dto.response.RouteOptimizationResponse;
import com.wego.entity.TransportMode;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.security.UserPrincipal;
import com.wego.service.ActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActivityApiController.class)
@Import(TestSecurityConfig.class)
class ActivityApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityService activityService;

    private ObjectMapper objectMapper;
    private User testUser;
    private UserPrincipal userPrincipal;
    private UUID tripId;
    private UUID userId;
    private UUID activityId;
    private UUID placeId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        userId = UUID.randomUUID();
        tripId = UUID.randomUUID();
        activityId = UUID.randomUUID();
        placeId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .provider("google")
                .providerId("google-123")
                .build();

        userPrincipal = new UserPrincipal(testUser);
    }

    private ActivityResponse createTestActivityResponse() {
        return ActivityResponse.builder()
                .id(activityId)
                .tripId(tripId)
                .day(1)
                .sortOrder(0)
                .startTime(LocalTime.of(9, 0))
                .durationMinutes(60)
                .note("Test Activity")
                .transportMode(TransportMode.WALKING)
                .createdAt(Instant.now())
                .build();
    }

    // ========== POST /api/trips/{tripId}/activities ==========

    @Nested
    @DisplayName("POST /api/trips/{tripId}/activities - Create Activity")
    class CreateActivityTests {

        @Test
        @DisplayName("should create activity with valid request")
        void createActivity_validRequest_shouldReturn201() throws Exception {
            CreateActivityRequest request = CreateActivityRequest.builder()
                    .placeId(placeId)
                    .day(1)
                    .startTime(LocalTime.of(10, 0))
                    .durationMinutes(90)
                    .note("Morning visit")
                    .transportMode(TransportMode.WALKING)
                    .build();

            ActivityResponse response = createTestActivityResponse();
            when(activityService.createActivity(eq(tripId), any(CreateActivityRequest.class), any(UUID.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/trips/{tripId}/activities", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(activityId.toString()));

            verify(activityService).createActivity(eq(tripId), any(CreateActivityRequest.class), any(UUID.class));
        }

        @Test
        @DisplayName("should return 400 when placeId is null")
        void createActivity_nullPlaceId_shouldReturn400() throws Exception {
            CreateActivityRequest request = CreateActivityRequest.builder()
                    .day(1)
                    .build();

            mockMvc.perform(post("/api/trips/{tripId}/activities", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when day is null")
        void createActivity_nullDay_shouldReturn400() throws Exception {
            CreateActivityRequest request = CreateActivityRequest.builder()
                    .placeId(placeId)
                    .build();

            mockMvc.perform(post("/api/trips/{tripId}/activities", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void createActivity_unauthenticated_shouldReturn403() throws Exception {
            CreateActivityRequest request = CreateActivityRequest.builder()
                    .placeId(placeId)
                    .day(1)
                    .build();

            mockMvc.perform(post("/api/trips/{tripId}/activities", tripId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 403 when user lacks permission")
        void createActivity_forbidden_shouldReturn403() throws Exception {
            CreateActivityRequest request = CreateActivityRequest.builder()
                    .placeId(placeId)
                    .day(1)
                    .build();

            when(activityService.createActivity(eq(tripId), any(CreateActivityRequest.class), any(UUID.class)))
                    .thenThrow(new ForbiddenException("activity", "create"));

            mockMvc.perform(post("/api/trips/{tripId}/activities", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== GET /api/trips/{tripId}/activities ==========

    @Nested
    @DisplayName("GET /api/trips/{tripId}/activities - Get Activities")
    class GetActivitiesTests {

        @Test
        @DisplayName("should return all activities for trip")
        void getActivities_shouldReturnAll() throws Exception {
            List<ActivityResponse> responses = List.of(createTestActivityResponse());
            when(activityService.getActivitiesByTrip(eq(tripId), any(UUID.class)))
                    .thenReturn(responses);

            mockMvc.perform(get("/api/trips/{tripId}/activities", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(activityId.toString()));
        }

        @Test
        @DisplayName("should return activities filtered by day when param provided")
        void getActivities_withDayParam_shouldFilterByDay() throws Exception {
            List<ActivityResponse> responses = List.of(createTestActivityResponse());
            when(activityService.getActivitiesByDay(eq(tripId), eq(1), any(UUID.class)))
                    .thenReturn(responses);

            mockMvc.perform(get("/api/trips/{tripId}/activities", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .param("day", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());

            verify(activityService).getActivitiesByDay(eq(tripId), eq(1), any(UUID.class));
            verify(activityService, never()).getActivitiesByTrip(any(), any());
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void getActivities_unauthenticated_shouldReturn403() throws Exception {
            mockMvc.perform(get("/api/trips/{tripId}/activities", tripId))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== PUT /api/activities/{activityId} ==========

    @Nested
    @DisplayName("PUT /api/activities/{activityId} - Update Activity")
    class UpdateActivityTests {

        @Test
        @DisplayName("should update activity with valid request")
        void updateActivity_validRequest_shouldReturn200() throws Exception {
            UpdateActivityRequest request = UpdateActivityRequest.builder()
                    .day(2)
                    .note("Updated note")
                    .build();

            ActivityResponse response = createTestActivityResponse();
            when(activityService.updateActivity(eq(activityId), any(UpdateActivityRequest.class), any(UUID.class)))
                    .thenReturn(response);

            mockMvc.perform(put("/api/activities/{activityId}", activityId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(activityId.toString()));
        }

        @Test
        @DisplayName("should return 404 when activity not found")
        void updateActivity_notFound_shouldReturn404() throws Exception {
            UpdateActivityRequest request = UpdateActivityRequest.builder().day(2).build();

            when(activityService.updateActivity(eq(activityId), any(UpdateActivityRequest.class), any(UUID.class)))
                    .thenThrow(new ResourceNotFoundException("Activity", activityId.toString()));

            mockMvc.perform(put("/api/activities/{activityId}", activityId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    // ========== DELETE /api/activities/{activityId} ==========

    @Nested
    @DisplayName("DELETE /api/activities/{activityId} - Delete Activity")
    class DeleteActivityTests {

        @Test
        @DisplayName("should delete activity and return 204")
        void deleteActivity_shouldReturn204() throws Exception {
            doNothing().when(activityService).deleteActivity(eq(activityId), any(UUID.class));

            mockMvc.perform(delete("/api/activities/{activityId}", activityId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(activityService).deleteActivity(eq(activityId), any(UUID.class));
        }

        @Test
        @DisplayName("should return 404 when activity not found")
        void deleteActivity_notFound_shouldReturn404() throws Exception {
            doThrow(new ResourceNotFoundException("Activity", activityId.toString()))
                    .when(activityService).deleteActivity(eq(activityId), any(UUID.class));

            mockMvc.perform(delete("/api/activities/{activityId}", activityId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 403 when CSRF token missing")
        void deleteActivity_noCsrf_shouldReturn403() throws Exception {
            mockMvc.perform(delete("/api/activities/{activityId}", activityId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== PUT /api/trips/{tripId}/activities/reorder ==========

    @Nested
    @DisplayName("PUT /api/trips/{tripId}/activities/reorder - Reorder")
    class ReorderActivitiesTests {

        @Test
        @DisplayName("should reorder activities successfully")
        void reorderActivities_shouldReturn200() throws Exception {
            ReorderActivitiesRequest request = ReorderActivitiesRequest.builder()
                    .day(1)
                    .activityIds(List.of(UUID.randomUUID(), UUID.randomUUID()))
                    .build();

            List<ActivityResponse> responses = List.of(createTestActivityResponse());
            when(activityService.reorderActivities(eq(tripId), any(ReorderActivitiesRequest.class), any(UUID.class)))
                    .thenReturn(responses);

            mockMvc.perform(put("/api/trips/{tripId}/activities/reorder", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ========== GET /api/trips/{tripId}/activities/optimize ==========

    @Nested
    @DisplayName("GET /api/trips/{tripId}/activities/optimize - Optimize Route")
    class OptimizeRouteTests {

        @Test
        @DisplayName("should return optimization result")
        void getOptimizedRoute_shouldReturn200() throws Exception {
            RouteOptimizationResponse response = RouteOptimizationResponse.builder()
                    .tripId(tripId)
                    .day(1)
                    .originalOrder(List.of())
                    .optimizedOrder(List.of())
                    .originalDistanceMeters(1000)
                    .optimizedDistanceMeters(800)
                    .distanceSavedMeters(200)
                    .savingsPercentage(20.0)
                    .distanceSavedFormatted("200 m")
                    .optimizationApplied(true)
                    .activityCount(3)
                    .build();

            when(activityService.getOptimizedRoute(eq(tripId), eq(1), any(UUID.class)))
                    .thenReturn(response);

            mockMvc.perform(get("/api/trips/{tripId}/activities/optimize", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .param("day", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.activityCount").value(3));
        }
    }

    // ========== POST /api/trips/{tripId}/activities/apply-optimization ==========

    @Nested
    @DisplayName("POST /api/trips/{tripId}/activities/apply-optimization")
    class ApplyOptimizationTests {

        @Test
        @DisplayName("should apply optimization successfully")
        void applyOptimization_shouldReturn200() throws Exception {
            ApplyOptimizationRequest request = ApplyOptimizationRequest.builder()
                    .day(1)
                    .optimizedOrder(List.of(UUID.randomUUID(), UUID.randomUUID()))
                    .build();

            List<ActivityResponse> responses = List.of(createTestActivityResponse());
            when(activityService.applyOptimizedRoute(eq(tripId), eq(1), any(), any(UUID.class)))
                    .thenReturn(responses);

            mockMvc.perform(post("/api/trips/{tripId}/activities/apply-optimization", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
