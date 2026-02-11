package com.wego.controller.web;

import com.wego.dto.response.TripResponse;
import com.wego.entity.Role;
import com.wego.entity.User;
import com.wego.dto.response.TodoResponse;
import com.wego.entity.TodoStatus;
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
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
public class TripController extends BaseWebController {

    private final TripService tripService;
    private final ActivityService activityService;
    private final TodoService todoService;
    private final ExpenseService expenseService;
    private final DocumentService documentService;
    private final InviteLinkService inviteLinkService;

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
                                e -> e.getExpenseDate() != null
                                        ? e.getExpenseDate()
                                        : (e.getCreatedAt() != null
                                                ? e.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate()
                                                : LocalDate.now()),
                                () -> new TreeMap<>(Comparator.reverseOrder()),
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

}
