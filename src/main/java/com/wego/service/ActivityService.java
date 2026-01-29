package com.wego.service;

import com.wego.domain.permission.PermissionChecker;
import com.wego.dto.request.CreateActivityRequest;
import com.wego.dto.request.ReorderActivitiesRequest;
import com.wego.dto.request.UpdateActivityRequest;
import com.wego.dto.response.ActivityResponse;
import com.wego.entity.Activity;
import com.wego.entity.Place;
import com.wego.exception.BusinessException;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.repository.ActivityRepository;
import com.wego.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

        Activity saved = activityRepository.save(activity);
        log.info("Created activity {} for trip {}", saved.getId(), tripId);

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
        if (request.getTransportMode() != null) {
            activity.setTransportMode(request.getTransportMode());
        }

        activity.setUpdatedAt(Instant.now());

        Activity saved = activityRepository.save(activity);
        log.info("Updated activity {}", activityId);

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

        // Save all
        List<Activity> saved = activityRepository.saveAll(existingActivities);
        log.info("Reordered {} activities for trip {} day {}", saved.size(), tripId, request.getDay());

        return mapActivitiesToResponses(saved);
    }

    /**
     * Maps a list of activities to activity responses.
     *
     * @param activities The activities to map
     * @return List of activity responses
     */
    private List<ActivityResponse> mapActivitiesToResponses(List<Activity> activities) {
        return activities.stream()
                .map(activity -> {
                    Place place = activity.getPlaceId() != null
                            ? placeRepository.findById(activity.getPlaceId()).orElse(null)
                            : null;
                    return ActivityResponse.fromEntity(activity, place);
                })
                .collect(Collectors.toList());
    }
}
