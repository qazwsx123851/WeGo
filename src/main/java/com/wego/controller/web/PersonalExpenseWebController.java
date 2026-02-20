package com.wego.controller.web;

import com.wego.constant.ExpenseCategories;
import com.wego.dto.request.CreatePersonalExpenseRequest;
import com.wego.dto.request.UpdatePersonalExpenseRequest;
import com.wego.exception.ResourceNotFoundException;
import com.wego.exception.ValidationException;
import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
import com.wego.service.PersonalExpenseService;
import com.wego.service.PersonalExpenseService.TripDateRange;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Web controller for personal expense form CRUD operations.
 *
 * @contract
 *   - Handles create/edit form display and form submissions for MANUAL personal expenses
 *   - Uses @ModelAttribute for form binding (not @RequestBody)
 *   - Requires authentication for all endpoints
 *   - Permission checks (isMember) are delegated to PersonalExpenseService
 *   - calls: PersonalExpenseService
 *   - calledBy: Web browser requests, HTML form submissions
 */
@Slf4j
@Controller
@RequestMapping("/trips/{tripId}/personal-expenses")
@RequiredArgsConstructor
public class PersonalExpenseWebController {

    private final PersonalExpenseService personalExpenseService;

    /**
     * GET /trips/{tripId}/personal-expenses/create — show create form.
     *
     * @contract
     *   - pre: tripId != null, principal != null
     *   - post: Returns personal expense create form with trip date range for min/max
     *   - calls: PersonalExpenseService#getBaseCurrency, PersonalExpenseService#getTripDateRange
     *   - calledBy: Web browser GET /trips/{tripId}/personal-expenses/create
     */
    @GetMapping("/create")
    public String showCreateForm(
            @PathVariable UUID tripId,
            Model model,
            @CurrentUser UserPrincipal principal) {
        UUID userId = principal.getUser().getId();
        String baseCurrency = personalExpenseService.getBaseCurrency(tripId, userId);
        TripDateRange dateRange = personalExpenseService.getTripDateRange(tripId, userId);
        model.addAttribute("tripId", tripId);
        model.addAttribute("baseCurrency", baseCurrency);
        model.addAttribute("tripStartDate", dateRange.startDate());
        model.addAttribute("tripEndDate", dateRange.endDate());
        model.addAttribute("categories", ExpenseCategories.ALL);
        model.addAttribute("request", new CreatePersonalExpenseRequest());
        return "expense/personal-create";
    }

    /**
     * POST /trips/{tripId}/personal-expenses — create and redirect.
     *
     * @contract
     *   - pre: tripId != null, principal != null
     *   - pre: request.description not blank, request.amount > 0
     *   - post: PersonalExpense persisted and redirect to expenses tab=personal
     *   - post: Returns form with validation errors if bindingResult.hasErrors()
     *   - post: Returns form with dateError if date outside trip range
     *   - calls: PersonalExpenseService#createPersonalExpense
     *   - calledBy: Web browser POST /trips/{tripId}/personal-expenses
     */
    @PostMapping
    public String createPersonalExpense(
            @PathVariable UUID tripId,
            @Valid @ModelAttribute("request") CreatePersonalExpenseRequest request,
            BindingResult bindingResult,
            Model model,
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getUser().getId();

        if (bindingResult.hasErrors()) {
            populateCreateFormModel(model, tripId, userId);
            return "expense/personal-create";
        }

        try {
            personalExpenseService.createPersonalExpense(userId, tripId, request);
        } catch (ValidationException ex) {
            populateCreateFormModel(model, tripId, userId);
            model.addAttribute("dateError", ex.getMessage());
            return "expense/personal-create";
        }

        log.info("Created personal expense for trip {} by user {}", tripId, userId);
        return "redirect:/trips/" + tripId + "/expenses?tab=personal";
    }

