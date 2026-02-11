package com.wego.controller.web;

import com.wego.dto.request.CreateActivityRequest;
import com.wego.dto.request.UpdateActivityRequest;
import com.wego.dto.response.ActivityResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.Place;
import com.wego.entity.Role;
import com.wego.entity.TransportMode;
import com.wego.entity.User;
import com.wego.service.ActivityService;
import com.wego.service.ExpenseService;
import com.wego.service.PlaceService;
import com.wego.service.TripService;
import com.wego.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for Activity-related web pages within a Trip.
 *
 * @contract
 *   - Handles activity CRUD, listing, and transport recalculation
 *   - Requires authentication for all endpoints
 *   - calledBy: Web browser requests
 *   - calls: TripService, UserService, ActivityService, PlaceService, ExpenseService
 */
@Controller
@RequestMapping("/trips/{tripId}")
@RequiredArgsConstructor
@Slf4j
public class ActivityWebController extends BaseWebController {

    private final TripService tripService;
    private final ActivityService activityService;
    private final PlaceService placeService;
    private final ExpenseService expenseService;

    /**
     * Show trip activities page (timeline view).
     *
     * @contract
     *   - pre: tripId != null, principal != null
     *   - post: Returns activity list view with activities grouped by date
     *   - calls: TripService#getTrip, ActivityService#getActivitiesByTrip
     *   - calledBy: Web browser requests
     */
    @GetMapping("/activities")
    public String showActivities(@PathVariable UUID tripId,
                                  @RequestParam(required = false) Integer day,
                                  @AuthenticationPrincipal OAuth2User principal,
                                  Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(tripId, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", tripId, e.getMessage());
            return "redirect:/dashboard?error=trip_not_found";
        }

        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        // Find current member's role
        TripResponse.MemberSummary currentMember = trip.getMembers().stream()
                .filter(m -> m.getUserId().equals(user.getId()))
                .findFirst()
                .orElse(null);

        boolean canEdit = currentMember != null &&
                (currentMember.getRole() == Role.OWNER ||
                 currentMember.getRole() == Role.EDITOR);

        // Get all activities for the trip
        List<ActivityResponse> activities =
                activityService.getActivitiesByTrip(tripId, user.getId());

        // Group activities by date
        Map<LocalDate, List<ActivityResponse>> activitiesByDate =
                new LinkedHashMap<>();

        // Generate dates from startDate to endDate
        List<LocalDate> dates = trip.getStartDate().datesUntil(trip.getEndDate().plusDays(1))
                .collect(Collectors.toList());

        // Initialize all dates in the map
        for (LocalDate date : dates) {
            activitiesByDate.put(date, new java.util.ArrayList<>());
        }

        // Group activities by their scheduled date
        for (ActivityResponse activity : activities) {
            int activityDay = activity.getDay();
            if (activityDay >= 1 && activityDay <= dates.size()) {
                LocalDate activityDate = trip.getStartDate().plusDays(activityDay - 1);
                activitiesByDate.computeIfAbsent(activityDate, k -> new java.util.ArrayList<>())
                        .add(activity);
            }
        }

        // Sort activities within each day by sortOrder
        for (List<ActivityResponse> dayActivities : activitiesByDate.values()) {
            dayActivities.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
        }

        // Calculate total activity count
        int totalActivityCount = activities.size();

        model.addAttribute("trip", trip);
        model.addAttribute("dates", dates);
        model.addAttribute("activitiesByDate", activitiesByDate);
        model.addAttribute("allActivities", activities);
        model.addAttribute("totalActivityCount", totalActivityCount);
        model.addAttribute("currentDay", day != null ? day : 1);
        model.addAttribute("currentMember", currentMember);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("isOwner", currentMember != null &&
                currentMember.getRole() == Role.OWNER);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        return "activity/list";
    }

