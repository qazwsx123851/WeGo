package com.wego.service.external;

import com.wego.dto.response.DirectionResult;
import com.wego.dto.response.PlaceDetails;
import com.wego.dto.response.PlaceSearchResult;
import com.wego.entity.TransportMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Mock implementation of GoogleMapsClient for testing and development.
 * Uses Haversine distance calculation for simulated directions.
 *
 * @contract
 *   - Only active when google-maps.enabled is false (default)
 *   - Uses realistic speed estimates for each transport mode
 *   - Returns simulated but structurally valid data
 *
 * @see GoogleMapsClient
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "wego.external-api.google-maps.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class MockGoogleMapsClient implements GoogleMapsClient {

    private static final double EARTH_RADIUS_METERS = 6371000;

    // Average speeds in km/h for different transport modes
    private static final double WALKING_SPEED_KMH = 5.0;
    private static final double BICYCLING_SPEED_KMH = 15.0;
    private static final double TRANSIT_SPEED_KMH = 30.0;
    private static final double DRIVING_SPEED_KMH = 40.0;

    private final Random random = new Random();

    /**
     * {@inheritDoc}
     *
     * Uses simulated coordinates based on address string hash
     * to generate consistent results for the same addresses.
     */
    @Override
    public DirectionResult getDirections(String origin, String destination, TransportMode mode) {
        validateNotEmpty(origin, "Origin");
        validateNotEmpty(destination, "Destination");

        log.debug("[MOCK] Getting directions from '{}' to '{}' via {}", origin, destination, mode);

        // Generate pseudo-random but consistent coordinates from address hashes
        double originLat = 35.0 + (Math.abs(origin.hashCode()) % 1000) / 10000.0;
        double originLng = 139.0 + (Math.abs(origin.hashCode() >> 16) % 1000) / 10000.0;
        double destLat = 35.0 + (Math.abs(destination.hashCode()) % 1000) / 10000.0;
        double destLng = 139.0 + (Math.abs(destination.hashCode() >> 16) % 1000) / 10000.0;

        DirectionResult result = getDirections(originLat, originLng, destLat, destLng, mode);

        // Override addresses with provided strings
        return DirectionResult.builder()
                .originAddress(origin + " (Mock)")
                .destinationAddress(destination + " (Mock)")
                .distanceMeters(result.getDistanceMeters())
                .distanceText(result.getDistanceText())
                .durationSeconds(result.getDurationSeconds())
                .durationText(result.getDurationText())
                .transportMode(mode)
                .apiSource(DirectionResult.ApiSource.DISTANCE_MATRIX)
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * Calculates distance using Haversine formula and estimates
     * duration based on transport mode average speeds.
     */
    @Override
    public DirectionResult getDirections(
            double originLat, double originLng,
            double destLat, double destLng,
            TransportMode mode
    ) {
        log.debug("[MOCK] Getting directions from ({}, {}) to ({}, {}) via {}",
                originLat, originLng, destLat, destLng, mode);

        int distanceMeters = (int) calculateHaversineDistance(
                originLat, originLng, destLat, destLng
        );

        double speedKmH = getSpeedForMode(mode);
        int durationSeconds = calculateDuration(distanceMeters, speedKmH);

        String distanceText = formatDistance(distanceMeters);
        String durationText = formatDuration(durationSeconds);

        log.info("[MOCK] Calculated route: {} ({}) via {}",
                distanceText, durationText, mode);

        return DirectionResult.builder()
                .originAddress(String.format("%.4f, %.4f (Mock)", originLat, originLng))
                .destinationAddress(String.format("%.4f, %.4f (Mock)", destLat, destLng))
                .distanceMeters(distanceMeters)
                .distanceText(distanceText)
                .durationSeconds(durationSeconds)
                .durationText(durationText)
                .transportMode(mode)
                .apiSource(DirectionResult.ApiSource.DISTANCE_MATRIX)
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * Returns simulated places around the specified location.
     */
    @Override
    public List<PlaceSearchResult> searchPlaces(String query, double lat, double lng, int radiusMeters) {
        validateNotEmpty(query, "Query");
        if (radiusMeters <= 0) {
            throw new IllegalArgumentException("Radius must be positive");
        }

        log.debug("[MOCK] Searching places for '{}' near ({}, {}) within {}m",
                query, lat, lng, radiusMeters);

        List<PlaceSearchResult> results = new ArrayList<>();

        // Generate 3-8 mock results
        int count = 3 + random.nextInt(6);
        count = Math.min(count, 20); // Cap at 20

        String[] placeTypes = {"restaurant", "cafe", "bar", "hotel", "museum", "park"};
        String[] prefixes = {"Tokyo", "Shibuya", "Ginza", "Akihabara", "Harajuku"};

        for (int i = 0; i < count; i++) {
            // Generate random location within radius
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * radiusMeters;

            // Convert distance to lat/lng offset (approximate)
            double latOffset = (distance * Math.cos(angle)) / 111000.0;
            double lngOffset = (distance * Math.sin(angle)) / (111000.0 * Math.cos(Math.toRadians(lat)));

            String prefix = prefixes[random.nextInt(prefixes.length)];
            String placeType = placeTypes[random.nextInt(placeTypes.length)];

            PlaceSearchResult place = PlaceSearchResult.builder()
                    .placeId("mock-place-" + UUID.randomUUID().toString().substring(0, 8))
                    .name(prefix + " " + capitalize(query) + " " + (i + 1))
                    .address(String.format("%d-%d %s, Tokyo", random.nextInt(10) + 1, random.nextInt(30) + 1, prefix))
                    .latitude(lat + latOffset)
                    .longitude(lng + lngOffset)
                    .rating(3.0 + random.nextDouble() * 2.0) // 3.0 - 5.0
                    .userRatingsTotal(random.nextInt(5000) + 10)
                    .types(Arrays.asList(placeType, "point_of_interest"))
                    .photoReference("mock-photo-" + i)
                    .isOpen(random.nextBoolean())
                    .build();

            results.add(place);
        }

        log.info("[MOCK] Found {} places for query '{}'", results.size(), query);
        return results;
    }

    /**
     * {@inheritDoc}
     *
     * Returns simulated place details.
     */
    @Override
    public PlaceDetails getPlaceDetails(String placeId) {
        validateNotEmpty(placeId, "Place ID");

        log.debug("[MOCK] Getting details for place: {}", placeId);

        List<PlaceDetails.Review> reviews = Arrays.asList(
                PlaceDetails.Review.builder()
                        .authorName("Mock User 1")
                        .rating(5)
                        .text("Great place! Highly recommended.")
                        .relativeTimeDescription("a week ago")
                        .build(),
                PlaceDetails.Review.builder()
                        .authorName("Mock User 2")
                        .rating(4)
                        .text("Nice atmosphere and good service.")
                        .relativeTimeDescription("2 weeks ago")
                        .build(),
                PlaceDetails.Review.builder()
                        .authorName("Mock User 3")
                        .rating(4)
                        .text("Will definitely visit again.")
                        .relativeTimeDescription("a month ago")
                        .build()
        );

        PlaceDetails.OpeningHours openingHours = PlaceDetails.OpeningHours.builder()
                .isOpenNow(true)
                .weekdayText(Arrays.asList(
                        "Monday: 9:00 AM - 9:00 PM",
                        "Tuesday: 9:00 AM - 9:00 PM",
                        "Wednesday: 9:00 AM - 9:00 PM",
                        "Thursday: 9:00 AM - 9:00 PM",
                        "Friday: 9:00 AM - 10:00 PM",
                        "Saturday: 10:00 AM - 10:00 PM",
                        "Sunday: 10:00 AM - 8:00 PM"
                ))
                .build();

        PlaceDetails details = PlaceDetails.builder()
                .placeId(placeId)
                .name("Mock Place - " + placeId.substring(0, Math.min(8, placeId.length())))
                .formattedAddress("1-2-3 Shibuya, Shibuya-ku, Tokyo 150-0001, Japan")
                .formattedPhoneNumber("03-1234-5678")
                .internationalPhoneNumber("+81 3-1234-5678")
                .website("https://example.com/mock-place")
                .url("https://maps.google.com/?cid=mock-" + placeId)
                .latitude(35.6595 + random.nextDouble() * 0.01)
                .longitude(139.7004 + random.nextDouble() * 0.01)
                .rating(4.0 + random.nextDouble())
                .userRatingsTotal(random.nextInt(10000) + 100)
                .priceLevel(random.nextInt(4) + 1)
                .types(Arrays.asList("restaurant", "food", "point_of_interest"))
                .photoReferences(Arrays.asList("mock-photo-1", "mock-photo-2", "mock-photo-3"))
                .reviews(reviews)
                .openingHours(openingHours)
                .utcOffset(540) // Japan UTC+9
                .build();

        log.info("[MOCK] Returned details for place: {}", placeId);
        return details;
    }

    /**
     * Calculates the Haversine distance between two points on Earth.
     *
     * @param lat1 Latitude of point 1
     * @param lon1 Longitude of point 1
     * @param lat2 Latitude of point 2
     * @param lon2 Longitude of point 2
     * @return Distance in meters
     */
    private double calculateHaversineDistance(
            double lat1, double lon1,
            double lat2, double lon2
    ) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Returns average speed in km/h for a transport mode.
     */
    private double getSpeedForMode(TransportMode mode) {
        return switch (mode) {
            case WALKING -> WALKING_SPEED_KMH;
            case BICYCLING -> BICYCLING_SPEED_KMH;
            case TRANSIT -> TRANSIT_SPEED_KMH;
            case DRIVING -> DRIVING_SPEED_KMH;
            case HIGH_SPEED_RAIL -> 250.0;
            case FLIGHT -> 800.0;
            case NOT_CALCULATED -> 0.0;
        };
    }

    /**
     * Calculates duration in seconds based on distance and speed.
     */
    private int calculateDuration(int distanceMeters, double speedKmH) {
        if (distanceMeters == 0) {
            return 0;
        }
        double distanceKm = distanceMeters / 1000.0;
        double durationHours = distanceKm / speedKmH;
        return (int) (durationHours * 3600);
    }

    /**
     * Formats distance for human readability.
     */
    private String formatDistance(int meters) {
        if (meters < 1000) {
            return meters + " m";
        }
        double km = meters / 1000.0;
        return String.format("%.1f km", km);
    }

    /**
     * Formats duration for human readability.
     */
    private String formatDuration(int seconds) {
        if (seconds < 60) {
            return seconds + " secs";
        } else if (seconds < 3600) {
            int mins = seconds / 60;
            return mins + " mins";
        } else {
            int hours = seconds / 3600;
            int mins = (seconds % 3600) / 60;
            if (mins == 0) {
                return hours + " hours";
            }
            return hours + " hours " + mins + " mins";
        }
    }

    /**
     * Validates that a string is not null or empty.
     */
    private void validateNotEmpty(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be null or empty");
        }
    }

    /**
     * Capitalizes the first letter of a string.
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
