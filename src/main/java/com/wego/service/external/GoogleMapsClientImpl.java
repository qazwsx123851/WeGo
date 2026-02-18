package com.wego.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wego.config.GoogleMapsProperties;
import com.wego.dto.response.DirectionResult;
import com.wego.dto.response.PlaceDetails;
import com.wego.dto.response.PlaceSearchResult;
import com.wego.dto.response.TransitDetails;
import com.wego.entity.TransportMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real implementation of GoogleMapsClient that calls Google Maps APIs.
 *
 * Uses the following Google APIs:
 * - Routes API (computeRouteMatrix) for directions (New API, preferred)
 * - Distance Matrix API for directions (Legacy API, fallback)
 * - Places API (New) Text Search for place search
 * - Places API (New) Place Details for place information
 *
 * @contract
 *   - pre: GoogleMapsProperties must be configured with valid apiKey
 *   - post: All API calls include API key in request
 *   - throws: GoogleMapsException on API errors or invalid responses
 *   - When useRoutesApi=true: Uses Routes API with fallback chain
 *   - Fallback: TRANSIT → DRIVING → GoogleMapsException
 *
 * @see GoogleMapsClient
 * @see <a href="https://developers.google.com/maps/documentation/routes">Routes API</a>
 * @see <a href="https://developers.google.com/maps/documentation/places/web-service/overview">Places API (New)</a>
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "wego.external-api.google-maps.enabled",
        havingValue = "true"
)
public class GoogleMapsClientImpl implements GoogleMapsClient {

    // Legacy Distance Matrix API
    private static final String DISTANCE_MATRIX_URL =
            "https://maps.googleapis.com/maps/api/distancematrix/json";

    // Routes API (New)
    private static final String ROUTES_API_URL =
            "https://routes.googleapis.com/distanceMatrix/v2:computeRouteMatrix";

    // Places API (New) endpoints
    private static final String PLACES_TEXT_SEARCH_URL =
            "https://places.googleapis.com/v1/places:searchText";
    private static final String PLACE_DETAILS_URL =
            "https://places.googleapis.com/v1/places/";

    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;
    private static final long CIRCUIT_BREAKER_COOLDOWN_MS = 5 * 60 * 1000L; // 5 minutes

    private final GoogleMapsProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Circuit breaker state
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong circuitOpenedAt = new AtomicLong(0);

