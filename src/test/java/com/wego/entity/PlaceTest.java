package com.wego.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Place entity.
 */
@Tag("fast")
@DisplayName("Place Entity Tests")
class PlaceTest {

    @Nested
    @DisplayName("Place Creation")
    class PlaceCreation {

        @Test
        @DisplayName("Should create place with all fields")
        void createPlace_withAllFields_shouldCreatePlace() {
            Place place = Place.builder()
                    .name("淺草寺")
                    .address("東京都台東區淺草2-3-1")
                    .latitude(35.7148)
                    .longitude(139.7967)
                    .googlePlaceId("ChIJh5jX9i2MGGAR5fOqH9TtmHI")
                    .category("tourist_attraction")
                    .phoneNumber("+81-3-3842-0181")
                    .website("https://www.senso-ji.jp/")
                    .rating(4.5)
                    .priceLevel(2)
                    .build();

            assertNotNull(place);
            assertEquals("淺草寺", place.getName());
            assertEquals("東京都台東區淺草2-3-1", place.getAddress());
            assertEquals(35.7148, place.getLatitude(), 0.0001);
            assertEquals(139.7967, place.getLongitude(), 0.0001);
            assertEquals("ChIJh5jX9i2MGGAR5fOqH9TtmHI", place.getGooglePlaceId());
        }

        @Test
        @DisplayName("Should set createdAt timestamp on creation")
        void createPlace_shouldHaveCreatedAtTimestamp() {
            Place place = Place.builder()
                    .name("Test Place")
                    .latitude(25.0330)
                    .longitude(121.5654)
                    .build();

            assertNotNull(place.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Coordinate Validation")
    class CoordinateValidation {

        @Test
        @DisplayName("Should allow valid latitude range")
        void latitude_validRange_shouldBeAccepted() {
            Place place1 = Place.builder()
                    .name("North Pole")
                    .latitude(90.0)
                    .longitude(0.0)
                    .build();

            Place place2 = Place.builder()
                    .name("South Pole")
                    .latitude(-90.0)
                    .longitude(0.0)
                    .build();

            assertEquals(90.0, place1.getLatitude());
            assertEquals(-90.0, place2.getLatitude());
        }

        @Test
        @DisplayName("Should allow valid longitude range")
        void longitude_validRange_shouldBeAccepted() {
            Place place1 = Place.builder()
                    .name("Date Line East")
                    .latitude(0.0)
                    .longitude(180.0)
                    .build();

            Place place2 = Place.builder()
                    .name("Date Line West")
                    .latitude(0.0)
                    .longitude(-180.0)
                    .build();

            assertEquals(180.0, place1.getLongitude());
            assertEquals(-180.0, place2.getLongitude());
        }
    }

    @Nested
    @DisplayName("Place Equality")
    class PlaceEquality {

        @Test
        @DisplayName("Same ID should be equal")
        void equals_sameId_shouldBeEqual() {
            UUID placeId = UUID.randomUUID();

            Place place1 = new Place();
            place1.setId(placeId);
            place1.setName("Place 1");

            Place place2 = new Place();
            place2.setId(placeId);
            place2.setName("Place 2");

            assertEquals(place1, place2);
            assertEquals(place1.hashCode(), place2.hashCode());
        }

        @Test
        @DisplayName("Different IDs should not be equal")
        void equals_differentId_shouldNotBeEqual() {
            Place place1 = new Place();
            place1.setId(UUID.randomUUID());

            Place place2 = new Place();
            place2.setId(UUID.randomUUID());

            assertNotEquals(place1, place2);
        }
    }

    @Nested
    @DisplayName("Distance Calculation")
    class DistanceCalculation {

        @Test
        @DisplayName("Should calculate distance between two places")
        void distanceTo_shouldCalculateHaversineDistance() {
            // Tokyo Station
            Place tokyo = Place.builder()
                    .name("Tokyo Station")
                    .latitude(35.6812)
                    .longitude(139.7671)
                    .build();

            // Shibuya Station (~6.5 km from Tokyo Station)
            Place shibuya = Place.builder()
                    .name("Shibuya Station")
                    .latitude(35.6580)
                    .longitude(139.7016)
                    .build();

            double distance = tokyo.distanceTo(shibuya);

            // Should be approximately 6.5 km (6500 meters)
            assertTrue(distance > 6000 && distance < 7000,
                    "Distance should be approximately 6.5 km, but was " + distance);
        }

        @Test
        @DisplayName("Same location should return 0 distance")
        void distanceTo_sameLocation_shouldReturnZero() {
            Place place1 = Place.builder()
                    .name("Place 1")
                    .latitude(35.6812)
                    .longitude(139.7671)
                    .build();

            Place place2 = Place.builder()
                    .name("Place 2")
                    .latitude(35.6812)
                    .longitude(139.7671)
                    .build();

            assertEquals(0.0, place1.distanceTo(place2), 0.001);
        }

        @Test
        @DisplayName("Opposite side of earth should return max distance")
        void distanceTo_oppositePoints_shouldReturnMaxDistance() {
            Place north = Place.builder()
                    .name("North")
                    .latitude(0.0)
                    .longitude(0.0)
                    .build();

            Place south = Place.builder()
                    .name("South")
                    .latitude(0.0)
                    .longitude(180.0)
                    .build();

            double distance = north.distanceTo(south);

            // Should be approximately half Earth's circumference (20,015 km)
            assertTrue(distance > 19000000 && distance < 21000000,
                    "Distance should be approximately 20,015 km");
        }
    }

}
