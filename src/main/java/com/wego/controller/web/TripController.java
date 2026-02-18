package com.wego.controller.web;

import com.wego.dto.response.TripResponse;
import com.wego.entity.User;
import com.wego.service.ActivityService;
import com.wego.service.TripViewHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.wego.dto.request.CreateTripRequest;
import com.wego.dto.request.UpdateTripRequest;
import com.wego.exception.ValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
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

    private final ActivityService activityService;
    private final TripViewHelper tripViewHelper;

    /**
     * List all trips for the current user.
     */
    @GetMapping
    public String listTrips(@CurrentUser UserPrincipal principal, Model model) {
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
    public String showCreateForm(@CurrentUser UserPrincipal principal, Model model) {
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
                             @CurrentUser UserPrincipal principal,
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
                                  @CurrentUser UserPrincipal principal,
                                  Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip = loadTrip(id, user.getId());
        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        // Permissions
        TripResponse.MemberSummary currentMember = findCurrentMember(trip, user.getId());
        model.addAttribute("trip", trip);
        model.addAttribute("currentMember", currentMember);
        model.addAttribute("canEdit", canEdit(currentMember));
        model.addAttribute("isOwner", isOwner(currentMember));
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        // Trip duration
        TripViewHelper.TripDuration duration = tripViewHelper.calculateTripDuration(trip);
        model.addAttribute("tripDays", duration.days());
        model.addAttribute("tripNights", duration.nights());
        if (duration.daysUntil() != null) {
            model.addAttribute("daysUntil", duration.daysUntil());
        }

        // Members
        List<TripResponse.MemberSummary> members = trip.getMembers();
        model.addAttribute("members", members);
        model.addAttribute("memberCount", members != null ? members.size() : 0);

        // Activities
        List<com.wego.dto.response.ActivityResponse> activities = List.of();
        try {
            activities = activityService.getActivitiesByTrip(id, user.getId());
        } catch (Exception e) {
            log.warn("Failed to load activities for trip {}: {}", id, e.getMessage());
        }
        model.addAttribute("activityCount", activities.size());
        model.addAttribute("upcomingActivities", activities.stream().limit(3).collect(Collectors.toList()));

        // Expense & document summary
        String baseCurrency = trip.getBaseCurrency() != null ? trip.getBaseCurrency() : "TWD";
        int memberCount = members != null ? members.size() : 0;
        TripViewHelper.ExpenseSummary expense = tripViewHelper.getExpenseSummary(id, user.getId(), baseCurrency, memberCount);
        model.addAttribute("expenseCount", expense.expenseCount());
        model.addAttribute("documentCount", expense.documentCount());
        model.addAttribute("totalExpense", expense.totalExpense());
        model.addAttribute("averageExpense", expense.averageExpense());

        // Weather fallback coordinates
        TripViewHelper.WeatherCoordinates weather = tripViewHelper.getWeatherFallbackCoordinates(activities, trip);
        model.addAttribute("weatherFallbackLat", weather.latitude());
        model.addAttribute("weatherFallbackLng", weather.longitude());

        // Todo preview
        TripViewHelper.TodoPreview todo = tripViewHelper.getTodoPreview(id, user.getId());
        model.addAttribute("todoCount", todo.totalCount());
        model.addAttribute("todoCompletedCount", todo.completedCount());
        model.addAttribute("upcomingTodos", todo.upcomingTodos());

        return "trip/view";
    }

    /**
     * Show edit trip form.
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable UUID id,
                                @CurrentUser UserPrincipal principal,
                                Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip = loadTrip(id, user.getId());
        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        TripResponse.MemberSummary currentMember = findCurrentMember(trip, user.getId());
        if (!canEdit(currentMember)) {
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
                             @CurrentUser UserPrincipal principal,
                             Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        // Get existing trip to verify permission
        TripResponse existingTrip = loadTrip(id, user.getId());
        if (existingTrip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        // Check permission
        TripResponse.MemberSummary currentMember = findCurrentMember(existingTrip, user.getId());

        if (!canEdit(currentMember)) {
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
