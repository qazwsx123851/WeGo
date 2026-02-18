package com.wego.domain.route;

import com.wego.domain.geo.GeoUtils;
import com.wego.entity.Activity;
import com.wego.entity.Place;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Domain service for optimizing activity routes using Greedy Nearest Neighbor algorithm.
 *
 * Calculates the shortest route through a set of activities by always
 * choosing the nearest unvisited location next. The first activity is
 * preserved as the starting point.
 *
 * @contract
 *   - pre: activities have valid places with lat/lng coordinates
 *   - post: optimized route minimizes total travel distance
 *   - post: first activity remains first (starting point preserved)
 *   - thread-safe (stateless)
 *
 * @see OptimizationResult
 */
@Component
public class RouteOptimizer {

    /**
     * Maximum recommended activities per day.
     * Optimization still works above this limit but adds a warning.
     */
    public static final int MAX_ACTIVITIES_PER_DAY = 15;


    /**
     * Optimizes the route order for a list of activities.
     *
     * Algorithm (Greedy Nearest Neighbor):
     * 1. If activities.size() <= 2, return original order (no optimization needed)
     * 2. If activities.size() > 15, add warning message
     * 3. Start from first activity (preserve user's starting point)
     * 4. While unvisited activities remain:
     *    a. Find nearest unvisited activity using Haversine distance
     *    b. Add to optimized route
     *    c. Mark as visited
     * 5. Calculate total distances for both orders
     * 6. Return OptimizationResult with comparison
     *
     * Time complexity: O(n^2) where n is number of activities
     *
     * @contract
     *   - pre: activities != null
     *   - pre: placeLookup contains places for all activities with non-null placeId
     *   - post: returns OptimizationResult with valid order
     *   - post: if activities.size() <= 2, optimizationApplied is false
     *   - calledBy: ActivityService#getOptimizedRoute
     *
     * @param activities The activities to optimize (ordered by current sortOrder)
     * @param placeLookup Map of placeId to Place entity
     * @return OptimizationResult containing original and optimized orders
     */
    public OptimizationResult optimize(List<Activity> activities, Map<UUID, Place> placeLookup) {
        if (activities == null || activities.isEmpty()) {
            return OptimizationResult.noOptimizationNeeded(List.of(), 0.0);
        }

        // Filter out activities without valid places
        List<Activity> validActivities = filterActivitiesWithValidPlaces(activities, placeLookup);

        if (validActivities.isEmpty()) {
            List<UUID> originalOrder = activities.stream()
                    .map(Activity::getId)
                    .toList();
            return OptimizationResult.noOptimizationNeeded(originalOrder, 0.0);
        }

        // Extract original order
        List<UUID> originalOrder = validActivities.stream()
                .map(Activity::getId)
                .toList();

        // If 2 or fewer activities, no optimization possible
        if (validActivities.size() <= 2) {
            double totalDistance = calculateTotalDistance(validActivities, placeLookup);
            return OptimizationResult.noOptimizationNeeded(originalOrder, totalDistance);
        }

        // Check for warning condition
        String warningMessage = null;
        if (validActivities.size() > MAX_ACTIVITIES_PER_DAY) {
            warningMessage = String.format(
                    "Warning: %d activities exceeds recommended maximum of %d per day. " +
                    "Consider splitting into multiple days for better planning.",
                    validActivities.size(), MAX_ACTIVITIES_PER_DAY);
        }

        // Build activity lookup map
        Map<UUID, Activity> activityMap = new HashMap<>();
        for (Activity activity : validActivities) {
            activityMap.put(activity.getId(), activity);
        }

        // Apply Greedy Nearest Neighbor algorithm
        List<UUID> optimizedOrder = applyNearestNeighbor(validActivities, placeLookup);

        // Calculate distances
        double originalDistance = calculateTotalDistance(validActivities, placeLookup);
        List<Activity> optimizedActivities = optimizedOrder.stream()
                .map(activityMap::get)
                .toList();
        double optimizedDistance = calculateTotalDistance(optimizedActivities, placeLookup);

        // Check if optimization actually improved the route
        boolean optimizationApplied = optimizedDistance < originalDistance;

        // If no improvement, return original order
        if (!optimizationApplied) {
            return OptimizationResult.create(
                    originalOrder,
                    originalOrder,
                    originalDistance,
                    originalDistance,
                    warningMessage,
                    false);
        }

        return OptimizationResult.create(
                originalOrder,
                optimizedOrder,
                originalDistance,
                optimizedDistance,
                warningMessage,
                true);
    }