    /**
     * Creates a GoogleMapsClientImpl with the specified properties.
     *
     * @contract
     *   - pre: properties != null
     *   - post: Client is ready to make API calls with configured timeouts
     *
     * @param properties Google Maps configuration properties
     * @param restTemplate RestTemplate for HTTP calls (shared, connection-pooled)
     */
    @org.springframework.beans.factory.annotation.Autowired
    public GoogleMapsClientImpl(GoogleMapsProperties properties,
                                @org.springframework.beans.factory.annotation.Qualifier("externalApiRestTemplate") RestTemplate restTemplate) {
        if (properties == null) {
            throw new IllegalArgumentException("GoogleMapsProperties cannot be null");
        }
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * {@inheritDoc}
     *
     * Note: Routes API is only used for coordinate-based queries.
     * Address-based queries still use Distance Matrix API as Routes API
     * has limited support for place names.
     */
    @Override
    public DirectionResult getDirections(String origin, String destination, TransportMode mode) {
        validateNotEmpty(origin, "Origin");
        validateNotEmpty(destination, "Destination");
        checkCircuitBreaker();

        log.debug("Getting directions from '{}' to '{}' via {}", origin, destination, mode);

        // Address-based queries use legacy Distance Matrix API
        // (Routes API works better with coordinates)
        String baseUrl = buildDistanceMatrixUrlBase(
                encodeUrl(origin),
                encodeUrl(destination),
                mode
        );

        return executeDirectionsRequest(baseUrl, mode);
    }

    /**
     * {@inheritDoc}
     *
     * When useRoutesApi is enabled, uses Routes API with fallback chain:
     * 1. Try Routes API with requested mode
     * 2. If TRANSIT fails, fallback to DRIVING (if configured)
     * 3. If all fail, throw GoogleMapsException
     */
    @Override
    public DirectionResult getDirections(
            double originLat, double originLng,
            double destLat, double destLng,
            TransportMode mode
    ) {
        checkCircuitBreaker();
        log.debug("Getting directions from ({}, {}) to ({}, {}) via {}",
                originLat, originLng, destLat, destLng, mode);

        // Use Routes API if enabled
        if (properties.isUseRoutesApi()) {
            return getDirectionsViaRoutesApi(originLat, originLng, destLat, destLng, mode);
        }

        // Legacy: Use Distance Matrix API
        String origin = originLat + "," + originLng;
        String destination = destLat + "," + destLng;
        String baseUrl = buildDistanceMatrixUrlBase(origin, destination, mode);
        return executeDirectionsRequest(baseUrl, mode);
    }

    /**
     * Gets directions using the new Routes API (computeRouteMatrix).
     *
     * Implements fallback chain: TRANSIT → DRIVING when configured.
     *
     * @contract
     *   - pre: valid coordinates
     *   - post: Returns DirectionResult with apiSource = ROUTES_API
     *   - throws: GoogleMapsException if no route found after fallbacks
     */
    private DirectionResult getDirectionsViaRoutesApi(
            double originLat, double originLng,
            double destLat, double destLng,
            TransportMode mode
    ) {
        log.debug("[Routes API] Getting directions from ({}, {}) to ({}, {}) via {}",
                originLat, originLng, destLat, destLng, mode);

        try {
            DirectionResult result = executeRoutesApiRequest(originLat, originLng, destLat, destLng, mode);
            log.info("[Routes API] Success: {} ({}) via {}",
                    result.getDistanceText(), result.getDurationText(), mode);
            return result;

        } catch (GoogleMapsException e) {
            // Check if we should fallback to DRIVING for TRANSIT failures
            if (mode == TransportMode.TRANSIT &&
                    properties.getRoutesApi().isFallbackToDriving() &&
                    "NO_ROUTE".equals(e.getErrorCode())) {

                log.warn("[Routes API] TRANSIT failed ({}), falling back to DRIVING", e.getMessage());
                try {
                    DirectionResult fallbackResult = executeRoutesApiRequest(
                            originLat, originLng, destLat, destLng, TransportMode.DRIVING);
                    fallbackResult.setFromFallback(true);
                    log.info("[Routes API] DRIVING fallback success: {} ({})",
                            fallbackResult.getDistanceText(), fallbackResult.getDurationText());
                    return fallbackResult;
                } catch (GoogleMapsException fallbackEx) {
                    log.error("[Routes API] DRIVING fallback also failed: {}", fallbackEx.getMessage());
                    throw e; // Throw original exception
                }
            }
            throw e;
        }
    }

    /**
     * Executes a request to the Routes API computeRouteMatrix endpoint.
     *
     * @contract
     *   - pre: valid coordinates and mode
     *   - post: Returns DirectionResult
     *   - throws: GoogleMapsException on API error or no route
     */
    private DirectionResult executeRoutesApiRequest(
            double originLat, double originLng,
            double destLat, double destLng,
            TransportMode mode
    ) {
        try {
            // Build request body
            ObjectNode requestBody = objectMapper.createObjectNode();

            // Origins array
            ArrayNode origins = objectMapper.createArrayNode();
            ObjectNode originWaypoint = objectMapper.createObjectNode();
            ObjectNode originLocation = objectMapper.createObjectNode();
            ObjectNode originLatLng = objectMapper.createObjectNode();
            originLatLng.put("latitude", originLat);
            originLatLng.put("longitude", originLng);
            originLocation.set("latLng", originLatLng);
            originWaypoint.set("waypoint", objectMapper.createObjectNode().set("location", originLocation));
            origins.add(originWaypoint);
            requestBody.set("origins", origins);

            // Destinations array
            ArrayNode destinations = objectMapper.createArrayNode();
            ObjectNode destWaypoint = objectMapper.createObjectNode();
            ObjectNode destLocation = objectMapper.createObjectNode();
            ObjectNode destLatLng = objectMapper.createObjectNode();
            destLatLng.put("latitude", destLat);
            destLatLng.put("longitude", destLng);
            destLocation.set("latLng", destLatLng);
            destWaypoint.set("waypoint", objectMapper.createObjectNode().set("location", destLocation));
            destinations.add(destWaypoint);
            requestBody.set("destinations", destinations);

            // Travel mode
            String travelMode = mapTransportModeToRoutesApi(mode);
            requestBody.put("travelMode", travelMode);

            // Add departure time for TRANSIT (required for transit routes)
            if (mode == TransportMode.TRANSIT) {
                // Use current time + 10 minutes to ensure we get valid transit schedules
                String departureTime = Instant.now().plusSeconds(600).toString();
                requestBody.put("departureTime", departureTime);

                // Add transit preferences
                ObjectNode transitPreferences = objectMapper.createObjectNode();
                ArrayNode allowedModes = objectMapper.createArrayNode();
                for (String transitMode : properties.getRoutesApi().getDefaultTransitModes()) {
                    allowedModes.add(transitMode);
                }
                transitPreferences.set("allowedTravelModes", allowedModes);
                transitPreferences.put("routingPreference",
                        properties.getRoutesApi().getDefaultRoutingPreference());
                requestBody.set("transitPreferences", transitPreferences);
            }

            // Set routing preference for non-transit
            if (mode != TransportMode.TRANSIT) {
                requestBody.put("routingPreference", "TRAFFIC_AWARE");
            }

            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Goog-Api-Key", properties.getApiKey());
            headers.set("X-Goog-FieldMask",
                    "originIndex,destinationIndex,status,condition,distanceMeters,duration");

            HttpEntity<String> entity = new HttpEntity<>(
                    objectMapper.writeValueAsString(requestBody), headers);

            log.debug("[Routes API] Request body: {}", objectMapper.writeValueAsString(requestBody));

            ResponseEntity<String> response = restTemplate.exchange(
                    ROUTES_API_URL, HttpMethod.POST, entity, String.class);

            log.debug("[Routes API] Response: {}", response.getBody());

            // Parse response (array of elements)
            JsonNode root = objectMapper.readTree(response.getBody());

            // Routes API returns an array - get first element
            JsonNode firstElement;
            if (root.isArray() && root.size() > 0) {
                firstElement = root.get(0);
            } else {
                throw new GoogleMapsException("NO_ROUTE", "No route elements returned");
            }

            // Check status
            JsonNode statusNode = firstElement.path("status");
            if (!statusNode.isMissingNode()) {
                String statusCode = statusNode.path("code").asText("");
                if (!statusCode.isEmpty() && !"0".equals(statusCode)) {
                    String message = statusNode.path("message").asText("Unknown error");
                    throw new GoogleMapsException("API_ERROR", message);
                }
            }

            // Check condition
            String condition = firstElement.path("condition").asText("");
            if ("ROUTE_NOT_FOUND".equals(condition)) {
                throw new GoogleMapsException("NO_ROUTE",
                        "No route found between the specified locations");
            }

            // Extract distance and duration
            int distanceMeters = firstElement.path("distanceMeters").asInt(0);
            String durationStr = firstElement.path("duration").asText("0s");

            // Parse duration (format: "123s" for seconds)
            int durationSeconds = parseDurationString(durationStr);

            DirectionResult result = DirectionResult.builder()
                    .originAddress(String.format("%.6f, %.6f", originLat, originLng))
                    .destinationAddress(String.format("%.6f, %.6f", destLat, destLng))
                    .distanceMeters(distanceMeters)
                    .distanceText(formatDistance(distanceMeters))
                    .durationSeconds(durationSeconds)
                    .durationText(formatDuration(durationSeconds))
                    .transportMode(mode)
                    .apiSource(DirectionResult.ApiSource.ROUTES_API)
                    .build();
            recordSuccess();
            return result;

        } catch (GoogleMapsException e) {
            recordFailure();
            throw e;
        } catch (RestClientException e) {
            recordFailure();
            log.error("[Routes API] HTTP error: {}", e.getMessage());
            throw GoogleMapsException.apiError("HTTP error: " + e.getMessage());
        } catch (Exception e) {
            recordFailure();
            log.error("[Routes API] Error: {}", e.getMessage(), e);
            throw GoogleMapsException.apiError("Failed to get directions: " + e.getMessage());
        }
    }

    /**
     * Maps WeGo TransportMode to Routes API travelMode string.
     */
    private String mapTransportModeToRoutesApi(TransportMode mode) {
        return switch (mode) {
            case WALKING -> "WALK";
            case BICYCLING -> "BICYCLE";
            case DRIVING -> "DRIVE";
            case TRANSIT -> "TRANSIT";
            default -> "DRIVE";
        };
    }

    /**
     * Parses duration string from Routes API (format: "123s").
     */
    private int parseDurationString(String duration) {
        if (duration == null || duration.isEmpty()) {
            return 0;
        }
        // Remove 's' suffix and parse
        String numericPart = duration.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Formats distance in meters to human-readable text.
     */
    private String formatDistance(int meters) {
        if (meters < 1000) {
            return meters + " m";
        }
        return String.format("%.1f km", meters / 1000.0);
    }

    /**
     * Formats duration in seconds to human-readable text.
     */
    private String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return String.format("%d 小時 %d 分", hours, minutes);
        }
        return String.format("%d 分鐘", minutes);
    }