    /**
     * Show activity detail page.
     *
     * @contract
     *   - pre: tripId != null, activityId != null, principal != null
     *   - post: Returns activity detail view
     *   - calls: TripService#getTrip, ActivityService#getActivity
     *   - calledBy: Web browser requests
     */
    @GetMapping("/activities/{activityId}")
    public String showActivityDetail(@PathVariable UUID tripId,
                                      @PathVariable UUID activityId,
                                      @AuthenticationPrincipal OAuth2User principal,
                                      Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(tripId, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", tripId, e.getMessage());
            return "redirect:/dashboard?error=trip_not_found";
        }

        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        // Get activity details
        ActivityResponse activity;
        try {
            activity = activityService.getActivity(activityId, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get activity {}: {}", activityId, e.getMessage());
            return "redirect:/trips/" + tripId + "/activities?error=activity_not_found";
        }

        // Find current member's role
        TripResponse.MemberSummary currentMember = trip.getMembers().stream()
                .filter(m -> m.getUserId().equals(user.getId()))
                .findFirst()
                .orElse(null);

        boolean canEdit = currentMember != null &&
                (currentMember.getRole() == Role.OWNER ||
                 currentMember.getRole() == Role.EDITOR);

        // Get next activity for navigation (if exists)
        List<ActivityResponse> allActivities =
                activityService.getActivitiesByTrip(tripId, user.getId());
        ActivityResponse nextActivity = null;
        for (int i = 0; i < allActivities.size() - 1; i++) {
            if (allActivities.get(i).getId().equals(activityId)) {
                nextActivity = allActivities.get(i + 1);
                break;
            }
        }

        model.addAttribute("trip", trip);
        model.addAttribute("activity", activity);
        model.addAttribute("nextActivity", nextActivity);
        model.addAttribute("currentMember", currentMember);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("isOwner", currentMember != null &&
                currentMember.getRole() == Role.OWNER);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        // Get related expenses for this activity
        List<com.wego.dto.response.ExpenseResponse> relatedExpenses = List.of();
        try {
            relatedExpenses = expenseService.getExpensesByActivity(tripId, activityId, user.getId());
        } catch (Exception e) {
            log.warn("Failed to load related expenses for activity {}: {}", activityId, e.getMessage());
        }
        model.addAttribute("relatedExpenses", relatedExpenses);

        return "activity/detail";
    }

    /**
     * Show create activity form.
     *
     * @contract
     *   - pre: tripId != null, principal != null, user has OWNER or EDITOR role
     *   - post: Returns activity create form
     *   - calls: TripService#getTrip
     *   - calledBy: Web browser requests
     */
    @GetMapping("/activities/new")
    public String showActivityCreateForm(@PathVariable UUID tripId,
                                          @RequestParam(required = false) Integer day,
                                          @AuthenticationPrincipal OAuth2User principal,
                                          Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(tripId, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", tripId, e.getMessage());
            return "redirect:/dashboard?error=trip_not_found";
        }

        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        // Check permission
        TripResponse.MemberSummary currentMember = trip.getMembers().stream()
                .filter(m -> m.getUserId().equals(user.getId()))
                .findFirst()
                .orElse(null);

        if (currentMember == null ||
            (currentMember.getRole() != Role.OWNER &&
             currentMember.getRole() != Role.EDITOR)) {
            return "redirect:/trips/" + tripId + "?error=access_denied";
        }

        // Generate dates from startDate to endDate
        List<LocalDate> dates = trip.getStartDate() != null && trip.getEndDate() != null
                ? trip.getStartDate().datesUntil(trip.getEndDate().plusDays(1)).collect(Collectors.toList())
                : List.of(LocalDate.now());

        model.addAttribute("trip", trip);
        model.addAttribute("tripId", tripId);
        model.addAttribute("dates", dates);
        model.addAttribute("selectedDay", day != null ? day : 1);
        model.addAttribute("canEdit", true);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        // Add default coordinates for place search (from first activity or default to Taipei)
        addSearchCoordinatesToModel(tripId, user.getId(), model);

        return "activity/create";
    }

    /**
     * Handle create activity form submission.
     *
     * @contract
     *   - pre: tripId != null, principal != null, user has OWNER or EDITOR role
     *   - post: Activity is created, redirects to activities list
     *   - calls: PlaceService#findOrCreate, ActivityService#createActivity
     *   - calledBy: Web browser form submission
     */
    @PostMapping("/activities")
    public String createActivity(@PathVariable UUID tripId,
                                  @RequestParam String placeName,
                                  @RequestParam(required = false) String address,
                                  @RequestParam(required = false) String placeId,
                                  @RequestParam(required = false) Double latitude,
                                  @RequestParam(required = false) Double longitude,
                                  @RequestParam String activityDate,
                                  @RequestParam(required = false) String startTime,
                                  @RequestParam(required = false) String endTime,
                                  @RequestParam(required = false) Integer durationMinutes,
                                  @RequestParam(required = false) String notes,
                                  @RequestParam(required = false, defaultValue = "ATTRACTION") String type,
                                  @RequestParam(required = false, defaultValue = "WALKING") String transportMode,
                                  @RequestParam(required = false) Integer manualTransportMinutes,
                                  @AuthenticationPrincipal OAuth2User principal,
                                  Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            // Get trip to verify permission and calculate day
            TripResponse trip = tripService.getTrip(tripId, user.getId());
            if (trip == null) {
                return "redirect:/dashboard?error=trip_not_found";
            }

            // Check permission
            TripResponse.MemberSummary currentMember = trip.getMembers().stream()
                    .filter(m -> m.getUserId().equals(user.getId()))
                    .findFirst()
                    .orElse(null);

            if (currentMember == null ||
                (currentMember.getRole() != Role.OWNER &&
                 currentMember.getRole() != Role.EDITOR)) {
                return "redirect:/trips/" + tripId + "?error=access_denied";
            }

            // Find or create Place via PlaceService
            Place place = placeService.findOrCreate(placeId, placeName, address, latitude, longitude, type);

            // Calculate day from activityDate
            LocalDate selectedDate = LocalDate.parse(activityDate);
            int day = (int) ChronoUnit.DAYS.between(trip.getStartDate(), selectedDate) + 1;

            // Parse times
            LocalTime parsedStartTime = (startTime != null && !startTime.isEmpty())
                    ? LocalTime.parse(startTime)
                    : null;

            // Parse transport mode
            TransportMode parsedTransportMode;
            try {
                parsedTransportMode = TransportMode.valueOf(transportMode);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid transport mode '{}', defaulting to WALKING", transportMode);
                parsedTransportMode = TransportMode.WALKING;
            }

            // Validate manualTransportMinutes range (0-2880, max 48 hours)
            Integer validatedManualMinutes = manualTransportMinutes;
            if (validatedManualMinutes != null) {
                if (validatedManualMinutes < 0) {
                    log.warn("Invalid manualTransportMinutes '{}', must be >= 0, setting to null",
                            validatedManualMinutes);
                    validatedManualMinutes = null;
                } else if (validatedManualMinutes > 2880) {
                    log.warn("manualTransportMinutes '{}' exceeds max (2880), capping",
                            validatedManualMinutes);
                    validatedManualMinutes = 2880;
                }
            }

            // Validate: FLIGHT/HIGH_SPEED_RAIL requires manual input
            if (parsedTransportMode.requiresManualInput() &&
                (validatedManualMinutes == null || validatedManualMinutes <= 0)) {
                log.warn("Transport mode '{}' requires manual duration but none provided",
                        parsedTransportMode);
                model.addAttribute("error", "選擇飛機或高鐵時，必須輸入預估交通時間");
                return "redirect:/trips/" + tripId + "/activities/new?error=manual_time_required";
            }

            // Build request
            CreateActivityRequest request = CreateActivityRequest.builder()
                    .placeId(place.getId())
                    .day(day)
                    .startTime(parsedStartTime)
                    .durationMinutes(durationMinutes)
                    .note(notes)
                    .transportMode(parsedTransportMode)
                    .manualTransportMinutes(validatedManualMinutes)
                    .build();

            // Create activity
            activityService.createActivity(tripId, request, user.getId());
            log.info("Created activity for trip {} on day {}", tripId, day);

            return "redirect:/trips/" + tripId + "/activities";

        } catch (Exception e) {
            log.error("Failed to create activity: {}", e.getMessage(), e);
            model.addAttribute("error", "新增景點失敗：" + e.getMessage());
            return "redirect:/trips/" + tripId + "/activities/new?error=create_failed";
        }
    }

