package com.wego.service.external;

import com.wego.dto.response.DirectionResult;
import com.wego.dto.response.PlaceDetails;
import com.wego.dto.response.PlaceSearchResult;
import com.wego.entity.TransportMode;

import java.util.List;

/**
 * Interface for Google Maps API operations.
 * Allows swapping between real and mock implementations.
 *
 * @contract
 *   - All methods throw GoogleMapsException on API failures
 *   - Mock implementation uses Haversine distance calculation
 *   - Real implementation calls Google Maps APIs
 *
 * @see MockGoogleMapsClient
 * @see GoogleMapsClientImpl
 */
public interface GoogleMapsClient {

    /**
     * Gets directions between two addresses.
     *
     * @contract
     *   - pre: origin != null and not empty
     *   - pre: destination != null and not empty
     *   - pre: mode != null
     *   - post: Returns DirectionResult with distance and duration
     *   - throws: GoogleMapsException if route cannot be calculated
     *   - throws: IllegalArgumentException if parameters are invalid
     *
     * @param origin The starting address
     * @param destination The destination address
     * @param mode The transportation mode
     * @return DirectionResult containing route information
     */
    DirectionResult getDirections(String origin, String destination, TransportMode mode);

    /**
     * Gets directions between two geographic coordinates.
     *
     * @contract
     *   - pre: latitude values between -90 and 90
     *   - pre: longitude values between -180 and 180
     *   - pre: mode != null
     *   - post: Returns DirectionResult with distance and duration
     *   - throws: GoogleMapsException if route cannot be calculated
     *
     * @param originLat Origin latitude
     * @param originLng Origin longitude
     * @param destLat Destination latitude
     * @param destLng Destination longitude
     * @param mode The transportation mode
     * @return DirectionResult containing route information
     */
    DirectionResult getDirections(
            double originLat, double originLng,
            double destLat, double destLng,
            TransportMode mode
    );

    /**
     * Searches for nearby places matching the query.
     *
     * @contract
     *   - pre: query != null and not empty
     *   - pre: radiusMeters > 0
     *   - post: Returns list of PlaceSearchResult (may be empty)
     *   - throws: GoogleMapsException on API error
     *   - throws: IllegalArgumentException if parameters are invalid
     *
     * @param query Search query (e.g., "restaurant", "cafe")
     * @param lat Center latitude for search
     * @param lng Center longitude for search
     * @param radiusMeters Search radius in meters
     * @return List of matching places
     */
    List<PlaceSearchResult> searchPlaces(String query, double lat, double lng, int radiusMeters);

    /**
     * Gets detailed information about a specific place.
     *
     * @contract
     *   - pre: placeId != null and not empty
     *   - post: Returns PlaceDetails with full information
     *   - throws: GoogleMapsException if place not found
     *   - throws: IllegalArgumentException if placeId is invalid
     *
     * @param placeId The unique place identifier
     * @return Detailed place information
     */
    PlaceDetails getPlaceDetails(String placeId);
}
