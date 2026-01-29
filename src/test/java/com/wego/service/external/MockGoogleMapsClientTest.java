package com.wego.service.external;

import com.wego.dto.response.DirectionResult;
import com.wego.dto.response.PlaceDetails;
import com.wego.dto.response.PlaceSearchResult;
import com.wego.entity.TransportMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for MockGoogleMapsClient.
 *
 * Tests the mock implementation that uses Haversine distance
 * calculation and returns simulated data.
 *
 * @see MockGoogleMapsClient
 */
@DisplayName("MockGoogleMapsClient")
class MockGoogleMapsClientTest {

    private MockGoogleMapsClient client;

    // Tokyo Station coordinates
    private static final double TOKYO_LAT = 35.6812;
    private static final double TOKYO_LNG = 139.7671;

    // Shibuya Station coordinates (approximately 5km from Tokyo Station)
    private static final double SHIBUYA_LAT = 35.6580;
    private static final double SHIBUYA_LNG = 139.7016;

    // New York coordinates (very far from Tokyo)
    private static final double NY_LAT = 40.7128;
    private static final double NY_LNG = -74.0060;

    @BeforeEach
    void setUp() {
        client = new MockGoogleMapsClient();
    }

    @Nested
    @DisplayName("getDirections with coordinates")
    class GetDirectionsWithCoordinates {

        @Test
        @DisplayName("should return direction result for nearby locations")
        void shouldReturnDirectionResultForNearbyLocations() {
            DirectionResult result = client.getDirections(
                    TOKYO_LAT, TOKYO_LNG,
                    SHIBUYA_LAT, SHIBUYA_LNG,
                    TransportMode.WALKING
            );

            assertThat(result).isNotNull();
            assertThat(result.getDistanceMeters()).isGreaterThan(0);
            assertThat(result.getDurationSeconds()).isGreaterThan(0);
            assertThat(result.getTransportMode()).isEqualTo(TransportMode.WALKING);
        }

        @Test
        @DisplayName("should calculate distance using Haversine formula")
        void shouldCalculateDistanceUsingHaversineFormula() {
            // Tokyo to Shibuya is approximately 5-6 km
            DirectionResult result = client.getDirections(
                    TOKYO_LAT, TOKYO_LNG,
                    SHIBUYA_LAT, SHIBUYA_LNG,
                    TransportMode.DRIVING
            );

            // Distance should be between 4000m and 7000m
            assertThat(result.getDistanceMeters()).isBetween(4000, 7000);
        }

        @Test
        @DisplayName("should calculate different durations for different transport modes")
        void shouldCalculateDifferentDurationsForDifferentModes() {
            DirectionResult walkingResult = client.getDirections(
                    TOKYO_LAT, TOKYO_LNG,
                    SHIBUYA_LAT, SHIBUYA_LNG,
                    TransportMode.WALKING
            );

            DirectionResult drivingResult = client.getDirections(
                    TOKYO_LAT, TOKYO_LNG,
                    SHIBUYA_LAT, SHIBUYA_LNG,
                    TransportMode.DRIVING
            );

            // Walking should take longer than driving
            assertThat(walkingResult.getDurationSeconds())
                    .isGreaterThan(drivingResult.getDurationSeconds());
        }

        @Test
        @DisplayName("should include origin and destination in result")
        void shouldIncludeOriginAndDestinationInResult() {
            DirectionResult result = client.getDirections(
                    TOKYO_LAT, TOKYO_LNG,
                    SHIBUYA_LAT, SHIBUYA_LNG,
                    TransportMode.TRANSIT
            );

            assertThat(result.getOriginAddress()).isNotNull();
            assertThat(result.getDestinationAddress()).isNotNull();
        }

