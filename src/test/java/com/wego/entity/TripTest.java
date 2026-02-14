package com.wego.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Trip entity.
 *
 * Covers test cases: T-001 to T-006 (entity validation)
 */
@Tag("fast")
@DisplayName("Trip Entity Tests")
class TripTest {

    @Nested
    @DisplayName("Trip Creation")
    class TripCreation {

        @Test
        @DisplayName("Should create trip with default base currency TWD")
        void createTrip_withoutBaseCurrency_shouldDefaultToTWD() {
            Trip trip = Trip.builder()
                    .title("Test Trip")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(3))
                    .ownerId(UUID.randomUUID())
                    .build();

            assertEquals("TWD", trip.getBaseCurrency());
        }

        @Test
        @DisplayName("Should create trip with createdAt timestamp")
        void createTrip_shouldHaveCreatedAtTimestamp() {
            Instant before = Instant.now();

            Trip trip = Trip.builder()
                    .title("Test Trip")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(3))
                    .ownerId(UUID.randomUUID())
                    .build();

            Instant after = Instant.now();

            assertNotNull(trip.getCreatedAt());
            assertTrue(trip.getCreatedAt().compareTo(before) >= 0);
            assertTrue(trip.getCreatedAt().compareTo(after) <= 0);
        }

        @Test
        @DisplayName("Should allow setting cover image URL")
        void createTrip_withCoverImage_shouldSetCoverImageUrl() {
            String coverUrl = "https://example.com/cover.jpg";

            Trip trip = Trip.builder()
                    .title("Test Trip")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(3))
                    .ownerId(UUID.randomUUID())
                    .coverImageUrl(coverUrl)
                    .build();

            assertEquals(coverUrl, trip.getCoverImageUrl());
        }
    }

    @Nested
    @DisplayName("Trip Duration Calculation")
    class TripDurationCalculation {

        @Test
        @DisplayName("Should calculate duration in days correctly")
        void getDurationDays_withValidDates_shouldReturnCorrectDuration() {
            Trip trip = Trip.builder()
                    .title("Test Trip")
                    .startDate(LocalDate.of(2024, 3, 15))
                    .endDate(LocalDate.of(2024, 3, 19))
                    .ownerId(UUID.randomUUID())
                    .build();

            assertEquals(5, trip.getDurationDays());
        }

        @Test
        @DisplayName("Should return 1 for same day trip")
        void getDurationDays_sameDayTrip_shouldReturnOne() {
            LocalDate sameDay = LocalDate.of(2024, 3, 15);

            Trip trip = Trip.builder()
                    .title("Day Trip")
                    .startDate(sameDay)
                    .endDate(sameDay)
                    .ownerId(UUID.randomUUID())
                    .build();

            assertEquals(1, trip.getDurationDays());
        }
    }

    @Nested
    @DisplayName("Trip Equality and HashCode")
    class TripEqualityAndHashCode {

        @Test
        @DisplayName("Two trips with same ID should be equal")
        void equals_sameId_shouldBeEqual() {
            UUID tripId = UUID.randomUUID();

            Trip trip1 = new Trip();
            trip1.setId(tripId);
            trip1.setTitle("Trip 1");

            Trip trip2 = new Trip();
            trip2.setId(tripId);
            trip2.setTitle("Trip 2");

            assertEquals(trip1, trip2);
            assertEquals(trip1.hashCode(), trip2.hashCode());
        }

        @Test
        @DisplayName("Two trips with different IDs should not be equal")
        void equals_differentId_shouldNotBeEqual() {
            Trip trip1 = new Trip();
            trip1.setId(UUID.randomUUID());

            Trip trip2 = new Trip();
            trip2.setId(UUID.randomUUID());

            assertNotEquals(trip1, trip2);
        }

        @Test
        @DisplayName("Trip without ID should not equal trip with ID")
        void equals_oneWithoutId_shouldNotBeEqual() {
            Trip trip1 = new Trip();
            trip1.setId(UUID.randomUUID());

            Trip trip2 = new Trip();

            assertNotEquals(trip1, trip2);
        }
    }

}