    /**
     * {@inheritDoc}
     *
     * Uses Places API (New) Text Search endpoint.
     * @see <a href="https://developers.google.com/maps/documentation/places/web-service/text-search">Text Search (New)</a>
     */
    @Override
    public List<PlaceSearchResult> searchPlaces(String query, double lat, double lng, int radiusMeters) {
        validateNotEmpty(query, "Query");
        if (radiusMeters <= 0) {
            throw new IllegalArgumentException("Radius must be positive");
        }
        checkCircuitBreaker();

        log.debug("Searching places for '{}' near ({}, {}) within {}m", query, lat, lng, radiusMeters);

        try {
            // Build request body for Places API (New)
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("textQuery", query);
            requestBody.put("pageSize", 20);
            requestBody.put("languageCode", "zh-TW");

            // Add location bias
            ObjectNode locationBias = objectMapper.createObjectNode();
            ObjectNode circle = objectMapper.createObjectNode();
            ObjectNode center = objectMapper.createObjectNode();
            center.put("latitude", lat);
            center.put("longitude", lng);
            circle.set("center", center);
            circle.put("radius", (double) radiusMeters);
            locationBias.set("circle", circle);
            requestBody.set("locationBias", locationBias);

            // Create headers for Places API (New)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Goog-Api-Key", properties.getApiKey());
            headers.set("X-Goog-FieldMask",
                    "places.id,places.displayName,places.formattedAddress," +
                    "places.location,places.rating,places.userRatingCount," +
                    "places.types,places.photos,places.regularOpeningHours");

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    PLACES_TEXT_SEARCH_URL, HttpMethod.POST, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());

            // Check for error response
            if (root.has("error")) {
                String errorMessage = root.path("error").path("message").asText("Unknown error");
                String errorStatus = root.path("error").path("status").asText("");
                log.error("Places API error: {} - {}", errorStatus, errorMessage);
                throw GoogleMapsException.apiError(errorMessage);
            }

            List<PlaceSearchResult> results = new ArrayList<>();
            JsonNode placesNode = root.path("places");

            if (placesNode.isMissingNode() || placesNode.isEmpty()) {
                log.debug("No places found for query: {}", query);
                return results;
            }

            for (JsonNode placeNode : placesNode) {
                PlaceSearchResult place = parsePlaceSearchResultNew(placeNode);
                results.add(place);
            }

            log.info("Found {} places for query '{}'", results.size(), query);
            recordSuccess();
            return results;

        } catch (GoogleMapsException e) {
            recordFailure();
            throw e;
        } catch (RestClientException e) {
            recordFailure();
            log.error("HTTP error searching places: {}", e.getMessage());
            throw GoogleMapsException.apiError("HTTP error: " + e.getMessage());
        } catch (Exception e) {
            recordFailure();
            log.error("Error searching places: {}", e.getMessage(), e);
            throw GoogleMapsException.apiError("Failed to search places: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * Uses Places API (New) Place Details endpoint.
     * @see <a href="https://developers.google.com/maps/documentation/places/web-service/place-details">Place Details (New)</a>
     */
    @Override
    public PlaceDetails getPlaceDetails(String placeId) {
        validateNotEmpty(placeId, "Place ID");
        checkCircuitBreaker();

        log.debug("Getting details for place: {}", placeId);

        try {
            // Create headers for Places API (New)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Goog-Api-Key", properties.getApiKey());
            headers.set("X-Goog-FieldMask",
                    "id,displayName,formattedAddress,nationalPhoneNumber," +
                    "internationalPhoneNumber,websiteUri,googleMapsUri,location," +
                    "rating,userRatingCount,priceLevel,types,photos,reviews," +
                    "regularOpeningHours,utcOffsetMinutes");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = PLACE_DETAILS_URL + placeId;

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());

            // Check for error response
            if (root.has("error")) {
                String errorMessage = root.path("error").path("message").asText("Unknown error");
                String errorStatus = root.path("error").path("status").asText("");
                if ("NOT_FOUND".equals(errorStatus)) {
                    throw new GoogleMapsException("NOT_FOUND", "Place not found: " + placeId);
                }
                log.error("Places API error: {} - {}", errorStatus, errorMessage);
                throw GoogleMapsException.apiError(errorMessage);
            }

            PlaceDetails details = parsePlaceDetailsNew(root);

            log.info("Retrieved details for place: {}", placeId);
            recordSuccess();
            return details;

        } catch (GoogleMapsException e) {
            recordFailure();
            throw e;
        } catch (RestClientException e) {
            recordFailure();
            log.error("HTTP error getting place details: {}", e.getMessage());
            throw GoogleMapsException.apiError("HTTP error: " + e.getMessage());
        } catch (Exception e) {
            recordFailure();
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

            DirectionResult result = DirectionResult.builder()
                    .originAddress(originAddress)
                    .destinationAddress(destinationAddress)
                    .distanceMeters(distanceMeters)
                    .distanceText(distanceText)
                    .durationSeconds(durationSeconds)
                    .durationText(durationText)
                    .transportMode(mode)
                    .apiSource(DirectionResult.ApiSource.DISTANCE_MATRIX)
                    .build();
            recordSuccess();
            return result;

        } catch (GoogleMapsException e) {
            recordFailure();
            throw e;
        } catch (RestClientException e) {
            recordFailure();
            log.error("HTTP error getting directions: {}", e.getMessage());
            throw GoogleMapsException.apiError("HTTP error: " + e.getMessage());
        } catch (Exception e) {
            recordFailure();
            log.error("Error getting directions: {}", e.getMessage(), e);
            throw GoogleMapsException.apiError("Failed to get directions: " + e.getMessage());
        }
    }

    /**
     * Parses a place search result from Places API (New) JSON response.
     */
    private PlaceSearchResult parsePlaceSearchResultNew(JsonNode node) {
        JsonNode locationNode = node.path("location");
        JsonNode photosNode = node.path("photos");
        JsonNode openingHoursNode = node.path("regularOpeningHours");

        List<String> types = new ArrayList<>();
        for (JsonNode typeNode : node.path("types")) {
            types.add(typeNode.asText());
        }

        // In Places API (New), photos have a "name" field that serves as photo reference
        String photoReference = null;
        if (photosNode.isArray() && photosNode.size() > 0) {
            // Format: places/{placeId}/photos/{photoReference}
            String photoName = photosNode.get(0).path("name").asText();
            if (!photoName.isEmpty()) {
                photoReference = photoName;
            }
        }

        Boolean isOpen = null;
        if (!openingHoursNode.isMissingNode()) {
            isOpen = openingHoursNode.path("openNow").asBoolean();
        }

        return PlaceSearchResult.builder()
                .placeId(node.path("id").asText())
                .name(node.path("displayName").path("text").asText())
                .address(node.path("formattedAddress").asText())
                .latitude(locationNode.path("latitude").asDouble())
                .longitude(locationNode.path("longitude").asDouble())
                .rating(node.path("rating").asDouble())
                .userRatingsTotal(node.path("userRatingCount").asInt())
                .types(types)
                .photoReference(photoReference)
                .isOpen(isOpen)
                .build();
    }

    /**
     * Parses place details from Places API (New) JSON response.
     */
    private PlaceDetails parsePlaceDetailsNew(JsonNode node) {
        JsonNode locationNode = node.path("location");

        // Parse types
        List<String> types = new ArrayList<>();
        for (JsonNode typeNode : node.path("types")) {
            types.add(typeNode.asText());
        }

        // Parse photo references (format: places/{placeId}/photos/{photoReference})
        List<String> photoReferences = new ArrayList<>();
        for (JsonNode photoNode : node.path("photos")) {
            String photoName = photoNode.path("name").asText();
            if (!photoName.isEmpty()) {
                photoReferences.add(photoName);
            }
        }

        // Parse reviews
        List<PlaceDetails.Review> reviews = new ArrayList<>();
        for (JsonNode reviewNode : node.path("reviews")) {
            PlaceDetails.Review review = PlaceDetails.Review.builder()
                    .authorName(reviewNode.path("authorAttribution").path("displayName").asText())
                    .rating(reviewNode.path("rating").asInt())
                    .text(reviewNode.path("text").path("text").asText())
                    .relativeTimeDescription(reviewNode.path("relativePublishTimeDescription").asText())
                    .build();
            reviews.add(review);
        }

        // Parse opening hours
        PlaceDetails.OpeningHours openingHours = null;
        JsonNode openingHoursNode = node.path("regularOpeningHours");
        if (!openingHoursNode.isMissingNode()) {
            List<String> weekdayText = new ArrayList<>();
            for (JsonNode dayNode : openingHoursNode.path("weekdayDescriptions")) {
                weekdayText.add(dayNode.asText());
            }

            openingHours = PlaceDetails.OpeningHours.builder()
                    .isOpenNow(openingHoursNode.path("openNow").asBoolean())
                    .weekdayText(weekdayText)
                    .build();
        }

        // Parse price level (enum in new API: PRICE_LEVEL_FREE, PRICE_LEVEL_INEXPENSIVE, etc.)
        int priceLevel = 0;
        String priceLevelStr = node.path("priceLevel").asText("");
        if (priceLevelStr.contains("INEXPENSIVE")) priceLevel = 1;
        else if (priceLevelStr.contains("MODERATE")) priceLevel = 2;
        else if (priceLevelStr.contains("EXPENSIVE")) priceLevel = 3;
        else if (priceLevelStr.contains("VERY_EXPENSIVE")) priceLevel = 4;

        return PlaceDetails.builder()
                .placeId(node.path("id").asText())
                .name(node.path("displayName").path("text").asText())
                .formattedAddress(node.path("formattedAddress").asText())
                .formattedPhoneNumber(node.path("nationalPhoneNumber").asText())
                .internationalPhoneNumber(node.path("internationalPhoneNumber").asText())
                .website(node.path("websiteUri").asText())
                .url(node.path("googleMapsUri").asText())
                .latitude(locationNode.path("latitude").asDouble())
                .longitude(locationNode.path("longitude").asDouble())
                .rating(node.path("rating").asDouble())
                .userRatingsTotal(node.path("userRatingCount").asInt())
                .priceLevel(priceLevel)
                .types(types)
                .photoReferences(photoReferences)
                .reviews(reviews.isEmpty() ? null : reviews)
                .openingHours(openingHours)
                .utcOffset(node.path("utcOffsetMinutes").asInt())
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

    /**
     * Checks if the circuit breaker is open.
     * Rejects requests if too many consecutive failures.
     */
    private void checkCircuitBreaker() {
        long openedAt = circuitOpenedAt.get();
        if (openedAt > 0) {
            if (System.currentTimeMillis() - openedAt < CIRCUIT_BREAKER_COOLDOWN_MS) {
                log.warn("Google Maps circuit breaker is open, rejecting request");
                throw GoogleMapsException.apiError("Service temporarily unavailable (circuit breaker open)");
            } else {
                log.info("Google Maps circuit breaker cooldown passed, attempting request");
            }
        }
    }

    private void recordSuccess() {
        consecutiveFailures.set(0);
        circuitOpenedAt.set(0);
    }

    private void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= CIRCUIT_BREAKER_THRESHOLD) {
            long now = System.currentTimeMillis();
            if (circuitOpenedAt.compareAndSet(0, now)) {
                log.warn("Google Maps circuit breaker opened after {} consecutive failures", failures);
            }
        }
    }
}
