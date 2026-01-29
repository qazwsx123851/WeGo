package com.wego.controller.web;

import com.wego.dto.response.TripResponse;
import com.wego.entity.Role;
import com.wego.entity.User;
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

import com.wego.dto.request.CreateTripRequest;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
 *   - calls: TripService, UserService
 */
@Controller
@RequestMapping("/trips")
@RequiredArgsConstructor
@Slf4j
public class TripController {

    private final TripService tripService;
    private final UserService userService;

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
        // Add empty trip object for form binding
        model.addAttribute("trip", CreateTripRequest.builder().build());

        return "trip/create";
    }

    /**
     * Handle create trip form submission.
     *
     * @contract
     *   - pre: user is authenticated, form data is valid
     *   - post: Trip is created, redirects to trip detail page
     *   - calls: TripService#createTrip
     */
    @PostMapping("/create")
    public String createTrip(@RequestParam String title,
                             @RequestParam(required = false) String description,
                             @RequestParam LocalDate startDate,
                             @RequestParam LocalDate endDate,
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
            model.addAttribute("trip", CreateTripRequest.builder()
                    .title(title)
                    .description(description)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build());
            return "trip/create";
        }

        try {
            CreateTripRequest request = CreateTripRequest.builder()
                    .title(title)
                    .description(description)
                    .startDate(startDate)
                    .endDate(endDate)
                    .baseCurrency("TWD")
                    .build();

            TripResponse createdTrip = tripService.createTrip(request, user);
            log.info("Created trip: {} by user: {}", createdTrip.getId(), user.getId());

            return "redirect:/trips/" + createdTrip.getId();
        } catch (Exception e) {
            log.error("Failed to create trip: {}", e.getMessage());
            model.addAttribute("error", "建立行程失敗：" + e.getMessage());
            model.addAttribute("name", user.getNickname());
            model.addAttribute("picture", user.getAvatarUrl());
            return "trip/create";
        }
    }

    /**
     * Show trip detail page.
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

        // Calculate days until trip
        if (trip.getStartDate() != null && trip.getStartDate().isAfter(LocalDate.now())) {
            long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), trip.getStartDate());
            model.addAttribute("daysUntil", daysUntil);
        }

        return "trip/view";
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

        model.addAttribute("trip", trip);
        model.addAttribute("members", members);
        model.addAttribute("currentMember", currentMember);
        model.addAttribute("currentUserId", user.getId());
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("canEdit", isOwner ||
                (currentMember != null && currentMember.getRole() == Role.EDITOR));
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        return "trip/members";
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

        return "trip/create";
    }

    private User getCurrentUser(OAuth2User principal) {
        if (principal == null) {
            return null;
        }
        String email = principal.getAttribute("email");
        return userService.getUserByEmail(email);
    }
}
