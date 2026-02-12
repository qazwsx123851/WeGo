package com.wego.controller.api;

import com.wego.dto.response.DirectionResult;
import com.wego.entity.TransportMode;
import com.wego.service.RateLimitService;
import com.wego.service.external.GoogleMapsClient;
import com.wego.service.external.GoogleMapsException;
import com.wego.entity.User;
import com.wego.security.UserPrincipal;

import java.util.UUID;

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
 * Tests for DirectionApiController.
 *
 * @contract
 *   - Tests all direction API endpoints
 *   - Verifies request validation
 *   - Tests error handling
 *   - Verifies authentication requirements
 */
@WebMvcTest(DirectionApiController.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class DirectionApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoogleMapsClient googleMapsClient;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private org.springframework.cache.CacheManager cacheManager;

    private DirectionResult testDirectionResult;

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

        testDirectionResult = DirectionResult.builder()
                .originAddress("Sydney Opera House, Bennelong Point, Sydney NSW 2000")
                .destinationAddress("Sydney Harbour Bridge, Sydney NSW")
                .distanceMeters(2500)
                .distanceText("2.5 km")
                .durationSeconds(1800)
                .durationText("30 mins")
                .transportMode(TransportMode.WALKING)
                .build();

        // Configure rate limit service to allow requests by default
        when(rateLimitService.isAllowed(anyString(), anyInt())).thenReturn(true);

        // CacheManager mock returns null for getCache() by default = cache miss
    }

    @Nested
    @DisplayName("GET /api/directions")
    class GetDirectionsTests {

        @Test
        @DisplayName("should return direction result with 200 when valid coordinates provided")
        void getDirections_withValidCoordinates_shouldReturn200() throws Exception {
            // Given
            when(googleMapsClient.getDirections(
                    eq(-33.8568), eq(151.2153),
                    eq(-33.8523), eq(151.2108),
                    eq(TransportMode.WALKING)))
                    .thenReturn(testDirectionResult);

            // When & Then
            mockMvc.perform(get("/api/directions")
                            .param("originLat", "-33.8568")
                            .param("originLng", "151.2153")
                            .param("destLat", "-33.8523")
                            .param("destLng", "151.2108")
                            .param("mode", "WALKING")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.distanceMeters").value(2500))
                    .andExpect(jsonPath("$.data.distanceText").value("2.5 km"))
                    .andExpect(jsonPath("$.data.durationSeconds").value(1800))
                    .andExpect(jsonPath("$.data.durationText").value("30 mins"))
                    .andExpect(jsonPath("$.data.transportMode").value("WALKING"));
        }

        @Test
        @DisplayName("should use default mode DRIVING when not provided")
        void getDirections_withoutMode_shouldUseDriving() throws Exception {
            // Given
            DirectionResult drivingResult = DirectionResult.builder()
                    .originAddress("Origin")
                    .destinationAddress("Destination")
                    .distanceMeters(5000)
                    .distanceText("5 km")
                    .durationSeconds(600)
                    .durationText("10 mins")
                    .transportMode(TransportMode.DRIVING)
                    .build();

            when(googleMapsClient.getDirections(
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    eq(TransportMode.DRIVING)))
                    .thenReturn(drivingResult);

            // When & Then
            mockMvc.perform(get("/api/directions")
                            .param("originLat", "-33.8568")
                            .param("originLng", "151.2153")
                            .param("destLat", "-33.8523")
                            .param("destLng", "151.2108")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.transportMode").value("DRIVING"));
        }

        @Test
        @DisplayName("should return 200 with all transport modes")
        void getDirections_withDifferentModes_shouldReturn200() throws Exception {
            // Given
            for (TransportMode mode : TransportMode.values()) {
                DirectionResult result = DirectionResult.builder()
                        .originAddress("Origin")
                        .destinationAddress("Destination")
                        .distanceMeters(1000)
                        .distanceText("1 km")
                        .durationSeconds(600)
                        .durationText("10 mins")
                        .transportMode(mode)
                        .build();

                when(googleMapsClient.getDirections(
                        anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                        eq(mode)))
                        .thenReturn(result);

                // When & Then
                mockMvc.perform(get("/api/directions")
                                .param("originLat", "-33.8568")
                                .param("originLng", "151.2153")
                                .param("destLat", "-33.8523")
                                .param("destLng", "151.2108")
                                .param("mode", mode.name())
                                                        .with(oauth2Login().oauth2User(userPrincipal)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.transportMode").value(mode.name()));
            }
        }

        @Test
        @DisplayName("should return 400 when originLat is missing")
        void getDirections_missingOriginLat_shouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/directions")
                            .param("originLng", "151.2153")
                            .param("destLat", "-33.8523")
                            .param("destLng", "151.2108")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when originLng is missing")
        void getDirections_missingOriginLng_shouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/directions")
                            .param("originLat", "-33.8568")
                            .param("destLat", "-33.8523")
                            .param("destLng", "151.2108")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when destLat is missing")
        void getDirections_missingDestLat_shouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/directions")
                            .param("originLat", "-33.8568")
                            .param("originLng", "151.2153")
                            .param("destLng", "151.2108")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when destLng is missing")
        void getDirections_missingDestLng_shouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/directions")
                            .param("originLat", "-33.8568")
                            .param("originLng", "151.2153")
                            .param("destLat", "-33.8523")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when originLat is out of range")
        void getDirections_invalidOriginLat_shouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/directions")
                            .param("originLat", "91.0")
                            .param("originLng", "151.2153")
                            .param("destLat", "-33.8523")
                            .param("destLng", "151.2108")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when originLat is below -90")
        void getDirections_originLatTooLow_shouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/directions")
                            .param("originLat", "-91.0")
                            .param("originLng", "151.2153")
                            .param("destLat", "-33.8523")
                            .param("destLng", "151.2108")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when originLng is out of range")
        void getDirections_invalidOriginLng_shouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/directions")
                            .param("originLat", "-33.8568")
                            .param("originLng", "181.0")
                            .param("destLat", "-33.8523")
                            .param("destLng", "151.2108")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when destLat is out of range")
        void getDirections_invalidDestLat_shouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/directions")
                            .param("originLat", "-33.8568")
                            .param("originLng", "151.2153")
                            .param("destLat", "95.0")
                            .param("destLng", "151.2108")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when destLng is out of range")
        void getDirections_invalidDestLng_shouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/directions")
                            .param("originLat", "-33.8568")
                            .param("originLng", "151.2153")
                            .param("destLat", "-33.8523")
                            .param("destLng", "-181.0")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when mode is invalid")
        void getDirections_invalidMode_shouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/directions")
                            .param("originLat", "-33.8568")
                            .param("originLng", "151.2153")
                            .param("destLat", "-33.8523")
                            .param("destLng", "151.2108")
                            .param("mode", "FLYING")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should return 502 when Google Maps API fails")
        void getDirections_googleMapsApiError_shouldReturn502() throws Exception {
            // Given
            when(googleMapsClient.getDirections(
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(TransportMode.class)))
                    .thenThrow(new GoogleMapsException("GOOGLE_MAPS_ERROR", "Google Maps API is unavailable"));

            // When & Then
            mockMvc.perform(get("/api/directions")
                            .param("originLat", "-33.8568")
                            .param("originLng", "151.2153")
                            .param("destLat", "-33.8523")
                            .param("destLng", "151.2108")
                            .param("mode", "WALKING")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("GOOGLE_MAPS_ERROR"));
        }

        @Test
        @DisplayName("should return 404 when no route found")
        void getDirections_noRouteFound_shouldReturn404() throws Exception {
            // Given
            when(googleMapsClient.getDirections(
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(TransportMode.class)))
                    .thenThrow(new GoogleMapsException("NO_ROUTE_FOUND", "No route could be found"));

            // When & Then
            mockMvc.perform(get("/api/directions")
                            .param("originLat", "-33.8568")
                            .param("originLng", "151.2153")
                            .param("destLat", "35.6762")
                            .param("destLng", "139.6503")
                            .param("mode", "WALKING")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("NO_ROUTE_FOUND"));
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void getDirections_notAuthenticated_shouldReturn403() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/directions")
                            .param("originLat", "-33.8568")
                            .param("originLng", "151.2153")
                            .param("destLat", "-33.8523")
                            .param("destLng", "151.2108"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should handle edge case coordinates (poles)")
        void getDirections_poleCoordinates_shouldReturn200() throws Exception {
            // Given
            DirectionResult poleResult = DirectionResult.builder()
                    .originAddress("North Pole")
                    .destinationAddress("South Pole")
                    .distanceMeters(20000000)
                    .distanceText("20,000 km")
                    .durationSeconds(86400)
                    .durationText("24 hours")
                    .transportMode(TransportMode.DRIVING)
                    .build();

            when(googleMapsClient.getDirections(
                    eq(90.0), eq(0.0),
                    eq(-90.0), eq(0.0),
                    any(TransportMode.class)))
                    .thenReturn(poleResult);

            // When & Then
            mockMvc.perform(get("/api/directions")
                            .param("originLat", "90.0")
                            .param("originLng", "0.0")
                            .param("destLat", "-90.0")
                            .param("destLng", "0.0")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should handle edge case coordinates (international date line)")
        void getDirections_dateLine_shouldReturn200() throws Exception {
            // Given
            DirectionResult dateLineResult = DirectionResult.builder()
                    .originAddress("West of Date Line")
                    .destinationAddress("East of Date Line")
                    .distanceMeters(100)
                    .distanceText("100 m")
                    .durationSeconds(60)
                    .durationText("1 min")
                    .transportMode(TransportMode.WALKING)
                    .build();

            when(googleMapsClient.getDirections(
                    eq(0.0), eq(180.0),
                    eq(0.0), eq(-180.0),
                    any(TransportMode.class)))
                    .thenReturn(dateLineResult);

            // When & Then
            mockMvc.perform(get("/api/directions")
                            .param("originLat", "0.0")
                            .param("originLng", "180.0")
                            .param("destLat", "0.0")
                            .param("destLng", "-180.0")
                                                    .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
