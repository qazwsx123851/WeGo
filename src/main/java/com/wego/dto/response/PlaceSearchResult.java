package com.wego.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing a place search result from Google Places API.
 *
 * Contains basic information about a place found in nearby search.
 *
 * @contract
 *   - placeId: Unique identifier for the place (required)
 *   - name: Display name of the place (required)
 *   - latitude/longitude: Geographic coordinates
 *   - rating: Value between 0.0 and 5.0
 *
 * @see com.wego.service.external.GoogleMapsClient
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSearchResult {

    /**
     * Unique identifier for this place, used to retrieve details.
     */
    private String placeId;

    /**
     * Human-readable name of the place.
     */
    private String name;

    /**
     * Vicinity or address of the place.
     */
    private String address;

    /**
     * Latitude coordinate.
     */
    private double latitude;

    /**
     * Longitude coordinate.
     */
    private double longitude;

    /**
     * Overall rating (0.0 - 5.0).
     */
    private double rating;

    /**
     * Total number of user ratings.
     */
    private int userRatingsTotal;

    /**
     * List of place types (e.g., "restaurant", "cafe").
     */
    private List<String> types;

    /**
     * Reference to a photo for this place.
     */
    private String photoReference;

    /**
     * Whether the place is currently open.
     */
    private Boolean isOpen;
}
