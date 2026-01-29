package com.wego.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing detailed information about a place from Google Places API.
 *
 * Contains comprehensive information including contact details, reviews,
 * and opening hours.
 *
 * @contract
 *   - placeId: Unique identifier for the place (required)
 *   - name: Display name of the place (required)
 *   - rating: Value between 0.0 and 5.0
 *   - priceLevel: Value between 0 and 4 (0=free, 4=very expensive)
 *
 * @see com.wego.service.external.GoogleMapsClient
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDetails {

    /**
     * Unique identifier for this place.
     */
    private String placeId;

    /**
     * Human-readable name of the place.
     */
    private String name;

    /**
     * Full formatted address.
     */
    private String formattedAddress;

    /**
     * Local phone number in national format.
     */
    private String formattedPhoneNumber;

    /**
     * Phone number in international format.
     */
    private String internationalPhoneNumber;

    /**
     * Official website URL.
     */
    private String website;

    /**
     * Google Maps URL for this place.
     */
    private String url;

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
     * Price level (0=free, 1=inexpensive, 2=moderate, 3=expensive, 4=very expensive).
     */
    private int priceLevel;

    /**
     * List of place types (e.g., "restaurant", "cafe").
     */
    private List<String> types;

    /**
     * List of photo references.
     */
    private List<String> photoReferences;

    /**
     * User reviews for this place.
     */
    private List<Review> reviews;

    /**
     * Opening hours information.
     */
    private OpeningHours openingHours;

    /**
     * UTC offset in minutes.
     */
    private int utcOffset;

    /**
     * Nested class representing a user review.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Review {

        /**
         * Name of the review author.
         */
        private String authorName;

        /**
         * Rating given by this reviewer (1-5).
         */
        private int rating;

        /**
         * Review text content.
         */
        private String text;

        /**
         * Human-readable time description (e.g., "a week ago").
         */
        private String relativeTimeDescription;
    }

    /**
     * Nested class representing opening hours information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpeningHours {

        /**
         * Whether the place is currently open.
         */
        private Boolean isOpenNow;

        /**
         * Human-readable weekday opening hours.
         */
        private List<String> weekdayText;
    }
}
