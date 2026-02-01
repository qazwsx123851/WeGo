package com.wego.service.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.config.GoogleMapsProperties;
import com.wego.dto.response.DirectionResult;
import com.wego.dto.response.PlaceDetails;
import com.wego.dto.response.PlaceSearchResult;
import com.wego.entity.TransportMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GoogleMapsClientImpl.
 *
 * Tests the real implementation that calls Google Maps APIs.
 * Uses mocked RestTemplate to avoid actual API calls.
 *
 * @see GoogleMapsClientImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleMapsClientImpl")
class GoogleMapsClientImplTest {

    @Mock
    private RestTemplate restTemplate;

    private GoogleMapsClientImpl client;
    private GoogleMapsProperties properties;

    private static final String TEST_API_KEY = "test-api-key-12345";

    // Sample API responses
    private static final String DISTANCE_MATRIX_SUCCESS_RESPONSE = """
            {
                "status": "OK",
                "origin_addresses": ["Tokyo Station, Japan"],
                "destination_addresses": ["Shibuya Station, Japan"],
                "rows": [{
                    "elements": [{
                        "status": "OK",
                        "distance": {
                            "value": 5400,
                            "text": "5.4 km"
                        },
                        "duration": {
                            "value": 900,
                            "text": "15 mins"
                        }
                    }]
                }]
            }
            """;

    // Places API (New) format
    private static final String PLACES_SEARCH_SUCCESS_RESPONSE = """
            {
                "places": [{
                    "id": "ChIJN1t_tDeuEmsRUsoyG83frY4",
                    "displayName": {
                        "text": "Tokyo Tower"
                    },
                    "formattedAddress": "4-2-8 Shibakoen, Minato City",
                    "location": {
                        "latitude": 35.6585805,
                        "longitude": 139.7454329
                    },
                    "rating": 4.5,
                    "userRatingCount": 12345,
                    "types": ["tourist_attraction", "point_of_interest"],
                    "photos": [{
                        "name": "places/ChIJN1t_tDeuEmsRUsoyG83frY4/photos/AWU5eFhqX8Y"
                    }],
                    "regularOpeningHours": {
                        "openNow": true
                    }
                }]
            }
            """;

    // Places API (New) format - no wrapper, direct place object
    private static final String PLACE_DETAILS_SUCCESS_RESPONSE = """
            {
                "id": "ChIJN1t_tDeuEmsRUsoyG83frY4",
                "displayName": {
                    "text": "Tokyo Tower"
                },
                "formattedAddress": "4-2-8 Shibakoen, Minato City, Tokyo 105-0011",
                "nationalPhoneNumber": "03-3433-5111",
                "internationalPhoneNumber": "+81 3-3433-5111",
                "websiteUri": "https://www.tokyotower.co.jp",
                "googleMapsUri": "https://maps.google.com/?cid=12345",
                "location": {
                    "latitude": 35.6585805,
                    "longitude": 139.7454329
                },
                "rating": 4.5,
                "userRatingCount": 12345,
                "priceLevel": "PRICE_LEVEL_MODERATE",
                "types": ["tourist_attraction", "point_of_interest"],
                "photos": [{
                    "name": "places/ChIJN1t_tDeuEmsRUsoyG83frY4/photos/photo1"
                }, {
                    "name": "places/ChIJN1t_tDeuEmsRUsoyG83frY4/photos/photo2"
                }],
                "reviews": [{
                    "authorAttribution": {
                        "displayName": "John Doe"
                    },
                    "rating": 5,
                    "text": {
                        "text": "Amazing view!"
                    },
                    "relativePublishTimeDescription": "a week ago"
                }],
                "regularOpeningHours": {
                    "openNow": true,
                    "weekdayDescriptions": [
                        "Monday: 9:00 AM - 11:00 PM",
                        "Tuesday: 9:00 AM - 11:00 PM"
                    ]
                },
                "utcOffsetMinutes": 540
            }
            """;

