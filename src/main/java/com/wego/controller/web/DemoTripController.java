package com.wego.controller.web;

import com.wego.dto.response.ActivityResponse;
import com.wego.dto.response.ExpenseResponse;
import com.wego.dto.response.TodoResponse;
import com.wego.dto.response.TripResponse;
import com.wego.service.DemoDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Web controller for demo trip overview (unauthenticated).
 *
 * <p>Mirrors {@link TripController#showTripDetail} structure to render the same
 * trip overview layout for unauthenticated demo visitors. All write actions
 * are intercepted by {@code data-demo-cta} on the client side.
 *
 * @contract
 *   - pre: No authentication required
 *   - post: Renders demo trip overview view with synthetic data
 *   - calls: DemoDataProvider
 */
@Controller
@RequestMapping("/demo/trip")
@RequiredArgsConstructor
public class DemoTripController {

    private final DemoDataProvider demoDataProvider;

    /**
     * Renders the demo trip overview page.
     *
     * @contract
     *   - pre: None (public endpoint, /demo/** is permitAll)
     *   - post: Model populated with attributes matching real trip/view.html
     *   - calls: DemoDataProvider#getDemoTrip, getAllDemoActivities,
     *            getDemoExpenses, getDemoSettlement, getDemoTodos
     */
    @GetMapping
    public String showDemoTrip(Model model) {
        TripResponse trip = demoDataProvider.getDemoTrip();
        List<ActivityResponse> activities = demoDataProvider.getAllDemoActivities();
        List<ExpenseResponse> expenses = demoDataProvider.getDemoExpenses();
        List<TodoResponse> todos = demoDataProvider.getDemoTodos();

        // Trip + permissions (demo: always treat viewer as owner so all action
        // buttons render, then data-demo-cta intercepts writes on click)
        model.addAttribute("trip", trip);
        model.addAttribute("canEdit", true);
        model.addAttribute("isOwner", true);
        model.addAttribute("isDemo", true);
        model.addAttribute("name", "Demo User");
        model.addAttribute("picture", null);
        model.addAttribute("currentPath", "/demo/trip");

        // Trip duration (Tokyo demo: 4 days 3 nights, hardcoded for stability)
        model.addAttribute("tripDays", 4);
        model.addAttribute("tripNights", 3);

        // Members
        List<TripResponse.MemberSummary> members = trip.getMembers();
        model.addAttribute("members", members);
        model.addAttribute("memberCount", members != null ? members.size() : 0);

        // Activities — only first 3 for "upcoming" preview, like real view
        model.addAttribute("activityCount", activities.size());
        model.addAttribute("upcomingActivities", activities.stream().limit(3).toList());

        // Expense summary — derived from total / per-person
        BigDecimal totalExpense = demoDataProvider.getDemoSettlement().getTotalExpenses();
        BigDecimal averageExpense = totalExpense.divide(
                BigDecimal.valueOf(members != null && !members.isEmpty() ? members.size() : 1),
                0, RoundingMode.HALF_UP);
        model.addAttribute("expenseCount", (long) expenses.size());
        model.addAttribute("documentCount", 0L);
        model.addAttribute("totalExpense", totalExpense);
        model.addAttribute("averageExpense", averageExpense);

        // Weather fallback coords — Taipei 101 (demo always shows Taipei weather,
        // independent of the Tokyo trip itinerary)
        model.addAttribute("weatherFallbackLat", 25.0330);
        model.addAttribute("weatherFallbackLng", 121.5654);

        // Todos — preview (5 most relevant non-completed)
        long todoCount = todos.size();
        long todoCompletedCount = todos.stream()
                .filter(t -> "COMPLETED".equals(t.getStatus().name()))
                .count();
        model.addAttribute("todoCount", todoCount);
        model.addAttribute("todoCompletedCount", todoCompletedCount);
        model.addAttribute("upcomingTodos", todos.stream()
                .filter(t -> !"COMPLETED".equals(t.getStatus().name()))
                .limit(5)
                .toList());

        return "demo/trip/view";
    }
}
