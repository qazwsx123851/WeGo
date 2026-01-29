package com.wego.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Activity entity.
 */
@Tag("fast")
@DisplayName("Activity Entity Tests")
class ActivityTest {

    @Nested
    @DisplayName("Activity Creation")
    class ActivityCreation {

        @Test
        @DisplayName("Should create activity with all fields")
        void createActivity_withAllFields_shouldCreateActivity() {
            UUID tripId = UUID.randomUUID();
            UUID placeId = UUID.randomUUID();

            Activity activity = Activity.builder()
                    .tripId(tripId)
                    .placeId(placeId)
                    .day(1)
                    .sortOrder(0)
                    .startTime(LocalTime.of(10, 0))
                    .durationMinutes(120)
                    .note("Visit the main hall")
                    .transportMode(TransportMode.TRANSIT)
                    .build();

            assertNotNull(activity);
            assertEquals(tripId, activity.getTripId());
            assertEquals(placeId, activity.getPlaceId());
            assertEquals(1, activity.getDay());
            assertEquals(0, activity.getSortOrder());
            assertEquals(LocalTime.of(10, 0), activity.getStartTime());
            assertEquals(120, activity.getDurationMinutes());
            assertEquals(TransportMode.TRANSIT, activity.getTransportMode());
        }

        @Test
        @DisplayName("Should default to WALKING transport mode")
        void createActivity_shouldDefaultToWalking() {
            Activity activity = Activity.builder()
                    .tripId(UUID.randomUUID())
                    .day(1)
                    .build();

            assertEquals(TransportMode.WALKING, activity.getTransportMode());
        }

        @Test
        @DisplayName("Should have createdAt timestamp")
        void createActivity_shouldHaveCreatedAtTimestamp() {
            Activity activity = Activity.builder()
                    .tripId(UUID.randomUUID())
                    .day(1)
                    .build();

            assertNotNull(activity.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("End Time Calculation")
    class EndTimeCalculation {

        @Test
        @DisplayName("Should calculate end time correctly")
        void getEndTime_withStartAndDuration_shouldCalculateEndTime() {
            Activity activity = Activity.builder()
                    .tripId(UUID.randomUUID())
                    .day(1)
                    .startTime(LocalTime.of(10, 0))
                    .durationMinutes(90)
                    .build();

            assertEquals(LocalTime.of(11, 30), activity.getEndTime());
        }

        @Test
        @DisplayName("Should return null when startTime is missing")
        void getEndTime_withoutStartTime_shouldReturnNull() {
            Activity activity = Activity.builder()
                    .tripId(UUID.randomUUID())
                    .day(1)
                    .durationMinutes(90)
                    .build();

            assertNull(activity.getEndTime());
        }

        @Test
        @DisplayName("Should return null when duration is missing")
        void getEndTime_withoutDuration_shouldReturnNull() {
            Activity activity = Activity.builder()
                    .tripId(UUID.randomUUID())
                    .day(1)
                    .startTime(LocalTime.of(10, 0))
                    .build();

            assertNull(activity.getEndTime());
        }

        @Test
        @DisplayName("Should handle overnight activities")
        void getEndTime_overnightActivity_shouldWrapAround() {
            Activity activity = Activity.builder()
                    .tripId(UUID.randomUUID())
                    .day(1)
                    .startTime(LocalTime.of(23, 0))
                    .durationMinutes(120)
                    .build();

            assertEquals(LocalTime.of(1, 0), activity.getEndTime());
        }
    }

    @Nested
    @DisplayName("Activity Equality")
    class ActivityEquality {

        @Test
        @DisplayName("Same ID should be equal")
        void equals_sameId_shouldBeEqual() {
            UUID activityId = UUID.randomUUID();

            Activity activity1 = new Activity();
            activity1.setId(activityId);

            Activity activity2 = new Activity();
            activity2.setId(activityId);

            assertEquals(activity1, activity2);
            assertEquals(activity1.hashCode(), activity2.hashCode());
        }
    }
}