        @Test
        @DisplayName("should return zero distance for same location")
        void shouldReturnZeroDistanceForSameLocation() {
            DirectionResult result = client.getDirections(
                    TOKYO_LAT, TOKYO_LNG,
                    TOKYO_LAT, TOKYO_LNG,
                    TransportMode.WALKING
            );

            assertThat(result.getDistanceMeters()).isZero();
            assertThat(result.getDurationSeconds()).isZero();
        }

        @Test
        @DisplayName("should handle very long distances")
        void shouldHandleVeryLongDistances() {
            // Tokyo to New York
            DirectionResult result = client.getDirections(
                    TOKYO_LAT, TOKYO_LNG,
                    NY_LAT, NY_LNG,
                    TransportMode.DRIVING
            );

            // Distance should be approximately 10,000-11,000 km
            assertThat(result.getDistanceMeters()).isGreaterThan(10_000_000);
        }

        @Test
        @DisplayName("should include distance and duration text")
        void shouldIncludeDistanceAndDurationText() {
            DirectionResult result = client.getDirections(
                    TOKYO_LAT, TOKYO_LNG,
                    SHIBUYA_LAT, SHIBUYA_LNG,
                    TransportMode.WALKING
            );

            assertThat(result.getDistanceText()).isNotNull();
            assertThat(result.getDistanceText()).isNotEmpty();
            assertThat(result.getDurationText()).isNotNull();
            assertThat(result.getDurationText()).isNotEmpty();
        }

        @Test
        @DisplayName("should support all transport modes")
        void shouldSupportAllTransportModes() {
            for (TransportMode mode : TransportMode.values()) {
                DirectionResult result = client.getDirections(
                        TOKYO_LAT, TOKYO_LNG,
                        SHIBUYA_LAT, SHIBUYA_LNG,
                        mode
                );

                assertThat(result).isNotNull();
                assertThat(result.getTransportMode()).isEqualTo(mode);
            }
        }
    }

    @Nested
    @DisplayName("getDirections with address strings")
    class GetDirectionsWithAddresses {

