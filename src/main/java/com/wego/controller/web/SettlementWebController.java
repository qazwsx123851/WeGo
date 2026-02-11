package com.wego.controller.web;

import com.wego.domain.permission.PermissionChecker;
import com.wego.dto.response.SettlementResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.service.SettlementService;
import com.wego.service.TripService;
import com.wego.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Web controller for settlement (debt simplification) page.
 *
 * @contract
 *   - Displays settlement page with debt simplification results
 *   - Shows total expenses, per-person average, and simplified transfers
 *   - Requires authentication and view permission on trip
 *   - calls: SettlementService, TripService, UserService
 *   - calledBy: Web browser requests from expense list page
 */
@Controller
@RequestMapping("/trips/{tripId}/settlement")
@RequiredArgsConstructor
@Slf4j
public class SettlementWebController extends BaseWebController {

    private final TripService tripService;
    private final SettlementService settlementService;
    private final PermissionChecker permissionChecker;

    /**
     * Shows the settlement page with debt simplification.
     *
     * @contract
     *   - pre: tripId != null, principal != null
     *   - pre: user has view permission on trip
     *   - post: Returns settlement view with trip info and settlement data
     *   - calls: TripService#getTrip, SettlementService#calculateSettlement
     *   - calledBy: Web browser GET /trips/{tripId}/settlement
     *
     * @param tripId The trip ID
     * @param principal The authenticated user
     * @param model The Spring MVC model
     * @return The settlement view name
     */
    @GetMapping
    public String showSettlement(@PathVariable UUID tripId,
                                 @AuthenticationPrincipal OAuth2User principal,
                                 Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(tripId, user.getId());
        } catch (ForbiddenException e) {
            log.warn("User {} has no permission to view trip {}", user.getId(), tripId);
            return "redirect:/dashboard?error=access_denied";
        } catch (ResourceNotFoundException e) {
            log.warn("Trip {} not found", tripId);
            return "redirect:/dashboard?error=trip_not_found";
        } catch (Exception e) {
            log.error("Failed to get trip {}: {}", tripId, e.getMessage(), e);
            return "redirect:/dashboard?error=trip_not_found";
        }

        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        SettlementResponse settlement;
        try {
            settlement = settlementService.calculateSettlement(tripId, user.getId());
        } catch (ForbiddenException e) {
            log.warn("User {} has no permission to view settlement for trip {}", user.getId(), tripId);
            return "redirect:/trips/" + tripId + "/expenses?error=access_denied";
        } catch (Exception e) {
            log.error("Failed to calculate settlement for trip {}: {}", tripId, e.getMessage(), e);
            return "redirect:/trips/" + tripId + "/expenses?error=settlement_failed";
        }

        // Calculate per-person average
        BigDecimal perPersonAverage = BigDecimal.ZERO;
        if (trip.getMembers() != null && !trip.getMembers().isEmpty()
                && settlement.getTotalExpenses() != null
                && settlement.getTotalExpenses().compareTo(BigDecimal.ZERO) > 0) {
            perPersonAverage = settlement.getTotalExpenses()
                    .divide(BigDecimal.valueOf(trip.getMembers().size()), 2, RoundingMode.HALF_UP);
        }

        boolean canEdit = permissionChecker.canEdit(tripId, user.getId());

        model.addAttribute("trip", trip);
        model.addAttribute("settlement", settlement);
        model.addAttribute("perPersonAverage", perPersonAverage);
        model.addAttribute("currentUserId", user.getId());
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        return "expense/settlement";
    }

}
