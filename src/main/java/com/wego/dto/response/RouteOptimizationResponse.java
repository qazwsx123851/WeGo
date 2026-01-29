package com.wego.dto.response;

import com.wego.domain.route.OptimizationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for route optimization results.
 *
 * Contains both the original and optimized activity orders with
 * distance metrics for comparison.
 *
 * @contract
 *   - originalOrder and optimizedOrder always have same size
 *   - all distance values are in meters
 *   - savingsPercentage is between 0 and 100
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteOptimizationResponse {

    /**
     * The trip ID for this optimization.
     */
    private UUID tripId;

    /**
     * The day number being optimized.
     */
    private int day;

    /**
     * The original order of activity IDs.
     */
    private List<UUID> originalOrder;

    /**
     * The optimized order of activity IDs.
     */
    private List<UUID> optimizedOrder;

    /**
     * Total distance in meters for the original route.
     */
    private double originalDistanceMeters;

    /**
     * Total distance in meters for the optimized route.
     */
    private double optimizedDistanceMeters;

    /**
     * Distance saved by optimization in meters.
     */
    private double distanceSavedMeters;

    /**
     * Percentage of distance saved (0-100).
     */
    private double savingsPercentage;

    /**
     * Human-readable original distance (e.g., "2.5 km").
     */
    private String originalDistanceFormatted;

    /**
     * Human-readable optimized distance (e.g., "1.8 km").
     */
    private String optimizedDistanceFormatted;

    /**
     * Human-readable saved distance (e.g., "700 m").
     */
    private String distanceSavedFormatted;

    /**
     * Warning message if any constraints were exceeded.
     */
    private String warningMessage;

    /**
     * Whether optimization was actually performed.
     */
    private boolean optimizationApplied;

    /**
     * Number of activities in the route.
     */
    private int activityCount;

    /**
     * Creates a RouteOptimizationResponse from an OptimizationResult.
     *
     * @contract
     *   - pre: result != null
     *   - pre: tripId != null
     *   - pre: day >= 1
     *   - post: returns fully populated response
     *   - calledBy: ActivityService#getOptimizedRoute
     *
     * @param result The domain optimization result
     * @param tripId The trip ID
     * @param day The day number
     * @return RouteOptimizationResponse DTO
     */
    public static RouteOptimizationResponse fromResult(OptimizationResult result, UUID tripId, int day) {
        if (result == null) {
            return null;
        }

        return RouteOptimizationResponse.builder()
                .tripId(tripId)
                .day(day)
                .originalOrder(result.getOriginalOrder())
                .optimizedOrder(result.getOptimizedOrder())
                .originalDistanceMeters(result.getOriginalDistanceMeters())
                .optimizedDistanceMeters(result.getOptimizedDistanceMeters())
                .distanceSavedMeters(result.getDistanceSavedMeters())
                .savingsPercentage(Math.round(result.getSavingsPercentage() * 100.0) / 100.0)
                .originalDistanceFormatted(formatDistance(result.getOriginalDistanceMeters()))
                .optimizedDistanceFormatted(formatDistance(result.getOptimizedDistanceMeters()))
                .distanceSavedFormatted(formatDistance(result.getDistanceSavedMeters()))
                .warningMessage(result.getWarningMessage())
                .optimizationApplied(result.isOptimizationApplied())
                .activityCount(result.getOriginalOrder().size())
                .build();
    }

    /**
     * Formats a distance in meters to a human-readable string.
     *
     * @contract
     *   - post: returns "X.X km" for distances >= 1000m
     *   - post: returns "X m" for distances < 1000m
     *   - post: returns "0 m" for zero or negative distances
     *
     * @param meters The distance in meters
     * @return Formatted distance string
     */
    public static String formatDistance(double meters) {
        if (meters < 0) {
            return "0 m";
        }
        if (meters >= 1000) {
            return String.format("%.1f km", meters / 1000.0);
        }
        return String.format("%.0f m", meters);
    }
}
