package com.wego.service;

import com.wego.domain.permission.PermissionChecker;
import com.wego.domain.route.OptimizationResult;
import com.wego.domain.route.RouteOptimizer;
import com.wego.dto.request.CreateActivityRequest;
import com.wego.dto.request.ReorderActivitiesRequest;
import com.wego.dto.request.UpdateActivityRequest;
import com.wego.dto.response.ActivityResponse;
import com.wego.dto.response.RecalculationResult;
import com.wego.dto.response.RouteOptimizationResponse;
import com.wego.entity.Activity;
import com.wego.entity.Place;
import com.wego.exception.BusinessException;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.repository.ActivityRepository;
import com.wego.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for managing activities within trips.
 *
 * Handles CRUD operations and reordering of activities with permission checks.
 *
 * @contract
 *   - invariant: All operations check user permissions before execution
 *   - calledBy: ActivityApiController
 *   - calls: ActivityRepository, PlaceRepository, PermissionChecker
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final PlaceRepository placeRepository;
    private final PermissionChecker permissionChecker;
    private final RouteOptimizer routeOptimizer;
    private final TransportCalculationService transportCalculationService;

    /**
     * Gets a single activity by ID.
     *
     * @contract
     *   - pre: activityId != null, userId != null
     *   - pre: User must have view permission on the trip
     *   - post: Returns activity details with place information
     *   - calls: PermissionChecker#canView, ActivityRepository#findById, PlaceRepository#findById
     *   - calledBy: TripController#showActivityDetail
     *
     * @param activityId The activity ID
     * @param userId The user ID performing the operation
     * @return The activity response
     * @throws ForbiddenException if user lacks view permission
     * @throws ResourceNotFoundException if activity not found
     */
    @Transactional(readOnly = true)
    public ActivityResponse getActivity(UUID activityId, UUID userId) {
        log.debug("Getting activity {} by user {}", activityId, userId);

        // Find activity
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", activityId.toString()));

        // Check permission
        if (!permissionChecker.canView(activity.getTripId(), userId)) {
            throw new ForbiddenException("activity", "view");
        }

        // Get place info
        Place place = activity.getPlaceId() != null
                ? placeRepository.findById(activity.getPlaceId()).orElse(null)
                : null;

        return ActivityResponse.fromEntity(activity, place);
    }

    /**
     * Creates a new activity in the specified trip.
     *
     * @contract
     *   - pre: tripId != null, request != null, userId != null
     *   - pre: User must have edit permission on the trip
     *   - pre: Place with placeId must exist
     *   - post: Activity is persisted with auto-assigned sortOrder
     *   - calls: PermissionChecker#canEdit, PlaceRepository#findById, ActivityRepository#save
     *   - calledBy: ActivityApiController#createActivity
     *
     * @param tripId The trip ID
     * @param request The create activity request
     * @param userId The user ID performing the operation
     * @return The created activity response
     * @throws ForbiddenException if user lacks edit permission
     * @throws ResourceNotFoundException if place not found
     */
    @Transactional
    public ActivityResponse createActivity(UUID tripId, CreateActivityRequest request, UUID userId) {
        log.debug("Creating activity for trip {} by user {}", tripId, userId);

        // Check permission
        if (!permissionChecker.canEdit(tripId, userId)) {
            throw new ForbiddenException("activity", "create");
        }

        // Validate place exists
        Place place = placeRepository.findById(request.getPlaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Place", request.getPlaceId().toString()));

        // Calculate next sortOrder
        int nextSortOrder = activityRepository.findMaxSortOrderByTripIdAndDay(tripId, request.getDay())
                .map(max -> max + 1)
                .orElse(0);

        // Build and save activity
        Activity activity = Activity.builder()
                .tripId(tripId)
                .placeId(request.getPlaceId())
                .day(request.getDay())
                .sortOrder(nextSortOrder)
                .startTime(request.getStartTime())
                .durationMinutes(request.getDurationMinutes())
                .note(request.getNote())
                .transportMode(request.getTransportMode())
                .build();

        // Handle transport calculation or manual input
        if (request.hasManualTransport()) {
            // Use manual transport duration
            transportCalculationService.setManualTransportDuration(activity, request.getManualTransportMinutes());
            log.debug("Using manual transport duration: {} minutes", request.getManualTransportMinutes());
        } else {
            // Calculate transport info from previous activity
            transportCalculationService.calculateTransportFromPrevious(activity);
        }

        Activity saved = activityRepository.save(activity);
        log.info("Created activity {} for trip {}", saved.getId(), tripId);

        // Update transport info for next activity (if exists)
        updateNextActivityTransport(saved);

        return ActivityResponse.fromEntity(saved, place);
    }

    /**
     * Retrieves all activities for a trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - pre: User must have view permission on the trip
     *   - post: Returns activities ordered by day and sortOrder
     *   - calls: PermissionChecker#canView, ActivityRepository#findByTripIdOrderByDayAscSortOrderAsc
     *   - calledBy: ActivityApiController#getActivities
     *
     * @param tripId The trip ID
     * @param userId The user ID performing the operation
     * @return List of activity responses
     * @throws ForbiddenException if user lacks view permission
     */
    @Transactional(readOnly = true)
    public List<ActivityResponse> getActivitiesByTrip(UUID tripId, UUID userId) {
        log.debug("Getting all activities for trip {} by user {}", tripId, userId);

        // Check permission
        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("activity", "view");
        }

        List<Activity> activities = activityRepository.findByTripIdOrderByDayAscSortOrderAsc(tripId);
        return mapActivitiesToResponses(activities);
    }

    /**
     * Retrieves activities for a specific day of a trip.
     *
     * @contract
     *   - pre: tripId != null, day >= 1, userId != null
     *   - pre: User must have view permission on the trip
     *   - post: Returns activities for the day ordered by sortOrder
     *   - calls: PermissionChecker#canView, ActivityRepository#findByTripIdAndDayOrderBySortOrderAsc
     *   - calledBy: ActivityApiController#getActivities
     *
     * @param tripId The trip ID
     * @param day The day number
     * @param userId The user ID performing the operation
     * @return List of activity responses for the day
     * @throws ForbiddenException if user lacks view permission
     */
    @Transactional(readOnly = true)
    public List<ActivityResponse> getActivitiesByDay(UUID tripId, int day, UUID userId) {
        log.debug("Getting activities for trip {} day {} by user {}", tripId, day, userId);

        // Check permission
        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("activity", "view");
        }

        List<Activity> activities = activityRepository.findByTripIdAndDayOrderBySortOrderAsc(tripId, day);
        return mapActivitiesToResponses(activities);
    }

    /**
     * Updates an existing activity.
     *
     * @contract
     *   - pre: activityId != null, request != null, userId != null
     *   - pre: User must have edit permission on the activity's trip
     *   - pre: If placeId is provided, the place must exist
     *   - post: Activity is updated with provided fields only
     *   - post: updatedAt is set
     *   - calls: PermissionChecker#canEdit, ActivityRepository#findById, ActivityRepository#save
     *   - calledBy: ActivityApiController#updateActivity
     *
     * @param activityId The activity ID
     * @param request The update activity request
     * @param userId The user ID performing the operation
     * @return The updated activity response
     * @throws ResourceNotFoundException if activity or place not found
     * @throws ForbiddenException if user lacks edit permission
     */
    @Transactional
    public ActivityResponse updateActivity(UUID activityId, UpdateActivityRequest request, UUID userId) {
        log.debug("Updating activity {} by user {}", activityId, userId);

        // Find activity
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", activityId.toString()));

        // Check permission
        if (!permissionChecker.canEdit(activity.getTripId(), userId)) {
            throw new ForbiddenException("activity", "update");
        }

        // Update place if provided
        Place place;
        if (request.getPlaceId() != null) {
            place = placeRepository.findById(request.getPlaceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Place", request.getPlaceId().toString()));
            activity.setPlaceId(request.getPlaceId());
        } else {
            place = activity.getPlaceId() != null
                    ? placeRepository.findById(activity.getPlaceId()).orElse(null)
                    : null;
        }

        // Update fields if provided (partial update pattern)
        if (request.getDay() != null) {
            activity.setDay(request.getDay());
        }
        if (request.getStartTime() != null) {
            activity.setStartTime(request.getStartTime());
        }
        if (request.getDurationMinutes() != null) {
            activity.setDurationMinutes(request.getDurationMinutes());
        }
        if (request.getNote() != null) {
            activity.setNote(request.getNote());
        }

        // Handle transport mode and manual transport input
        boolean transportModeChanged = request.getTransportMode() != null &&
                !request.getTransportMode().equals(activity.getTransportMode());

        if (request.getTransportMode() != null) {
            activity.setTransportMode(request.getTransportMode());
        }

        // Handle manual transport or recalculate if mode changed
        if (request.hasManualTransport()) {
            // Use manual transport duration
            transportCalculationService.setManualTransportDuration(activity, request.getManualTransportMinutes());
            log.debug("Using manual transport duration: {} minutes", request.getManualTransportMinutes());
        } else if (transportModeChanged || request.getPlaceId() != null) {
            // Recalculate transport if mode changed or place changed
            transportCalculationService.calculateTransportFromPrevious(activity);
            log.debug("Recalculated transport due to mode or place change");
        }

        activity.setUpdatedAt(Instant.now());

        Activity saved = activityRepository.save(activity);
        log.info("Updated activity {}", activityId);

        // Update transport for next activity if place or transport changed
        if (request.getPlaceId() != null || transportModeChanged) {
            updateNextActivityTransport(saved);
        }

        return ActivityResponse.fromEntity(saved, place);
    }

    /**
     * Deletes an activity.
     *
     * @contract
     *   - pre: activityId != null, userId != null
     *   - pre: User must have edit permission on the activity's trip
     *   - post: Activity is deleted
     *   - calls: PermissionChecker#canEdit, ActivityRepository#findById, ActivityRepository#delete
     *   - calledBy: ActivityApiController#deleteActivity
     *
     * @param activityId The activity ID
     * @param userId The user ID performing the operation
     * @throws ResourceNotFoundException if activity not found
     * @throws ForbiddenException if user lacks edit permission
     */
    @Transactional
    public void deleteActivity(UUID activityId, UUID userId) {
        log.debug("Deleting activity {} by user {}", activityId, userId);

        // Find activity
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", activityId.toString()));

        // Check permission
        if (!permissionChecker.canEdit(activity.getTripId(), userId)) {
            throw new ForbiddenException("activity", "delete");
        }

        activityRepository.delete(activity);
        log.info("Deleted activity {}", activityId);
    }

    /**
     * Reorders activities within a day of a trip.
     *
     * @contract
     *   - pre: tripId != null, request != null, userId != null
     *   - pre: User must have edit permission on the trip
     *   - pre: activityIds must contain all activities for the day
     *   - post: Activities are reordered according to activityIds order
     *   - calls: PermissionChecker#canEdit, ActivityRepository#findByTripIdAndDayOrderBySortOrderAsc
     *   - calledBy: ActivityApiController#reorderActivities
     *
     * @param tripId The trip ID
     * @param request The reorder request
     * @param userId The user ID performing the operation
     * @return List of reordered activity responses
     * @throws ForbiddenException if user lacks edit permission
     * @throws BusinessException if activity count mismatch
     */
    @Transactional
    public List<ActivityResponse> reorderActivities(UUID tripId, ReorderActivitiesRequest request, UUID userId) {
        log.debug("Reordering activities for trip {} day {} by user {}", tripId, request.getDay(), userId);

        // Check permission
        if (!permissionChecker.canEdit(tripId, userId)) {
            throw new ForbiddenException("activity", "reorder");
        }

        // Get existing activities for the day
        List<Activity> existingActivities = activityRepository
                .findByTripIdAndDayOrderBySortOrderAsc(tripId, request.getDay());

        // Validate activity count matches
        if (existingActivities.size() != request.getActivityIds().size()) {
            throw new BusinessException("REORDER_MISMATCH",
                    "Activity count mismatch: expected " + existingActivities.size() +
                    ", got " + request.getActivityIds().size());
        }

        // Build lookup map
        Map<UUID, Activity> activityMap = new HashMap<>();
        for (Activity activity : existingActivities) {
            activityMap.put(activity.getId(), activity);
        }

        // Update sortOrder based on new order
        List<UUID> newOrder = request.getActivityIds();
        for (int i = 0; i < newOrder.size(); i++) {
            Activity activity = activityMap.get(newOrder.get(i));
            if (activity != null) {
                activity.setSortOrder(i);
                activity.setUpdatedAt(Instant.now());
            }
        }

        // Batch recalculate transport info after reordering
        transportCalculationService.batchCalculateTransport(existingActivities);

        // Save all
        List<Activity> saved = activityRepository.saveAll(existingActivities);
        log.info("Reordered {} activities for trip {} day {}", saved.size(), tripId, request.getDay());

        return mapActivitiesToResponses(saved);
    }

    /**
     * Gets optimized route suggestion for a specific day.
     *
     * Uses the Greedy Nearest Neighbor algorithm to find a more efficient
     * route through the day's activities. Does NOT modify the database.
     *
     * @contract
     *   - pre: tripId != null, day >= 1, userId != null
     *   - pre: user has view permission on the trip
     *   - post: returns optimization result (does NOT modify database)
     *   - calls: PermissionChecker#canView, RouteOptimizer#optimize
     *   - calledBy: ActivityApiController#getOptimizedRoute
     *
     * @param tripId The trip ID
     * @param day The day number to optimize
     * @param userId The user ID performing the operation
     * @return RouteOptimizationResponse with comparison data
     * @throws ForbiddenException if user lacks view permission
     */
    @Transactional(readOnly = true)
    public RouteOptimizationResponse getOptimizedRoute(UUID tripId, int day, UUID userId) {
        log.debug("Getting optimized route for trip {} day {} by user {}", tripId, day, userId);

        // Check permission
        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("activity", "view");
        }

        // Get activities for the day
        List<Activity> activities = activityRepository.findByTripIdAndDayOrderBySortOrderAsc(tripId, day);

        if (activities.isEmpty()) {
            return RouteOptimizationResponse.builder()
                    .tripId(tripId)
                    .day(day)
                    .originalOrder(List.of())
                    .optimizedOrder(List.of())
                    .originalDistanceMeters(0)
                    .optimizedDistanceMeters(0)
                    .distanceSavedMeters(0)
                    .savingsPercentage(0)
                    .originalDistanceFormatted("0 m")
                    .optimizedDistanceFormatted("0 m")
                    .distanceSavedFormatted("0 m")
                    .optimizationApplied(false)
                    .activityCount(0)
                    .build();
        }

        // Build place lookup map
        Map<UUID, Place> placeLookup = buildPlaceLookup(activities);

        // Run optimization
        OptimizationResult result = routeOptimizer.optimize(activities, placeLookup);

        log.info("Route optimization for trip {} day {}: {} -> {} meters (saved {})",
                tripId, day,
                String.format("%.0f", result.getOriginalDistanceMeters()),
                String.format("%.0f", result.getOptimizedDistanceMeters()),
                String.format("%.0f", result.getDistanceSavedMeters()));

        return RouteOptimizationResponse.fromResult(result, tripId, day);
    }

    /**
     * Applies the optimized route order.
     *
     * Reorders activities according to the provided optimized order.
     * This modifies the database.
     *
     * @contract
     *   - pre: tripId != null, day >= 1, userId != null
     *   - pre: user has edit permission on the trip
     *   - pre: optimizedOrder contains all activity IDs for the day
     *   - post: activities reordered according to optimizedOrder
     *   - calls: PermissionChecker#canEdit, ActivityRepository#saveAll
     *   - calledBy: ActivityApiController#applyOptimizedRoute
     *
     * @param tripId The trip ID
     * @param day The day number
     * @param optimizedOrder The optimized order of activity IDs
     * @param userId The user ID performing the operation
     * @return List of reordered activity responses
     * @throws ForbiddenException if user lacks edit permission
     * @throws BusinessException if activity count mismatch
     */
    @Transactional
    public List<ActivityResponse> applyOptimizedRoute(UUID tripId, int day, List<UUID> optimizedOrder, UUID userId) {
        log.debug("Applying optimized route for trip {} day {} by user {}", tripId, day, userId);

        // Check permission
        if (!permissionChecker.canEdit(tripId, userId)) {
            throw new ForbiddenException("activity", "reorder");
        }

        // Get existing activities for the day
        List<Activity> existingActivities = activityRepository
                .findByTripIdAndDayOrderBySortOrderAsc(tripId, day);

        // Validate activity count matches
        if (existingActivities.size() != optimizedOrder.size()) {
            throw new BusinessException("OPTIMIZATION_MISMATCH",
                    "Activity count mismatch: expected " + existingActivities.size() +
                    ", got " + optimizedOrder.size());
        }

        // Build lookup map
        Map<UUID, Activity> activityMap = new HashMap<>();
        for (Activity activity : existingActivities) {
            activityMap.put(activity.getId(), activity);
        }

        // Validate all IDs exist
        for (UUID activityId : optimizedOrder) {
            if (!activityMap.containsKey(activityId)) {
                throw new BusinessException("ACTIVITY_NOT_FOUND",
                        "Activity " + activityId + " not found in day " + day);
            }
        }

        // Update sortOrder based on optimized order
        for (int i = 0; i < optimizedOrder.size(); i++) {
            Activity activity = activityMap.get(optimizedOrder.get(i));
            activity.setSortOrder(i);
            activity.setUpdatedAt(Instant.now());
        }

        // Batch recalculate transport info after optimization
        transportCalculationService.batchCalculateTransport(existingActivities);

        // Save all
        List<Activity> saved = activityRepository.saveAll(existingActivities);
        log.info("Applied optimized route for trip {} day {}, reordered {} activities",
                tripId, day, saved.size());

        // Return in new order
        List<Activity> orderedSaved = optimizedOrder.stream()
                .map(activityMap::get)
                .toList();

        return mapActivitiesToResponses(orderedSaved);
    }

    /**
     * Builds a place lookup map for the given activities.
     *
     * Uses a single batch query to avoid N+1 problem.
     *
     * @param activities The activities to look up places for
     * @return Map of placeId to Place entity
     */
    private Map<UUID, Place> buildPlaceLookup(List<Activity> activities) {
        List<UUID> placeIds = activities.stream()
                .map(Activity::getPlaceId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (placeIds.isEmpty()) {
            return new HashMap<>();
        }

        return placeRepository.findAllById(placeIds).stream()
                .collect(Collectors.toMap(Place::getId, place -> place));
    }

    /**
     * Maps a list of activities to activity responses.
     *
     * Uses a single batch query to avoid N+1 problem.
     *
     * @param activities The activities to map
     * @return List of activity responses
     */
    private List<ActivityResponse> mapActivitiesToResponses(List<Activity> activities) {
        // Pre-fetch all places in a single query
        Map<UUID, Place> placeMap = buildPlaceLookup(activities);

        return activities.stream()
                .map(activity -> {
                    Place place = activity.getPlaceId() != null
                            ? placeMap.get(activity.getPlaceId())
                            : null;
                    return ActivityResponse.fromEntity(activity, place);
                })
                .collect(Collectors.toList());
    }

    /**
     * Updates transport info for the activity following the given activity.
     *
     * When a new activity is inserted, the next activity's transport info
     * needs to be recalculated based on the new previous activity.
     *
     * @contract
     *   - pre: saved != null and already persisted
     *   - post: Next activity's transport info updated if exists
     *   - calls: TransportCalculationService#calculateTransportFromPrevious
     *   - calledBy: createActivity
     *
     * @param saved The newly created/updated activity
     */
    private void updateNextActivityTransport(Activity saved) {
        activityRepository.findByTripIdAndDayOrderBySortOrderAsc(saved.getTripId(), saved.getDay())
                .stream()
                .filter(a -> a.getSortOrder() == saved.getSortOrder() + 1)
                .findFirst()
                .ifPresent(next -> {
                    transportCalculationService.calculateTransportFromPrevious(next);
                    activityRepository.save(next);
                    log.debug("Updated transport for next activity {}", next.getId());
                });
    }

    /**
     * Recalculates all transport times for a trip.
     *
     * Uses Google Maps API with rate limiting. Falls back to Haversine when API fails.
     * Manual inputs (FLIGHT/HIGH_SPEED_RAIL) are preserved.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - pre: user has edit permission on the trip
     *   - pre: maxApiCalls > 0
     *   - post: All activities in trip have transport info recalculated
     *   - post: Returns RecalculationResult with statistics
     *   - calls: PermissionChecker#canEdit, TransportCalculationService#batchRecalculateWithRateLimit
     *   - calledBy: TripController#recalculateTransport
     *
     * @param tripId The trip ID
     * @param userId The user ID performing the operation
     * @param maxApiCalls Maximum number of API calls allowed (default 50)
     * @return RecalculationResult with statistics
     * @throws ForbiddenException if user lacks edit permission
     */
    /**
     * Checks if user has edit permission on the trip.
     * Used by controller to validate before dispatching async operations.
     *
     * @throws ForbiddenException if user lacks edit permission
     */
    public void checkTransportRecalculatePermission(UUID tripId, UUID userId) {
        if (!permissionChecker.canEdit(tripId, userId)) {
            throw new ForbiddenException("activity", "recalculate transport");
        }
    }

    @Transactional
    public RecalculationResult recalculateAllTransport(UUID tripId, UUID userId, int maxApiCalls) {
        log.info("Recalculating all transport for trip {} by user {}, max API calls: {}",
                tripId, userId, maxApiCalls);
        return recalculateAllTransportInternal(tripId, userId, maxApiCalls);
    }

    /**
     * Async variant of recalculateAllTransport for non-blocking execution.
     *
     * Runs on the "transportExecutor" thread pool. Falls back to synchronous
     * execution (CallerRunsPolicy) when the pool is saturated.
     *
     * @contract
     *   - pre: tripId != null, userId != null, maxApiCalls > 0
     *   - post: All activities have transport times recalculated asynchronously
     *   - post: Returns CompletableFuture with RecalculationResult
     *   - calls: recalculateAllTransport (synchronous)
     *   - calledBy: ActivityWebController#recalculateTransport (AJAX path)
     */
    @Async("transportExecutor")
    @Transactional
    public CompletableFuture<RecalculationResult> recalculateAllTransportAsync(
            UUID tripId, UUID userId, int maxApiCalls) {
        log.info("Starting async transport recalculation for trip {} by user {}", tripId, userId);
        try {
            RecalculationResult result = recalculateAllTransportInternal(tripId, userId, maxApiCalls);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Async transport recalculation failed for trip {}: {}", tripId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Internal implementation shared by sync and async variants.
     */
    private RecalculationResult recalculateAllTransportInternal(UUID tripId, UUID userId, int maxApiCalls) {
        if (!permissionChecker.canEdit(tripId, userId)) {
            throw new ForbiddenException("activity", "recalculate transport");
        }

        List<Activity> activities = activityRepository.findByTripIdOrderByDayAscSortOrderAsc(tripId);

        if (activities.isEmpty()) {
            return RecalculationResult.builder()
                    .totalActivities(0)
                    .message("此行程沒有任何景點")
                    .build();
        }

        RecalculationResult result = transportCalculationService.batchRecalculateWithRateLimit(
                activities, maxApiCalls);

        activityRepository.saveAll(activities);

        log.info("Completed transport recalculation for trip {}: {}",
                tripId, result.getMessage());

        return result;
    }
}
