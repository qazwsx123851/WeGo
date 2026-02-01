package com.wego.dto.response;

import com.wego.entity.TransportMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing the result of a directions/routing request.
 *
 * Contains distance, duration, and route information between two locations.
 * When using TRANSIT mode with Routes API, may include transit-specific details.
 *
 * @contract
 *   - distanceMeters: Direct distance in meters (>= 0)
 *   - durationSeconds: Estimated travel time in seconds (>= 0)
 *   - transportMode: The mode of transportation used for calculation
 *   - transitDetails: Optional, only populated for TRANSIT routes via Routes API
 *   - apiSource: Indicates which API was used (DISTANCE_MATRIX or ROUTES_API)
 *
 * @see com.wego.service.external.GoogleMapsClient
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectionResult {

    /**
     * Indicates which Google API was used for the calculation.
     */
    public enum ApiSource {
        /** Legacy Distance Matrix API */
        DISTANCE_MATRIX,
        /** New Routes API (computeRouteMatrix) */
        ROUTES_API
    }

    /**
     * The starting point address (resolved or provided).
     */
    private String originAddress;

    /**
     * The destination address (resolved or provided).
     */
    private String destinationAddress;

    /**
     * Distance between origin and destination in meters.
     */
    private int distanceMeters;

    /**
     * Human-readable distance text (e.g., "5.4 km").
     */
    private String distanceText;

    /**
     * Estimated travel duration in seconds.
     */
    private int durationSeconds;

    /**
     * Human-readable duration text (e.g., "15 mins").
     */
    private String durationText;

    /**
     * The transportation mode used for this route calculation.
     */
    private TransportMode transportMode;

    /**
     * Transit-specific details (only for TRANSIT mode via Routes API).
     * May contain multiple segments if the route involves transfers.
     */
    private List<TransitDetails> transitDetails;

    /**
     * Which API was used to obtain this result.
     * Defaults to DISTANCE_MATRIX for backward compatibility.
     */
    @Builder.Default
    private ApiSource apiSource = ApiSource.DISTANCE_MATRIX;

    /**
     * Whether this result was obtained from a fallback mechanism.
     * True if the primary API failed and a secondary method was used.
     */
    @Builder.Default
    private boolean fromFallback = false;
}
