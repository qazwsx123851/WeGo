package com.wego.service;

import com.wego.dto.response.DirectionResult;
import com.wego.dto.response.TransportCalculationResult;
import com.wego.entity.Activity;
import com.wego.entity.Place;
import com.wego.entity.TransportMode;
import com.wego.entity.TransportSource;
import com.wego.entity.TransportWarning;
import com.wego.repository.ActivityRepository;
import com.wego.repository.PlaceRepository;
import com.wego.service.external.GoogleMapsClient;
import com.wego.service.external.GoogleMapsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.wego.dto.response.RecalculationResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for calculating transport information between activities.
 *
 * Calculates travel duration and distance using Google Maps Directions API,
 * with fallback to Haversine formula when API is unavailable.
 * Tracks calculation source and determines appropriate warnings.
 *
 * @contract
 *   - All calculations are based on Place coordinates
 *   - Falls back to Haversine distance if Google API fails
 *   - First activity of each day has null transport info (no previous activity)
 *   - Tracks source (GOOGLE_API, HAVERSINE, MANUAL, NOT_APPLICABLE)
 *   - Determines warnings based on distance and mode thresholds
 *   - calledBy: ActivityService
 *   - calls: GoogleMapsClient, PlaceRepository, ActivityRepository
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransportCalculationService {

    // Speed constants (km/h)
    private static final double WALKING_SPEED_KMH = 5.0;
    private static final double BICYCLING_SPEED_KMH = 15.0;
    private static final double TRANSIT_SPEED_KMH = 25.0;
    private static final double DRIVING_SPEED_KMH = 40.0;
    private static final double HIGH_SPEED_RAIL_SPEED_KMH = 250.0;
    private static final double FLIGHT_SPEED_KMH = 800.0;

    // Warning thresholds (meters)
    private static final double WALKING_WARNING_THRESHOLD_METERS = 5_000;      // 5 km
    private static final double BICYCLING_WARNING_THRESHOLD_METERS = 30_000;   // 30 km
    private static final double LONG_DISTANCE_WARNING_THRESHOLD_METERS = 100_000; // 100 km

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final GoogleMapsClient googleMapsClient;
    private final PlaceRepository placeRepository;
    private final ActivityRepository activityRepository;

    /**
     * Calculates transport info from the previous activity to the given activity.
     *
     * Uses Google Maps Directions API if available, falls back to Haversine calculation.
     * Sets transportSource and transportWarning fields on the activity.
     *
     * @contract
     *   - pre: activity != null
     *   - pre: activity.tripId != null
     *   - pre: activity.day >= 1
     *   - post: Updates activity.transportDurationMinutes, transportDistanceMeters,
     *           transportSource, and transportWarning
     *   - post: If no previous activity or no coordinates, fields are set to null/NOT_APPLICABLE
     *   - calls: GoogleMapsClient#getDirections with fallback to calculateHaversineFallback
     *   - calledBy: ActivityService#createActivity
     *
     * @param activity The activity to calculate transport for
     * @return Activity with updated transport fields (same instance, mutated)
     */
    public Activity calculateTransportFromPrevious(Activity activity) {
        if (activity == null || activity.getPlaceId() == null) {
            log.debug("Activity or placeId is null, skipping transport calculation");
            return activity;
        }

        // Handle NOT_CALCULATED mode - skip calculation entirely
        if (activity.getTransportMode() == TransportMode.NOT_CALCULATED) {
            log.debug("Transport mode is NOT_CALCULATED, skipping calculation");
            clearTransportInfo(activity);
            activity.setTransportSource(TransportSource.NOT_APPLICABLE);
            activity.setTransportWarning(TransportWarning.NONE);
            return activity;
        }

        // Find the previous activity in the same day by sortOrder
        List<Activity> dayActivities = activityRepository
                .findByTripIdAndDayOrderBySortOrderAsc(activity.getTripId(), activity.getDay());

        Optional<Activity> previousOpt = dayActivities.stream()
                .filter(a -> a.getSortOrder() < activity.getSortOrder())
                .reduce((first, second) -> second); // Get the last one (highest sortOrder before current)

        if (previousOpt.isEmpty()) {
            // First activity of the day - no transport info needed
            log.debug("No previous activity found for activity {}, clearing transport info", activity.getId());
            clearTransportInfo(activity);
            activity.setTransportSource(TransportSource.NOT_APPLICABLE);
            activity.setTransportWarning(TransportWarning.NONE);
            return activity;
        }

        Activity previous = previousOpt.get();
        if (previous.getPlaceId() == null) {
            log.debug("Previous activity has no place, clearing transport info");
            clearTransportInfo(activity);
            activity.setTransportSource(TransportSource.NOT_APPLICABLE);
            activity.setTransportWarning(TransportWarning.NONE);
            return activity;
        }

        // Get places for both activities
        Optional<Place> fromPlaceOpt = placeRepository.findById(previous.getPlaceId());
        Optional<Place> toPlaceOpt = placeRepository.findById(activity.getPlaceId());

        if (fromPlaceOpt.isEmpty() || toPlaceOpt.isEmpty()) {
            log.warn("Place not found for activity transport calculation");
            return activity;
        }

        Place fromPlace = fromPlaceOpt.get();
        Place toPlace = toPlaceOpt.get();

        // Validate coordinates
        if (!hasValidCoordinates(fromPlace) || !hasValidCoordinates(toPlace)) {
            log.debug("Invalid coordinates for transport calculation");
            clearTransportInfo(activity);
            activity.setTransportSource(TransportSource.NOT_APPLICABLE);
            activity.setTransportWarning(TransportWarning.NONE);
            return activity;
        }

        // Calculate transport info with warnings
        TransportMode mode = activity.getTransportMode() != null
                ? activity.getTransportMode()
                : TransportMode.WALKING;

        TransportCalculationResult result = calculateTransportWithWarnings(
                fromPlace.getLatitude(), fromPlace.getLongitude(),
                toPlace.getLatitude(), toPlace.getLongitude(),
                mode
        );

        // Apply result to activity
        applyResultToActivity(activity, result);

        log.debug("Calculated transport for activity {}: {} minutes, {} meters, source={}, warning={}",
                activity.getId(), activity.getTransportDurationMinutes(),
                activity.getTransportDistanceMeters(), activity.getTransportSource(),
                activity.getTransportWarning());

        return activity;
    }

    /**
     * Calculates transport with full warning and source tracking.
     *
     * @contract
     *   - pre: valid coordinates
     *   - post: returns TransportCalculationResult with source and warning set
     *   - calls: GoogleMapsClient#getDirections with fallback to Haversine
     *   - calledBy: calculateTransportFromPrevious, batchCalculateTransport
     *
     * @param originLat Origin latitude
     * @param originLng Origin longitude
     * @param destLat Destination latitude
     * @param destLng Destination longitude
     * @param mode Transport mode
     * @return Calculation result with source and warning
     */
    public TransportCalculationResult calculateTransportWithWarnings(
            double originLat, double originLng,
            double destLat, double destLng,
            TransportMode mode) {

        // Handle modes that don't support auto-calculation (FLIGHT, HIGH_SPEED_RAIL)
        // These modes require manual input - do NOT use Haversine estimation
        if (!mode.supportsAutoCalculation()) {
            log.debug("Mode {} does not support auto-calculation, returning NOT_APPLICABLE (requires manual input)", mode);
            return TransportCalculationResult.builder()
                    .distanceMeters(null)
                    .durationMinutes(null)
                    .distanceText(null)
                    .durationText(null)
                    .transportMode(mode)
                    .source(TransportSource.NOT_APPLICABLE)
                    .warning(TransportWarning.NONE)
                    .build();
        }

        // Try Google Maps API first
        TransportSource source = TransportSource.GOOGLE_API;
        DirectionResult directionResult = null;
        double distanceMeters;
        int durationSeconds;

        try {
            directionResult = googleMapsClient.getDirections(
                    originLat, originLng, destLat, destLng, mode);
            distanceMeters = directionResult.getDistanceMeters();
            durationSeconds = directionResult.getDurationSeconds();
        } catch (GoogleMapsException e) {
            log.warn("Google Maps API failed, using Haversine fallback: {}", e.getMessage());
            source = TransportSource.HAVERSINE;
            distanceMeters = calculateDistanceMeters(originLat, originLng, destLat, destLng);
            durationSeconds = estimateDurationSeconds(distanceMeters, mode);
        } catch (Exception e) {
            log.error("Unexpected error getting directions, using fallback: {}", e.getMessage());
            source = TransportSource.HAVERSINE;
            distanceMeters = calculateDistanceMeters(originLat, originLng, destLat, destLng);
            durationSeconds = estimateDurationSeconds(distanceMeters, mode);
        }

        // Determine warning based on distance, mode, and source
        TransportWarning warning = determineWarning(distanceMeters, mode, source);

        return TransportCalculationResult.builder()
                .distanceMeters((int) distanceMeters)
                .durationMinutes(durationSeconds / 60)
                .distanceText(formatDistance(distanceMeters))
                .durationText(formatDuration(durationSeconds))
                .transportMode(mode)
                .source(source)
                .warning(warning)
                .build();
    }

    /**
     * Determines the appropriate warning based on distance, mode, and source.
     *
     * Warning thresholds:
     * - WALKING > 5 km → UNREALISTIC_WALKING
     * - BICYCLING > 30 km → UNREALISTIC_BICYCLING
     * - Any mode > 100 km → VERY_LONG_DISTANCE
     * - Haversine source → ESTIMATED_DISTANCE
     *
     * @contract
     *   - pre: distanceMeters >= 0
     *   - post: returns appropriate TransportWarning
     *   - calledBy: calculateTransportWithWarnings
     *
     * @param distanceMeters Distance in meters
     * @param mode Transport mode
     * @param source Calculation source
     * @return Appropriate warning, or NONE if no issues
     */
    public TransportWarning determineWarning(double distanceMeters, TransportMode mode, TransportSource source) {
        // Check for very long distance first (highest priority)
        if (distanceMeters > LONG_DISTANCE_WARNING_THRESHOLD_METERS) {
            return TransportWarning.VERY_LONG_DISTANCE;
        }

        // Check mode-specific unrealistic distances
        if (mode == TransportMode.WALKING && distanceMeters > WALKING_WARNING_THRESHOLD_METERS) {
            return TransportWarning.UNREALISTIC_WALKING;
        }

        if (mode == TransportMode.BICYCLING && distanceMeters > BICYCLING_WARNING_THRESHOLD_METERS) {
            return TransportWarning.UNREALISTIC_BICYCLING;
        }

        // Check if using Haversine fallback (estimated)
        if (source == TransportSource.HAVERSINE) {
            return TransportWarning.ESTIMATED_DISTANCE;
        }

        return TransportWarning.NONE;
    }

    /**
     * Batch calculates transport info for a list of ordered activities.
     *
     * Efficiently processes multiple activities using a single place lookup.
     * Sets transportSource and transportWarning for each activity.
     *
     * @contract
     *   - pre: activities ordered by day and sortOrder ascending
     *   - post: All activities (except first of each day) have transport info calculated
     *   - calledBy: ActivityService#reorderActivities, ActivityService#applyOptimizedRoute
     *
     * @param activities List of activities to calculate transport for
     * @return Same list with updated transport fields
     */
    public List<Activity> batchCalculateTransport(List<Activity> activities) {
        if (activities == null || activities.size() < 2) {
            if (activities != null && activities.size() == 1) {
                // Single activity - clear transport info
                Activity single = activities.get(0);
                clearTransportInfo(single);
                single.setTransportSource(TransportSource.NOT_APPLICABLE);
                single.setTransportWarning(TransportWarning.NONE);
            }
            return activities;
        }

        // Pre-fetch all places for efficiency
        List<UUID> placeIds = activities.stream()
                .map(Activity::getPlaceId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, Place> placeMap = placeRepository.findAllById(placeIds).stream()
                .collect(Collectors.toMap(Place::getId, Function.identity()));

        // Group by day to handle first activity of each day correctly
        Map<Integer, List<Activity>> byDay = activities.stream()
                .collect(Collectors.groupingBy(Activity::getDay));

        for (List<Activity> dayActivities : byDay.values()) {
            // Sort by sortOrder within day
            dayActivities.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));

            for (int i = 0; i < dayActivities.size(); i++) {
                Activity current = dayActivities.get(i);

                if (i == 0) {
                    // First activity of the day - no transport from previous
                    clearTransportInfo(current);
                    current.setTransportSource(TransportSource.NOT_APPLICABLE);
                    current.setTransportWarning(TransportWarning.NONE);
                    continue;
                }

                // Handle NOT_CALCULATED mode
                if (current.getTransportMode() == TransportMode.NOT_CALCULATED) {
                    clearTransportInfo(current);
                    current.setTransportSource(TransportSource.NOT_APPLICABLE);
                    current.setTransportWarning(TransportWarning.NONE);
                    continue;
                }

                Activity previous = dayActivities.get(i - 1);

                if (previous.getPlaceId() == null || current.getPlaceId() == null) {
                    clearTransportInfo(current);
                    current.setTransportSource(TransportSource.NOT_APPLICABLE);
                    current.setTransportWarning(TransportWarning.NONE);
                    continue;
                }

                Place fromPlace = placeMap.get(previous.getPlaceId());
                Place toPlace = placeMap.get(current.getPlaceId());

                if (fromPlace == null || toPlace == null ||
                        !hasValidCoordinates(fromPlace) || !hasValidCoordinates(toPlace)) {
                    clearTransportInfo(current);
                    current.setTransportSource(TransportSource.NOT_APPLICABLE);
                    current.setTransportWarning(TransportWarning.NONE);
                    continue;
                }

                TransportMode mode = current.getTransportMode() != null
                        ? current.getTransportMode()
                        : TransportMode.WALKING;

                TransportCalculationResult result = calculateTransportWithWarnings(
                        fromPlace.getLatitude(), fromPlace.getLongitude(),
                        toPlace.getLatitude(), toPlace.getLongitude(),
                        mode
                );

                applyResultToActivity(current, result);
            }
        }

        log.debug("Batch calculated transport for {} activities", activities.size());
        return activities;
    }

    /**
     * Batch recalculates transport for activities with rate limiting and statistics.
     *
     * Uses Google Maps API with 100ms delay between calls to avoid rate limiting.
     * Falls back to Haversine when API fails. Returns detailed statistics.
     *
     * @contract
     *   - pre: activities ordered by day and sortOrder ascending
     *   - pre: maxApiCalls >= 0
     *   - post: All activities have transport info recalculated
     *   - post: Returns RecalculationResult with statistics
     *   - calledBy: ActivityService#recalculateAllTransport
     *
     * @param activities List of activities to recalculate
     * @param maxApiCalls Maximum number of API calls allowed (0 = unlimited)
     * @return RecalculationResult with statistics
     */
    public RecalculationResult batchRecalculateWithRateLimit(List<Activity> activities, int maxApiCalls) {
        int total = activities != null ? activities.size() : 0;
        int recalculated = 0;
        int apiSuccess = 0;
        int fallback = 0;
        int skipped = 0;
        int manual = 0;
        int apiCallCount = 0;
        boolean rateLimitReached = false;

        if (activities == null || activities.isEmpty()) {
            return RecalculationResult.builder()
                    .totalActivities(0)
                    .message("沒有活動需要計算")
                    .build();
        }

        // Pre-fetch all places for efficiency
        List<UUID> placeIds = activities.stream()
                .map(Activity::getPlaceId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, Place> placeMap = placeRepository.findAllById(placeIds).stream()
                .collect(Collectors.toMap(Place::getId, Function.identity()));

        // Group by day
        Map<Integer, List<Activity>> byDay = activities.stream()
                .collect(Collectors.groupingBy(Activity::getDay));

        for (List<Activity> dayActivities : byDay.values()) {
            dayActivities.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));

            for (int i = 0; i < dayActivities.size(); i++) {
                Activity current = dayActivities.get(i);

                // First activity of the day - skip
                if (i == 0) {
                    clearTransportInfo(current);
                    current.setTransportSource(TransportSource.NOT_APPLICABLE);
                    current.setTransportWarning(TransportWarning.NONE);
                    skipped++;
                    continue;
                }

                // NOT_CALCULATED mode - skip
                if (current.getTransportMode() == TransportMode.NOT_CALCULATED) {
                    clearTransportInfo(current);
                    current.setTransportSource(TransportSource.NOT_APPLICABLE);
                    current.setTransportWarning(TransportWarning.NONE);
                    skipped++;
                    continue;
                }

                // Manual mode (FLIGHT/HIGH_SPEED_RAIL) - keep existing or skip if no manual input
                TransportMode mode = current.getTransportMode() != null
                        ? current.getTransportMode()
                        : TransportMode.WALKING;

                if (!mode.supportsAutoCalculation()) {
                    // Keep existing manual input if present
                    if (current.getTransportSource() == TransportSource.MANUAL &&
                        current.getTransportDurationMinutes() != null) {
                        manual++;
                        continue;
                    }
                    // No manual input - skip and don't use Haversine
                    clearTransportInfo(current);
                    current.setTransportSource(TransportSource.NOT_APPLICABLE);
                    current.setTransportWarning(TransportWarning.NONE);
                    skipped++;
                    continue;
                }

                Activity previous = dayActivities.get(i - 1);

                if (previous.getPlaceId() == null || current.getPlaceId() == null) {
                    clearTransportInfo(current);
                    current.setTransportSource(TransportSource.NOT_APPLICABLE);
                    current.setTransportWarning(TransportWarning.NONE);
                    skipped++;
                    continue;
                }

                Place fromPlace = placeMap.get(previous.getPlaceId());
                Place toPlace = placeMap.get(current.getPlaceId());

                if (fromPlace == null || toPlace == null ||
                        !hasValidCoordinates(fromPlace) || !hasValidCoordinates(toPlace)) {
                    clearTransportInfo(current);
                    current.setTransportSource(TransportSource.NOT_APPLICABLE);
                    current.setTransportWarning(TransportWarning.NONE);
                    skipped++;
                    continue;
                }

                // Check if rate limit reached
                if (maxApiCalls > 0 && apiCallCount >= maxApiCalls) {
                    rateLimitReached = true;
                    // Use Haversine fallback
                    double distanceMeters = calculateDistanceMeters(
                            fromPlace.getLatitude(), fromPlace.getLongitude(),
                            toPlace.getLatitude(), toPlace.getLongitude());
                    int durationSeconds = estimateDurationSeconds(distanceMeters, mode);

                    current.setTransportDurationMinutes(durationSeconds / 60);
                    current.setTransportDistanceMeters((int) distanceMeters);
                    current.setTransportSource(TransportSource.HAVERSINE);
                    current.setTransportWarning(TransportWarning.ESTIMATED_DISTANCE);
                    fallback++;
                    recalculated++;
                    continue;
                }

                // Rate limiting: wait 100ms between API calls
                if (apiCallCount > 0) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Rate limiting sleep interrupted");
                    }
                }

                // Call API
                TransportCalculationResult result = calculateTransportWithWarnings(
                        fromPlace.getLatitude(), fromPlace.getLongitude(),
                        toPlace.getLatitude(), toPlace.getLongitude(),
                        mode
                );

                applyResultToActivity(current, result);
                recalculated++;

                if (result.getSource() == TransportSource.GOOGLE_API) {
                    apiSuccess++;
                    apiCallCount++;
                } else {
                    fallback++;
                }
            }
        }

        String message = String.format(
                "處理完成：%d 個景點中，%d 個使用 Google API，%d 個使用估算，%d 個跳過",
                total, apiSuccess, fallback, skipped);

        if (rateLimitReached) {
            message += "（已達 API 呼叫上限）";
        }

        log.info("Batch recalculation complete: total={}, api={}, fallback={}, skipped={}, manual={}",
                total, apiSuccess, fallback, skipped, manual);

        return RecalculationResult.builder()
                .totalActivities(total)
                .recalculatedCount(recalculated)
                .apiSuccessCount(apiSuccess)
                .fallbackCount(fallback)
                .skippedCount(skipped)
                .manualCount(manual)
                .rateLimitReached(rateLimitReached)
                .message(message)
                .build();
    }

    /**
     * Recalculates transport for a specific activity and the next activity if exists.
     *
     * @contract
     *   - pre: activity != null and already saved to database
     *   - post: Current activity's transport info updated based on previous
     *   - post: Next activity's transport info updated based on current
     *   - calledBy: ActivityService after activity update
     *
     * @param activity The activity that was updated
     */
    public void recalculateForActivityAndNext(Activity activity) {
        if (activity == null) {
            return;
        }

        // Recalculate for current activity
        calculateTransportFromPrevious(activity);
        activityRepository.save(activity);

        // Find and recalculate next activity
        List<Activity> dayActivities = activityRepository
                .findByTripIdAndDayOrderBySortOrderAsc(activity.getTripId(), activity.getDay());

        dayActivities.stream()
                .filter(a -> a.getSortOrder() == activity.getSortOrder() + 1)
                .findFirst()
                .ifPresent(next -> {
                    calculateTransportFromPrevious(next);
                    activityRepository.save(next);
                    log.debug("Updated transport for next activity {}", next.getId());
                });
    }

    /**
     * Sets manual transport duration for an activity.
     *
     * Used when transport mode is FLIGHT, HIGH_SPEED_RAIL, or user wants to override.
     *
     * @contract
     *   - pre: activity != null
     *   - pre: durationMinutes >= 0
     *   - post: activity.transportDurationMinutes = durationMinutes
     *   - post: activity.transportSource = MANUAL
     *   - calledBy: ActivityService#updateActivity
     *
     * @param activity The activity to update
     * @param durationMinutes Manual duration in minutes
     * @return Updated activity
     */
    public Activity setManualTransportDuration(Activity activity, Integer durationMinutes) {
        if (activity == null) {
            return null;
        }

        activity.setTransportDurationMinutes(durationMinutes);
        activity.setTransportDistanceMeters(null); // Distance not applicable for manual input
        activity.setTransportSource(TransportSource.MANUAL);
        activity.setTransportWarning(TransportWarning.NONE);

        log.debug("Set manual transport duration for activity {}: {} minutes",
                activity.getId(), durationMinutes);

        return activity;
    }

    /**
     * Clears transport info fields on an activity.
     */
    private void clearTransportInfo(Activity activity) {
        activity.setTransportDurationMinutes(null);
        activity.setTransportDistanceMeters(null);
    }

    /**
     * Applies calculation result to activity fields.
     */
    private void applyResultToActivity(Activity activity, TransportCalculationResult result) {
        activity.setTransportDurationMinutes(result.getDurationMinutes());
        activity.setTransportDistanceMeters(result.getDistanceMeters());
        activity.setTransportSource(result.getSource());
        activity.setTransportWarning(result.getWarning());
    }

    /**
     * Calculates distance between two coordinates using Haversine formula.
     *
     * @return Distance in meters
     */
    private double calculateDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Estimates travel duration based on distance and transport mode.
     *
     * @return Duration in seconds
     */
    private int estimateDurationSeconds(double distanceMeters, TransportMode mode) {
        double speedKmh = switch (mode) {
            case WALKING -> WALKING_SPEED_KMH;
            case BICYCLING -> BICYCLING_SPEED_KMH;
            case TRANSIT -> TRANSIT_SPEED_KMH;
            case DRIVING -> DRIVING_SPEED_KMH;
            case HIGH_SPEED_RAIL -> HIGH_SPEED_RAIL_SPEED_KMH;
            case FLIGHT -> FLIGHT_SPEED_KMH;
            case NOT_CALCULATED -> 0.0;
        };

        if (speedKmh == 0) {
            return 0;
        }

        double hours = (distanceMeters / 1000.0) / speedKmh;
        return (int) (hours * 3600);
    }

    /**
     * Formats distance for human-readable display.
     */
    private String formatDistance(double meters) {
        if (meters >= 1000) {
            return String.format("%.1f km", meters / 1000);
        }
        return String.format("%.0f m", meters);
    }

    /**
     * Formats duration for human-readable display.
     */
    private String formatDuration(int seconds) {
        int minutes = seconds / 60;
        if (minutes >= 60) {
            int hours = minutes / 60;
            int mins = minutes % 60;
            return mins > 0 ? hours + " 小時 " + mins + " 分鐘" : hours + " 小時";
        }
        return minutes + " 分鐘";
    }

    /**
     * Checks if a place has valid coordinates within valid range.
     *
     * @param place The place to validate
     * @return true if coordinates are valid, false otherwise
     */
    private boolean hasValidCoordinates(Place place) {
        if (place == null) {
            return false;
        }
        double lat = place.getLatitude();
        double lng = place.getLongitude();

        // Check valid latitude range: -90 to 90
        // Check valid longitude range: -180 to 180
        // Also check that coordinates are not both zero (likely unset)
        return lat >= -90 && lat <= 90 &&
                lng >= -180 && lng <= 180 &&
                (lat != 0 || lng != 0);
    }
}
