package com.wego.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PlaceDetails DTO.
 *
 * @see PlaceDetails
 */
@DisplayName("PlaceDetails")
class PlaceDetailsTest {

    @Nested
    @DisplayName("Builder")
    class Builder {

        @Test
        @DisplayName("should create PlaceDetails with all fields")
        void shouldCreateWithAllFields() {
            List<String> types = Arrays.asList("restaurant", "food");
            List<String> photoReferences = Arrays.asList("photo1", "photo2");
            List<PlaceDetails.Review> reviews = Collections.singletonList(
                    PlaceDetails.Review.builder()
                            .authorName("John Doe")
                            .rating(5)
                            .text("Great place!")
                            .relativeTimeDescription("a week ago")
                            .build()
            );
            PlaceDetails.OpeningHours openingHours = PlaceDetails.OpeningHours.builder()
                    .isOpenNow(true)
                    .weekdayText(Arrays.asList("Monday: 9:00 AM - 9:00 PM"))
                    .build();

            PlaceDetails result = PlaceDetails.builder()
                    .placeId("ChIJN1t_tDeuEmsRUsoyG83frY4")
                    .name("Tokyo Tower")
                    .formattedAddress("4-2-8 Shibakoen, Minato City, Tokyo 105-0011")
                    .formattedPhoneNumber("+81 3-3433-5111")
                    .internationalPhoneNumber("+81 3-3433-5111")
                    .website("https://www.tokyotower.co.jp")
                    .url("https://maps.google.com/?cid=...")
                    .latitude(35.6585805)
                    .longitude(139.7454329)
                    .rating(4.5)
                    .userRatingsTotal(12345)
                    .priceLevel(2)
                    .types(types)
                    .photoReferences(photoReferences)
                    .reviews(reviews)
                    .openingHours(openingHours)
                    .utcOffset(540)
                    .build();

            assertThat(result.getPlaceId()).isEqualTo("ChIJN1t_tDeuEmsRUsoyG83frY4");
            assertThat(result.getName()).isEqualTo("Tokyo Tower");
            assertThat(result.getFormattedAddress()).isEqualTo("4-2-8 Shibakoen, Minato City, Tokyo 105-0011");
            assertThat(result.getFormattedPhoneNumber()).isEqualTo("+81 3-3433-5111");
            assertThat(result.getInternationalPhoneNumber()).isEqualTo("+81 3-3433-5111");
            assertThat(result.getWebsite()).isEqualTo("https://www.tokyotower.co.jp");
            assertThat(result.getUrl()).isEqualTo("https://maps.google.com/?cid=...");
            assertThat(result.getLatitude()).isEqualTo(35.6585805);
            assertThat(result.getLongitude()).isEqualTo(139.7454329);
            assertThat(result.getRating()).isEqualTo(4.5);
            assertThat(result.getUserRatingsTotal()).isEqualTo(12345);
            assertThat(result.getPriceLevel()).isEqualTo(2);
            assertThat(result.getTypes()).containsExactly("restaurant", "food");
            assertThat(result.getPhotoReferences()).containsExactly("photo1", "photo2");
            assertThat(result.getReviews()).hasSize(1);
            assertThat(result.getOpeningHours().getIsOpenNow()).isTrue();
            assertThat(result.getUtcOffset()).isEqualTo(540);
        }

        @Test
        @DisplayName("should create PlaceDetails with minimal fields")
        void shouldCreateWithMinimalFields() {
            PlaceDetails result = PlaceDetails.builder()
                    .placeId("test-place-id")
                    .name("Test Place")
                    .build();

            assertThat(result.getPlaceId()).isEqualTo("test-place-id");
            assertThat(result.getName()).isEqualTo("Test Place");
            assertThat(result.getFormattedAddress()).isNull();
            assertThat(result.getWebsite()).isNull();
            assertThat(result.getReviews()).isNull();
        }
    }

    @Nested
    @DisplayName("All Args Constructor")
    class AllArgsConstructor {

