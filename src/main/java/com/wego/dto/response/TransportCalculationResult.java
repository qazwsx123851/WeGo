package com.wego.dto.response;

import com.wego.entity.TransportMode;
import com.wego.entity.TransportSource;
import com.wego.entity.TransportWarning;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of transport calculation between two activities.
 *
 * Contains distance, duration, source tracking, and warnings
 * for unrealistic or estimated routes.
 *
 * @contract
 *   - post: distanceMeters >= 0
 *   - post: durationMinutes >= 0
 *   - post: source is never null
 *   - post: warning is never null
 *   - calledBy: TransportCalculationService
 *
 * @see TransportSource
 * @see TransportWarning
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportCalculationResult {

    /**
     * Distance in meters.
     */
    private Integer distanceMeters;

    /**
     * Duration in minutes.
     */
    private Integer durationMinutes;

    /**
     * Human-readable distance text (e.g., "2.5 km").
     */
    private String distanceText;

    /**
     * Human-readable duration text (e.g., "30 min").
     */
    private String durationText;

    /**
     * The transport mode used for calculation.
     */
    private TransportMode transportMode;

    /**
     * Source of the calculation data.
     * - GOOGLE_API: From Google Maps Directions API
     * - HAVERSINE: Straight-line distance estimate
     * - MANUAL: User-provided value
     * - NOT_APPLICABLE: First activity or skipped calculation
     */
    @Builder.Default
    private TransportSource source = TransportSource.NOT_APPLICABLE;

    /**
     * Warning about the calculation result.
     * - NONE: No issues
     * - ESTIMATED_DISTANCE: Using Haversine (straight-line)
     * - UNREALISTIC_WALKING: Walking > 5km
     * - UNREALISTIC_BICYCLING: Bicycling > 30km
     * - VERY_LONG_DISTANCE: Any mode > 100km
     * - NO_ROUTE_AVAILABLE: Google API couldn't find route
     */
    @Builder.Default
    private TransportWarning warning = TransportWarning.NONE;

    /**
     * Creates a result for first activity of the day (no transport needed).
     *
     * @return Result with NOT_APPLICABLE source
     */
    public static TransportCalculationResult notApplicable() {
        return TransportCalculationResult.builder()
                .source(TransportSource.NOT_APPLICABLE)
                .warning(TransportWarning.NONE)
                .build();
    }

    /**
     * Creates a result for manual input.
     *
     * @param durationMinutes User-provided duration
     * @param mode Transport mode
     * @return Result with MANUAL source
     */
    public static TransportCalculationResult manual(Integer durationMinutes, TransportMode mode) {
        return TransportCalculationResult.builder()
                .durationMinutes(durationMinutes)
                .durationText(formatDuration(durationMinutes))
                .transportMode(mode)
                .source(TransportSource.MANUAL)
                .warning(TransportWarning.NONE)
                .build();
    }

    /**
     * Checks if the transport data is estimated (not from Google API).
     *
     * @return true if source is HAVERSINE or MANUAL
     */
    public boolean isEstimated() {
        return source == TransportSource.HAVERSINE || source == TransportSource.MANUAL;
    }

    /**
     * Checks if there's a warning that should be displayed.
     *
     * @return true if warning is not NONE
     */
    public boolean hasWarning() {
        return warning != null && warning != TransportWarning.NONE;
    }

    private static String formatDuration(Integer minutes) {
        if (minutes == null || minutes == 0) {
            return null;
        }
        if (minutes >= 60) {
            int hours = minutes / 60;
            int mins = minutes % 60;
            return mins > 0 ? hours + " 小時 " + mins + " 分鐘" : hours + " 小時";
        }
        return minutes + " 分鐘";
    }
}
