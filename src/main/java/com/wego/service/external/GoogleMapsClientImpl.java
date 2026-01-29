package com.wego.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.config.GoogleMapsProperties;
import com.wego.dto.response.DirectionResult;
import com.wego.dto.response.PlaceDetails;
import com.wego.dto.response.PlaceSearchResult;
import com.wego.entity.TransportMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Real implementation of GoogleMapsClient that calls Google Maps APIs.
 *
 * Uses the following Google APIs:
 * - Distance Matrix API for directions
 * - Places Nearby Search API for place search
 * - Place Details API for place information
 *
 * @contract
 *   - pre: GoogleMapsProperties must be configured with valid apiKey
 *   - post: All API calls include API key in request
 *   - throws: GoogleMapsException on API errors or invalid responses
 *
 * @see GoogleMapsClient
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "wego.external-api.google-maps.enabled",
        havingValue = "true"
)
public class GoogleMapsClientImpl implements GoogleMapsClient {

    private static final String DISTANCE_MATRIX_URL =
            "https://maps.googleapis.com/maps/api/distancematrix/json";
    private static final String PLACES_NEARBY_URL =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
    private static final String PLACE_DETAILS_URL =
            "https://maps.googleapis.com/maps/api/place/details/json";

    private final GoogleMapsProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Creates a GoogleMapsClientImpl with the specified properties.
     *
     * @contract
     *   - pre: properties != null
     *   - post: Client is ready to make API calls with configured timeouts
     *
     * @param properties Google Maps configuration properties
     * @param restTemplate RestTemplate for HTTP calls (should have timeouts configured)
     */
    public GoogleMapsClientImpl(GoogleMapsProperties properties, RestTemplate restTemplate) {
        if (properties == null) {
            throw new IllegalArgumentException("GoogleMapsProperties cannot be null");
        }
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Default constructor for Spring injection.
     * Configures RestTemplate with timeouts from properties.
     */
    @org.springframework.beans.factory.annotation.Autowired
    public GoogleMapsClientImpl(GoogleMapsProperties properties) {
        this(properties, createRestTemplateWithTimeouts(properties));
    }

    /**
     * Creates a RestTemplate with configured timeouts.
     */
    private static RestTemplate createRestTemplateWithTimeouts(GoogleMapsProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        return new RestTemplate(factory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DirectionResult getDirections(String origin, String destination, TransportMode mode) {
        validateNotEmpty(origin, "Origin");
        validateNotEmpty(destination, "Destination");

        log.debug("Getting directions from '{}' to '{}' via {}", origin, destination, mode);

        String baseUrl = buildDistanceMatrixUrlBase(
                encodeUrl(origin),
                encodeUrl(destination),
                mode
        );

        return executeDirectionsRequest(baseUrl, mode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DirectionResult getDirections(
            double originLat, double originLng,
            double destLat, double destLng,
            TransportMode mode
    ) {
        log.debug("Getting directions from ({}, {}) to ({}, {}) via {}",
                originLat, originLng, destLat, destLng, mode);

        String origin = originLat + "," + originLng;
        String destination = destLat + "," + destLng;

        String baseUrl = buildDistanceMatrixUrlBase(origin, destination, mode);

        return executeDirectionsRequest(baseUrl, mode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlaceSearchResult> searchPlaces(String query, double lat, double lng, int radiusMeters) {
        validateNotEmpty(query, "Query");
        if (radiusMeters <= 0) {
            throw new IllegalArgumentException("Radius must be positive");
        }

        log.debug("Searching places for '{}' near ({}, {}) within {}m", query, lat, lng, radiusMeters);

        String baseUrl = String.format(
                "%s?location=%f,%f&radius=%d&keyword=%s",
                PLACES_NEARBY_URL,
                lat, lng,
                radiusMeters,
                encodeUrl(query)
        );

        try {
            String url = appendApiKey(baseUrl);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            String status = root.path("status").asText();
            if ("ZERO_RESULTS".equals(status)) {
                log.debug("No places found for query: {}", query);
                return new ArrayList<>();
            }

            validateApiStatus(status, root);

            List<PlaceSearchResult> results = new ArrayList<>();
            JsonNode resultsNode = root.path("results");

            for (JsonNode placeNode : resultsNode) {
                PlaceSearchResult place = parsePlaceSearchResult(placeNode);
                results.add(place);
            }

            log.info("Found {} places for query '{}'", results.size(), query);
            return results;

        } catch (GoogleMapsException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("HTTP error searching places: {}", e.getMessage());
            throw GoogleMapsException.apiError("HTTP error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error searching places: {}", e.getMessage(), e);
            throw GoogleMapsException.apiError("Failed to search places: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PlaceDetails getPlaceDetails(String placeId) {
        validateNotEmpty(placeId, "Place ID");

        log.debug("Getting details for place: {}", placeId);

        String fields = String.join(",", List.of(
                "place_id", "name", "formatted_address", "formatted_phone_number",
                "international_phone_number", "website", "url", "geometry",
                "rating", "user_ratings_total", "price_level", "types",
                "photos", "reviews", "opening_hours", "utc_offset"
        ));

        String baseUrl = String.format(
                "%s?place_id=%s&fields=%s",
                PLACE_DETAILS_URL,
                encodeUrl(placeId),
                encodeUrl(fields)
        );

        try {
            String url = appendApiKey(baseUrl);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            String status = root.path("status").asText();
            if ("NOT_FOUND".equals(status) || "INVALID_REQUEST".equals(status)) {
                throw new GoogleMapsException("NOT_FOUND",
                        "Place not found: " + placeId);
            }

            validateApiStatus(status, root);

            JsonNode resultNode = root.path("result");
            PlaceDetails details = parsePlaceDetails(resultNode);

            log.info("Retrieved details for place: {}", placeId);
            return details;

        } catch (GoogleMapsException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("HTTP error getting place details: {}", e.getMessage());
            throw GoogleMapsException.apiError("HTTP error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error getting place details: {}", e.getMessage(), e);
            throw GoogleMapsException.apiError("Failed to get place details: " + e.getMessage());
        }
    }

    /**
     * Builds the Distance Matrix API URL (without API key for safety).
     */
    private String buildDistanceMatrixUrlBase(String origin, String destination, TransportMode mode) {
        return String.format(
                "%s?origins=%s&destinations=%s&mode=%s",
                DISTANCE_MATRIX_URL,
                origin,
                destination,
                mode.toGoogleMapsMode()
        );
    }

    /**
     * Appends API key to URL. Should only be used immediately before making request.
     */
    private String appendApiKey(String baseUrl) {
        return baseUrl + "&key=" + properties.getApiKey();
    }

    /**
     * Executes a directions request and parses the response.
     * @param baseUrl URL without API key (safe to log)
     */
    private DirectionResult executeDirectionsRequest(String baseUrl, TransportMode mode) {
        try {
            String url = appendApiKey(baseUrl);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            String status = root.path("status").asText();
            validateApiStatus(status, root);

            JsonNode rowsNode = root.path("rows");
            if (rowsNode.isEmpty()) {
                throw GoogleMapsException.noResults("directions");
            }

            JsonNode elementNode = rowsNode.get(0).path("elements").get(0);
            String elementStatus = elementNode.path("status").asText();

            if ("ZERO_RESULTS".equals(elementStatus) || "NOT_FOUND".equals(elementStatus)) {
                throw new GoogleMapsException("NO_ROUTE",
                        "No route found between the specified locations");
            }

            if (!"OK".equals(elementStatus)) {
                throw GoogleMapsException.apiError("Element status: " + elementStatus);
            }

            int distanceMeters = elementNode.path("distance").path("value").asInt();
            String distanceText = elementNode.path("distance").path("text").asText();
            int durationSeconds = elementNode.path("duration").path("value").asInt();
            String durationText = elementNode.path("duration").path("text").asText();

            String originAddress = root.path("origin_addresses").get(0).asText();
            String destinationAddress = root.path("destination_addresses").get(0).asText();

            log.info("Calculated route: {} ({}) via {}", distanceText, durationText, mode);

            return DirectionResult.builder()
                    .originAddress(originAddress)
                    .destinationAddress(destinationAddress)
                    .distanceMeters(distanceMeters)
                    .distanceText(distanceText)
                    .durationSeconds(durationSeconds)
                    .durationText(durationText)
                    .transportMode(mode)
                    .build();

        } catch (GoogleMapsException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("HTTP error getting directions: {}", e.getMessage());
            throw GoogleMapsException.apiError("HTTP error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error getting directions: {}", e.getMessage(), e);
            throw GoogleMapsException.apiError("Failed to get directions: " + e.getMessage());
        }
    }

    /**
     * Parses a place search result from JSON.
     */
    private PlaceSearchResult parsePlaceSearchResult(JsonNode node) {
        JsonNode geometryNode = node.path("geometry").path("location");
        JsonNode photosNode = node.path("photos");
        JsonNode openingHoursNode = node.path("opening_hours");

        List<String> types = new ArrayList<>();
        for (JsonNode typeNode : node.path("types")) {
            types.add(typeNode.asText());
        }

        String photoReference = null;
        if (photosNode.isArray() && photosNode.size() > 0) {
            photoReference = photosNode.get(0).path("photo_reference").asText();
        }

        Boolean isOpen = null;
        if (!openingHoursNode.isMissingNode()) {
            isOpen = openingHoursNode.path("open_now").asBoolean();
        }

        return PlaceSearchResult.builder()
                .placeId(node.path("place_id").asText())
                .name(node.path("name").asText())
                .address(node.path("vicinity").asText())
                .latitude(geometryNode.path("lat").asDouble())
                .longitude(geometryNode.path("lng").asDouble())
                .rating(node.path("rating").asDouble())
                .userRatingsTotal(node.path("user_ratings_total").asInt())
                .types(types)
                .photoReference(photoReference)
                .isOpen(isOpen)
                .build();
    }

    /**
     * Parses place details from JSON.
     */
    private PlaceDetails parsePlaceDetails(JsonNode node) {
        JsonNode geometryNode = node.path("geometry").path("location");

        // Parse types
        List<String> types = new ArrayList<>();
        for (JsonNode typeNode : node.path("types")) {
            types.add(typeNode.asText());
        }

        // Parse photo references
        List<String> photoReferences = new ArrayList<>();
        for (JsonNode photoNode : node.path("photos")) {
            photoReferences.add(photoNode.path("photo_reference").asText());
        }

        // Parse reviews
        List<PlaceDetails.Review> reviews = new ArrayList<>();
        for (JsonNode reviewNode : node.path("reviews")) {
            PlaceDetails.Review review = PlaceDetails.Review.builder()
                    .authorName(reviewNode.path("author_name").asText())
                    .rating(reviewNode.path("rating").asInt())
                    .text(reviewNode.path("text").asText())
                    .relativeTimeDescription(reviewNode.path("relative_time_description").asText())
                    .build();
            reviews.add(review);
        }

        // Parse opening hours
        PlaceDetails.OpeningHours openingHours = null;
        JsonNode openingHoursNode = node.path("opening_hours");
        if (!openingHoursNode.isMissingNode()) {
            List<String> weekdayText = new ArrayList<>();
            for (JsonNode dayNode : openingHoursNode.path("weekday_text")) {
                weekdayText.add(dayNode.asText());
            }

            openingHours = PlaceDetails.OpeningHours.builder()
                    .isOpenNow(openingHoursNode.path("open_now").asBoolean())
                    .weekdayText(weekdayText)
                    .build();
        }

        return PlaceDetails.builder()
                .placeId(node.path("place_id").asText())
                .name(node.path("name").asText())
                .formattedAddress(node.path("formatted_address").asText())
                .formattedPhoneNumber(node.path("formatted_phone_number").asText())
                .internationalPhoneNumber(node.path("international_phone_number").asText())
                .website(node.path("website").asText())
                .url(node.path("url").asText())
                .latitude(geometryNode.path("lat").asDouble())
                .longitude(geometryNode.path("lng").asDouble())
                .rating(node.path("rating").asDouble())
                .userRatingsTotal(node.path("user_ratings_total").asInt())
                .priceLevel(node.path("price_level").asInt())
                .types(types)
                .photoReferences(photoReferences)
                .reviews(reviews.isEmpty() ? null : reviews)
                .openingHours(openingHours)
                .utcOffset(node.path("utc_offset").asInt())
                .build();
    }

    /**
     * Validates the API response status.
     */
    private void validateApiStatus(String status, JsonNode root) {
        switch (status) {
            case "OK":
                return;
            case "ZERO_RESULTS":
                return; // Valid response, just no results
            case "REQUEST_DENIED":
                String errorMessage = root.path("error_message").asText("API key is invalid");
                log.error("Google Maps API request denied: {}", errorMessage);
                throw GoogleMapsException.invalidApiKey();
            case "OVER_QUERY_LIMIT":
                log.warn("Google Maps API rate limit exceeded");
                throw GoogleMapsException.rateLimitExceeded();
            case "INVALID_REQUEST":
                String invalidMsg = root.path("error_message").asText("Invalid request parameters");
                throw GoogleMapsException.apiError(invalidMsg);
            default:
                throw GoogleMapsException.apiError("Unknown status: " + status);
        }
    }

    /**
     * URL-encodes a string.
     */
    private String encodeUrl(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            // Should never happen with UTF-8
            return value;
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
}
