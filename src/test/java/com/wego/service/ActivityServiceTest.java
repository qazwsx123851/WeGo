package com.wego.service;

import com.wego.domain.permission.PermissionChecker;
import com.wego.domain.route.OptimizationResult;
import com.wego.domain.route.RouteOptimizer;
import com.wego.dto.request.CreateActivityRequest;
import com.wego.dto.request.ReorderActivitiesRequest;
import com.wego.dto.request.UpdateActivityRequest;
import com.wego.dto.response.ActivityResponse;
import com.wego.dto.response.RecalculationResult;
import com.wego.dto.response.RouteOptimizationResponse;
import com.wego.entity.Activity;
import com.wego.entity.Place;
import com.wego.entity.TransportMode;
import com.wego.exception.BusinessException;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.repository.ActivityRepository;
import com.wego.repository.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PermissionChecker permissionChecker;

    @Mock
    private RouteOptimizer routeOptimizer;

    @Mock
    private TransportCalculationService transportCalculationService;

    @InjectMocks
    private ActivityService activityService;

    private UUID tripId;
    private UUID userId;
    private UUID activityId;
    private UUID placeId;
    private Place testPlace;
    private Activity testActivity;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        userId = UUID.randomUUID();
        activityId = UUID.randomUUID();
        placeId = UUID.randomUUID();

        testPlace = Place.builder()
                .id(placeId)
                .name("Tokyo Tower")
                .latitude(35.6586)
                .longitude(139.7454)
                .build();

        testActivity = Activity.builder()
                .id(activityId)
                .tripId(tripId)
                .placeId(placeId)
                .day(1)
                .sortOrder(0)
                .startTime(LocalTime.of(9, 0))
                .durationMinutes(60)
                .note("Visit Tokyo Tower")
                .transportMode(TransportMode.WALKING)
                .createdAt(Instant.now())
                .build();
    }

    // ========== getActivity ==========

    @Nested
    @DisplayName("getActivity")
    class GetActivityTests {

        @Test
        @DisplayName("should return activity when user has view permission")
        void getActivity_withPermission_shouldReturnActivity() {
            when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(placeRepository.findById(placeId)).thenReturn(Optional.of(testPlace));

            ActivityResponse result = activityService.getActivity(activityId, userId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(activityId);
            assertThat(result.getPlace()).isNotNull();
            assertThat(result.getPlace().getName()).isEqualTo("Tokyo Tower");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when activity not found")
        void getActivity_notFound_shouldThrow() {
            when(activityRepository.findById(activityId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> activityService.getActivity(activityId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ForbiddenException when user lacks view permission")
        void getActivity_noPermission_shouldThrow() {
            when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            assertThatThrownBy(() -> activityService.getActivity(activityId, userId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should return activity with null place when placeId is null")
        void getActivity_nullPlaceId_shouldReturnWithNullPlace() {
            Activity activityNoPlace = Activity.builder()
                    .id(activityId)
                    .tripId(tripId)
                    .placeId(null)
                    .day(1)
                    .sortOrder(0)
                    .build();

            when(activityRepository.findById(activityId)).thenReturn(Optional.of(activityNoPlace));
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);

            ActivityResponse result = activityService.getActivity(activityId, userId);

            assertThat(result).isNotNull();
            assertThat(result.getPlace()).isNull();
        }
    }

    // ========== createActivity ==========

    @Nested
    @DisplayName("createActivity")
    class CreateActivityTests {

        @Test
        @DisplayName("should create activity with valid request")
        void createActivity_validRequest_shouldCreate() {
            CreateActivityRequest request = CreateActivityRequest.builder()
                    .placeId(placeId)
                    .day(1)
                    .startTime(LocalTime.of(10, 0))
                    .durationMinutes(90)
                    .note("Morning visit")
                    .transportMode(TransportMode.WALKING)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(placeRepository.findById(placeId)).thenReturn(Optional.of(testPlace));
            when(activityRepository.findMaxSortOrderByTripIdAndDay(tripId, 1)).thenReturn(Optional.of(2));
            when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> {
                Activity saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });
            when(activityRepository.findByTripIdAndDayOrderBySortOrderAsc(eq(tripId), eq(1)))
                    .thenReturn(List.of());

            ActivityResponse result = activityService.createActivity(tripId, request, userId);

            assertThat(result).isNotNull();
            verify(activityRepository).save(any(Activity.class));
            verify(transportCalculationService).calculateTransportFromPrevious(any(Activity.class));
        }

        @Test
        @DisplayName("should throw ForbiddenException when user lacks edit permission")
        void createActivity_noPermission_shouldThrow() {
            CreateActivityRequest request = CreateActivityRequest.builder()
                    .placeId(placeId)
                    .day(1)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(false);

            assertThatThrownBy(() -> activityService.createActivity(tripId, request, userId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when place not found")
        void createActivity_placeNotFound_shouldThrow() {
            CreateActivityRequest request = CreateActivityRequest.builder()
                    .placeId(placeId)
                    .day(1)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(placeRepository.findById(placeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> activityService.createActivity(tripId, request, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should use manual transport when provided")
        void createActivity_withManualTransport_shouldUseManual() {
            CreateActivityRequest request = CreateActivityRequest.builder()
                    .placeId(placeId)
                    .day(1)
                    .transportMode(TransportMode.FLIGHT)
                    .manualTransportMinutes(120)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(placeRepository.findById(placeId)).thenReturn(Optional.of(testPlace));
            when(activityRepository.findMaxSortOrderByTripIdAndDay(tripId, 1)).thenReturn(Optional.empty());
            when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> {
                Activity saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });
            when(activityRepository.findByTripIdAndDayOrderBySortOrderAsc(eq(tripId), eq(1)))
                    .thenReturn(List.of());

            activityService.createActivity(tripId, request, userId);

            verify(transportCalculationService).setManualTransportDuration(any(Activity.class), eq(120));
            verify(transportCalculationService, never()).calculateTransportFromPrevious(any());
        }

        @Test
        @DisplayName("should set sortOrder to 0 when no existing activities")
        void createActivity_firstActivity_shouldHaveSortOrderZero() {
            CreateActivityRequest request = CreateActivityRequest.builder()
                    .placeId(placeId)
                    .day(1)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(placeRepository.findById(placeId)).thenReturn(Optional.of(testPlace));
            when(activityRepository.findMaxSortOrderByTripIdAndDay(tripId, 1)).thenReturn(Optional.empty());
            when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> {
                Activity saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                assertThat(saved.getSortOrder()).isEqualTo(0);
                return saved;
            });
            when(activityRepository.findByTripIdAndDayOrderBySortOrderAsc(eq(tripId), eq(1)))
                    .thenReturn(List.of());

            activityService.createActivity(tripId, request, userId);

            verify(activityRepository).save(any(Activity.class));
        }
    }

    // ========== getActivitiesByTrip ==========

    @Nested
    @DisplayName("getActivitiesByTrip")
    class GetActivitiesByTripTests {

        @Test
        @DisplayName("should return activities when user has view permission")
        void getActivitiesByTrip_withPermission_shouldReturnList() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(activityRepository.findByTripIdOrderByDayAscSortOrderAsc(tripId))
                    .thenReturn(List.of(testActivity));
            when(placeRepository.findAllById(any())).thenReturn(List.of(testPlace));

            List<ActivityResponse> result = activityService.getActivitiesByTrip(tripId, userId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(activityId);
        }

        @Test
        @DisplayName("should throw ForbiddenException when user lacks permission")
        void getActivitiesByTrip_noPermission_shouldThrow() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            assertThatThrownBy(() -> activityService.getActivitiesByTrip(tripId, userId))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ========== getActivitiesByDay ==========

    @Nested
    @DisplayName("getActivitiesByDay")
    class GetActivitiesByDayTests {

        @Test
        @DisplayName("should return activities for specific day")
        void getActivitiesByDay_shouldReturnFiltered() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(activityRepository.findByTripIdAndDayOrderBySortOrderAsc(tripId, 1))
                    .thenReturn(List.of(testActivity));
            when(placeRepository.findAllById(any())).thenReturn(List.of(testPlace));

            List<ActivityResponse> result = activityService.getActivitiesByDay(tripId, 1, userId);

            assertThat(result).hasSize(1);
        }
    }

    // ========== updateActivity ==========

    @Nested
    @DisplayName("updateActivity")
    class UpdateActivityTests {

        @Test
        @DisplayName("should update activity fields")
        void updateActivity_validRequest_shouldUpdate() {
            UpdateActivityRequest request = UpdateActivityRequest.builder()
                    .day(2)
                    .startTime(LocalTime.of(14, 0))
                    .durationMinutes(120)
                    .note("Updated note")
                    .build();

            when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));
            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(placeRepository.findById(placeId)).thenReturn(Optional.of(testPlace));
            when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);

            ActivityResponse result = activityService.updateActivity(activityId, request, userId);

            assertThat(result).isNotNull();
            verify(activityRepository).save(any(Activity.class));
        }

        @Test
        @DisplayName("should throw ForbiddenException when user lacks edit permission")
        void updateActivity_noPermission_shouldThrow() {
            UpdateActivityRequest request = UpdateActivityRequest.builder().day(2).build();

            when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));
            when(permissionChecker.canEdit(tripId, userId)).thenReturn(false);

            assertThatThrownBy(() -> activityService.updateActivity(activityId, request, userId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when activity not found")
        void updateActivity_notFound_shouldThrow() {
            UpdateActivityRequest request = UpdateActivityRequest.builder().day(2).build();

            when(activityRepository.findById(activityId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> activityService.updateActivity(activityId, request, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should update place when placeId provided")
        void updateActivity_withNewPlace_shouldUpdatePlace() {
            UUID newPlaceId = UUID.randomUUID();
            Place newPlace = Place.builder()
                    .id(newPlaceId)
                    .name("Senso-ji")
                    .latitude(35.7148)
                    .longitude(139.7967)
                    .build();

            UpdateActivityRequest request = UpdateActivityRequest.builder()
                    .placeId(newPlaceId)
                    .build();

            when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));
            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(placeRepository.findById(newPlaceId)).thenReturn(Optional.of(newPlace));
            when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);
            when(activityRepository.findByTripIdAndDayOrderBySortOrderAsc(any(), anyInt()))
                    .thenReturn(List.of());

            activityService.updateActivity(activityId, request, userId);

            verify(transportCalculationService).calculateTransportFromPrevious(any());
        }

        @Test
        @DisplayName("should use manual transport when provided in update")
        void updateActivity_withManualTransport_shouldUseManual() {
            UpdateActivityRequest request = UpdateActivityRequest.builder()
                    .transportMode(TransportMode.HIGH_SPEED_RAIL)
                    .manualTransportMinutes(90)
                    .build();

            when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));
            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(placeRepository.findById(placeId)).thenReturn(Optional.of(testPlace));
            when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);

            activityService.updateActivity(activityId, request, userId);

            verify(transportCalculationService).setManualTransportDuration(any(), eq(90));
        }
    }

    // ========== deleteActivity ==========

    @Nested
    @DisplayName("deleteActivity")
    class DeleteActivityTests {

        @Test
        @DisplayName("should delete activity when user has edit permission")
        void deleteActivity_withPermission_shouldDelete() {
            when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));
            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);

            activityService.deleteActivity(activityId, userId);

            verify(activityRepository).delete(testActivity);
        }

        @Test
        @DisplayName("should throw ForbiddenException when user lacks permission")
        void deleteActivity_noPermission_shouldThrow() {
            when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));
            when(permissionChecker.canEdit(tripId, userId)).thenReturn(false);

            assertThatThrownBy(() -> activityService.deleteActivity(activityId, userId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when activity not found")
        void deleteActivity_notFound_shouldThrow() {
            when(activityRepository.findById(activityId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> activityService.deleteActivity(activityId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========== reorderActivities ==========

    @Nested
    @DisplayName("reorderActivities")
    class ReorderActivitiesTests {

        @Test
        @DisplayName("should reorder activities when valid request")
        void reorderActivities_valid_shouldReorder() {
            UUID actId1 = UUID.randomUUID();
            UUID actId2 = UUID.randomUUID();
            UUID placeId2 = UUID.randomUUID();

            Activity a1 = Activity.builder().id(actId1).tripId(tripId).placeId(placeId)
                    .day(1).sortOrder(0).build();
            Activity a2 = Activity.builder().id(actId2).tripId(tripId).placeId(placeId2)
                    .day(1).sortOrder(1).build();

            ReorderActivitiesRequest request = ReorderActivitiesRequest.builder()
                    .day(1)
                    .activityIds(List.of(actId2, actId1))
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(activityRepository.findByTripIdAndDayOrderBySortOrderAsc(tripId, 1))
                    .thenReturn(List.of(a1, a2));
            when(activityRepository.saveAll(any())).thenReturn(List.of(a2, a1));
            when(placeRepository.findAllById(any())).thenReturn(List.of(testPlace));

            List<ActivityResponse> result = activityService.reorderActivities(tripId, request, userId);

            assertThat(result).isNotNull();
            verify(transportCalculationService).batchCalculateTransport(any());
            verify(activityRepository).saveAll(any());
        }

        @Test
        @DisplayName("should throw BusinessException when activity count mismatch")
        void reorderActivities_mismatch_shouldThrow() {
            ReorderActivitiesRequest request = ReorderActivitiesRequest.builder()
                    .day(1)
                    .activityIds(List.of(UUID.randomUUID()))
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(activityRepository.findByTripIdAndDayOrderBySortOrderAsc(tripId, 1))
                    .thenReturn(List.of(testActivity, Activity.builder().id(UUID.randomUUID())
                            .tripId(tripId).day(1).sortOrder(1).build()));

            assertThatThrownBy(() -> activityService.reorderActivities(tripId, request, userId))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("should throw ForbiddenException when user lacks permission")
        void reorderActivities_noPermission_shouldThrow() {
            ReorderActivitiesRequest request = ReorderActivitiesRequest.builder()
                    .day(1)
                    .activityIds(List.of(activityId))
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(false);

            assertThatThrownBy(() -> activityService.reorderActivities(tripId, request, userId))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ========== getOptimizedRoute ==========

    @Nested
    @DisplayName("getOptimizedRoute")
    class GetOptimizedRouteTests {

        @Test
        @DisplayName("should return empty result when no activities")
        void getOptimizedRoute_noActivities_shouldReturnEmpty() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(activityRepository.findByTripIdAndDayOrderBySortOrderAsc(tripId, 1))
                    .thenReturn(List.of());

            RouteOptimizationResponse result = activityService.getOptimizedRoute(tripId, 1, userId);

            assertThat(result).isNotNull();
            assertThat(result.getActivityCount()).isEqualTo(0);
            assertThat(result.isOptimizationApplied()).isFalse();
        }

        @Test
        @DisplayName("should return optimization result when activities exist")
        void getOptimizedRoute_withActivities_shouldReturnResult() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(activityRepository.findByTripIdAndDayOrderBySortOrderAsc(tripId, 1))
                    .thenReturn(List.of(testActivity));
            when(placeRepository.findAllById(any())).thenReturn(List.of(testPlace));

            OptimizationResult optimResult = OptimizationResult.builder()
                    .originalOrder(List.of(activityId))
                    .optimizedOrder(List.of(activityId))
                    .originalDistanceMeters(1000)
                    .optimizedDistanceMeters(800)
                    .distanceSavedMeters(200)
                    .savingsPercentage(20.0)
                    .optimizationApplied(true)
                    .build();

            when(routeOptimizer.optimize(any(), any())).thenReturn(optimResult);

            RouteOptimizationResponse result = activityService.getOptimizedRoute(tripId, 1, userId);

            assertThat(result).isNotNull();
            verify(routeOptimizer).optimize(any(), any());
        }

        @Test
        @DisplayName("should throw ForbiddenException when user lacks permission")
        void getOptimizedRoute_noPermission_shouldThrow() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            assertThatThrownBy(() -> activityService.getOptimizedRoute(tripId, 1, userId))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ========== applyOptimizedRoute ==========

    @Nested
    @DisplayName("applyOptimizedRoute")
    class ApplyOptimizedRouteTests {

        @Test
        @DisplayName("should apply optimization successfully")
        void applyOptimizedRoute_valid_shouldApply() {
            Activity a1 = Activity.builder().id(activityId).tripId(tripId).placeId(placeId)
                    .day(1).sortOrder(0).build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(activityRepository.findByTripIdAndDayOrderBySortOrderAsc(tripId, 1))
                    .thenReturn(List.of(a1));
            when(activityRepository.saveAll(any())).thenReturn(List.of(a1));
            when(placeRepository.findAllById(any())).thenReturn(List.of(testPlace));

            List<ActivityResponse> result = activityService.applyOptimizedRoute(
                    tripId, 1, List.of(activityId), userId);

            assertThat(result).isNotNull();
            verify(transportCalculationService).batchCalculateTransport(any());
        }

        @Test
        @DisplayName("should throw BusinessException when count mismatch")
        void applyOptimizedRoute_mismatch_shouldThrow() {
            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(activityRepository.findByTripIdAndDayOrderBySortOrderAsc(tripId, 1))
                    .thenReturn(List.of(testActivity));

            assertThatThrownBy(() -> activityService.applyOptimizedRoute(
                    tripId, 1, List.of(activityId, UUID.randomUUID()), userId))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("should throw BusinessException when activity not found in day")
        void applyOptimizedRoute_unknownActivity_shouldThrow() {
            UUID unknownId = UUID.randomUUID();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(activityRepository.findByTripIdAndDayOrderBySortOrderAsc(tripId, 1))
                    .thenReturn(List.of(testActivity));

            assertThatThrownBy(() -> activityService.applyOptimizedRoute(
                    tripId, 1, List.of(unknownId), userId))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ========== recalculateAllTransport ==========

    @Nested
    @DisplayName("recalculateAllTransport")
    class RecalculateAllTransportTests {

        @Test
        @DisplayName("should return empty result when no activities")
        void recalculateAllTransport_noActivities_shouldReturnEmpty() {
            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(activityRepository.findByTripIdOrderByDayAscSortOrderAsc(tripId))
                    .thenReturn(List.of());

            RecalculationResult result = activityService.recalculateAllTransport(tripId, userId, 50);

            assertThat(result.getTotalActivities()).isEqualTo(0);
        }

        @Test
        @DisplayName("should delegate to transport service when activities exist")
        void recalculateAllTransport_withActivities_shouldDelegate() {
            RecalculationResult expectedResult = RecalculationResult.builder()
                    .totalActivities(2)
                    .recalculatedCount(1)
                    .apiSuccessCount(1)
                    .message("OK")
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(activityRepository.findByTripIdOrderByDayAscSortOrderAsc(tripId))
                    .thenReturn(List.of(testActivity));
            when(transportCalculationService.batchRecalculateWithRateLimit(any(), eq(50)))
                    .thenReturn(expectedResult);

            RecalculationResult result = activityService.recalculateAllTransport(tripId, userId, 50);

            assertThat(result.getTotalActivities()).isEqualTo(2);
            verify(activityRepository).saveAll(any());
        }

        @Test
        @DisplayName("should throw ForbiddenException when user lacks permission")
        void recalculateAllTransport_noPermission_shouldThrow() {
            when(permissionChecker.canEdit(tripId, userId)).thenReturn(false);

            assertThatThrownBy(() -> activityService.recalculateAllTransport(tripId, userId, 50))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