    @BeforeEach
    void setUp() {
        properties = new GoogleMapsProperties();
        properties.setApiKey(TEST_API_KEY);
        properties.setEnabled(true);
        properties.setConnectTimeoutMs(5000);
        properties.setReadTimeoutMs(10000);

        client = new GoogleMapsClientImpl(properties, restTemplate);
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("should create client with valid properties")
        void shouldCreateClientWithValidProperties() {
            GoogleMapsClientImpl newClient = new GoogleMapsClientImpl(properties, restTemplate);

            assertThat(newClient).isNotNull();
        }

        @Test
        @DisplayName("should throw when properties is null")
        void shouldThrowWhenPropertiesIsNull() {
            assertThatThrownBy(() -> new GoogleMapsClientImpl(null, restTemplate))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("getDirections with coordinates")
    class GetDirectionsWithCoordinates {

        @Test
        @DisplayName("should return direction result on success")
        void shouldReturnDirectionResultOnSuccess() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(DISTANCE_MATRIX_SUCCESS_RESPONSE, HttpStatus.OK));

            DirectionResult result = client.getDirections(
                    35.6812, 139.7671,
                    35.6580, 139.7016,
                    TransportMode.TRANSIT
            );

            assertThat(result).isNotNull();
            assertThat(result.getDistanceMeters()).isEqualTo(5400);
            assertThat(result.getDurationSeconds()).isEqualTo(900);
            assertThat(result.getDistanceText()).isEqualTo("5.4 km");
            assertThat(result.getDurationText()).isEqualTo("15 mins");
            assertThat(result.getTransportMode()).isEqualTo(TransportMode.TRANSIT);
        }

        @Test
        @DisplayName("should include origin and destination addresses")
        void shouldIncludeOriginAndDestinationAddresses() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(DISTANCE_MATRIX_SUCCESS_RESPONSE, HttpStatus.OK));

            DirectionResult result = client.getDirections(
                    35.6812, 139.7671,
                    35.6580, 139.7016,
                    TransportMode.DRIVING
            );

