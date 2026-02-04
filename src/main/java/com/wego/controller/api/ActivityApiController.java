package com.wego.controller.api;

import com.wego.dto.ApiResponse;
import com.wego.dto.request.ApplyOptimizationRequest;
import com.wego.dto.request.CreateActivityRequest;
import com.wego.dto.request.ReorderActivitiesRequest;
import com.wego.dto.request.UpdateActivityRequest;
import com.wego.dto.response.ActivityResponse;
import com.wego.dto.response.RouteOptimizationResponse;
import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
import com.wego.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wego.exception.UnauthorizedException;

import java.util.List;
import java.util.UUID;

/**
 * REST API controller for activity operations.
 *
 * Provides endpoints for CRUD operations and reordering of activities within trips.
 *
 * @contract
 *   - pre: User must be authenticated
 *   - post: All responses follow ApiResponse format
 *   - calls: ActivityService
 *   - calledBy: Frontend, external API clients
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActivityApiController {

    private final ActivityService activityService;

    /**
     * Creates a new activity in a trip.
     *
     * @contract
     *   - pre: User authenticated, has edit permission on trip
     *   - pre: Request body validated
     *   - post: Returns 201 with created activity
     *   - calls: ActivityService#createActivity
     *
     * @param tripId The trip ID
     * @param request The create activity request
     * @param principal The current user
     * @return Response with created activity
     */
    @PostMapping("/trips/{tripId}/activities")
    public ResponseEntity<ApiResponse<ActivityResponse>> createActivity(
            @PathVariable UUID tripId,
            @Valid @RequestBody CreateActivityRequest request,
            @CurrentUser UserPrincipal principal) {

        log.debug("POST /api/trips/{}/activities - Creating activity", tripId);

        UUID userId = requireUserId(principal);
        ActivityResponse response = activityService.createActivity(tripId, request, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Activity created successfully"));
    }

    /**
     * Gets activities for a trip, optionally filtered by day.
     *
     * @contract
     *   - pre: User authenticated, has view permission on trip
     *   - post: Returns 200 with list of activities
     *   - post: If day param provided, returns activities for that day only
     *   - calls: ActivityService#getActivitiesByTrip or ActivityService#getActivitiesByDay
     *
     * @param tripId The trip ID
     * @param day Optional day filter
     * @param principal The current user
     * @return Response with list of activities
     */
    @GetMapping("/trips/{tripId}/activities")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getActivities(
            @PathVariable UUID tripId,
            @RequestParam(required = false) Integer day,
            @CurrentUser UserPrincipal principal) {

        log.debug("GET /api/trips/{}/activities - day={}", tripId, day);

        UUID userId = requireUserId(principal);
        List<ActivityResponse> responses;

        if (day != null) {
            responses = activityService.getActivitiesByDay(tripId, day, userId);
        } else {
            responses = activityService.getActivitiesByTrip(tripId, userId);
        }

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Updates an existing activity.
     *
     * @contract
     *   - pre: User authenticated, has edit permission on activity's trip
     *   - pre: Request body validated
     *   - post: Returns 200 with updated activity
     *   - calls: ActivityService#updateActivity
     *
     * @param activityId The activity ID
     * @param request The update activity request
     * @param principal The current user
     * @return Response with updated activity
     */
    @PutMapping("/activities/{activityId}")
    public ResponseEntity<ApiResponse<ActivityResponse>> updateActivity(
            @PathVariable UUID activityId,
            @Valid @RequestBody UpdateActivityRequest request,
            @CurrentUser UserPrincipal principal) {

        log.debug("PUT /api/activities/{} - Updating activity", activityId);

        UUID userId = requireUserId(principal);
        ActivityResponse response = activityService.updateActivity(activityId, request, userId);

        return ResponseEntity.ok(ApiResponse.success(response, "Activity updated successfully"));
    }

    /**
     * Deletes an activity.
     *
     * @contract
     *   - pre: User authenticated, has edit permission on activity's trip
     *   - post: Returns 204 No Content
     *   - calls: ActivityService#deleteActivity
     *
     * @param activityId The activity ID
     * @param principal The current user
     * @return Empty response with 204 status
     */
    @DeleteMapping("/activities/{activityId}")
    public ResponseEntity<Void> deleteActivity(
            @PathVariable UUID activityId,
            @CurrentUser UserPrincipal principal) {

        log.debug("DELETE /api/activities/{} - Deleting activity", activityId);

        UUID userId = requireUserId(principal);
        activityService.deleteActivity(activityId, userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Reorders activities within a day of a trip.
     *
     * @contract
     *   - pre: User authenticated, has edit permission on trip
     *   - pre: Request body validated
     *   - post: Returns 200 with reordered activities
     *   - calls: ActivityService#reorderActivities
     *
     * @param tripId The trip ID
     * @param request The reorder request
     * @param principal The current user
     * @return Response with reordered activities
     */
    @PutMapping("/trips/{tripId}/activities/reorder")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> reorderActivities(
            @PathVariable UUID tripId,
            @Valid @RequestBody ReorderActivitiesRequest request,
            @CurrentUser UserPrincipal principal) {

        log.debug("PUT /api/trips/{}/activities/reorder - Reordering activities for day {}",
                tripId, request.getDay());

        UUID userId = requireUserId(principal);
        List<ActivityResponse> responses = activityService.reorderActivities(tripId, request, userId);

        return ResponseEntity.ok(ApiResponse.success(responses, "Activities reordered successfully"));
    }

    /**
     * Gets an optimized route suggestion for activities on a specific day.
     *
     * Uses Greedy Nearest Neighbor algorithm to suggest an optimized route.
     * This does NOT modify the database - it only returns a suggestion.
     *
     * @contract
     *   - pre: User authenticated, has view permission on trip
     *   - pre: day parameter required and >= 1
     *   - post: Returns 200 with optimization result
     *   - calls: ActivityService#getOptimizedRoute
     *
     * @param tripId The trip ID
     * @param day The day number to optimize
     * @param principal The current user
     * @return Response with optimization suggestion
     */
    @GetMapping("/trips/{tripId}/activities/optimize")
    public ResponseEntity<ApiResponse<RouteOptimizationResponse>> getOptimizedRoute(
            @PathVariable UUID tripId,
            @RequestParam int day,
            @CurrentUser UserPrincipal principal) {

        log.debug("GET /api/trips/{}/activities/optimize?day={} - Getting route optimization", tripId, day);

        UUID userId = requireUserId(principal);
        RouteOptimizationResponse response = activityService.getOptimizedRoute(tripId, day, userId);

        String message = response.isOptimizationApplied()
                ? String.format("Route can be optimized: save %s (%.1f%%)",
                        response.getDistanceSavedFormatted(),
                        response.getSavingsPercentage())
                : "Route is already optimal or cannot be improved";

        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    /**
     * Applies an optimized route order to activities on a specific day.
     *
     * Reorders activities according to the provided optimized order.
     * This DOES modify the database.
     *
     * @contract
     *   - pre: User authenticated, has edit permission on trip
     *   - pre: Request body validated
     *   - post: Returns 200 with reordered activities
     *   - calls: ActivityService#applyOptimizedRoute
     *
     * @param tripId The trip ID
     * @param request The apply optimization request
     * @param principal The current user
     * @return Response with reordered activities
     */
    @PostMapping("/trips/{tripId}/activities/apply-optimization")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> applyOptimizedRoute(
            @PathVariable UUID tripId,
            @Valid @RequestBody ApplyOptimizationRequest request,
            @CurrentUser UserPrincipal principal) {

        log.debug("POST /api/trips/{}/activities/apply-optimization - Applying route optimization for day {}",
                tripId, request.getDay());

        UUID userId = requireUserId(principal);
        List<ActivityResponse> responses = activityService.applyOptimizedRoute(
                tripId, request.getDay(), request.getOptimizedOrder(), userId);

        return ResponseEntity.ok(ApiResponse.success(responses, "Route optimization applied successfully"));
    }

    /**
     * Extracts user ID from the principal or throws UnauthorizedException if not authenticated.
     *
     * @contract
     *   - pre: principal != null
     *   - post: returns valid user UUID
     *   - throws: UnauthorizedException if principal is null
     */
    private UUID requireUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("認證已過期，請重新登入");
        }
        return principal.getId();
    }
}
