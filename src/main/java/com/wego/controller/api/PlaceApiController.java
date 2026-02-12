package com.wego.controller.api;

import com.wego.dto.ApiResponse;
import com.wego.dto.response.PlaceDetails;
import com.wego.dto.response.PlaceSearchResult;
import com.wego.exception.ValidationException;
import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
import com.wego.service.RateLimitService;
import com.wego.service.external.GoogleMapsClient;
import com.wego.service.external.GoogleMapsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.regex.Pattern;

/**
 * REST API controller for place search and details operations.
 *
 * Provides endpoints for searching places and retrieving place details
 * using Google Maps API.
 *
 * @contract
 *   - All endpoints require authentication
 *   - Returns ApiResponse wrapper for all responses
 *   - Validates coordinate ranges and radius limits
 *
 * @see GoogleMapsClient
 */
@Slf4j
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceApiController {

    private static final int DEFAULT_RADIUS_METERS = 1500;
    private static final int MAX_RADIUS_METERS = 50000;
    private static final int MAX_QUERY_LENGTH = 200;
    private static final int RATE_LIMIT_SEARCH = 30; // 30 requests per minute
    private static final int RATE_LIMIT_DETAILS = 60; // 60 requests per minute

    // Pattern to detect potentially dangerous characters (XSS, SQL injection)
    private static final Pattern SAFE_QUERY_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\s\\-_.,&'()]+$");

    private final GoogleMapsClient googleMapsClient;
    private final RateLimitService rateLimitService;
    private final CacheManager cacheManager;

    /**
     * Searches for places near a location.
     *
     * @contract
     *   - pre: query is not null or empty
     *   - pre: lat is between -90 and 90
     *   - pre: lng is between -180 and 180
     *   - pre: radius is between 1 and 50000 meters (default: 1500)
     *   - post: Returns 200 with list of PlaceSearchResult
     *   - post: Returns 400 if validation fails
     *   - post: Returns 502 if Google Maps API fails
     *   - calls: GoogleMapsClient#searchPlaces
     *   - calledBy: Frontend place search component
     *
     * GET /api/places/search?query={q}&lat={lat}&lng={lng}&radius={r}
     *
     * @param query Search query (e.g., "restaurant", "cafe")
     * @param lat Center latitude for search
     * @param lng Center longitude for search
     * @param radius Search radius in meters (optional, default 1500)
     * @return List of matching places
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PlaceSearchResult>>> searchPlaces(
            @RequestParam String query,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1500") int radius,
            @CurrentUser UserPrincipal principal) {

        log.debug("GET /api/places/search?query={}&lat={}&lng={}&radius={}", query, lat, lng, radius);

        // Validate and sanitize query
        String sanitizedQuery = validateAndSanitizeQuery(query);

        // Validate coordinates
        validateCoordinates(lat, lng);

        // Validate radius
        validateRadius(radius);

        // Check rate limit
        String rateLimitKey = "places:search:" + principal.getId();
        if (!rateLimitService.isAllowed(rateLimitKey, RATE_LIMIT_SEARCH)) {
            log.warn("Rate limit exceeded for place search");
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("RATE_LIMIT_EXCEEDED", "Too many requests. Please try again later."));
        }

        // Check cache
        String cacheKey = String.format("search:%s:%.4f:%.4f:%d", sanitizedQuery, lat, lng, radius);
        Cache cache = cacheManager.getCache("places");
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper != null) {
                @SuppressWarnings("unchecked")
                List<PlaceSearchResult> cachedResult = (List<PlaceSearchResult>) wrapper.get();
                log.debug("Cache hit for place search: {}", cacheKey);
                return ResponseEntity.ok(ApiResponse.success(cachedResult));
            }
        }

        try {
            List<PlaceSearchResult> results = googleMapsClient.searchPlaces(sanitizedQuery, lat, lng, radius);

            // Cache the results
            if (cache != null) {
                cache.put(cacheKey, results);
            }

            return ResponseEntity.ok(ApiResponse.success(results));
        } catch (GoogleMapsException e) {
            log.error("Google Maps API error during place search: {}", e.getMessage());
            return handleGoogleMapsException(e);
        }
    }

    /**
     * Gets detailed information about a specific place.
     *
     * @contract
     *   - pre: placeId is not null or empty
     *   - post: Returns 200 with PlaceDetails
     *   - post: Returns 400 if placeId is invalid
     *   - post: Returns 404 if place not found
     *   - post: Returns 502 if Google Maps API fails
     *   - calls: GoogleMapsClient#getPlaceDetails
     *   - calledBy: Frontend place detail page
     *
     * GET /api/places/{placeId}
     *
     * @param placeId The unique Google Place ID
     * @return Detailed place information
     */
    @GetMapping("/{placeId}")
    public ResponseEntity<ApiResponse<PlaceDetails>> getPlaceDetails(
            @PathVariable String placeId,
            @CurrentUser UserPrincipal principal) {

        log.debug("GET /api/places/{}", placeId);

        // Validate placeId
        String sanitizedPlaceId = validateAndSanitizePlaceId(placeId);

        // Check rate limit
        String rateLimitKey = "places:details:" + principal.getId();
        if (!rateLimitService.isAllowed(rateLimitKey, RATE_LIMIT_DETAILS)) {
            log.warn("Rate limit exceeded for place details");
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("RATE_LIMIT_EXCEEDED", "Too many requests. Please try again later."));
        }

        // Check cache
        String cacheKey = "details:" + sanitizedPlaceId;
        Cache cache = cacheManager.getCache("places");
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper != null) {
                @SuppressWarnings("unchecked")
                PlaceDetails cachedResult = (PlaceDetails) wrapper.get();
                log.debug("Cache hit for place details: {}", sanitizedPlaceId);
                return ResponseEntity.ok(ApiResponse.success(cachedResult));
            }
        }

        try {
            PlaceDetails details = googleMapsClient.getPlaceDetails(sanitizedPlaceId);

            // Cache the results
            if (cache != null) {
                cache.put(cacheKey, details);
            }

            return ResponseEntity.ok(ApiResponse.success(details));
        } catch (GoogleMapsException e) {
            log.error("Google Maps API error getting place details for {}: {}", sanitizedPlaceId, e.getMessage());
            return handleGoogleMapsException(e);
        }
    }

    /**
     * Validates and sanitizes search query.
     *
     * @contract
     *   - pre: query is not null
     *   - post: Returns sanitized query string
     *   - throws: ValidationException if query is invalid or potentially malicious
     *
     * @param query The search query to validate
     * @return Sanitized query string
     */
    private String validateAndSanitizeQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new ValidationException("VALIDATION_ERROR", "Search query is required");
        }

        String trimmed = query.trim();

        if (trimmed.length() > MAX_QUERY_LENGTH) {
            throw new ValidationException("VALIDATION_ERROR",
                    "Query must not exceed " + MAX_QUERY_LENGTH + " characters");
        }

        // Check for potentially dangerous characters
        if (!SAFE_QUERY_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("VALIDATION_ERROR",
                    "Query contains invalid characters");
        }

        return trimmed;
    }

    /**
     * Validates and sanitizes place ID.
     *
     * @contract
     *   - pre: placeId is not null
     *   - post: Returns sanitized place ID
     *   - throws: ValidationException if placeId is invalid
     *
     * @param placeId The place ID to validate
     * @return Sanitized place ID
     */
    private String validateAndSanitizePlaceId(String placeId) {
        if (placeId == null || placeId.trim().isEmpty()) {
            throw new ValidationException("VALIDATION_ERROR", "Place ID is required");
        }

        String trimmed = placeId.trim();

        // Google Place IDs are alphanumeric with underscores and dashes
        if (!trimmed.matches("^[A-Za-z0-9_-]+$")) {
            throw new ValidationException("VALIDATION_ERROR", "Invalid Place ID format");
        }

        if (trimmed.length() > 500) {
            throw new ValidationException("VALIDATION_ERROR", "Invalid Place ID");
        }

        return trimmed;
    }

    /**
     * Validates latitude and longitude values.
     *
     * @contract
     *   - pre: lat and lng are finite numbers
     *   - post: throws ValidationException if invalid
     *
     * @param lat Latitude value to validate
     * @param lng Longitude value to validate
     */
    private void validateCoordinates(double lat, double lng) {
        if (lat < -90 || lat > 90) {
            throw new ValidationException("VALIDATION_ERROR",
                    "Latitude must be between -90 and 90 degrees");
        }
        if (lng < -180 || lng > 180) {
            throw new ValidationException("VALIDATION_ERROR",
                    "Longitude must be between -180 and 180 degrees");
        }
    }

    /**
     * Validates the search radius value.
     *
     * @contract
     *   - pre: radius is a positive integer
     *   - post: throws ValidationException if invalid
     *
     * @param radius Radius value to validate
     */
    private void validateRadius(int radius) {
        if (radius <= 0) {
            throw new ValidationException("VALIDATION_ERROR",
                    "Radius must be greater than 0");
        }
        if (radius > MAX_RADIUS_METERS) {
            throw new ValidationException("VALIDATION_ERROR",
                    "Radius must not exceed " + MAX_RADIUS_METERS + " meters");
        }
    }

    /**
     * Handles GoogleMapsException and returns appropriate HTTP response.
     *
     * @contract
     *   - PLACE_NOT_FOUND -> 404
     *   - Other errors -> 502
     *
     * @param e The GoogleMapsException to handle
     * @return Appropriate ResponseEntity
     */
    private <T> ResponseEntity<ApiResponse<T>> handleGoogleMapsException(GoogleMapsException e) {
        String errorCode = e.getErrorCode();

        if ("PLACE_NOT_FOUND".equals(errorCode)) {
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
