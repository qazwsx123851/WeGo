package com.wego.controller.web;

import com.wego.service.DemoDataProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Web controller for demo trip preview (unauthenticated).
 *
 * @contract
 *   - pre: No authentication required
 *   - post: Renders demo trip view with synthetic data
 *   - calls: DemoDataProvider
 */
@Controller
@RequestMapping("/demo")
@RequiredArgsConstructor
public class DemoWebController {

    private final DemoDataProvider demoDataProvider;
    private final ObjectMapper objectMapper;

    /**
     * Renders the demo trip view with all data from DemoDataProvider.
     *
     * @contract
     *   - pre: None (public endpoint)
     *   - post: Model populated with trip, activities, expenses, todos, and personal summary
     *   - calls: DemoDataProvider methods
     */
    @GetMapping("/trip")
    public String showDemoTrip(Model model) throws JsonProcessingException {
        var trip = demoDataProvider.getDemoTrip();
        var allActivities = demoDataProvider.getAllDemoActivities();
        var expenses = demoDataProvider.getDemoExpenses();
        var settlement = demoDataProvider.getDemoSettlement();
        var personalSummary = demoDataProvider.getDemoPersonalSummary();
        var todos = demoDataProvider.getDemoTodos();

        // Trip overview
        model.addAttribute("trip", trip);
        model.addAttribute("tripId", trip.getId());
        model.addAttribute("tripDays", 4);
        model.addAttribute("tripNights", 3);
        model.addAttribute("members", trip.getMembers());
        model.addAttribute("memberCount", trip.getMemberCount());
        model.addAttribute("canEdit", true);  // Keep buttons visible for CTA interception
        model.addAttribute("isOwner", true);
        model.addAttribute("isDemo", true);

        // Activities
        model.addAttribute("dates", demoDataProvider.getDemoDates());
        model.addAttribute("activitiesByDate", demoDataProvider.getDemoActivitiesByDate());
        model.addAttribute("allActivities", allActivities);
        model.addAttribute("totalActivityCount", allActivities.size());
        model.addAttribute("activityCount", allActivities.size());
        model.addAttribute("upcomingActivities", allActivities.stream().limit(3).toList());
        model.addAttribute("currentDay", 1);

        // Expenses - team
        model.addAttribute("expenses", expenses);
        model.addAttribute("expensesByDate", demoDataProvider.getDemoExpensesByDate());
        model.addAttribute("totalExpense", settlement.getTotalExpenses());
        BigDecimal perPerson = settlement.getTotalExpenses()
                .divide(BigDecimal.valueOf(3), 0, RoundingMode.HALF_UP);
        model.addAttribute("perPersonAverage", perPerson);
        model.addAttribute("averageExpense", perPerson);
        model.addAttribute("userBalance", new BigDecimal("3200"));
        model.addAttribute("defaultCurrency", "JPY");
        model.addAttribute("conversionWarnings", java.util.List.of());

        // Expenses - personal
        model.addAttribute("personalExpenses", demoDataProvider.getDemoPersonalExpenses());
        model.addAttribute("personalSummary", personalSummary);
        model.addAttribute("hasBudget", true);
        model.addAttribute("personalCategoryBreakdownJson",
                objectMapper.writeValueAsString(personalSummary.getCategoryBreakdown()));
        model.addAttribute("personalDailyAmountsJson",
                objectMapper.writeValueAsString(personalSummary.getDailyAmounts()));
        model.addAttribute("budgetPercentageCapped", 75);

        // Todos
        model.addAttribute("todoCount", (long) todos.size());
        long completedCount = todos.stream()
                .filter(t -> "COMPLETED".equals(t.getStatus().name()))
                .count();
        model.addAttribute("todoCompletedCount", completedCount);
        model.addAttribute("upcomingTodos", todos.stream()
                .filter(t -> !"COMPLETED".equals(t.getStatus().name()))
                .limit(5)
                .toList());

        // Weather fallback coords (Tokyo)
        model.addAttribute("weatherFallbackLat", 35.6762);
        model.addAttribute("weatherFallbackLng", 139.6503);

        // Counts for trip overview
        model.addAttribute("expenseCount", (long) expenses.size());
        model.addAttribute("documentCount", 0L);

        // User info placeholders (not authenticated but template may reference)
        model.addAttribute("name", "Demo User");
        model.addAttribute("picture", null);
        model.addAttribute("currentPath", "/demo/trip");

        return "demo/trip-view";
    }
}
