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

/**
 * Unit tests for MockGoogleMapsClient.
 *
 * Smoke tests for core mock functionality only.
 * Detailed Haversine/speed validation removed as test infrastructure.
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

    @BeforeEach
    void setUp() {
        client = new MockGoogleMapsClient();
    }

    @Nested
    @DisplayName("getDirections")
    class GetDirections {

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
    }
}