    /**
     * Applies the Greedy Nearest Neighbor algorithm to find an optimized route.
     *
     * @contract
     *   - pre: activities.size() >= 3
     *   - pre: all activities have valid places in placeLookup
     *   - post: returns list with same size as input
     *   - post: first element is always first activity (starting point)
     *
     * @param activities The activities to optimize
     * @param placeLookup Map of placeId to Place
     * @return Optimized order of activity IDs
     */
    private List<UUID> applyNearestNeighbor(List<Activity> activities, Map<UUID, Place> placeLookup) {
        List<UUID> optimizedOrder = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();

        // Start with the first activity (preserve starting point)
        Activity current = activities.get(0);
        optimizedOrder.add(current.getId());
        visited.add(current.getId());

        // Find nearest neighbor for remaining activities
        while (visited.size() < activities.size()) {
            Place currentPlace = placeLookup.get(current.getPlaceId());
            Activity nearest = null;
            double minDistance = Double.MAX_VALUE;

            for (Activity candidate : activities) {
                if (visited.contains(candidate.getId())) {
                    continue;
                }

                Place candidatePlace = placeLookup.get(candidate.getPlaceId());
                if (candidatePlace == null) {
                    continue;
                }

                double distance = calculateDistanceMeters(
                        currentPlace.getLatitude(), currentPlace.getLongitude(),
                        candidatePlace.getLatitude(), candidatePlace.getLongitude());

                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = candidate;
                }
            }

            if (nearest != null) {
                optimizedOrder.add(nearest.getId());
                visited.add(nearest.getId());
                current = nearest;
            } else {
                // Should not happen if preconditions are met
                break;
            }
        }

        return optimizedOrder;
    }

    /**
     * Filters activities to only include those with valid places.
     *
     * @param activities The activities to filter
     * @param placeLookup Map of placeId to Place
     * @return List of activities with valid places
     */
    private List<Activity> filterActivitiesWithValidPlaces(
            List<Activity> activities, Map<UUID, Place> placeLookup) {
        return activities.stream()
                .filter(a -> a.getPlaceId() != null)
                .filter(a -> placeLookup.containsKey(a.getPlaceId()))
                .filter(a -> {
                    Place place = placeLookup.get(a.getPlaceId());
                    return isValidCoordinate(place.getLatitude(), place.getLongitude());
                })
                .toList();
    }

    /**
     * Checks if coordinates are valid (non-zero).
     *
     * @param lat Latitude
     * @param lng Longitude
     * @return true if coordinates are valid
     */
    private boolean isValidCoordinate(double lat, double lng) {
        // Check for valid coordinate ranges
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

    /**
     * Calculates the total distance for a route through activities.
     *
     * @contract
     *   - pre: activities != null
     *   - pre: placeLookup contains places for activities
     *   - post: returns distance >= 0
     *
     * @param activities The ordered list of activities
     * @param placeLookup Map of placeId to Place
     * @return Total distance in meters
     */
    public double calculateTotalDistance(List<Activity> activities, Map<UUID, Place> placeLookup) {
        if (activities == null || activities.size() < 2) {
            return 0.0;
        }

        double totalDistance = 0.0;

        for (int i = 0; i < activities.size() - 1; i++) {
            Activity current = activities.get(i);
            Activity next = activities.get(i + 1);

            Place currentPlace = placeLookup.get(current.getPlaceId());
            Place nextPlace = placeLookup.get(next.getPlaceId());

            if (currentPlace != null && nextPlace != null) {
                totalDistance += calculateDistanceMeters(
                        currentPlace.getLatitude(), currentPlace.getLongitude(),
                        nextPlace.getLatitude(), nextPlace.getLongitude());
            }
        }

        return totalDistance;
    }

    /**
     * Calculates the distance between two points using Haversine formula.
     *
     * The Haversine formula determines the great-circle distance between two points
     * on a sphere given their longitudes and latitudes.
     *
     * @contract
     *   - pre: lat1, lat2 in range [-90, 90]
     *   - pre: lng1, lng2 in range [-180, 180]
     *   - post: returns distance >= 0
     *
     * @param lat1 Latitude of point 1 in degrees
     * @param lng1 Longitude of point 1 in degrees
     * @param lat2 Latitude of point 2 in degrees
     * @param lng2 Longitude of point 2 in degrees
     * @return Distance in meters
     */
    public double calculateDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
        return GeoUtils.haversineDistanceMeters(lat1, lng1, lat2, lng2);
    }
}
