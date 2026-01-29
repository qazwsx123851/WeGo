package com.wego.dto.response;

import com.wego.entity.TransportMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the result of a directions/routing request.
 *
 * Contains distance, duration, and route information between two locations.
 *
 * @contract
 *   - distanceMeters: Direct distance in meters (>= 0)
 *   - durationSeconds: Estimated travel time in seconds (>= 0)
 *   - transportMode: The mode of transportation used for calculation
 *
 * @see com.wego.service.external.GoogleMapsClient
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectionResult {

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
}
