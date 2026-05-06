package com.wego.controller.web;

import com.wego.dto.response.ExpenseResponse;
import com.wego.dto.response.SettlementResponse;
import com.wego.dto.response.TripResponse;
import com.wego.exception.ResourceNotFoundException;
import com.wego.service.DemoDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Web controller for demo team expenses (unauthenticated).
 *
 * <p>Mirrors the read-only portion of {@link ExpenseWebController}'s
 * list and detail endpoints for demo visitors.
 *
 * @contract
 *   - pre: No authentication required
 *   - post: Renders demo expense list/detail with synthetic data
 *   - calls: DemoDataProvider
 */
@Controller
@RequestMapping("/demo/trip/expenses")
@RequiredArgsConstructor
public class DemoExpenseController {

    private final DemoDataProvider demoDataProvider;

    @GetMapping
    public String listDemoExpenses(Model model) {
        TripResponse trip = demoDataProvider.getDemoTrip();
        List<ExpenseResponse> expenses = demoDataProvider.getDemoExpenses();
        SettlementResponse settlement = demoDataProvider.getDemoSettlement();

        BigDecimal totalExpense = settlement.getTotalExpenses();
        int memberCount = trip.getMembers() != null && !trip.getMembers().isEmpty()
                ? trip.getMembers().size() : 1;
        BigDecimal perPersonAverage = totalExpense.divide(
                BigDecimal.valueOf(memberCount), 0, RoundingMode.HALF_UP);

        model.addAttribute("trip", trip);
        model.addAttribute("tripId", trip.getId());
        model.addAttribute("expenses", expenses);
        model.addAttribute("expensesByDate", demoDataProvider.getDemoExpensesByDate());
        model.addAttribute("totalExpense", totalExpense);
        model.addAttribute("perPersonAverage", perPersonAverage);
        // Demo: show a positive balance to suggest the user is "owed" money — illustrative
        model.addAttribute("userBalance", new BigDecimal("3200"));
        model.addAttribute("defaultCurrency", trip.getBaseCurrency() != null ? trip.getBaseCurrency() : "JPY");
        model.addAttribute("conversionWarnings", List.of());

        model.addAttribute("canEdit", true);
        model.addAttribute("isOwner", true);
        model.addAttribute("isDemo", true);
        model.addAttribute("name", "Demo User");
        model.addAttribute("picture", null);
        model.addAttribute("currentPath", "/demo/trip/expenses");

        return "demo/expense/list";
    }

    @GetMapping("/{expenseId}")
    public String showDemoExpenseDetail(@PathVariable UUID expenseId, Model model) {
        TripResponse trip = demoDataProvider.getDemoTrip();
        ExpenseResponse expense = demoDataProvider.getExpense(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId.toString()));

        model.addAttribute("trip", trip);
        model.addAttribute("tripId", trip.getId());
        model.addAttribute("expense", expense);
        model.addAttribute("defaultCurrency", trip.getBaseCurrency() != null ? trip.getBaseCurrency() : "JPY");
        model.addAttribute("canEdit", true);
        model.addAttribute("isOwner", true);
        model.addAttribute("isDemo", true);
        model.addAttribute("name", "Demo User");
        model.addAttribute("picture", null);
        model.addAttribute("currentPath", "/demo/trip/expenses");

        return "demo/expense/detail";
    }
}
