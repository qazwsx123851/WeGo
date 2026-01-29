package com.wego.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PlaceSearchResult DTO.
 *
 * @see PlaceSearchResult
 */
@DisplayName("PlaceSearchResult")
class PlaceSearchResultTest {

    @Nested
    @DisplayName("Builder")
    class Builder {

        @Test
        @DisplayName("should create PlaceSearchResult with all fields")
        void shouldCreateWithAllFields() {
            List<String> types = Arrays.asList("restaurant", "food");

            PlaceSearchResult result = PlaceSearchResult.builder()
                    .placeId("ChIJN1t_tDeuEmsRUsoyG83frY4")
                    .name("Tokyo Tower")
                    .address("4-2-8 Shibakoen, Minato City")
                    .latitude(35.6585805)
                    .longitude(139.7454329)
                    .rating(4.5)
                    .userRatingsTotal(12345)
                    .types(types)
                    .photoReference("AWU5eFhqX8Y...")
                    .isOpen(true)
                    .build();

            assertThat(result.getPlaceId()).isEqualTo("ChIJN1t_tDeuEmsRUsoyG83frY4");
            assertThat(result.getName()).isEqualTo("Tokyo Tower");
            assertThat(result.getAddress()).isEqualTo("4-2-8 Shibakoen, Minato City");
            assertThat(result.getLatitude()).isEqualTo(35.6585805);
            assertThat(result.getLongitude()).isEqualTo(139.7454329);
            assertThat(result.getRating()).isEqualTo(4.5);
            assertThat(result.getUserRatingsTotal()).isEqualTo(12345);
            assertThat(result.getTypes()).containsExactly("restaurant", "food");
            assertThat(result.getPhotoReference()).isEqualTo("AWU5eFhqX8Y...");
            assertThat(result.getIsOpen()).isTrue();
        }

        @Test
        @DisplayName("should create PlaceSearchResult with minimal fields")
        void shouldCreateWithMinimalFields() {
            PlaceSearchResult result = PlaceSearchResult.builder()
                    .placeId("test-place-id")
                    .name("Test Place")
                    .build();

            assertThat(result.getPlaceId()).isEqualTo("test-place-id");
            assertThat(result.getName()).isEqualTo("Test Place");
            assertThat(result.getAddress()).isNull();
            assertThat(result.getLatitude()).isZero();
            assertThat(result.getLongitude()).isZero();
            assertThat(result.getRating()).isZero();
            assertThat(result.getTypes()).isNull();
        }
    }

    @Nested
    @DisplayName("All Args Constructor")
    class AllArgsConstructor {

        @Test
        @DisplayName("should create PlaceSearchResult with all args")
        void shouldCreateWithAllArgs() {
            List<String> types = Collections.singletonList("cafe");

            PlaceSearchResult result = new PlaceSearchResult(
                    "place-123",
                    "Starbucks",
                    "123 Main St",
                    35.0,
                    139.0,
                    4.2,
                    500,
                    types,
                    "photo-ref",
                    true
            );

            assertThat(result.getPlaceId()).isEqualTo("place-123");
            assertThat(result.getName()).isEqualTo("Starbucks");
            assertThat(result.getAddress()).isEqualTo("123 Main St");
            assertThat(result.getLatitude()).isEqualTo(35.0);
            assertThat(result.getLongitude()).isEqualTo(139.0);
            assertThat(result.getRating()).isEqualTo(4.2);
            assertThat(result.getUserRatingsTotal()).isEqualTo(500);
            assertThat(result.getTypes()).containsExactly("cafe");
            assertThat(result.getPhotoReference()).isEqualTo("photo-ref");
            assertThat(result.getIsOpen()).isTrue();
        }
    }

    @Nested
    @DisplayName("No Args Constructor")
    class NoArgsConstructor {

        @Test
        @DisplayName("should create empty PlaceSearchResult")
        void shouldCreateEmptyResult() {
            PlaceSearchResult result = new PlaceSearchResult();

            assertThat(result.getPlaceId()).isNull();
            assertThat(result.getName()).isNull();
            assertThat(result.getLatitude()).isZero();
            assertThat(result.getLongitude()).isZero();
        }
    }

