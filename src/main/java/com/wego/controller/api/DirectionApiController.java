package com.wego.controller.api;

import com.wego.dto.ApiResponse;
import com.wego.dto.response.DirectionResult;
import com.wego.entity.TransportMode;
import com.wego.exception.ValidationException;
import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
import com.wego.service.CacheService;
import com.wego.service.RateLimitService;
import com.wego.service.external.GoogleMapsClient;
import com.wego.service.external.GoogleMapsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * REST API controller for direction/routing operations.
 *
 * Provides endpoints for getting directions between two locations
 * using Google Maps Directions API.
 *
 * @contract
 *   - All endpoints require authentication
 *   - Returns ApiResponse wrapper for all responses
 *   - Validates coordinate ranges
 *
 * @see GoogleMapsClient
 */
@Slf4j
@RestController
@RequestMapping("/api/directions")
@RequiredArgsConstructor
public class DirectionApiController {

    private static final int RATE_LIMIT_DIRECTIONS = 30; // 30 requests per minute
    private static final long CACHE_TTL_MS = 10 * 60 * 1000L; // 10 minutes (directions change less frequently)

    private final GoogleMapsClient googleMapsClient;
    private final RateLimitService rateLimitService;
    private final CacheService cacheService;

    /**
     * Gets directions between two geographic coordinates.
     *
     * @contract
     *   - pre: originLat is between -90 and 90
     *   - pre: originLng is between -180 and 180
     *   - pre: destLat is between -90 and 90
     *   - pre: destLng is between -180 and 180
     *   - pre: mode is a valid TransportMode (default: DRIVING)
     *   - post: Returns 200 with DirectionResult
     *   - post: Returns 400 if validation fails or mode is invalid
     *   - post: Returns 404 if no route can be found
     *   - post: Returns 502 if Google Maps API fails
     *   - calls: GoogleMapsClient#getDirections
     *   - calledBy: Frontend route calculation component
     *
     * GET /api/directions?originLat={}&originLng={}&destLat={}&destLng={}&mode={}
     *
     * @param originLat Origin latitude
     * @param originLng Origin longitude
     * @param destLat Destination latitude
     * @param destLng Destination longitude
     * @param mode Transportation mode (optional, default DRIVING)
     * @return Direction result with distance and duration
     */
    @GetMapping
    public ResponseEntity<ApiResponse<DirectionResult>> getDirections(
            @RequestParam double originLat,
            @RequestParam double originLng,
            @RequestParam double destLat,
            @RequestParam double destLng,
            @RequestParam(required = false) String mode,
            @CurrentUser UserPrincipal principal) {

        log.debug("GET /api/directions?originLat={}&originLng={}&destLat={}&destLng={}&mode={}",
                originLat, originLng, destLat, destLng, mode);

        // Validate origin coordinates
        validateCoordinates("origin", originLat, originLng);

        // Validate destination coordinates
        validateCoordinates("destination", destLat, destLng);

        // Parse and validate transport mode
        TransportMode transportMode = parseTransportMode(mode);

        // Check rate limit
        String rateLimitKey = "directions:" + principal.getId();
        if (!rateLimitService.isAllowed(rateLimitKey, RATE_LIMIT_DIRECTIONS)) {
            log.warn("Rate limit exceeded for directions");
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("RATE_LIMIT_EXCEEDED", "Too many requests. Please try again later."));
        }

        // Check cache (round coordinates to 4 decimal places for better cache hits)
        String cacheKey = String.format("directions:%.4f:%.4f:%.4f:%.4f:%s",
                originLat, originLng, destLat, destLng, transportMode);
        Optional<DirectionResult> cachedResult = cacheService.get(cacheKey, DirectionResult.class);
        if (cachedResult.isPresent()) {
            log.debug("Cache hit for directions: {}", cacheKey);
            return ResponseEntity.ok(ApiResponse.success(cachedResult.get()));
        }

        try {
            DirectionResult result = googleMapsClient.getDirections(
                    originLat, originLng,
                    destLat, destLng,
                    transportMode);

            // Cache the result
            cacheService.put(cacheKey, result, CACHE_TTL_MS);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (GoogleMapsException e) {
            log.error("Google Maps API error getting directions: {}", e.getMessage());
            return handleGoogleMapsException(e);
        }
    }

    /**
     * Validates latitude and longitude values for a location.
     *
     * @contract
     *   - pre: lat and lng are finite numbers
     *   - post: throws ValidationException if invalid
     *
     * @param locationName Name of the location for error messages
     * @param lat Latitude value to validate
     * @param lng Longitude value to validate
     */
    private void validateCoordinates(String locationName, double lat, double lng) {
        if (lat < -90 || lat > 90) {
            throw new ValidationException("VALIDATION_ERROR",
                    locationName + " latitude must be between -90 and 90 degrees");
        }
        if (lng < -180 || lng > 180) {
            throw new ValidationException("VALIDATION_ERROR",
                    locationName + " longitude must be between -180 and 180 degrees");
        }
    }

    /**
     * Parses and validates the transport mode parameter.
     *
     * @contract
     *   - pre: mode may be null (defaults to DRIVING)
     *   - post: Returns valid TransportMode
     *   - post: throws ValidationException if mode is invalid
     *
     * @param mode The mode string to parse
     * @return Parsed TransportMode
     */
    private TransportMode parseTransportMode(String mode) {
        if (mode == null || mode.trim().isEmpty()) {
            return TransportMode.DRIVING;
        }

        try {
            return TransportMode.valueOf(mode.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("VALIDATION_ERROR",
                    "Invalid transport mode: " + mode + ". Valid modes are: WALKING, TRANSIT, DRIVING, BICYCLING");
        }
    }

    /**
     * Handles GoogleMapsException and returns appropriate HTTP response.
     *
     * @contract
     *   - NO_ROUTE_FOUND -> 404
     *   - Other errors -> 502
     *
     * @param e The GoogleMapsException to handle
     * @return Appropriate ResponseEntity
     */
    private ResponseEntity<ApiResponse<DirectionResult>> handleGoogleMapsException(GoogleMapsException e) {
        String errorCode = e.getErrorCode();

        if ("NO_ROUTE_FOUND".equals(errorCode)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(errorCode, e.getMessage()));
        }

        // All other Google Maps errors are treated as external service failures
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(errorCode, e.getMessage()));
    }
}
