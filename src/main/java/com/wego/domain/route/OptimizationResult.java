package com.wego.domain.route;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

/**
 * Value object representing the result of route optimization.
 *
 * Contains both the original and optimized activity orders,
 * along with distance comparisons and any warnings.
 *
 * @contract
 *   - invariant: originalOrder and optimizedOrder have same size
 *   - invariant: originalDistanceMeters >= 0
 *   - invariant: optimizedDistanceMeters >= 0
 *   - invariant: distanceSavedMeters = originalDistanceMeters - optimizedDistanceMeters
 *   - immutable
 */
@Getter
@Builder
@EqualsAndHashCode
@ToString
public class OptimizationResult {

    /**
     * The original order of activity IDs.
     */
    private final List<UUID> originalOrder;

    /**
     * The optimized order of activity IDs.
     */
    private final List<UUID> optimizedOrder;

    /**
     * Total distance in meters for the original route.
     */
    private final double originalDistanceMeters;

    /**
     * Total distance in meters for the optimized route.
     */
    private final double optimizedDistanceMeters;

    /**
     * Distance saved by optimization in meters.
     */
    private final double distanceSavedMeters;

    /**
     * Percentage of distance saved (0-100).
     */
    private final double savingsPercentage;

    /**
     * Warning message if any constraints were exceeded.
     */
    private final String warningMessage;

    /**
     * Whether optimization was actually performed.
     * False if original order was already optimal or too few activities.
     */
    private final boolean optimizationApplied;

    /**
     * Creates an OptimizationResult with calculated savings.
     *
     * @contract
     *   - pre: originalOrder != null, optimizedOrder != null
     *   - pre: originalOrder.size() == optimizedOrder.size()
     *   - post: savingsPercentage is calculated correctly
     *
     * @param originalOrder The original activity order
     * @param optimizedOrder The optimized activity order
     * @param originalDistanceMeters Total original distance
     * @param optimizedDistanceMeters Total optimized distance
     * @param warningMessage Optional warning message
     * @param optimizationApplied Whether optimization changed the order
     * @return OptimizationResult with all fields calculated
     */
    public static OptimizationResult create(
            List<UUID> originalOrder,
            List<UUID> optimizedOrder,
            double originalDistanceMeters,
            double optimizedDistanceMeters,
            String warningMessage,
            boolean optimizationApplied) {

        double distanceSaved = originalDistanceMeters - optimizedDistanceMeters;
        double savingsPercentage = originalDistanceMeters > 0
                ? (distanceSaved / originalDistanceMeters) * 100.0
                : 0.0;

        return OptimizationResult.builder()
                .originalOrder(List.copyOf(originalOrder))
                .optimizedOrder(List.copyOf(optimizedOrder))
                .originalDistanceMeters(originalDistanceMeters)
                .optimizedDistanceMeters(optimizedDistanceMeters)
                .distanceSavedMeters(distanceSaved)
                .savingsPercentage(savingsPercentage)
                .warningMessage(warningMessage)
                .optimizationApplied(optimizationApplied)
                .build();
    }

    /**
     * Creates a no-op result when optimization is not needed.
     *
     * @contract
     *   - pre: originalOrder != null
     *   - post: optimizedOrder == originalOrder
     *   - post: optimizationApplied == false
     *
     * @param originalOrder The original activity order
     * @param totalDistance The total route distance
     * @return OptimizationResult with no changes
     */
    public static OptimizationResult noOptimizationNeeded(List<UUID> originalOrder, double totalDistance) {
        return OptimizationResult.builder()
                .originalOrder(List.copyOf(originalOrder))
                .optimizedOrder(List.copyOf(originalOrder))
                .originalDistanceMeters(totalDistance)
                .optimizedDistanceMeters(totalDistance)
                .distanceSavedMeters(0.0)
                .savingsPercentage(0.0)
                .warningMessage(null)
                .optimizationApplied(false)
                .build();
    }
}