    @Nested
    @DisplayName("Setters")
    class Setters {

        @Test
        @DisplayName("should set all fields via setters")
        void shouldSetAllFieldsViaSetters() {
            List<String> types = Arrays.asList("museum", "tourist_attraction");

            PlaceSearchResult result = new PlaceSearchResult();
            result.setPlaceId("place-456");
            result.setName("National Museum");
            result.setAddress("1-1 Ueno Park");
            result.setLatitude(35.7189);
            result.setLongitude(139.7745);
            result.setRating(4.7);
            result.setUserRatingsTotal(8000);
            result.setTypes(types);
            result.setPhotoReference("museum-photo");
            result.setIsOpen(false);

            assertThat(result.getPlaceId()).isEqualTo("place-456");
            assertThat(result.getName()).isEqualTo("National Museum");
            assertThat(result.getAddress()).isEqualTo("1-1 Ueno Park");
            assertThat(result.getLatitude()).isEqualTo(35.7189);
            assertThat(result.getLongitude()).isEqualTo(139.7745);
            assertThat(result.getRating()).isEqualTo(4.7);
            assertThat(result.getUserRatingsTotal()).isEqualTo(8000);
            assertThat(result.getTypes()).containsExactly("museum", "tourist_attraction");
            assertThat(result.getPhotoReference()).isEqualTo("museum-photo");
            assertThat(result.getIsOpen()).isFalse();
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            PlaceSearchResult result1 = PlaceSearchResult.builder()
                    .placeId("same-id")
                    .name("Same Place")
                    .latitude(35.0)
                    .longitude(139.0)
                    .build();

            PlaceSearchResult result2 = PlaceSearchResult.builder()
                    .placeId("same-id")
                    .name("Same Place")
                    .latitude(35.0)
                    .longitude(139.0)
                    .build();

            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when placeId differs")
        void shouldNotBeEqualWhenPlaceIdDiffers() {
            PlaceSearchResult result1 = PlaceSearchResult.builder()
                    .placeId("id-1")
                    .name("Place")
                    .build();

            PlaceSearchResult result2 = PlaceSearchResult.builder()
                    .placeId("id-2")
                    .name("Place")
                    .build();

            assertThat(result1).isNotEqualTo(result2);
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToString {

        @Test
        @DisplayName("should return readable string representation")
        void shouldReturnReadableString() {
            PlaceSearchResult result = PlaceSearchResult.builder()
                    .placeId("test-place")
                    .name("Test Restaurant")
                    .address("123 Test St")
                    .build();

            String toString = result.toString();

            assertThat(toString).contains("test-place");
            assertThat(toString).contains("Test Restaurant");
        }
    }

    @Nested
    @DisplayName("Types List")
    class TypesList {

        @Test
        @DisplayName("should handle null types list")
        void shouldHandleNullTypesList() {
            PlaceSearchResult result = PlaceSearchResult.builder()
                    .placeId("test")
                    .types(null)
                    .build();

            assertThat(result.getTypes()).isNull();
        }

        @Test
        @DisplayName("should handle empty types list")
        void shouldHandleEmptyTypesList() {
            PlaceSearchResult result = PlaceSearchResult.builder()
                    .placeId("test")
                    .types(Collections.emptyList())
                    .build();

            assertThat(result.getTypes()).isEmpty();
        }

        @Test
        @DisplayName("should handle multiple types")
        void shouldHandleMultipleTypes() {
            List<String> types = Arrays.asList("restaurant", "food", "point_of_interest");

            PlaceSearchResult result = PlaceSearchResult.builder()
                    .placeId("test")
                    .types(types)
                    .build();

            assertThat(result.getTypes()).hasSize(3);
            assertThat(result.getTypes()).containsExactly("restaurant", "food", "point_of_interest");
        }
    }
}
