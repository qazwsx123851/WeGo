package com.wego.controller.web;

import com.wego.dto.request.CreateActivityRequest;
import com.wego.dto.request.UpdateActivityRequest;
import com.wego.dto.response.ActivityResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.Place;
import com.wego.entity.Role;
import com.wego.entity.TransportMode;
import com.wego.entity.User;
import com.wego.dto.response.TodoResponse;
import com.wego.entity.TodoStatus;
import com.wego.repository.PlaceRepository;
import com.wego.dto.response.InviteLinkResponse;
import com.wego.service.ActivityService;
import com.wego.service.DocumentService;
import com.wego.service.ExpenseService;
import com.wego.service.InviteLinkService;
import com.wego.service.TodoService;
import com.wego.service.TripService;
import com.wego.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.wego.dto.request.CreateTripRequest;
import com.wego.dto.request.UpdateTripRequest;
import com.wego.exception.ValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for Trip-related web pages.
 *
 * @contract
 *   - Handles trip list, detail, create, and edit pages
 *   - Requires authentication for all endpoints
 *   - calledBy: Web browser requests
 *   - calls: TripService, UserService, ActivityService, TodoService
 */
@Controller
@RequestMapping("/trips")
@RequiredArgsConstructor
@Slf4j
public class TripController {

    private final TripService tripService;
    private final UserService userService;
    private final ActivityService activityService;
    private final TodoService todoService;
    private final ExpenseService expenseService;
    private final DocumentService documentService;
    private final InviteLinkService inviteLinkService;
    private final PlaceRepository placeRepository;