    /**
     * GET /trips/{tripId}/personal-expenses/{id}/edit — show edit form.
     *
     * @contract
     *   - pre: tripId != null, id != null, principal != null
     *   - pre: the expense must belong to the current user
     *   - post: Returns personal expense edit form pre-filled with expense data and trip date range
     *   - calls: PersonalExpenseService#getPersonalExpenses, PersonalExpenseService#getTripDateRange
     *   - calledBy: Web browser GET /trips/{tripId}/personal-expenses/{id}/edit
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable UUID tripId,
            @PathVariable UUID id,
            Model model,
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getUser().getId();

        String baseCurrency = personalExpenseService.getBaseCurrency(tripId, userId);
        TripDateRange dateRange = personalExpenseService.getTripDateRange(tripId, userId);

        var expense = personalExpenseService.getPersonalExpenses(userId, tripId)
                .stream()
                .filter(e -> id.equals(e.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("PersonalExpense", id.toString()));

        UpdatePersonalExpenseRequest updateRequest = UpdatePersonalExpenseRequest.builder()
                .description(expense.getDescription())
                .amount(expense.getOriginalAmount())
                .currency(expense.getOriginalCurrency())
                .exchangeRate(expense.getExchangeRate())
                .category(expense.getCategory())
                .expenseDate(expense.getExpenseDate())
                .build();

        model.addAttribute("tripId", tripId);
        model.addAttribute("baseCurrency", baseCurrency);
        model.addAttribute("tripStartDate", dateRange.startDate());
        model.addAttribute("tripEndDate", dateRange.endDate());
        model.addAttribute("expenseId", id);
        model.addAttribute("categories", ExpenseCategories.ALL);
        model.addAttribute("request", updateRequest);
        return "expense/personal-edit";
    }

    /**
     * POST /trips/{tripId}/personal-expenses/{id} — update and redirect.
     *
     * @contract
     *   - pre: tripId != null, id != null, principal != null
     *   - pre: the expense must belong to the current user
     *   - post: PersonalExpense updated and redirect to expenses tab=personal
     *   - post: Returns form with validation errors if bindingResult.hasErrors()
     *   - post: Returns form with dateError if date outside trip range
     *   - calls: PersonalExpenseService#updatePersonalExpense
     *   - calledBy: Web browser POST /trips/{tripId}/personal-expenses/{id}
     */
    @PostMapping("/{id}")
    public String updatePersonalExpense(
            @PathVariable UUID tripId,
            @PathVariable UUID id,
            @Valid @ModelAttribute("request") UpdatePersonalExpenseRequest request,
            BindingResult bindingResult,
            Model model,
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getUser().getId();

        if (bindingResult.hasErrors()) {
            populateEditFormModel(model, tripId, id, userId);
            return "expense/personal-edit";
        }

        try {
            personalExpenseService.updatePersonalExpense(id, userId, request);
        } catch (ValidationException ex) {
            populateEditFormModel(model, tripId, id, userId);
            model.addAttribute("dateError", ex.getMessage());
            return "expense/personal-edit";
        }

        log.info("Updated personal expense {} for trip {} by user {}", id, tripId, userId);
        return "redirect:/trips/" + tripId + "/expenses?tab=personal";
    }

    private void populateCreateFormModel(Model model, UUID tripId, UUID userId) {
        TripDateRange dateRange = personalExpenseService.getTripDateRange(tripId, userId);
        model.addAttribute("tripId", tripId);
        model.addAttribute("baseCurrency", personalExpenseService.getBaseCurrency(tripId, userId));
        model.addAttribute("tripStartDate", dateRange.startDate());
        model.addAttribute("tripEndDate", dateRange.endDate());
        model.addAttribute("categories", ExpenseCategories.ALL);
    }

    private void populateEditFormModel(Model model, UUID tripId, UUID id, UUID userId) {
        TripDateRange dateRange = personalExpenseService.getTripDateRange(tripId, userId);
        model.addAttribute("tripId", tripId);
        model.addAttribute("baseCurrency", personalExpenseService.getBaseCurrency(tripId, userId));
        model.addAttribute("tripStartDate", dateRange.startDate());
        model.addAttribute("tripEndDate", dateRange.endDate());
        model.addAttribute("expenseId", id);
        model.addAttribute("categories", ExpenseCategories.ALL);
    }
}