            assertThat(result.getOriginAddress()).contains("Tokyo Station");
            assertThat(result.getDestinationAddress()).contains("Shibuya Station");
        }

        @Test
        @DisplayName("should throw GoogleMapsException on API error")
        void shouldThrowGoogleMapsExceptionOnApiError() {
            String errorResponse = """
                    {
                        "status": "REQUEST_DENIED",
                        "error_message": "API key is invalid"
                    }
                    """;

            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(errorResponse, HttpStatus.OK));

            assertThatThrownBy(() -> client.getDirections(
                    35.6812, 139.7671,
                    35.6580, 139.7016,
                    TransportMode.WALKING
            ))
                    .isInstanceOf(GoogleMapsException.class)
                    .hasMessageContaining("API");
        }

        @Test
        @DisplayName("should throw GoogleMapsException on HTTP error")
        void shouldThrowGoogleMapsExceptionOnHttpError() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

            assertThatThrownBy(() -> client.getDirections(
                    35.6812, 139.7671,
                    35.6580, 139.7016,
                    TransportMode.TRANSIT
            ))
                    .isInstanceOf(GoogleMapsException.class);
        }

        @Test
        @DisplayName("should handle ZERO_RESULTS status")
        void shouldHandleZeroResultsStatus() {
            String zeroResultsResponse = """
                    {
                        "status": "OK",
                        "origin_addresses": ["Unknown"],
                        "destination_addresses": ["Unknown"],
                        "rows": [{
                            "elements": [{
                                "status": "ZERO_RESULTS"
                            }]
                        }]
                    }
                    """;

            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(zeroResultsResponse, HttpStatus.OK));

            assertThatThrownBy(() -> client.getDirections(
                    0.0, 0.0,
                    0.0, 0.0,
                    TransportMode.DRIVING
            ))
                    .isInstanceOf(GoogleMapsException.class)
                    .hasMessageContaining("No route found");
        }
    }

    @Nested
    @DisplayName("getDirections with addresses")
    class GetDirectionsWithAddresses {

        @Test
        @DisplayName("should return direction result for address strings")
        void shouldReturnDirectionResultForAddressStrings() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(DISTANCE_MATRIX_SUCCESS_RESPONSE, HttpStatus.OK));

            DirectionResult result = client.getDirections(
                    "Tokyo Station",
                    "Shibuya Station",
                    TransportMode.TRANSIT
            );

            assertThat(result).isNotNull();
            assertThat(result.getDistanceMeters()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should throw for null origin")
        void shouldThrowForNullOrigin() {
            assertThatThrownBy(() -> client.getDirections(
                    null, "Destination", TransportMode.WALKING
            ))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw for null destination")
        void shouldThrowForNullDestination() {
            assertThatThrownBy(() -> client.getDirections(
                    "Origin", null, TransportMode.WALKING
            ))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("searchPlaces")
    class SearchPlaces {

        @Test
        @DisplayName("should return list of places on success")
        void shouldReturnListOfPlacesOnSuccess() {
            // Places API (New) uses POST with exchange()
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(PLACES_SEARCH_SUCCESS_RESPONSE, HttpStatus.OK));

            List<PlaceSearchResult> results = client.searchPlaces(
                    "restaurant",
                    35.6812, 139.7671,
                    1000
            );

            assertThat(results).isNotNull();
            assertThat(results).hasSize(1);

            PlaceSearchResult place = results.get(0);
            assertThat(place.getPlaceId()).isEqualTo("ChIJN1t_tDeuEmsRUsoyG83frY4");
            assertThat(place.getName()).isEqualTo("Tokyo Tower");
            assertThat(place.getRating()).isEqualTo(4.5);
        }

        @Test
        @DisplayName("should include coordinates in results")
        void shouldIncludeCoordinatesInResults() {
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(PLACES_SEARCH_SUCCESS_RESPONSE, HttpStatus.OK));

            List<PlaceSearchResult> results = client.searchPlaces(
                    "cafe",
                    35.6812, 139.7671,
                    500
            );

            PlaceSearchResult place = results.get(0);
            assertThat(place.getLatitude()).isEqualTo(35.6585805);
            assertThat(place.getLongitude()).isEqualTo(139.7454329);
        }

        @Test
        @DisplayName("should return empty list for ZERO_RESULTS")
        void shouldReturnEmptyListForZeroResults() {
            // Places API (New) returns empty places array, no status field
            String zeroResultsResponse = """
                    {
                        "places": []
                    }
                    """;

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(zeroResultsResponse, HttpStatus.OK));

            List<PlaceSearchResult> results = client.searchPlaces(
                    "nonexistent",
                    35.6812, 139.7671,
                    100
            );

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should throw GoogleMapsException on API error")
        void shouldThrowGoogleMapsExceptionOnApiError() {
            // Places API (New) returns error in different format
            String errorResponse = """
                    {
                        "error": {
                            "status": "RESOURCE_EXHAUSTED",
                            "message": "You have exceeded your daily request quota"
                        }
                    }
                    """;

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(errorResponse, HttpStatus.OK));

            assertThatThrownBy(() -> client.searchPlaces(
                    "restaurant",
                    35.6812, 139.7671,
                    1000
            ))
                    .isInstanceOf(GoogleMapsException.class);
        }

        @Test
        @DisplayName("should throw for invalid radius")
        void shouldThrowForInvalidRadius() {
            assertThatThrownBy(() -> client.searchPlaces(
                    "cafe",
                    35.6812, 139.7671,
                    0
            ))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("getPlaceDetails")
    class GetPlaceDetails {

        @Test
        @DisplayName("should return place details on success")
        void shouldReturnPlaceDetailsOnSuccess() {
            // Places API (New) uses GET with exchange()
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(PLACE_DETAILS_SUCCESS_RESPONSE, HttpStatus.OK));

            PlaceDetails details = client.getPlaceDetails("ChIJN1t_tDeuEmsRUsoyG83frY4");

            assertThat(details).isNotNull();
            assertThat(details.getPlaceId()).isEqualTo("ChIJN1t_tDeuEmsRUsoyG83frY4");
            assertThat(details.getName()).isEqualTo("Tokyo Tower");
            assertThat(details.getFormattedAddress()).contains("Shibakoen");
        }

        @Test
        @DisplayName("should include contact information")
        void shouldIncludeContactInformation() {
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(PLACE_DETAILS_SUCCESS_RESPONSE, HttpStatus.OK));

            PlaceDetails details = client.getPlaceDetails("test-place-id");

            assertThat(details.getFormattedPhoneNumber()).isEqualTo("03-3433-5111");
            assertThat(details.getInternationalPhoneNumber()).isEqualTo("+81 3-3433-5111");
            assertThat(details.getWebsite()).isEqualTo("https://www.tokyotower.co.jp");
        }

        @Test
        @DisplayName("should include reviews")
        void shouldIncludeReviews() {
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(PLACE_DETAILS_SUCCESS_RESPONSE, HttpStatus.OK));

            PlaceDetails details = client.getPlaceDetails("test-place-id");

            assertThat(details.getReviews()).isNotNull();
            assertThat(details.getReviews()).hasSize(1);

            PlaceDetails.Review review = details.getReviews().get(0);
            assertThat(review.getAuthorName()).isEqualTo("John Doe");
            assertThat(review.getRating()).isEqualTo(5);
            assertThat(review.getText()).isEqualTo("Amazing view!");
        }

        @Test
        @DisplayName("should include opening hours")
        void shouldIncludeOpeningHours() {
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(PLACE_DETAILS_SUCCESS_RESPONSE, HttpStatus.OK));

            PlaceDetails details = client.getPlaceDetails("test-place-id");

            assertThat(details.getOpeningHours()).isNotNull();
            assertThat(details.getOpeningHours().getIsOpenNow()).isTrue();
            assertThat(details.getOpeningHours().getWeekdayText()).hasSize(2);
        }

        @Test
        @DisplayName("should throw for NOT_FOUND status")
        void shouldThrowForNotFoundStatus() {
            // Places API (New) returns error in different format
            String notFoundResponse = """
                    {
                        "error": {
                            "status": "NOT_FOUND",
                            "message": "Place not found"
                        }
                    }
                    """;

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(notFoundResponse, HttpStatus.OK));

            assertThatThrownBy(() -> client.getPlaceDetails("invalid-place-id"))
                    .isInstanceOf(GoogleMapsException.class);
        }

        @Test
        @DisplayName("should throw for null placeId")
        void shouldThrowForNullPlaceId() {
            assertThatThrownBy(() -> client.getPlaceDetails(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw for empty placeId")
        void shouldThrowForEmptyPlaceId() {
            assertThatThrownBy(() -> client.getPlaceDetails(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("API Key Handling")
    class ApiKeyHandling {

        @Test
        @DisplayName("should include API key in request URL")
        void shouldIncludeApiKeyInRequestUrl() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenAnswer(invocation -> {
                        String url = invocation.getArgument(0);
                        assertThat(url).contains("key=" + TEST_API_KEY);
                        return new ResponseEntity<>(DISTANCE_MATRIX_SUCCESS_RESPONSE, HttpStatus.OK);
                    });

            client.getDirections(35.0, 139.0, 35.1, 139.1, TransportMode.DRIVING);
        }

        @Test
        @DisplayName("should handle INVALID_KEY response")
        void shouldHandleInvalidKeyResponse() {
            String invalidKeyResponse = """
                    {
                        "status": "REQUEST_DENIED",
                        "error_message": "The provided API key is invalid."
                    }
                    """;

            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(invalidKeyResponse, HttpStatus.OK));

            assertThatThrownBy(() -> client.getDirections(
                    35.0, 139.0, 35.1, 139.1, TransportMode.WALKING
            ))
                    .isInstanceOf(GoogleMapsException.class);
        }
    }

    @Nested
    @DisplayName("Rate Limiting")
    class RateLimiting {

        @Test
        @DisplayName("should handle OVER_QUERY_LIMIT status")
        void shouldHandleOverQueryLimitStatus() {
            // Places API (New) returns error in different format
            String rateLimitResponse = """
                    {
                        "error": {
                            "status": "RESOURCE_EXHAUSTED",
                            "message": "You have exceeded your rate-limit for this API."
                        }
                    }
                    """;

            // searchPlaces uses POST with exchange()
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(rateLimitResponse, HttpStatus.OK));

            assertThatThrownBy(() -> client.searchPlaces(
                    "test", 35.0, 139.0, 1000
            ))
                    .isInstanceOf(GoogleMapsException.class);
        }
    }
}
