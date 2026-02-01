package com.wego.controller.web;

import com.wego.dto.response.GlobalExpenseOverviewResponse;
import com.wego.dto.response.TripExpenseSummaryResponse;
import com.wego.entity.User;
import com.wego.service.GlobalExpenseService;
import com.wego.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Controller for global expense overview page.
 *
 * @contract
 *   - Handles /expenses route for global expense view
 *   - Requires authentication
 *   - Shows aggregated expense data across all trips
 *   - calledBy: Web browser requests, bottom navigation
 *   - calls: GlobalExpenseService, UserService
 */
@Controller
@RequestMapping("/expenses")
@RequiredArgsConstructor
@Slf4j
public class GlobalExpenseController {

    private final GlobalExpenseService globalExpenseService;
    private final UserService userService;

    /**
     * Shows the global expense overview page.
     *
     * @contract
     *   - pre: user is authenticated
     *   - post: Returns expense overview with aggregated data across all user's trips
     *   - calls: GlobalExpenseService#getOverview, GlobalExpenseService#getUnsettledTrips
     *   - calledBy: Web browser request, bottom navigation
     *
     * @param principal The authenticated OAuth2 user
     * @param model The model for view rendering
     * @return Template name or redirect
     */
    @GetMapping
    public String showExpenseOverview(@AuthenticationPrincipal OAuth2User principal,
                                       Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        GlobalExpenseOverviewResponse overview = globalExpenseService.getOverview(user.getId());
        List<TripExpenseSummaryResponse> unsettledTrips =
                globalExpenseService.getUnsettledTrips(user.getId());

        model.addAttribute("overview", overview);
        model.addAttribute("unsettledTrips", unsettledTrips);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        log.debug("Showing expense overview for user {}: {} trips, net balance {}",
                user.getId(), overview.getTripCount(), overview.getNetBalance());

        return "expense/global-overview";
    }

    /**
     * Gets the current user from OAuth2 principal.
     *
     * @param principal The OAuth2 principal
     * @return User entity or null if not found
     */
    private User getCurrentUser(OAuth2User principal) {
        if (principal == null) {
            return null;
        }
        String email = principal.getAttribute("email");
        if (email == null) {
            return null;
        }
        try {
            return userService.getUserByEmail(email);
        } catch (Exception e) {
            log.warn("Failed to get user by email: {}", email, e);
            return null;
        }
    }
}