    /**
     * Show edit activity form.
     *
     * @contract
     *   - pre: tripId != null, activityId != null, principal != null
     *   - pre: user has OWNER or EDITOR role
     *   - post: Returns activity edit form with existing data
     *   - calls: TripService#getTrip, ActivityService#getActivity
     *   - calledBy: Web browser requests
     */
    @GetMapping("/activities/{activityId}/edit")
    public String showActivityEditForm(@PathVariable UUID tripId,
                                        @PathVariable UUID activityId,
                                        @AuthenticationPrincipal OAuth2User principal,
                                        Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(tripId, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", tripId, e.getMessage());
            return "redirect:/dashboard?error=trip_not_found";
        }

        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        // Check permission
        TripResponse.MemberSummary currentMember = trip.getMembers().stream()
                .filter(m -> m.getUserId().equals(user.getId()))
                .findFirst()
                .orElse(null);

        if (currentMember == null ||
            (currentMember.getRole() != Role.OWNER &&
             currentMember.getRole() != Role.EDITOR)) {
            return "redirect:/trips/" + tripId + "?error=access_denied";
        }

        // Get activity details
        ActivityResponse activity;
        try {
            activity = activityService.getActivity(activityId, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get activity {}: {}", activityId, e.getMessage());
            return "redirect:/trips/" + tripId + "/activities?error=activity_not_found";
        }

        // Generate dates from startDate to endDate
        List<LocalDate> dates = trip.getStartDate() != null && trip.getEndDate() != null
                ? trip.getStartDate().datesUntil(trip.getEndDate().plusDays(1)).collect(Collectors.toList())
                : List.of(LocalDate.now());

        // Calculate selected date from activity's day
        LocalDate selectedDate = trip.getStartDate().plusDays(activity.getDay() - 1);

        model.addAttribute("trip", trip);
        model.addAttribute("tripId", tripId);
        model.addAttribute("activity", activity);
        model.addAttribute("dates", dates);
        model.addAttribute("selectedDay", activity.getDay());
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("isEdit", true);
        model.addAttribute("canEdit", true);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        // Add default coordinates for place search
        addSearchCoordinatesToModel(tripId, user.getId(), model);

        return "activity/create";
    }

    /**
     * Handle update activity form submission.
     *
     * @contract
     *   - pre: tripId != null, activityId != null, principal != null
     *   - pre: user has OWNER or EDITOR role
     *   - post: Activity is updated, transport recalculated if mode/place changed
     *   - post: redirects to activities list with success message
     *   - calls: PlaceService#findOrCreate, ActivityService#updateActivity
     *   - calledBy: Web browser form submission
     */
    @PostMapping("/activities/{activityId}")
    public String updateActivity(@PathVariable UUID tripId,
                                  @PathVariable UUID activityId,
                                  @RequestParam String placeName,
                                  @RequestParam(required = false) String address,
                                  @RequestParam(required = false) String placeId,
                                  @RequestParam(required = false) Double latitude,
                                  @RequestParam(required = false) Double longitude,
                                  @RequestParam String activityDate,
                                  @RequestParam(required = false) String startTime,
                                  @RequestParam(required = false) String endTime,
                                  @RequestParam(required = false) Integer durationMinutes,
                                  @RequestParam(required = false) String notes,
                                  @RequestParam(required = false, defaultValue = "WALKING") String transportMode,
                                  @RequestParam(required = false) Integer manualTransportMinutes,
                                  @RequestParam(required = false, defaultValue = "ATTRACTION") String type,
                                  @AuthenticationPrincipal OAuth2User principal,
                                  RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            // Get trip to verify permission and calculate day
            TripResponse trip = tripService.getTrip(tripId, user.getId());
            if (trip == null) {
                return "redirect:/dashboard?error=trip_not_found";
            }

            // Check permission
            TripResponse.MemberSummary currentMember = trip.getMembers().stream()
                    .filter(m -> m.getUserId().equals(user.getId()))
                    .findFirst()
                    .orElse(null);

            if (currentMember == null ||
                (currentMember.getRole() != Role.OWNER &&
                 currentMember.getRole() != Role.EDITOR)) {
                return "redirect:/trips/" + tripId + "?error=access_denied";
            }

            // Find or create Place via PlaceService
            Place place = placeService.findOrCreate(placeId, placeName, address, latitude, longitude, type);

            // Calculate day from activityDate
            LocalDate selectedDate = LocalDate.parse(activityDate);
            int day = (int) ChronoUnit.DAYS.between(trip.getStartDate(), selectedDate) + 1;

            // Parse times
            LocalTime parsedStartTime = (startTime != null && !startTime.isEmpty())
                    ? LocalTime.parse(startTime)
                    : null;

            // Parse transport mode
            TransportMode parsedTransportMode;
            try {
                parsedTransportMode = TransportMode.valueOf(transportMode);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid transport mode '{}', defaulting to WALKING", transportMode);
                parsedTransportMode = TransportMode.WALKING;
            }

            // Validate manualTransportMinutes range (0-2880, max 48 hours)
            Integer validatedManualMinutes = manualTransportMinutes;
            if (validatedManualMinutes != null) {
                if (validatedManualMinutes < 0) {
                    log.warn("Invalid manualTransportMinutes '{}', must be >= 0, setting to null",
                            validatedManualMinutes);
                    validatedManualMinutes = null;
                } else if (validatedManualMinutes > 2880) {
                    log.warn("manualTransportMinutes '{}' exceeds max (2880), capping",
                            validatedManualMinutes);
                    validatedManualMinutes = 2880;
                }
            }

            // Validate: FLIGHT/HIGH_SPEED_RAIL requires manual input
            if (parsedTransportMode.requiresManualInput() &&
                (validatedManualMinutes == null || validatedManualMinutes <= 0)) {
                log.warn("Transport mode '{}' requires manual duration but none provided",
                        parsedTransportMode);
                redirectAttributes.addFlashAttribute("error", "選擇飛機或高鐵時，必須輸入預估交通時間");
                return "redirect:/trips/" + tripId + "/activities/" + activityId + "/edit?error=manual_time_required";
            }

            // Build update request
            UpdateActivityRequest request = UpdateActivityRequest.builder()
                    .placeId(place.getId())
                    .day(day)
                    .startTime(parsedStartTime)
                    .durationMinutes(durationMinutes)
                    .note(notes)
                    .transportMode(parsedTransportMode)
                    .manualTransportMinutes(validatedManualMinutes)
                    .build();

            // Update activity (this will recalculate transport if needed)
            activityService.updateActivity(activityId, request, user.getId());
            log.info("Updated activity {} for trip {} on day {}", activityId, tripId, day);

            redirectAttributes.addFlashAttribute("success", "景點已更新，交通時間已重新計算");
            return "redirect:/trips/" + tripId + "/activities";

        } catch (Exception e) {
            log.error("Failed to update activity: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "更新景點失敗：" + e.getMessage());
            return "redirect:/trips/" + tripId + "/activities/" + activityId + "/edit?error=update_failed";
        }
    }

    /**
     * Delete an activity from a trip.
     *
     * @contract
     *   - pre: tripId != null, activityId != null, principal != null
     *   - pre: user has OWNER or EDITOR role
     *   - post: Activity is deleted
     *   - post: Redirects to activities list with success message
     *   - calls: ActivityService#deleteActivity
     *   - calledBy: Web browser form submission (delete confirmation dialog)
     */
    @PostMapping("/activities/{activityId}/delete")
    public String deleteActivity(@PathVariable UUID tripId,
                                 @PathVariable UUID activityId,
                                 @AuthenticationPrincipal OAuth2User principal,
                                 RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            activityService.deleteActivity(activityId, user.getId());
            redirectAttributes.addFlashAttribute("success", "景點已刪除");
        } catch (Exception e) {
            log.error("Failed to delete activity: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "刪除失敗：" + e.getMessage());
        }

        return "redirect:/trips/" + tripId + "/activities";
    }

    /**
     * Show activity duplicate form (pre-filled create form from existing activity).
     *
     * @contract
     *   - pre: tripId != null, activityId != null, principal != null
     *   - pre: user has OWNER or EDITOR role on the trip
     *   - post: Returns activity create form pre-filled with source activity data
     *   - post: Activity id is null so form creates a new activity
     *   - calls: TripService#getTrip, ActivityService#getActivity
     *   - calledBy: Web browser GET /trips/{tripId}/activities/{activityId}/duplicate
     */
    @GetMapping("/activities/{activityId}/duplicate")
    public String duplicateActivity(@PathVariable UUID tripId,
                                     @PathVariable UUID activityId,
                                     @AuthenticationPrincipal OAuth2User principal,
                                     Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(tripId, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", tripId, e.getMessage());
            return "redirect:/dashboard?error=trip_not_found";
        }

        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        // Check permission
        TripResponse.MemberSummary currentMember = trip.getMembers().stream()
                .filter(m -> m.getUserId().equals(user.getId()))
                .findFirst()
                .orElse(null);

        if (currentMember == null ||
            (currentMember.getRole() != Role.OWNER &&
             currentMember.getRole() != Role.EDITOR)) {
            return "redirect:/trips/" + tripId + "?error=access_denied";
        }

        // Get source activity
        ActivityResponse sourceActivity;
        try {
            sourceActivity = activityService.getActivity(activityId, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get activity {} for duplication: {}", activityId, e.getMessage());
            return "redirect:/trips/" + tripId + "/activities/" + activityId + "?error=activity_not_found";
        }

        // Create a copy with id set to null so the form creates a new activity
        ActivityResponse duplicated = ActivityResponse.builder()
                .id(null)
                .tripId(tripId)
                .place(sourceActivity.getPlace())
                .day(sourceActivity.getDay())
                .sortOrder(0)
                .startTime(sourceActivity.getStartTime())
                .endTime(sourceActivity.getEndTime())
                .durationMinutes(sourceActivity.getDurationMinutes())
                .note(sourceActivity.getNote())
                .transportMode(sourceActivity.getTransportMode())
                .build();

        // Generate dates from startDate to endDate
        List<LocalDate> dates = trip.getStartDate() != null && trip.getEndDate() != null
                ? trip.getStartDate().datesUntil(trip.getEndDate().plusDays(1)).collect(Collectors.toList())
                : List.of(LocalDate.now());

        model.addAttribute("trip", trip);
        model.addAttribute("tripId", tripId);
        model.addAttribute("dates", dates);
        model.addAttribute("selectedDay", sourceActivity.getDay());
        model.addAttribute("activity", duplicated);
        model.addAttribute("canEdit", true);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        addSearchCoordinatesToModel(tripId, user.getId(), model);

        return "activity/create";
    }

    /**
     * Recalculate all transport times for a trip.
     *
     * Uses Google Maps API with rate limiting. Falls back to Haversine when API fails.
     * Manual inputs (FLIGHT/HIGH_SPEED_RAIL) are preserved.
     *
     * @contract
     *   - pre: tripId != null, principal != null
     *   - pre: user has OWNER or EDITOR role
     *   - post: All activities have transport times recalculated
     *   - post: Redirects to activities list with result message
     *   - calls: ActivityService#recalculateAllTransport
     *   - calledBy: Web browser POST request
     */
    @PostMapping("/recalculate-transport")
    public String recalculateTransport(@PathVariable UUID tripId,
                                        @AuthenticationPrincipal OAuth2User principal,
                                        RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            // Recalculate all transport with default max 50 API calls
            var result = activityService.recalculateAllTransport(tripId, user.getId(), 50);

            // Build success message
            String message = result.getSuccessMessage();
            redirectAttributes.addFlashAttribute("success", message);

            log.info("Transport recalculation completed for trip {}: total={}, api={}, fallback={}",
                    tripId, result.getTotalActivities(), result.getApiSuccessCount(), result.getFallbackCount());

            return "redirect:/trips/" + tripId + "/activities";

        } catch (com.wego.exception.ForbiddenException e) {
            log.warn("User {} not authorized to recalculate transport for trip {}", user.getId(), tripId);
            redirectAttributes.addFlashAttribute("error", "您沒有權限重新計算此行程的交通時間");
            return "redirect:/trips/" + tripId + "/activities";
        } catch (Exception e) {
            log.error("Failed to recalculate transport for trip {}: {}", tripId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "重新計算交通時間失敗：" + e.getMessage());
            return "redirect:/trips/" + tripId + "/activities";
        }
    }

    /**
     * Adds default search coordinates for place search functionality.
     *
     * @contract
     *   - pre: tripId != null, userId != null, model != null
     *   - post: Adds searchLat, searchLng, searchRadius to model
     *   - post: Coordinates come from first activity's place, or default to Taipei (25.0330, 121.5654)
     *   - calls: ActivityService#getActivitiesByTrip
     *   - calledBy: showActivityCreateForm, showActivityEditForm, duplicateActivity
     */
    private void addSearchCoordinatesToModel(UUID tripId, UUID userId, Model model) {
        // Default coordinates (Taipei)
        double defaultLat = 25.0330;
        double defaultLng = 121.5654;
        int searchRadius = 50000; // 50km

        try {
            // Try to get coordinates from existing activities
            List<ActivityResponse> activities =
                    activityService.getActivitiesByTrip(tripId, userId);

            activities.stream()
                    .filter(a -> a.getPlace() != null &&
                            a.getPlace().getLatitude() != 0 &&
                            a.getPlace().getLongitude() != 0)
                    .findFirst()
                    .ifPresent(activity -> {
                        model.addAttribute("searchLat", activity.getPlace().getLatitude());
                        model.addAttribute("searchLng", activity.getPlace().getLongitude());
                    });

            // Set defaults if not set from activities
            if (!model.containsAttribute("searchLat")) {
                model.addAttribute("searchLat", defaultLat);
                model.addAttribute("searchLng", defaultLng);
            }
        } catch (Exception e) {
            log.warn("Failed to get search coordinates for trip {}: {}", tripId, e.getMessage());
            model.addAttribute("searchLat", defaultLat);
            model.addAttribute("searchLng", defaultLng);
        }

        model.addAttribute("searchRadius", searchRadius);
    }

}
