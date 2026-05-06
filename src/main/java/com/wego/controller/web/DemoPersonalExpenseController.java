package com.wego.controller.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.dto.response.PersonalExpenseItemResponse;
import com.wego.dto.response.PersonalExpenseSummaryResponse;
import com.wego.dto.response.TripResponse;
import com.wego.service.DemoDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Web controller for demo personal expenses page (unauthenticated).
 *
 * <p>Mirrors the personal-tab portion of {@link ExpenseWebController}'s list endpoint
 * but as a standalone page route for demo. All write actions are intercepted by
 * {@code data-demo-cta} on the client side.
 *
 * @contract
 *   - pre: No authentication required
 *   - post: Renders demo personal expense view with synthetic data + chart JSON
 *   - calls: DemoDataProvider, ObjectMapper
 */
@Controller
@RequestMapping("/demo/trip/personal-expenses")
@RequiredArgsConstructor
public class DemoPersonalExpenseController {

    private final DemoDataProvider demoDataProvider;
    private final ObjectMapper objectMapper;

    @GetMapping
    public String showDemoPersonalExpenses(Model model) throws JsonProcessingException {
        TripResponse trip = demoDataProvider.getDemoTrip();
        List<PersonalExpenseItemResponse> personalExpenses = demoDataProvider.getDemoPersonalExpenses();
        PersonalExpenseSummaryResponse personalSummary = demoDataProvider.getDemoPersonalSummary();

        model.addAttribute("trip", trip);
        model.addAttribute("tripId", trip.getId());
        model.addAttribute("defaultCurrency", trip.getBaseCurrency() != null ? trip.getBaseCurrency() : "JPY");

        // Personal data
        model.addAttribute("personalExpenses", personalExpenses);
        model.addAttribute("personalSummary", personalSummary);
        model.addAttribute("hasBudget", personalSummary != null && personalSummary.getBudget() != null);

        // Chart JSON (consumed by Chart.js in template)
        if (personalSummary != null) {
            model.addAttribute("personalCategoryBreakdownJson",
                    objectMapper.writeValueAsString(personalSummary.getCategoryBreakdown()));
            model.addAttribute("personalDailyAmountsJson",
                    objectMapper.writeValueAsString(personalSummary.getDailyAmounts()));
        } else {
            model.addAttribute("personalCategoryBreakdownJson", "{}");
            model.addAttribute("personalDailyAmountsJson", "{}");
        }
        model.addAttribute("budgetPercentageCapped", 75);

        // Demo-specific
        model.addAttribute("canEdit", true);
        model.addAttribute("isOwner", true);
        model.addAttribute("isDemo", true);
        model.addAttribute("name", "Demo User");
        model.addAttribute("picture", null);
        model.addAttribute("currentPath", "/demo/trip/personal-expenses");

        return "demo/expense/personal-tab";
    }
}
