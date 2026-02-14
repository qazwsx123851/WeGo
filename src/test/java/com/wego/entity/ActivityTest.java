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
        @DisplayName("Should default to WALKING transport mode")
        void createActivity_shouldDefaultToWalking() {
            Activity activity = Activity.builder()
                    .tripId(UUID.randomUUID())
                    .day(1)
                    .build();

            assertEquals(TransportMode.WALKING, activity.getTransportMode());
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

}