    /**
     * List all trips for the current user.
     */
    @GetMapping
    public String listTrips(@AuthenticationPrincipal OAuth2User principal, Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        Page<TripResponse> tripPage = tripService.getUserTrips(user.getId(),
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "startDate")));
        List<TripResponse> trips = tripPage.getContent();

        model.addAttribute("trips", trips);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        return "trip/list";
    }

    /**
     * Show create trip form.
     */
    @GetMapping("/create")
    public String showCreateForm(@AuthenticationPrincipal OAuth2User principal, Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());
        model.addAttribute("minDate", LocalDate.now());
        model.addAttribute("isEdit", false);
        // Add empty trip object for form binding
        model.addAttribute("trip", CreateTripRequest.builder().build());

        return "trip/create";
    }

    /**
     * Handle create trip form submission.
     *
     * @contract
     *   - pre: user is authenticated, form data is valid
     *   - post: Trip is created, cover image uploaded if provided
     *   - post: redirects to trip detail page
     *   - calls: TripService#createTrip, TripService#uploadCoverImage
     */
    @PostMapping("/create")
    public String createTrip(@RequestParam String title,
                             @RequestParam(required = false) String description,
                             @RequestParam LocalDate startDate,
                             @RequestParam LocalDate endDate,
                             @RequestParam(required = false) MultipartFile coverImage,
                             @AuthenticationPrincipal OAuth2User principal,
                             Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        // Validate dates
        if (endDate.isBefore(startDate)) {
            model.addAttribute("dateError", "結束日期不可早於開始日期");
            model.addAttribute("name", user.getNickname());
            model.addAttribute("picture", user.getAvatarUrl());
            model.addAttribute("minDate", LocalDate.now());
            model.addAttribute("isEdit", false);
            model.addAttribute("trip", CreateTripRequest.builder()
                    .title(title)
                    .description(description)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build());
            return "trip/create";
        }

        try {
            // Create trip first (without cover image)
            CreateTripRequest request = CreateTripRequest.builder()
                    .title(title)
                    .description(description)
                    .startDate(startDate)
                    .endDate(endDate)
                    .baseCurrency("TWD")
                    .build();

            TripResponse createdTrip = tripService.createTrip(request, user);
            log.info("Created trip: {} by user: {}", createdTrip.getId(), user.getId());

            // Upload cover image after trip is created (so we have tripId, no orphaned files)
            if (coverImage != null && !coverImage.isEmpty()) {
                try {
                    String coverImageUrl = tripService.uploadCoverImage(createdTrip.getId(), user.getId(), coverImage);
                    // Update trip with cover image URL
                    UpdateTripRequest updateRequest = UpdateTripRequest.builder()
                            .coverImageUrl(coverImageUrl)
                            .build();
                    tripService.updateTrip(createdTrip.getId(), updateRequest, user.getId());
                    log.info("Cover image uploaded successfully for trip {}: {}", createdTrip.getId(), coverImageUrl);
                } catch (ValidationException e) {
                    // Cover image upload failed, but trip was created - redirect with warning
                    log.warn("Cover image validation failed for trip {}: {}", createdTrip.getId(), e.getMessage());
                    return "redirect:/trips/" + createdTrip.getId() + "?warning=cover_upload_failed";
                } catch (com.wego.service.external.StorageException e) {
                    // Storage operation failed, but trip was created - redirect with warning
                    log.error("Cover image storage failed for trip {}: {}", createdTrip.getId(), e.getMessage());
                    return "redirect:/trips/" + createdTrip.getId() + "?warning=cover_upload_failed";
                }
            }

            return "redirect:/trips/" + createdTrip.getId();
        } catch (ValidationException e) {
            log.warn("Cover image validation failed: {}", e.getMessage());
            model.addAttribute("coverImageError", e.getMessage());
            model.addAttribute("name", user.getNickname());
            model.addAttribute("picture", user.getAvatarUrl());
            model.addAttribute("minDate", LocalDate.now());
            model.addAttribute("isEdit", false);
            model.addAttribute("trip", CreateTripRequest.builder()
                    .title(title)
                    .description(description)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build());
            return "trip/create";
        } catch (Exception e) {
            log.error("Failed to create trip: {}", e.getMessage());
            model.addAttribute("error", "建立行程失敗，請稍後再試");
            model.addAttribute("name", user.getNickname());
            model.addAttribute("picture", user.getAvatarUrl());
            model.addAttribute("minDate", LocalDate.now());
            model.addAttribute("isEdit", false);
            model.addAttribute("trip", CreateTripRequest.builder()
                    .title(title)
                    .description(description)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build());
            return "trip/create";
        }
    }

    /**
     * Show trip detail page.
     *
     * @contract
     *   - pre: id != null, principal != null
     *   - post: Returns trip detail view with weather coordinates if activities have places
     *   - calls: TripService#getTrip, ActivityService#getActivitiesByTrip
     *   - calledBy: Web browser requests
     */
    @GetMapping("/{id}")
    public String showTripDetail(@PathVariable UUID id,
                                  @AuthenticationPrincipal OAuth2User principal,
                                  Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(id, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", id, e.getMessage());
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

        model.addAttribute("trip", trip);
        model.addAttribute("currentMember", currentMember);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("isOwner", currentMember != null &&
                currentMember.getRole() == Role.OWNER);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        // Calculate trip duration
        if (trip.getStartDate() != null && trip.getEndDate() != null) {
            long tripDays = ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1;
            model.addAttribute("tripDays", tripDays);
            model.addAttribute("tripNights", tripDays - 1);
        } else {
            model.addAttribute("tripDays", 0L);
            model.addAttribute("tripNights", 0L);
        }

        // Calculate days until trip
        if (trip.getStartDate() != null && trip.getStartDate().isAfter(LocalDate.now())) {
            long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), trip.getStartDate());
            model.addAttribute("daysUntil", daysUntil);
        }

        // Member data
        List<TripResponse.MemberSummary> members = trip.getMembers();
        model.addAttribute("members", members);
        model.addAttribute("memberCount", members != null ? members.size() : 0);

        // Activity data
        List<com.wego.dto.response.ActivityResponse> activities = List.of();
        try {
            activities = activityService.getActivitiesByTrip(id, user.getId());
        } catch (Exception e) {
            log.warn("Failed to load activities for trip {}: {}", id, e.getMessage());
        }
        model.addAttribute("activityCount", activities.size());
        model.addAttribute("upcomingActivities", activities.stream().limit(3).collect(Collectors.toList()));

        // Expense and document statistics (via Service layer with permission checks)
        long expenseCount = expenseService.getExpenseCount(id, user.getId());
        long documentCount = documentService.getDocumentCount(id, user.getId());

        // Calculate total expense in trip's base currency
        String baseCurrency = trip.getBaseCurrency() != null ? trip.getBaseCurrency() : "TWD";
        BigDecimal totalExpense = expenseService.getTotalExpense(id, baseCurrency, user.getId());

        // Calculate average expense per member
        int memberCountForCalc = members != null && !members.isEmpty() ? members.size() : 1;
        BigDecimal averageExpense = totalExpense.compareTo(BigDecimal.ZERO) > 0
                ? totalExpense.divide(BigDecimal.valueOf(memberCountForCalc), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        model.addAttribute("expenseCount", expenseCount);
        model.addAttribute("documentCount", documentCount);
        model.addAttribute("totalExpense", totalExpense);
        model.addAttribute("averageExpense", averageExpense);

        // Get weather fallback coordinates (used when user denies geolocation)
        addWeatherCoordinatesToModel(activities, trip, model);

        // Load todo data for preview section
        addTodoDataToModel(id, user.getId(), model);

        return "trip/view";
    }

    /**
     * Loads todo data for the trip preview section.
     *
     * @contract
     *   - pre: tripId != null, userId != null, model != null
     *   - post: Adds todoCount, todoCompletedCount, upcomingTodos to model
     *   - calls: TodoService#getTodosByTrip, TodoService#getTodoStats
     *   - calledBy: showTripDetail
     */
    private void addTodoDataToModel(UUID tripId, UUID userId, Model model) {
        try {
            // Get todo stats
            Map<TodoStatus, Long> stats = todoService.getTodoStats(tripId, userId);
            long totalTodos = stats.values().stream().mapToLong(Long::longValue).sum();
            long completedTodos = stats.getOrDefault(TodoStatus.COMPLETED, 0L);

            model.addAttribute("todoCount", totalTodos);
            model.addAttribute("todoCompletedCount", completedTodos);

            // Get upcoming todos (first 3 PENDING or IN_PROGRESS, sorted by due date)
            List<TodoResponse> allTodos = todoService.getTodosByTrip(tripId, userId);
            List<TodoResponse> upcomingTodos = allTodos.stream()
                    .filter(t -> t.getStatus() == TodoStatus.PENDING || t.getStatus() == TodoStatus.IN_PROGRESS)
                    .limit(3)
                    .collect(Collectors.toList());

            model.addAttribute("upcomingTodos", upcomingTodos);

            log.debug("Todo data loaded: total={}, completed={}, upcoming={}",
                    totalTodos, completedTodos, upcomingTodos.size());
        } catch (Exception e) {
            log.warn("Failed to load todo data for trip {}: {}", tripId, e.getMessage());
            // Set defaults so the page doesn't break
            model.addAttribute("todoCount", 0L);
            model.addAttribute("todoCompletedCount", 0L);
            model.addAttribute("upcomingTodos", List.of());
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
     *   - calledBy: showActivityCreateForm
     */
    private void addSearchCoordinatesToModel(UUID tripId, UUID userId, Model model) {
        // Default coordinates (Taipei)
        double defaultLat = 25.0330;
        double defaultLng = 121.5654;
        int searchRadius = 50000; // 50km

        try {
            // Try to get coordinates from existing activities
            List<com.wego.dto.response.ActivityResponse> activities =
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

    /**
     * Extracts weather fallback coordinates for when user denies geolocation.
     *
     * Priority:
     * 1. Today's first activity with coordinates
     * 2. Any activity with coordinates (sorted by day)
     * 3. Default: Taipei 101 (25.0339, 121.5645)
     *
     * @contract
     *   - pre: activities != null, model != null, trip != null
     *   - post: Always adds weatherFallbackLat and weatherFallbackLng to model
     */
    private void addWeatherCoordinatesToModel(
            List<com.wego.dto.response.ActivityResponse> activities,
            com.wego.dto.response.TripResponse trip,
            Model model) {

        // Default: Taipei 101
        final double defaultLat = 25.0339;
        final double defaultLng = 121.5645;

        // Calculate today's day number within the trip
        LocalDate today = LocalDate.now();
        LocalDate tripStart = trip.getStartDate();
        int todayDayNumber = (int) java.time.temporal.ChronoUnit.DAYS.between(tripStart, today) + 1;

        // Priority 1: Find today's first activity with coordinates
        var todayActivity = activities.stream()
                .filter(a -> a.getDay() == todayDayNumber)
                .filter(a -> a.getPlace() != null)
                .filter(a -> a.getPlace().getLatitude() != 0 || a.getPlace().getLongitude() != 0)
                .min((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));

        if (todayActivity.isPresent()) {
            var place = todayActivity.get().getPlace();
            model.addAttribute("weatherFallbackLat", place.getLatitude());
            model.addAttribute("weatherFallbackLng", place.getLongitude());
            log.debug("Weather fallback: today's activity at ({}, {})", place.getLatitude(), place.getLongitude());
            return;
        }

        // Priority 2: Find any activity with coordinates (sorted by day, then sortOrder)
        var anyActivity = activities.stream()
                .filter(a -> a.getPlace() != null)
                .filter(a -> a.getPlace().getLatitude() != 0 || a.getPlace().getLongitude() != 0)
                .min((a, b) -> {
                    int dayCompare = Integer.compare(a.getDay(), b.getDay());
                    return dayCompare != 0 ? dayCompare : Integer.compare(a.getSortOrder(), b.getSortOrder());
                });

        if (anyActivity.isPresent()) {
            var place = anyActivity.get().getPlace();
            model.addAttribute("weatherFallbackLat", place.getLatitude());
            model.addAttribute("weatherFallbackLng", place.getLongitude());
            log.debug("Weather fallback: first activity at ({}, {})", place.getLatitude(), place.getLongitude());
            return;
        }

        // Priority 3: Default to Taipei 101
        model.addAttribute("weatherFallbackLat", defaultLat);
        model.addAttribute("weatherFallbackLng", defaultLng);
        log.debug("Weather fallback: default Taipei 101 ({}, {})", defaultLat, defaultLng);
    }

    /**
     * Show trip activities page (timeline view).
     *
     * @contract
     *   - pre: id != null, principal != null
     *   - post: Returns activity list view with activities grouped by date
     *   - calls: TripService#getTrip, ActivityService#getActivitiesByTrip
     *   - calledBy: Web browser requests
     */
    @GetMapping("/{id}/activities")
    public String showActivities(@PathVariable UUID id,
                                  @RequestParam(required = false) Integer day,
                                  @AuthenticationPrincipal OAuth2User principal,
                                  Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(id, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", id, e.getMessage());
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
        List<com.wego.dto.response.ActivityResponse> activities =
                activityService.getActivitiesByTrip(id, user.getId());

        // Group activities by date
        Map<LocalDate, List<com.wego.dto.response.ActivityResponse>> activitiesByDate =
                new LinkedHashMap<>();

        // Generate dates from startDate to endDate
        List<LocalDate> dates = trip.getStartDate().datesUntil(trip.getEndDate().plusDays(1))
                .collect(Collectors.toList());

        // Initialize all dates in the map
        for (LocalDate date : dates) {
            activitiesByDate.put(date, new java.util.ArrayList<>());
        }

        // Group activities by their scheduled date
        for (com.wego.dto.response.ActivityResponse activity : activities) {
            int activityDay = activity.getDay();
            if (activityDay >= 1 && activityDay <= dates.size()) {
                LocalDate activityDate = trip.getStartDate().plusDays(activityDay - 1);
                activitiesByDate.computeIfAbsent(activityDate, k -> new java.util.ArrayList<>())
                        .add(activity);
            }
        }

        // Sort activities within each day by sortOrder
        for (List<com.wego.dto.response.ActivityResponse> dayActivities : activitiesByDate.values()) {
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
     *   - pre: id != null, activityId != null, principal != null
     *   - post: Returns activity detail view
     *   - calls: TripService#getTrip, ActivityService#getActivity
     *   - calledBy: Web browser requests
     */
    @GetMapping("/{id}/activities/{activityId}")
    public String showActivityDetail(@PathVariable UUID id,
                                      @PathVariable UUID activityId,
                                      @AuthenticationPrincipal OAuth2User principal,
                                      Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(id, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", id, e.getMessage());
            return "redirect:/dashboard?error=trip_not_found";
        }

        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        // Get activity details
        com.wego.dto.response.ActivityResponse activity;
        try {
            activity = activityService.getActivity(activityId, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get activity {}: {}", activityId, e.getMessage());
            return "redirect:/trips/" + id + "/activities?error=activity_not_found";
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
        List<com.wego.dto.response.ActivityResponse> allActivities =
                activityService.getActivitiesByTrip(id, user.getId());
        com.wego.dto.response.ActivityResponse nextActivity = null;
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
            relatedExpenses = expenseService.getExpensesByActivity(id, activityId, user.getId());
        } catch (Exception e) {
            log.warn("Failed to load related expenses for activity {}: {}", activityId, e.getMessage());
        }
        model.addAttribute("relatedExpenses", relatedExpenses);

        return "activity/detail";
    }

    /**
     * Show trip members page.
     */
    @GetMapping("/{id}/members")
    public String showMembersPage(@PathVariable UUID id,
                                   @AuthenticationPrincipal OAuth2User principal,
                                   Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(id, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", id, e.getMessage());
            return "redirect:/dashboard?error=trip_not_found";
        }

        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        List<TripResponse.MemberSummary> members = tripService.getTripMembers(id, user.getId());

        TripResponse.MemberSummary currentMember = members.stream()
                .filter(m -> m.getUserId().equals(user.getId()))
                .findFirst()
                .orElse(null);

        boolean isOwner = currentMember != null &&
                currentMember.getRole() == Role.OWNER;

        boolean canEdit = isOwner ||
                (currentMember != null && currentMember.getRole() == Role.EDITOR);

        model.addAttribute("trip", trip);
        model.addAttribute("members", members);
        model.addAttribute("currentMember", currentMember);
        model.addAttribute("currentUserId", user.getId());
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("memberCount", members.size());
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        // Invite link attributes
        model.addAttribute("canInvite", canEdit);
        try {
            List<InviteLinkResponse> activeLinks = inviteLinkService.getActiveInviteLinks(id, user.getId());
            if (activeLinks != null && !activeLinks.isEmpty()) {
                InviteLinkResponse link = activeLinks.get(0);
                model.addAttribute("inviteLink", link.getInviteUrl());
                model.addAttribute("inviteLinkExpiry", link.getExpiresAt());
            } else {
                model.addAttribute("inviteLink", null);
                model.addAttribute("inviteLinkExpiry", null);
            }
        } catch (Exception e) {
            log.warn("Failed to load invite links for trip {}: {}", id, e.getMessage());
            model.addAttribute("inviteLink", null);
            model.addAttribute("inviteLinkExpiry", null);
        }

        return "trip/members";
    }

    /**
     * Show trip expenses page.
     *
     * @contract
     *   - pre: id != null, principal != null
     *   - post: Returns expense list view with expenses and summary
     *   - calls: TripService#getTrip, ExpenseService#getExpensesByTrip
     *   - calledBy: Web browser requests
     */
    @GetMapping("/{id}/expenses")
    public String showExpenses(@PathVariable UUID id,
                               @AuthenticationPrincipal OAuth2User principal,
                               Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(id, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", id, e.getMessage());
            return "redirect:/dashboard?error=trip_not_found";
        }

        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        // Get expenses for the trip
        var expenses = expenseService.getExpensesByTrip(id, user.getId());

        // Group expenses by date
        Map<LocalDate, List<com.wego.dto.response.ExpenseResponse>> expensesByDate =
                expenses.stream()
                        .collect(Collectors.groupingBy(
                                e -> e.getCreatedAt() != null
                                        ? e.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate()
                                        : LocalDate.now(),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        // Calculate totals
        String baseCurrency = trip.getBaseCurrency() != null ? trip.getBaseCurrency() : "TWD";
        BigDecimal totalExpense = expenseService.getTotalExpense(id, baseCurrency, user.getId());

        int memberCount = trip.getMembers() != null ? trip.getMembers().size() : 1;
        BigDecimal perPersonAverage = memberCount > 0 && totalExpense.compareTo(BigDecimal.ZERO) > 0
                ? totalExpense.divide(BigDecimal.valueOf(memberCount), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Calculate user balance
        BigDecimal userBalance;
        try {
            userBalance = expenseService.calculateUserBalanceInTrip(user.getId(), id);
        } catch (Exception e) {
            log.warn("Failed to calculate user balance for trip {}: {}", id, e.getMessage());
            userBalance = BigDecimal.ZERO;
        }

        model.addAttribute("trip", trip);
        model.addAttribute("expenses", expenses);
        model.addAttribute("expensesByDate", expensesByDate);
        model.addAttribute("totalExpense", totalExpense);
        model.addAttribute("perPersonAverage", perPersonAverage);
        model.addAttribute("userBalance", userBalance);
        model.addAttribute("defaultCurrency", baseCurrency);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        return "expense/list";
    }

    /**
     * Show trip documents page.
     *
     * @contract
     *   - pre: id != null, principal != null
     *   - post: Returns document list view
     *   - calls: TripService#getTrip, DocumentService#getDocumentsByTrip
     *   - calledBy: Web browser requests
     */
    @GetMapping("/{id}/documents")
    public String showDocuments(@PathVariable UUID id,
                                @AuthenticationPrincipal OAuth2User principal,
                                Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(id, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", id, e.getMessage());
            return "redirect:/dashboard?error=trip_not_found";
        }

        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        // Get documents for the trip
        var documents = documentService.getDocumentsByTrip(id, user.getId());

        model.addAttribute("trip", trip);
        model.addAttribute("tripId", id);
        model.addAttribute("documents", documents);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        return "document/list";
    }

    /**
     * Show document upload form.
     *
     * @contract
     *   - pre: id != null, principal != null
     *   - post: Returns document upload form view
     *   - calls: TripService#getTrip, ActivityService#getActivitiesByTrip
     *   - calledBy: Web browser GET /trips/{id}/documents/new
     */
    @GetMapping("/{id}/documents/new")
    public String showDocumentUploadForm(@PathVariable UUID id,
                                          @RequestParam(required = false) UUID activityId,
                                          @AuthenticationPrincipal OAuth2User principal,
                                          Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(id, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", id, e.getMessage());
            return "redirect:/dashboard?error=trip_not_found";
        }

        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        var activities = activityService.getActivitiesByTrip(id, user.getId());

        model.addAttribute("trip", trip);
        model.addAttribute("tripId", id);
        model.addAttribute("activities", activities);
        model.addAttribute("activityId", activityId);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        return "document/upload";
    }

    /**
     * Show activity duplicate form (pre-filled create form from existing activity).
     *
     * @contract
     *   - pre: id != null, activityId != null, principal != null
     *   - pre: user has OWNER or EDITOR role on the trip
     *   - post: Returns activity create form pre-filled with source activity data
     *   - post: Activity id is null so form creates a new activity
     *   - calls: TripService#getTrip, ActivityService#getActivity
     *   - calledBy: Web browser GET /trips/{id}/activities/{activityId}/duplicate
     */
    @GetMapping("/{id}/activities/{activityId}/duplicate")
    public String duplicateActivity(@PathVariable UUID id,
                                     @PathVariable UUID activityId,
                                     @AuthenticationPrincipal OAuth2User principal,
                                     Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(id, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", id, e.getMessage());
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
            return "redirect:/trips/" + id + "?error=access_denied";
        }

        // Get source activity
        ActivityResponse sourceActivity;
        try {
            sourceActivity = activityService.getActivity(activityId, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get activity {} for duplication: {}", activityId, e.getMessage());
            return "redirect:/trips/" + id + "/activities/" + activityId + "?error=activity_not_found";
        }

        // Create a copy with id set to null so the form creates a new activity
        ActivityResponse duplicated = ActivityResponse.builder()
                .id(null)
                .tripId(id)
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
        model.addAttribute("tripId", id);
        model.addAttribute("dates", dates);
        model.addAttribute("selectedDay", sourceActivity.getDay());
        model.addAttribute("activity", duplicated);
        model.addAttribute("canEdit", true);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        addSearchCoordinatesToModel(id, user.getId(), model);

        return "activity/create";
    }

    /**
     * Show create activity form.
     *
     * @contract
     *   - pre: id != null, principal != null, user has OWNER or EDITOR role
     *   - post: Returns activity create form
     *   - calls: TripService#getTrip
     *   - calledBy: Web browser requests
     */
    @GetMapping("/{id}/activities/new")
    public String showActivityCreateForm(@PathVariable UUID id,
                                          @RequestParam(required = false) Integer day,
                                          @AuthenticationPrincipal OAuth2User principal,
                                          Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(id, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", id, e.getMessage());
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
            return "redirect:/trips/" + id + "?error=access_denied";
        }

        // Generate dates from startDate to endDate
        List<LocalDate> dates = trip.getStartDate() != null && trip.getEndDate() != null
                ? trip.getStartDate().datesUntil(trip.getEndDate().plusDays(1)).collect(Collectors.toList())
                : List.of(LocalDate.now());

        model.addAttribute("trip", trip);
        model.addAttribute("tripId", id);
        model.addAttribute("dates", dates);
        model.addAttribute("selectedDay", day != null ? day : 1);
        model.addAttribute("canEdit", true);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        // Add default coordinates for place search (from first activity or default to Taipei)
        addSearchCoordinatesToModel(id, user.getId(), model);

        return "activity/create";
    }

    /**
     * Handle create activity form submission.
     *
     * @contract
     *   - pre: id != null, principal != null, user has OWNER or EDITOR role
     *   - post: Activity is created, redirects to activities list
     *   - calls: PlaceRepository#save, ActivityService#createActivity
     *   - calledBy: Web browser form submission
     */
    @PostMapping("/{id}/activities")
    public String createActivity(@PathVariable UUID id,
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
            TripResponse trip = tripService.getTrip(id, user.getId());
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
                return "redirect:/trips/" + id + "?error=access_denied";
            }

            // Find or create Place
            Place place;
            if (placeId != null && !placeId.isEmpty()) {
                // Try to find existing place by Google Place ID
                place = placeRepository.findByGooglePlaceId(placeId).orElse(null);
            } else {
                place = null;
            }

            if (place == null) {
                // Create new place with provided data
                place = Place.builder()
                        .name(placeName)
                        .address(address)
                        .latitude(latitude != null ? latitude : 0.0)
                        .longitude(longitude != null ? longitude : 0.0)
                        .googlePlaceId(placeId)
                        .category(mapTypeToCategory(type))
                        .build();
                place = placeRepository.save(place);
                log.info("Created new place: {} with id {} category {}", placeName, place.getId(), place.getCategory());
            } else if (mapTypeToCategory(type) != null) {
                // Update existing place's category if user selected a non-default type
                place.setCategory(mapTypeToCategory(type));
                place = placeRepository.save(place);
            }

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
                return "redirect:/trips/" + id + "/activities/new?error=manual_time_required";
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
            activityService.createActivity(id, request, user.getId());
            log.info("Created activity for trip {} on day {}", id, day);

            return "redirect:/trips/" + id + "/activities";

        } catch (Exception e) {
            log.error("Failed to create activity: {}", e.getMessage(), e);
            model.addAttribute("error", "新增景點失敗：" + e.getMessage());
            return "redirect:/trips/" + id + "/activities/new?error=create_failed";
        }
    }

    /**
     * Show edit activity form.
     *
     * @contract
     *   - pre: id != null, activityId != null, principal != null
     *   - pre: user has OWNER or EDITOR role
     *   - post: Returns activity edit form with existing data
     *   - calls: TripService#getTrip, ActivityService#getActivity
     *   - calledBy: Web browser requests
     */
    @GetMapping("/{id}/activities/{activityId}/edit")
    public String showActivityEditForm(@PathVariable UUID id,
                                        @PathVariable UUID activityId,
                                        @AuthenticationPrincipal OAuth2User principal,
                                        Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(id, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", id, e.getMessage());
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
            return "redirect:/trips/" + id + "?error=access_denied";
        }

        // Get activity details
        ActivityResponse activity;
        try {
            activity = activityService.getActivity(activityId, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get activity {}: {}", activityId, e.getMessage());
            return "redirect:/trips/" + id + "/activities?error=activity_not_found";
        }

        // Generate dates from startDate to endDate
        List<LocalDate> dates = trip.getStartDate() != null && trip.getEndDate() != null
                ? trip.getStartDate().datesUntil(trip.getEndDate().plusDays(1)).collect(Collectors.toList())
                : List.of(LocalDate.now());

        // Calculate selected date from activity's day
        LocalDate selectedDate = trip.getStartDate().plusDays(activity.getDay() - 1);

        model.addAttribute("trip", trip);
        model.addAttribute("tripId", id);
        model.addAttribute("activity", activity);
        model.addAttribute("dates", dates);
        model.addAttribute("selectedDay", activity.getDay());
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("isEdit", true);
        model.addAttribute("canEdit", true);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        // Add default coordinates for place search
        addSearchCoordinatesToModel(id, user.getId(), model);

        return "activity/create";
    }

    /**
     * Handle update activity form submission.
     *
     * @contract
     *   - pre: id != null, activityId != null, principal != null
     *   - pre: user has OWNER or EDITOR role
     *   - post: Activity is updated, transport recalculated if mode/place changed
     *   - post: redirects to activities list with success message
     *   - calls: ActivityService#updateActivity
     *   - calledBy: Web browser form submission
     */
    @PostMapping("/{id}/activities/{activityId}")
    public String updateActivity(@PathVariable UUID id,
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
            TripResponse trip = tripService.getTrip(id, user.getId());
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
                return "redirect:/trips/" + id + "?error=access_denied";
            }

            // Find or create Place (same logic as createActivity)
            Place place;
            if (placeId != null && !placeId.isEmpty()) {
                place = placeRepository.findByGooglePlaceId(placeId).orElse(null);
            } else {
                place = null;
            }

            if (place == null) {
                // Create new place with provided data
                place = Place.builder()
                        .name(placeName)
                        .address(address)
                        .latitude(latitude != null ? latitude : 0.0)
                        .longitude(longitude != null ? longitude : 0.0)
                        .googlePlaceId(placeId)
                        .category(mapTypeToCategory(type))
                        .build();
                place = placeRepository.save(place);
                log.info("Created new place for activity update: {} with id {} category {}", placeName, place.getId(), place.getCategory());
            } else {
                // Update existing place's category
                place.setCategory(mapTypeToCategory(type));
                place = placeRepository.save(place);
            }

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
                return "redirect:/trips/" + id + "/activities/" + activityId + "/edit?error=manual_time_required";
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
            log.info("Updated activity {} for trip {} on day {}", activityId, id, day);

            redirectAttributes.addFlashAttribute("success", "景點已更新，交通時間已重新計算");
            return "redirect:/trips/" + id + "/activities";

        } catch (Exception e) {
            log.error("Failed to update activity: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "更新景點失敗：" + e.getMessage());
            return "redirect:/trips/" + id + "/activities/" + activityId + "/edit?error=update_failed";
        }
    }

    /**
     * Delete an activity from a trip.
     *
     * @contract
     *   - pre: id != null, activityId != null, principal != null
     *   - pre: user has OWNER or EDITOR role
     *   - post: Activity is deleted
     *   - post: Redirects to activities list with success message
     *   - calls: ActivityService#deleteActivity
     *   - calledBy: Web browser form submission (delete confirmation dialog)
     */
    @PostMapping("/{id}/activities/{activityId}/delete")
    public String deleteActivity(@PathVariable UUID id,
                                 @PathVariable UUID activityId,
                                 @AuthenticationPrincipal OAuth2User principal,
                                 RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            activityService.deleteActivity(activityId, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "景點已刪除");
        } catch (Exception e) {
            log.error("Failed to delete activity: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "刪除失敗：" + e.getMessage());
        }

        return "redirect:/trips/" + id + "/activities";
    }

    /**
     * Recalculate all transport times for a trip.
     *
     * Uses Google Maps API with rate limiting. Falls back to Haversine when API fails.
     * Manual inputs (FLIGHT/HIGH_SPEED_RAIL) are preserved.
     *
     * @contract
     *   - pre: id != null, principal != null
     *   - pre: user has OWNER or EDITOR role
     *   - post: All activities have transport times recalculated
     *   - post: Redirects to activities list with result message
     *   - calls: ActivityService#recalculateAllTransport
     *   - calledBy: Web browser POST request
     */
    @PostMapping("/{id}/recalculate-transport")
    public String recalculateTransport(@PathVariable UUID id,
                                        @AuthenticationPrincipal OAuth2User principal,
                                        RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            // Recalculate all transport with default max 50 API calls
            var result = activityService.recalculateAllTransport(id, user.getId(), 50);

            // Build success message
            String message = result.getSuccessMessage();
            redirectAttributes.addFlashAttribute("success", message);

            log.info("Transport recalculation completed for trip {}: total={}, api={}, fallback={}",
                    id, result.getTotalActivities(), result.getApiSuccessCount(), result.getFallbackCount());

            return "redirect:/trips/" + id + "/activities";

        } catch (com.wego.exception.ForbiddenException e) {
            log.warn("User {} not authorized to recalculate transport for trip {}", user.getId(), id);
            redirectAttributes.addFlashAttribute("error", "您沒有權限重新計算此行程的交通時間");
            return "redirect:/trips/" + id + "/activities";
        } catch (Exception e) {
            log.error("Failed to recalculate transport for trip {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "重新計算交通時間失敗：" + e.getMessage());
            return "redirect:/trips/" + id + "/activities";
        }
    }

    /**
     * Show edit trip form.
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable UUID id,
                                @AuthenticationPrincipal OAuth2User principal,
                                Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(id, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", id, e.getMessage());
            return "redirect:/dashboard?error=trip_not_found";
        }

        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        TripResponse.MemberSummary currentMember = trip.getMembers().stream()
                .filter(m -> m.getUserId().equals(user.getId()))
                .findFirst()
                .orElse(null);

        if (currentMember == null ||
            (currentMember.getRole() != Role.OWNER &&
             currentMember.getRole() != Role.EDITOR)) {
            return "redirect:/trips/" + id + "?error=access_denied";
        }

        model.addAttribute("trip", trip);
        model.addAttribute("isEdit", true);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());
        model.addAttribute("minDate", LocalDate.now());

        return "trip/create";
    }

    /**
     * Handle edit trip form submission.
     *
     * @contract
     *   - pre: id != null, user is authenticated, user has OWNER or EDITOR role
     *   - post: Trip is updated, cover image replaced if new one provided
     *   - post: Old cover image deleted if replaced
     *   - calls: TripService#updateTrip, TripService#uploadCoverImage, TripService#deleteCoverImage
     *   - calledBy: Web browser form submission
     */
    @PostMapping("/{id}/edit")
    public String updateTrip(@PathVariable UUID id,
                             @RequestParam String title,
                             @RequestParam(required = false) String description,
                             @RequestParam LocalDate startDate,
                             @RequestParam LocalDate endDate,
                             @RequestParam(required = false) MultipartFile coverImage,
                             @AuthenticationPrincipal OAuth2User principal,
                             Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        // Get existing trip to verify permission
        TripResponse existingTrip;
        try {
            existingTrip = tripService.getTrip(id, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {} for update: {}", id, e.getMessage());
            return "redirect:/dashboard?error=trip_not_found";
        }

        if (existingTrip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        // Check permission
        TripResponse.MemberSummary currentMember = existingTrip.getMembers().stream()
                .filter(m -> m.getUserId().equals(user.getId()))
                .findFirst()
                .orElse(null);

        if (currentMember == null ||
            (currentMember.getRole() != Role.OWNER &&
             currentMember.getRole() != Role.EDITOR)) {
            return "redirect:/trips/" + id + "?error=access_denied";
        }

        // Validate dates
        if (endDate.isBefore(startDate)) {
            model.addAttribute("dateError", "結束日期不可早於開始日期");
            model.addAttribute("name", user.getNickname());
            model.addAttribute("picture", user.getAvatarUrl());
            model.addAttribute("minDate", LocalDate.now());
            model.addAttribute("isEdit", true);
            model.addAttribute("trip", existingTrip);
            return "trip/create";
        }

        try {
            // Handle cover image update
            String newCoverImageUrl = existingTrip.getCoverImageUrl(); // Keep existing by default

            if (coverImage != null && !coverImage.isEmpty()) {
                try {
                    // Upload new cover image
                    newCoverImageUrl = tripService.uploadCoverImage(id, user.getId(), coverImage);
                    log.info("Cover image uploaded successfully for trip {}: {}", id, newCoverImageUrl);

                    // Delete old cover image if it exists
                    if (existingTrip.getCoverImageUrl() != null) {
                        tripService.deleteCoverImage(existingTrip.getCoverImageUrl());
                    }
                } catch (com.wego.service.external.StorageException e) {
                    log.error("Cover image storage failed for trip {}: {}", id, e.getMessage());
                    model.addAttribute("coverImageError", "封面圖片上傳失敗：" + e.getMessage());
                    model.addAttribute("name", user.getNickname());
                    model.addAttribute("picture", user.getAvatarUrl());
                    model.addAttribute("minDate", LocalDate.now());
                    model.addAttribute("isEdit", true);
                    model.addAttribute("trip", existingTrip);
                    return "trip/create";
                }
            }

            UpdateTripRequest request = UpdateTripRequest.builder()
                    .title(title)
                    .description(description)
                    .startDate(startDate)
                    .endDate(endDate)
                    .coverImageUrl(newCoverImageUrl)
                    .build();

            tripService.updateTrip(id, request, user.getId());
            log.info("Updated trip: {} by user: {}", id, user.getId());

            return "redirect:/trips/" + id;
        } catch (ValidationException e) {
            log.warn("Cover image validation failed: {}", e.getMessage());
            model.addAttribute("coverImageError", e.getMessage());
            model.addAttribute("name", user.getNickname());
            model.addAttribute("picture", user.getAvatarUrl());
            model.addAttribute("minDate", LocalDate.now());
            model.addAttribute("isEdit", true);
            model.addAttribute("trip", existingTrip);
            return "trip/create";
        } catch (Exception e) {
            log.error("Failed to update trip: {}", e.getMessage());
            model.addAttribute("error", "更新行程失敗，請稍後再試");
            model.addAttribute("name", user.getNickname());
            model.addAttribute("picture", user.getAvatarUrl());
            model.addAttribute("minDate", LocalDate.now());
            model.addAttribute("isEdit", true);
            model.addAttribute("trip", existingTrip);
            return "trip/create";
        }
    }

    /**
     * Maps the form activity type value to the Place category value
     * used by the activity list template for icon rendering.
     *
     * @param type The form value (ATTRACTION, RESTAURANT, TRANSPORT, ACCOMMODATION)
     * @return The Place category value, or null for ATTRACTION (uses default icon)
     */
    private String mapTypeToCategory(String type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "RESTAURANT" -> "restaurant";
            case "TRANSPORT" -> "transit_station";
            case "ACCOMMODATION" -> "lodging";
            default -> null;
        };
    }

    private User getCurrentUser(OAuth2User principal) {
        if (principal == null) {
            return null;
        }
        String email = principal.getAttribute("email");
        return userService.getUserByEmail(email);
    }
}