        @Test
        @DisplayName("should return direction result for address strings")
        void shouldReturnDirectionResultForAddressStrings() {
            DirectionResult result = client.getDirections(
                    "Tokyo Station",
                    "Shibuya Station",
                    TransportMode.TRANSIT
            );

            assertThat(result).isNotNull();
            assertThat(result.getDistanceMeters()).isGreaterThan(0);
            assertThat(result.getDurationSeconds()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should use origin and destination as addresses")
        void shouldUseOriginAndDestinationAsAddresses() {
            DirectionResult result = client.getDirections(
                    "Tokyo Station",
                    "Shibuya Station",
                    TransportMode.DRIVING
            );

            assertThat(result.getOriginAddress()).contains("Tokyo Station");
            assertThat(result.getDestinationAddress()).contains("Shibuya Station");
        }

        @Test
        @DisplayName("should throw for null origin")
        void shouldThrowForNullOrigin() {
            assertThatThrownBy(() ->
                    client.getDirections(null, "Destination", TransportMode.WALKING)
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw for null destination")
        void shouldThrowForNullDestination() {
            assertThatThrownBy(() ->
                    client.getDirections("Origin", null, TransportMode.WALKING)
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw for empty origin")
        void shouldThrowForEmptyOrigin() {
            assertThatThrownBy(() ->
                    client.getDirections("", "Destination", TransportMode.WALKING)
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw for empty destination")
        void shouldThrowForEmptyDestination() {
            assertThatThrownBy(() ->
                    client.getDirections("Origin", "", TransportMode.WALKING)
            ).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("searchPlaces")
    class SearchPlaces {

        @Test
        @DisplayName("should return list of places for query")
        void shouldReturnListOfPlacesForQuery() {
            List<PlaceSearchResult> results = client.searchPlaces(
                    "restaurant",
                    TOKYO_LAT,
                    TOKYO_LNG,
                    1000
            );

            assertThat(results).isNotNull();
            assertThat(results).isNotEmpty();
        }

        @Test
        @DisplayName("should return places with required fields")
        void shouldReturnPlacesWithRequiredFields() {
            List<PlaceSearchResult> results = client.searchPlaces(
                    "cafe",
                    TOKYO_LAT,
                    TOKYO_LNG,
                    500
            );

            for (PlaceSearchResult place : results) {
                assertThat(place.getPlaceId()).isNotNull().isNotEmpty();
                assertThat(place.getName()).isNotNull().isNotEmpty();
                assertThat(place.getLatitude()).isNotZero();
                assertThat(place.getLongitude()).isNotZero();
            }
        }

        @Test
        @DisplayName("should return places within radius")
        void shouldReturnPlacesWithinRadius() {
            int radiusMeters = 1000;
            List<PlaceSearchResult> results = client.searchPlaces(
                    "restaurant",
                    TOKYO_LAT,
                    TOKYO_LNG,
                    radiusMeters
            );

            for (PlaceSearchResult place : results) {
                double distance = calculateHaversineDistance(
                        TOKYO_LAT, TOKYO_LNG,
                        place.getLatitude(), place.getLongitude()
                );
                assertThat(distance).isLessThanOrEqualTo(radiusMeters);
            }
        }

        @Test
        @DisplayName("should include rating and user ratings total")
        void shouldIncludeRatingAndUserRatingsTotal() {
            List<PlaceSearchResult> results = client.searchPlaces(
                    "hotel",
                    TOKYO_LAT,
                    TOKYO_LNG,
                    2000
            );

            for (PlaceSearchResult place : results) {
                assertThat(place.getRating()).isBetween(0.0, 5.0);
                assertThat(place.getUserRatingsTotal()).isGreaterThanOrEqualTo(0);
            }
        }

        @Test
        @DisplayName("should throw for null query")
        void shouldThrowForNullQuery() {
            assertThatThrownBy(() ->
                    client.searchPlaces(null, TOKYO_LAT, TOKYO_LNG, 1000)
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw for empty query")
        void shouldThrowForEmptyQuery() {
            assertThatThrownBy(() ->
                    client.searchPlaces("", TOKYO_LAT, TOKYO_LNG, 1000)
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw for invalid radius")
        void shouldThrowForInvalidRadius() {
            assertThatThrownBy(() ->
                    client.searchPlaces("cafe", TOKYO_LAT, TOKYO_LNG, 0)
            ).isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() ->
                    client.searchPlaces("cafe", TOKYO_LAT, TOKYO_LNG, -100)
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should limit results to reasonable number")
        void shouldLimitResultsToReasonableNumber() {
            List<PlaceSearchResult> results = client.searchPlaces(
                    "restaurant",
                    TOKYO_LAT,
                    TOKYO_LNG,
                    50000  // Large radius
            );

            // Should not return excessive results
            assertThat(results.size()).isLessThanOrEqualTo(20);
        }
    }

    @Nested
    @DisplayName("getPlaceDetails")
    class GetPlaceDetails {

        @Test
        @DisplayName("should return place details for valid placeId")
        void shouldReturnPlaceDetailsForValidPlaceId() {
            PlaceDetails details = client.getPlaceDetails("mock-place-id-123");

            assertThat(details).isNotNull();
            assertThat(details.getPlaceId()).isEqualTo("mock-place-id-123");
        }

        @Test
        @DisplayName("should return details with required fields")
        void shouldReturnDetailsWithRequiredFields() {
            PlaceDetails details = client.getPlaceDetails("test-place");

            assertThat(details.getPlaceId()).isNotNull();
            assertThat(details.getName()).isNotNull().isNotEmpty();
            assertThat(details.getFormattedAddress()).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("should return details with optional fields")
        void shouldReturnDetailsWithOptionalFields() {
            PlaceDetails details = client.getPlaceDetails("detailed-place");

            assertThat(details.getRating()).isBetween(0.0, 5.0);
            assertThat(details.getUserRatingsTotal()).isGreaterThanOrEqualTo(0);
            assertThat(details.getTypes()).isNotNull();
        }

        @Test
        @DisplayName("should include opening hours")
        void shouldIncludeOpeningHours() {
            PlaceDetails details = client.getPlaceDetails("place-with-hours");

            assertThat(details.getOpeningHours()).isNotNull();
        }

        @Test
        @DisplayName("should include reviews")
        void shouldIncludeReviews() {
            PlaceDetails details = client.getPlaceDetails("place-with-reviews");

            assertThat(details.getReviews()).isNotNull();
        }

        @Test
        @DisplayName("should throw for null placeId")
        void shouldThrowForNullPlaceId() {
            assertThatThrownBy(() ->
                    client.getPlaceDetails(null)
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw for empty placeId")
        void shouldThrowForEmptyPlaceId() {
            assertThatThrownBy(() ->
                    client.getPlaceDetails("")
            ).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Transport Mode Speed Calculations")
    class TransportModeSpeeds {

        @Test
        @DisplayName("WALKING should be approximately 5 km/h")
        void walkingShouldBeApproximately5KmH() {
            // 5 km distance
            DirectionResult result = client.getDirections(
                    TOKYO_LAT, TOKYO_LNG,
                    SHIBUYA_LAT, SHIBUYA_LNG,
                    TransportMode.WALKING
            );

            double distanceKm = result.getDistanceMeters() / 1000.0;
            double durationHours = result.getDurationSeconds() / 3600.0;
            double speedKmH = distanceKm / durationHours;

            // Walking speed should be around 4-6 km/h
            assertThat(speedKmH).isBetween(4.0, 6.0);
        }

        @Test
        @DisplayName("BICYCLING should be approximately 15 km/h")
        void bicyclingShouldBeApproximately15KmH() {
            DirectionResult result = client.getDirections(
                    TOKYO_LAT, TOKYO_LNG,
                    SHIBUYA_LAT, SHIBUYA_LNG,
                    TransportMode.BICYCLING
            );

            double distanceKm = result.getDistanceMeters() / 1000.0;
            double durationHours = result.getDurationSeconds() / 3600.0;
            double speedKmH = distanceKm / durationHours;

            // Bicycling speed should be around 12-18 km/h
            assertThat(speedKmH).isBetween(12.0, 18.0);
        }

        @Test
        @DisplayName("TRANSIT should be approximately 30 km/h")
        void transitShouldBeApproximately30KmH() {
            DirectionResult result = client.getDirections(
                    TOKYO_LAT, TOKYO_LNG,
                    SHIBUYA_LAT, SHIBUYA_LNG,
                    TransportMode.TRANSIT
            );

            double distanceKm = result.getDistanceMeters() / 1000.0;
            double durationHours = result.getDurationSeconds() / 3600.0;
            double speedKmH = distanceKm / durationHours;

            // Transit speed should be around 25-35 km/h
            assertThat(speedKmH).isBetween(25.0, 35.0);
        }

        @Test
        @DisplayName("DRIVING should be approximately 40 km/h")
        void drivingShouldBeApproximately40KmH() {
            DirectionResult result = client.getDirections(
                    TOKYO_LAT, TOKYO_LNG,
                    SHIBUYA_LAT, SHIBUYA_LNG,
                    TransportMode.DRIVING
            );

            double distanceKm = result.getDistanceMeters() / 1000.0;
            double durationHours = result.getDurationSeconds() / 3600.0;
            double speedKmH = distanceKm / durationHours;

            // Driving speed should be around 35-50 km/h (urban average)
            assertThat(speedKmH).isBetween(35.0, 50.0);
        }
    }

    /**
     * Helper method to calculate Haversine distance.
     */
    private double calculateHaversineDistance(
            double lat1, double lon1,
            double lat2, double lon2
    ) {
        final double R = 6371000; // Earth radius in meters

        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