        @Test
        @DisplayName("should create PlaceDetails with all args")
        void shouldCreateWithAllArgs() {
            List<String> types = Collections.singletonList("cafe");
            List<String> photos = Collections.singletonList("photo-ref");

            PlaceDetails result = new PlaceDetails(
                    "place-123",
                    "Starbucks",
                    "123 Main St, Tokyo",
                    "03-1234-5678",
                    "+81-3-1234-5678",
                    "https://starbucks.com",
                    "https://maps.google.com/?cid=123",
                    35.0,
                    139.0,
                    4.2,
                    500,
                    2,
                    types,
                    photos,
                    null,
                    null,
                    540
            );

            assertThat(result.getPlaceId()).isEqualTo("place-123");
            assertThat(result.getName()).isEqualTo("Starbucks");
            assertThat(result.getFormattedAddress()).isEqualTo("123 Main St, Tokyo");
            assertThat(result.getPriceLevel()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("No Args Constructor")
    class NoArgsConstructor {

        @Test
        @DisplayName("should create empty PlaceDetails")
        void shouldCreateEmptyResult() {
            PlaceDetails result = new PlaceDetails();

            assertThat(result.getPlaceId()).isNull();
            assertThat(result.getName()).isNull();
            assertThat(result.getLatitude()).isZero();
            assertThat(result.getLongitude()).isZero();
        }
    }

    @Nested
    @DisplayName("Nested Review Class")
    class NestedReviewClass {

        @Test
        @DisplayName("should create Review with builder")
        void shouldCreateReviewWithBuilder() {
            PlaceDetails.Review review = PlaceDetails.Review.builder()
                    .authorName("Jane Smith")
                    .rating(4)
                    .text("Nice atmosphere!")
                    .relativeTimeDescription("2 weeks ago")
                    .build();

            assertThat(review.getAuthorName()).isEqualTo("Jane Smith");
            assertThat(review.getRating()).isEqualTo(4);
            assertThat(review.getText()).isEqualTo("Nice atmosphere!");
            assertThat(review.getRelativeTimeDescription()).isEqualTo("2 weeks ago");
        }

        @Test
        @DisplayName("should create Review with all args constructor")
        void shouldCreateReviewWithAllArgs() {
            PlaceDetails.Review review = new PlaceDetails.Review(
                    "Bob",
                    3,
                    "Average",
                    "a month ago"
            );

            assertThat(review.getAuthorName()).isEqualTo("Bob");
            assertThat(review.getRating()).isEqualTo(3);
        }

        @Test
        @DisplayName("should create empty Review")
        void shouldCreateEmptyReview() {
            PlaceDetails.Review review = new PlaceDetails.Review();

            assertThat(review.getAuthorName()).isNull();
            assertThat(review.getRating()).isZero();
        }
    }

    @Nested
    @DisplayName("Nested OpeningHours Class")
    class NestedOpeningHoursClass {

        @Test
        @DisplayName("should create OpeningHours with builder")
        void shouldCreateOpeningHoursWithBuilder() {
            List<String> weekdayText = Arrays.asList(
                    "Monday: 9:00 AM - 6:00 PM",
                    "Tuesday: 9:00 AM - 6:00 PM"
            );

            PlaceDetails.OpeningHours hours = PlaceDetails.OpeningHours.builder()
                    .isOpenNow(true)
                    .weekdayText(weekdayText)
                    .build();

            assertThat(hours.getIsOpenNow()).isTrue();
            assertThat(hours.getWeekdayText()).hasSize(2);
        }

        @Test
        @DisplayName("should create OpeningHours with all args constructor")
        void shouldCreateOpeningHoursWithAllArgs() {
            List<String> weekdayText = Collections.singletonList("Monday: Closed");

            PlaceDetails.OpeningHours hours = new PlaceDetails.OpeningHours(
                    false,
                    weekdayText
            );

            assertThat(hours.getIsOpenNow()).isFalse();
            assertThat(hours.getWeekdayText()).containsExactly("Monday: Closed");
        }

        @Test
        @DisplayName("should create empty OpeningHours")
        void shouldCreateEmptyOpeningHours() {
            PlaceDetails.OpeningHours hours = new PlaceDetails.OpeningHours();

            assertThat(hours.getIsOpenNow()).isNull();
            assertThat(hours.getWeekdayText()).isNull();
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            PlaceDetails result1 = PlaceDetails.builder()
                    .placeId("same-id")
                    .name("Same Place")
                    .latitude(35.0)
                    .longitude(139.0)
                    .build();

            PlaceDetails result2 = PlaceDetails.builder()
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
            PlaceDetails result1 = PlaceDetails.builder()
                    .placeId("id-1")
                    .name("Place")
                    .build();

            PlaceDetails result2 = PlaceDetails.builder()
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
            PlaceDetails result = PlaceDetails.builder()
                    .placeId("test-place")
                    .name("Test Restaurant")
                    .formattedAddress("123 Test St")
                    .build();

            String toString = result.toString();

            assertThat(toString).contains("test-place");
            assertThat(toString).contains("Test Restaurant");
        }
    }

    @Nested
    @DisplayName("Price Level")
    class PriceLevel {

        @Test
        @DisplayName("should handle price level 0 (free)")
        void shouldHandlePriceLevelZero() {
            PlaceDetails result = PlaceDetails.builder()
                    .placeId("free-place")
                    .priceLevel(0)
                    .build();

            assertThat(result.getPriceLevel()).isZero();
        }

        @Test
        @DisplayName("should handle price level 4 (expensive)")
        void shouldHandlePriceLevelFour() {
            PlaceDetails result = PlaceDetails.builder()
                    .placeId("expensive-place")
                    .priceLevel(4)
                    .build();

            assertThat(result.getPriceLevel()).isEqualTo(4);
        }
    }
}
