package com.wego.controller.api;

import com.wego.dto.response.PlaceDetails;
import com.wego.dto.response.PlaceSearchResult;
import com.wego.service.CacheService;
import com.wego.service.RateLimitService;
import com.wego.service.external.GoogleMapsClient;
import com.wego.service.external.GoogleMapsException;
import com.wego.entity.User;
import com.wego.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for PlaceApiController.
 *
 * @contract
 *   - Tests all place search and details API endpoints
 *   - Verifies request validation
 *   - Tests error handling
 *   - Verifies authentication requirements
 */
@WebMvcTest(PlaceApiController.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class PlaceApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoogleMapsClient googleMapsClient;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private CacheService cacheService;

    private PlaceSearchResult testPlace1;
    private PlaceSearchResult testPlace2;
    private PlaceDetails testPlaceDetails;

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        User testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .nickname("Test User")
                .provider("test")
                .providerId("test-id")
                .build();
        userPrincipal = new UserPrincipal(testUser);

        testPlace1 = PlaceSearchResult.builder()
                .placeId("ChIJN1t_tDeuEmsRUsoyG83frY4")
                .name("Sydney Opera House")
                .address("Bennelong Point, Sydney NSW 2000")
                .latitude(-33.8568)
                .longitude(151.2153)
                .rating(4.7)
                .userRatingsTotal(12345)
                .types(Arrays.asList("tourist_attraction", "performing_arts_theater"))
                .photoReference("photo-reference-1")
                .isOpen(true)
                .build();

        testPlace2 = PlaceSearchResult.builder()
                .placeId("ChIJP3Sa8ziYEmsRUKgyFmh9AQM")
                .name("Sydney Harbour Bridge")
                .address("Sydney Harbour Bridge, Sydney NSW")
                .latitude(-33.8523)
                .longitude(151.2108)
                .rating(4.8)
                .userRatingsTotal(9876)
                .types(Arrays.asList("tourist_attraction", "point_of_interest"))
                .photoReference("photo-reference-2")
                .isOpen(null)
                .build();

        testPlaceDetails = PlaceDetails.builder()
                .placeId("ChIJN1t_tDeuEmsRUsoyG83frY4")
                .name("Sydney Opera House")
                .formattedAddress("Bennelong Point, Sydney NSW 2000, Australia")
                .formattedPhoneNumber("(02) 9250 7111")
                .internationalPhoneNumber("+61 2 9250 7111")
                .website("https://www.sydneyoperahouse.com")
                .url("https://maps.google.com/?cid=12345")
                .latitude(-33.8568)
                .longitude(151.2153)
                .rating(4.7)
                .userRatingsTotal(12345)
                .priceLevel(2)
                .types(Arrays.asList("tourist_attraction", "performing_arts_theater"))
                .photoReferences(Arrays.asList("photo-ref-1", "photo-ref-2"))
                .openingHours(PlaceDetails.OpeningHours.builder()
                        .isOpenNow(true)
                        .weekdayText(Arrays.asList(
                                "Monday: 9:00 AM - 5:00 PM",
                                "Tuesday: 9:00 AM - 5:00 PM"
                        ))
                        .build())
                .build();

        // Configure rate limit service to allow requests by default
        when(rateLimitService.isAllowed(anyString(), anyInt())).thenReturn(true);

        // Configure cache service to return empty (cache miss) by default
        when(cacheService.get(anyString(), any())).thenReturn(Optional.empty());
    }

    @Nested
    @DisplayName("GET /api/places/search")
    class SearchPlacesTests {

        @Test
        @DisplayName("should return places list with 200 when valid query provided")
        void searchPlaces_withValidQuery_shouldReturn200() throws Exception {
            // Given
            List<PlaceSearchResult> places = Arrays.asList(testPlace1, testPlace2);
            when(googleMapsClient.searchPlaces(eq("opera house"), eq(-33.8568), eq(151.2153), eq(5000)))
                    .thenReturn(places);

            // When & Then
            mockMvc.perform(get("/api/places/search")
                            .param("query", "opera house")
                            .param("lat", "-33.8568")
                            .param("lng", "151.2153")
                            .param("radius", "5000")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].placeId").value("ChIJN1t_tDeuEmsRUsoyG83frY4"))
                    .andExpect(jsonPath("$.data[0].name").value("Sydney Opera House"))
                    .andExpect(jsonPath("$.data[1].name").value("Sydney Harbour Bridge"));
        }

        @Test
        @DisplayName("should use default radius when not provided")
        void searchPlaces_withoutRadius_shouldUseDefaultRadius() throws Exception {
            // Given
            when(googleMapsClient.searchPlaces(eq("restaurant"), anyDouble(), anyDouble(), eq(1500)))
                    .thenReturn(Collections.singletonList(testPlace1));

            // When & Then
            mockMvc.perform(get("/api/places/search")
                            .param("query", "restaurant")
                            .param("lat", "-33.8568")
                            .param("lng", "151.2153")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("should return empty list when no places found")
        void searchPlaces_noResults_shouldReturnEmptyList() throws Exception {
            // Given
            when(googleMapsClient.searchPlaces(anyString(), anyDouble(), anyDouble(), anyInt()))
                    .thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/places/search")
                            .param("query", "nonexistent place xyz")
                            .param("lat", "-33.8568")
                            .param("lng", "151.2153")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("should return 400 when query is missing")
        void searchPlaces_missingQuery_shouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/places/search")
                            .param("lat", "-33.8568")
                            .param("lng", "151.2153")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when latitude is missing")
        void searchPlaces_missingLatitude_shouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/places/search")
                            .param("query", "restaurant")
                            .param("lng", "151.2153")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when longitude is missing")
        void searchPlaces_missingLongitude_shouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/places/search")
                            .param("query", "restaurant")
                            .param("lat", "-33.8568")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when latitude is out of range")
        void searchPlaces_invalidLatitude_shouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/places/search")
                            .param("query", "restaurant")
                            .param("lat", "91.0")
                            .param("lng", "151.2153")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when longitude is out of range")
        void searchPlaces_invalidLongitude_shouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/places/search")
                            .param("query", "restaurant")
                            .param("lat", "-33.8568")
                            .param("lng", "181.0")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when radius is negative")
        void searchPlaces_negativeRadius_shouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/places/search")
                            .param("query", "restaurant")
                            .param("lat", "-33.8568")
                            .param("lng", "151.2153")
                            .param("radius", "-100")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when radius exceeds maximum")
        void searchPlaces_radiusTooLarge_shouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/places/search")
                            .param("query", "restaurant")
                            .param("lat", "-33.8568")
                            .param("lng", "151.2153")
                            .param("radius", "60000")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 502 when Google Maps API fails")
        void searchPlaces_googleMapsApiError_shouldReturn502() throws Exception {
            // Given
            when(googleMapsClient.searchPlaces(anyString(), anyDouble(), anyDouble(), anyInt()))
                    .thenThrow(new GoogleMapsException("GOOGLE_MAPS_ERROR", "Google Maps API is unavailable"));

            // When & Then
            mockMvc.perform(get("/api/places/search")
                            .param("query", "restaurant")
                            .param("lat", "-33.8568")
                            .param("lng", "151.2153")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("GOOGLE_MAPS_ERROR"));
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void searchPlaces_notAuthenticated_shouldReturn403() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/places/search")
                            .param("query", "restaurant")
                            .param("lat", "-33.8568")
                            .param("lng", "151.2153"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/places/{placeId}")
    class GetPlaceDetailsTests {

        @Test
        @DisplayName("should return place details with 200")
        void getPlaceDetails_withValidPlaceId_shouldReturn200() throws Exception {
            // Given
            when(googleMapsClient.getPlaceDetails("ChIJN1t_tDeuEmsRUsoyG83frY4"))
                    .thenReturn(testPlaceDetails);

            // When & Then
            mockMvc.perform(get("/api/places/{placeId}", "ChIJN1t_tDeuEmsRUsoyG83frY4")
                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.placeId").value("ChIJN1t_tDeuEmsRUsoyG83frY4"))
                    .andExpect(jsonPath("$.data.name").value("Sydney Opera House"))
                    .andExpect(jsonPath("$.data.formattedAddress").value("Bennelong Point, Sydney NSW 2000, Australia"))
                    .andExpect(jsonPath("$.data.internationalPhoneNumber").value("+61 2 9250 7111"))
                    .andExpect(jsonPath("$.data.website").value("https://www.sydneyoperahouse.com"))
                    .andExpect(jsonPath("$.data.rating").value(4.7))
                    .andExpect(jsonPath("$.data.openingHours.isOpenNow").value(true));
        }

        @Test
        @DisplayName("should return 404 when place not found")
        void getPlaceDetails_placeNotFound_shouldReturn404() throws Exception {
            // Given
            when(googleMapsClient.getPlaceDetails("invalid-place-id"))
                    .thenThrow(new GoogleMapsException("PLACE_NOT_FOUND", "Place not found"));

            // When & Then
            mockMvc.perform(get("/api/places/{placeId}", "invalid-place-id")
                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("PLACE_NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 400 when placeId is empty")
        void getPlaceDetails_emptyPlaceId_shouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/places/{placeId}", " ")
                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 502 when Google Maps API fails")
        void getPlaceDetails_googleMapsApiError_shouldReturn502() throws Exception {
            // Given
            when(googleMapsClient.getPlaceDetails(anyString()))
                    .thenThrow(new GoogleMapsException("GOOGLE_MAPS_ERROR", "Google Maps API is unavailable"));

            // When & Then
            mockMvc.perform(get("/api/places/{placeId}", "ChIJN1t_tDeuEmsRUsoyG83frY4")
                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("GOOGLE_MAPS_ERROR"));
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void getPlaceDetails_notAuthenticated_shouldReturn403() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/places/{placeId}", "ChIJN1t_tDeuEmsRUsoyG83frY4"))
                    .andExpect(status().isForbidden());
        }
    }
}
