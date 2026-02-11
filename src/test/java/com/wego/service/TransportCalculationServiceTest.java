package com.wego.service;

import com.wego.dto.response.DirectionResult;
import com.wego.entity.Activity;
import com.wego.entity.Place;
import com.wego.entity.TransportMode;
import com.wego.entity.TransportSource;
import com.wego.entity.TransportWarning;
import com.wego.repository.ActivityRepository;
import com.wego.repository.PlaceRepository;
import com.wego.service.external.GoogleMapsClient;
import com.wego.service.external.GoogleMapsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransportCalculationServiceTest {

    @Mock
    private GoogleMapsClient googleMapsClient;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private TransportCalculationService transportCalculationService;

    private UUID tripId;
    private UUID placeId1;
    private UUID placeId2;
    private Place tokyoTower;
    private Place sensoJi;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        placeId1 = UUID.randomUUID();
        placeId2 = UUID.randomUUID();

        tokyoTower = Place.builder()
                .id(placeId1)
                .name("Tokyo Tower")
                .latitude(35.6586)
                .longitude(139.7454)
                .build();

        sensoJi = Place.builder()
                .id(placeId2)
                .name("Senso-ji")
                .latitude(35.7148)
                .longitude(139.7967)
                .build();
    }

    // ========== calculateTransportFromPrevious ==========

    @Nested
    @DisplayName("calculateTransportFromPrevious")
    class CalculateTransportFromPreviousTests {

        @Test
        @DisplayName("should return activity unchanged when null")
        void calculateTransportFromPrevious_null_shouldReturnNull() {
            Activity result = transportCalculationService.calculateTransportFromPrevious(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return activity unchanged when placeId is null")
        void calculateTransportFromPrevious_nullPlaceId_shouldReturnUnchanged() {
            Activity activity = Activity.builder().id(UUID.randomUUID()).build();
            Activity result = transportCalculationService.calculateTransportFromPrevious(activity);
            assertThat(result).isSameAs(activity);
        }

        @Test
        @DisplayName("should clear transport when mode is NOT_CALCULATED")
        void calculateTransportFromPrevious_notCalculated_shouldClear() {
            Activity activity = Activity.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .placeId(placeId1)
                    .day(1)
                    .sortOrder(0)
                    .transportMode(TransportMode.NOT_CALCULATED)
                    .build();

            Activity result = transportCalculationService.calculateTransportFromPrevious(activity);

            assertThat(result.getTransportDurationMinutes()).isNull();
            assertThat(result.getTransportDistanceMeters()).isNull();
            assertThat(result.getTransportSource()).isEqualTo(TransportSource.NOT_APPLICABLE);
            assertThat(result.getTransportWarning()).isEqualTo(TransportWarning.NONE);
        }

        @Test
        @DisplayName("should clear transport for first activity of day (no previous)")
        void calculateTransportFromPrevious_firstOfDay_shouldClear() {
            Activity activity = Activity.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .placeId(placeId1)
                    .day(1)
                    .sortOrder(0)
                    .transportMode(TransportMode.WALKING)
                    .build();

            when(activityRepository.findByTripIdAndDayOrderBySortOrderAsc(tripId, 1))
                    .thenReturn(List.of(activity));

            Activity result = transportCalculationService.calculateTransportFromPrevious(activity);

            assertThat(result.getTransportSource()).isEqualTo(TransportSource.NOT_APPLICABLE);
        }

        @Test
        @DisplayName("should calculate transport from previous activity using Google API")
        void calculateTransportFromPrevious_withPrevious_shouldCalculate() {
            Activity previous = Activity.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .placeId(placeId1)
                    .day(1)
                    .sortOrder(0)
                    .build();

            Activity current = Activity.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .placeId(placeId2)
                    .day(1)
                    .sortOrder(1)
                    .transportMode(TransportMode.WALKING)
                    .build();

            when(activityRepository.findByTripIdAndDayOrderBySortOrderAsc(tripId, 1))
                    .thenReturn(List.of(previous, current));
            when(placeRepository.findById(placeId1)).thenReturn(Optional.of(tokyoTower));
            when(placeRepository.findById(placeId2)).thenReturn(Optional.of(sensoJi));

            DirectionResult dirResult = DirectionResult.builder()
                    .distanceMeters(3000)
                    .durationSeconds(1800)
                    .distanceText("3.0 km")
                    .durationText("30 min")
                    .build();

            when(googleMapsClient.getDirections(anyDouble(), anyDouble(), anyDouble(), anyDouble(), eq(TransportMode.WALKING)))
                    .thenReturn(dirResult);

            Activity result = transportCalculationService.calculateTransportFromPrevious(current);

            assertThat(result.getTransportDurationMinutes()).isEqualTo(30);
            assertThat(result.getTransportDistanceMeters()).isEqualTo(3000);
            assertThat(result.getTransportSource()).isEqualTo(TransportSource.GOOGLE_API);
        }

        @Test
        @DisplayName("should fallback to Haversine when Google API fails")
        void calculateTransportFromPrevious_apiFails_shouldFallback() {
            Activity previous = Activity.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .placeId(placeId1)
                    .day(1)
                    .sortOrder(0)
                    .build();

            Activity current = Activity.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .placeId(placeId2)
                    .day(1)
                    .sortOrder(1)
                    .transportMode(TransportMode.WALKING)
                    .build();

            when(activityRepository.findByTripIdAndDayOrderBySortOrderAsc(tripId, 1))
                    .thenReturn(List.of(previous, current));
            when(placeRepository.findById(placeId1)).thenReturn(Optional.of(tokyoTower));
            when(placeRepository.findById(placeId2)).thenReturn(Optional.of(sensoJi));
            when(googleMapsClient.getDirections(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                    .thenThrow(new GoogleMapsException("API error", "QUOTA_EXCEEDED"));

            Activity result = transportCalculationService.calculateTransportFromPrevious(current);

            assertThat(result.getTransportSource()).isEqualTo(TransportSource.HAVERSINE);
            assertThat(result.getTransportDurationMinutes()).isNotNull();
            assertThat(result.getTransportDistanceMeters()).isNotNull();
        }
    }

    // ========== determineWarning ==========

    @Nested
    @DisplayName("determineWarning")
    class DetermineWarningTests {

        @Test
        @DisplayName("should return NONE for short walking distance from Google API")
        void determineWarning_shortWalk_shouldReturnNone() {
            TransportWarning result = transportCalculationService.determineWarning(
                    1000, TransportMode.WALKING, TransportSource.GOOGLE_API);
            assertThat(result).isEqualTo(TransportWarning.NONE);
        }

        @Test
        @DisplayName("should return UNREALISTIC_WALKING for walking > 5km")
        void determineWarning_longWalk_shouldReturnUnrealistic() {
            TransportWarning result = transportCalculationService.determineWarning(
                    6000, TransportMode.WALKING, TransportSource.GOOGLE_API);
            assertThat(result).isEqualTo(TransportWarning.UNREALISTIC_WALKING);
        }

        @Test
        @DisplayName("should return UNREALISTIC_BICYCLING for bicycling > 30km")
        void determineWarning_longBicycle_shouldReturnUnrealistic() {
            TransportWarning result = transportCalculationService.determineWarning(
                    35000, TransportMode.BICYCLING, TransportSource.GOOGLE_API);
            assertThat(result).isEqualTo(TransportWarning.UNREALISTIC_BICYCLING);
        }

        @Test
        @DisplayName("should return VERY_LONG_DISTANCE for > 100km any mode")
        void determineWarning_veryLong_shouldReturnVeryLong() {
            TransportWarning result = transportCalculationService.determineWarning(
                    150000, TransportMode.DRIVING, TransportSource.GOOGLE_API);
            assertThat(result).isEqualTo(TransportWarning.VERY_LONG_DISTANCE);
        }

        @Test
        @DisplayName("should return ESTIMATED_DISTANCE for Haversine source")
        void determineWarning_haversine_shouldReturnEstimated() {
            TransportWarning result = transportCalculationService.determineWarning(
                    1000, TransportMode.WALKING, TransportSource.HAVERSINE);
            assertThat(result).isEqualTo(TransportWarning.ESTIMATED_DISTANCE);
        }

        @Test
        @DisplayName("VERY_LONG_DISTANCE takes precedence over Haversine estimated")
        void determineWarning_veryLongHaversine_shouldReturnVeryLong() {
            TransportWarning result = transportCalculationService.determineWarning(
                    150000, TransportMode.DRIVING, TransportSource.HAVERSINE);
            assertThat(result).isEqualTo(TransportWarning.VERY_LONG_DISTANCE);
        }
    }

    // ========== setManualTransportDuration ==========

    @Nested
    @DisplayName("setManualTransportDuration")
    class SetManualTransportDurationTests {

        @Test
        @DisplayName("should set manual transport fields")
        void setManualTransportDuration_shouldSetFields() {
            Activity activity = Activity.builder()
                    .id(UUID.randomUUID())
                    .transportMode(TransportMode.FLIGHT)
                    .build();

            Activity result = transportCalculationService.setManualTransportDuration(activity, 120);

            assertThat(result.getTransportDurationMinutes()).isEqualTo(120);
            assertThat(result.getTransportDistanceMeters()).isNull();
            assertThat(result.getTransportSource()).isEqualTo(TransportSource.MANUAL);
            assertThat(result.getTransportWarning()).isEqualTo(TransportWarning.NONE);
        }

        @Test
        @DisplayName("should return null when activity is null")
        void setManualTransportDuration_null_shouldReturnNull() {
            Activity result = transportCalculationService.setManualTransportDuration(null, 120);
            assertThat(result).isNull();
        }
    }

    // ========== batchCalculateTransport ==========

    @Nested
    @DisplayName("batchCalculateTransport")
    class BatchCalculateTransportTests {

        @Test
        @DisplayName("should handle null input")
        void batchCalculateTransport_null_shouldReturnNull() {
            List<Activity> result = transportCalculationService.batchCalculateTransport(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should clear transport for single activity")
        void batchCalculateTransport_single_shouldClear() {
            Activity activity = Activity.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .placeId(placeId1)
                    .day(1)
                    .sortOrder(0)
                    .transportDurationMinutes(30)
                    .build();

            List<Activity> result = transportCalculationService.batchCalculateTransport(List.of(activity));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTransportDurationMinutes()).isNull();
            assertThat(result.get(0).getTransportSource()).isEqualTo(TransportSource.NOT_APPLICABLE);
        }

        @Test
        @DisplayName("should calculate transport for ordered activities")
        void batchCalculateTransport_multiple_shouldCalculate() {
            Activity a1 = Activity.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .placeId(placeId1)
                    .day(1)
                    .sortOrder(0)
                    .transportMode(TransportMode.WALKING)
                    .build();

            Activity a2 = Activity.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .placeId(placeId2)
                    .day(1)
                    .sortOrder(1)
                    .transportMode(TransportMode.WALKING)
                    .build();

            when(placeRepository.findAllById(any())).thenReturn(List.of(tokyoTower, sensoJi));

            DirectionResult dirResult = DirectionResult.builder()
                    .distanceMeters(3000)
                    .durationSeconds(1800)
                    .distanceText("3.0 km")
                    .durationText("30 min")
                    .build();

            when(googleMapsClient.getDirections(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                    .thenReturn(dirResult);

            List<Activity> result = transportCalculationService.batchCalculateTransport(List.of(a1, a2));

            assertThat(result).hasSize(2);
            // First activity should have no transport
            assertThat(result.get(0).getTransportSource()).isEqualTo(TransportSource.NOT_APPLICABLE);
            // Second activity should have calculated transport
            assertThat(result.get(1).getTransportDurationMinutes()).isEqualTo(30);
            assertThat(result.get(1).getTransportSource()).isEqualTo(TransportSource.GOOGLE_API);
        }

        @Test
        @DisplayName("should skip NOT_CALCULATED mode in batch")
        void batchCalculateTransport_notCalculated_shouldSkip() {
            Activity a1 = Activity.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .placeId(placeId1)
                    .day(1)
                    .sortOrder(0)
                    .build();

            Activity a2 = Activity.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .placeId(placeId2)
                    .day(1)
                    .sortOrder(1)
                    .transportMode(TransportMode.NOT_CALCULATED)
                    .build();

            when(placeRepository.findAllById(any())).thenReturn(List.of(tokyoTower, sensoJi));

            List<Activity> result = transportCalculationService.batchCalculateTransport(List.of(a1, a2));

            assertThat(result.get(1).getTransportSource()).isEqualTo(TransportSource.NOT_APPLICABLE);
            verifyNoInteractions(googleMapsClient);
        }
    }

    // ========== calculateTransportWithWarnings ==========

    @Nested
    @DisplayName("calculateTransportWithWarnings")
    class CalculateTransportWithWarningsTests {

        @Test
        @DisplayName("should return NOT_APPLICABLE for FLIGHT mode")
        void calculateTransportWithWarnings_flight_shouldReturnNotApplicable() {
            var result = transportCalculationService.calculateTransportWithWarnings(
                    35.6586, 139.7454, 35.7148, 139.7967, TransportMode.FLIGHT);

            assertThat(result.getSource()).isEqualTo(TransportSource.NOT_APPLICABLE);
            assertThat(result.getDurationMinutes()).isNull();
        }

        @Test
        @DisplayName("should return NOT_APPLICABLE for HIGH_SPEED_RAIL mode")
        void calculateTransportWithWarnings_hsr_shouldReturnNotApplicable() {
            var result = transportCalculationService.calculateTransportWithWarnings(
                    35.6586, 139.7454, 35.7148, 139.7967, TransportMode.HIGH_SPEED_RAIL);

            assertThat(result.getSource()).isEqualTo(TransportSource.NOT_APPLICABLE);
        }

        @Test
        @DisplayName("should use Google API for WALKING mode")
        void calculateTransportWithWarnings_walking_shouldUseGoogleApi() {
            DirectionResult dirResult = DirectionResult.builder()
                    .distanceMeters(2000)
                    .durationSeconds(1200)
                    .distanceText("2.0 km")
                    .durationText("20 min")
                    .build();

            when(googleMapsClient.getDirections(anyDouble(), anyDouble(), anyDouble(), anyDouble(), eq(TransportMode.WALKING)))
                    .thenReturn(dirResult);

            var result = transportCalculationService.calculateTransportWithWarnings(
                    35.6586, 139.7454, 35.7148, 139.7967, TransportMode.WALKING);

            assertThat(result.getSource()).isEqualTo(TransportSource.GOOGLE_API);
            assertThat(result.getDurationMinutes()).isEqualTo(20);
            assertThat(result.getDistanceMeters()).isEqualTo(2000);
        }

        @Test
        @DisplayName("should fallback to Haversine when Google API throws")
        void calculateTransportWithWarnings_apiError_shouldFallback() {
            when(googleMapsClient.getDirections(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                    .thenThrow(new GoogleMapsException("Failed", "QUOTA_EXCEEDED"));

            var result = transportCalculationService.calculateTransportWithWarnings(
                    35.6586, 139.7454, 35.7148, 139.7967, TransportMode.DRIVING);

            assertThat(result.getSource()).isEqualTo(TransportSource.HAVERSINE);
            assertThat(result.getDurationMinutes()).isNotNull();
            assertThat(result.getDistanceMeters()).isNotNull();
        }
    }

    // ========== batchRecalculateWithRateLimit ==========

    @Nested
    @DisplayName("batchRecalculateWithRateLimit")
    class BatchRecalculateWithRateLimitTests {

        @Test
        @DisplayName("should return empty result for null activities")
        void batchRecalculate_null_shouldReturnEmpty() {
            var result = transportCalculationService.batchRecalculateWithRateLimit(null, 50);
            assertThat(result.getTotalActivities()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return empty result for empty list")
        void batchRecalculate_empty_shouldReturnEmpty() {
            var result = transportCalculationService.batchRecalculateWithRateLimit(List.of(), 50);
            assertThat(result.getTotalActivities()).isEqualTo(0);
        }

        @Test
        @DisplayName("should skip first activity of day")
        void batchRecalculate_firstOfDay_shouldSkip() {
            Activity a1 = Activity.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .placeId(placeId1)
                    .day(1)
                    .sortOrder(0)
                    .transportMode(TransportMode.WALKING)
                    .build();

            when(placeRepository.findAllById(any())).thenReturn(List.of(tokyoTower));

            var result = transportCalculationService.batchRecalculateWithRateLimit(List.of(a1), 50);

            assertThat(result.getTotalActivities()).isEqualTo(1);
            assertThat(result.getSkippedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should preserve manual transport for FLIGHT mode")
        void batchRecalculate_manual_shouldPreserve() {
            Activity a1 = Activity.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .placeId(placeId1)
                    .day(1)
                    .sortOrder(0)
                    .transportMode(TransportMode.WALKING)
                    .build();

            Activity a2 = Activity.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .placeId(placeId2)
                    .day(1)
                    .sortOrder(1)
                    .transportMode(TransportMode.FLIGHT)
                    .transportSource(TransportSource.MANUAL)
                    .transportDurationMinutes(120)
                    .build();

            when(placeRepository.findAllById(any())).thenReturn(List.of(tokyoTower, sensoJi));

            var result = transportCalculationService.batchRecalculateWithRateLimit(List.of(a1, a2), 50);

            assertThat(result.getManualCount()).isEqualTo(1);
            assertThat(a2.getTransportDurationMinutes()).isEqualTo(120);
        }
    }
}
