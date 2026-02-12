package com.wego.controller.web;

import com.wego.dto.response.GlobalExpenseOverviewResponse;
import com.wego.dto.response.TripExpenseSummaryResponse;
import com.wego.entity.User;
import com.wego.service.GlobalExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
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
public class GlobalExpenseController extends BaseWebController {

    private final GlobalExpenseService globalExpenseService;

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
    public String showExpenseOverview(@CurrentUser UserPrincipal principal,
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

}
