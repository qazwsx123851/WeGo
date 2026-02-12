package com.wego.controller.web;

import com.wego.dto.request.CreateExpenseRequest;
import com.wego.dto.request.UpdateExpenseRequest;
import com.wego.dto.response.ActivityResponse;
import com.wego.dto.response.ExpenseResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.Role;
import com.wego.entity.SplitType;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.service.ActivityService;
import com.wego.service.ExpenseService;
import com.wego.service.ExpenseViewHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Web controller for expense-related pages and form submissions.
 *
 * @contract
 *   - Handles expense create form display and submission
 *   - Handles expense detail page
 *   - Uses @RequestParam for form data (not @RequestBody)
 *   - Requires authentication for all endpoints
 *   - Validates user permission (OWNER or EDITOR)
 *   - calls: ExpenseService, TripService, ActivityService
 *   - calledBy: Web browser requests, HTML form submissions
 */
@Controller
@RequestMapping("/trips/{tripId}/expenses")
@RequiredArgsConstructor
@Slf4j
public class ExpenseWebController extends BaseWebController {

    private final ExpenseService expenseService;
    private final ActivityService activityService;
    private final ExpenseViewHelper expenseViewHelper;

    /**
     * Show trip expenses page.
     *
     * @contract
     *   - pre: tripId != null, principal != null
     *   - post: Returns expense list view with expenses and summary
     *   - calls: TripService#getTrip, ExpenseService#getExpensesByTrip
     *   - calledBy: Web browser GET /trips/{tripId}/expenses
     */
    @GetMapping
    public String showExpenses(@PathVariable UUID tripId,
                               @CurrentUser UserPrincipal principal,
                               Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip = loadTrip(tripId, user.getId());
        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        // Get expenses for the trip
        var expenses = expenseService.getExpensesByTrip(tripId, user.getId());

        // Group expenses by date
        Map<LocalDate, List<ExpenseResponse>> expensesByDate = expenseViewHelper.groupExpensesByDate(expenses);

        // Calculate totals
        String baseCurrency = trip.getBaseCurrency() != null ? trip.getBaseCurrency() : "TWD";
        BigDecimal totalExpense = expenseService.getTotalExpense(tripId, baseCurrency, user.getId());

        int memberCount = trip.getMembers() != null ? trip.getMembers().size() : 1;
        BigDecimal perPersonAverage = expenseViewHelper.calculatePerPersonAverage(totalExpense, memberCount);

        // Calculate user balance
        BigDecimal userBalance;
        try {
            userBalance = expenseService.calculateUserBalanceInTrip(user.getId(), tripId);
        } catch (Exception e) {
            log.warn("Failed to calculate user balance for trip {}: {}", tripId, e.getMessage());
            userBalance = BigDecimal.ZERO;
        }

        model.addAttribute("trip", trip);
        model.addAttribute("expenses", expenses);
        model.addAttribute("expensesByDate", expensesByDate);
        model.addAttribute("totalExpense", totalExpense);
        model.addAttribute("perPersonAverage", perPersonAverage);
        model.addAttribute("userBalance", userBalance);
        model.addAttribute("defaultCurrency", baseCurrency);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        return "expense/list";
    }

    /**
     * Shows the expense creation form.
     *
     * @contract
     *   - pre: tripId != null, principal != null
     *   - pre: user has OWNER or EDITOR role on the trip
     *   - post: Returns expense create form with trip and members data
     *   - calls: TripService#getTrip, TripMemberRepository#findByTripId
     *   - calledBy: Web browser GET /trips/{tripId}/expenses/create
     *
     * @param tripId The trip ID
     * @param principal The authenticated user
     * @param model The Spring MVC model
     * @return The expense create view name
     */
    @GetMapping({"/create", "/new"})
    public String showCreateForm(@PathVariable UUID tripId,
                                 @RequestParam(required = false) UUID activityId,
                                 @CurrentUser UserPrincipal principal,
                                 Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip = loadTrip(tripId, user.getId());
        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        // Check permission - only OWNER or EDITOR can create expenses
        TripResponse.MemberSummary currentMember = findCurrentMember(trip, user.getId());
        if (!canEdit(currentMember)) {
            log.warn("User {} has no edit permission for trip {}", user.getId(), tripId);
            return "redirect:/trips/" + tripId + "/expenses?error=access_denied";
        }

        // If activityId is provided, pre-fill data from the activity
        if (activityId != null) {
            try {
                ActivityResponse activity = activityService.getActivity(activityId, user.getId());
                model.addAttribute("activityId", activityId);
                if (activity.getPlace() != null && activity.getPlace().getName() != null) {
                    model.addAttribute("activityName", activity.getPlace().getName());
                }
                // Calculate the activity date from trip start date + activity day
                if (trip.getStartDate() != null) {
                    LocalDate activityDate = trip.getStartDate().plusDays(activity.getDay() - 1);
                    model.addAttribute("activityDate", activityDate.toString());
                }
            } catch (Exception e) {
                log.warn("Failed to load activity {} for expense pre-fill: {}", activityId, e.getMessage());
            }
        }

        // Get members for the payer and participant selection (use trip.getMembers() which returns MemberSummary)
        model.addAttribute("trip", trip);
        model.addAttribute("tripId", tripId);
        model.addAttribute("members", trip.getMembers());
        model.addAttribute("currentUserId", user.getId());
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        return "expense/create";
    }

    /**
     * Handles expense creation form submission.
     *
     * @contract
     *   - pre: tripId != null, principal != null
     *   - pre: user has OWNER or EDITOR role on the trip
     *   - pre: amount > 0, description not blank, payerId not null
     *   - post: Expense is created with appropriate splits
     *   - post: Redirects to expense list on success
     *   - post: Returns to create form with error message on failure
     *   - calls: ExpenseService#createExpense
     *   - calledBy: Web browser POST /trips/{tripId}/expenses
     *
     * @param tripId The trip ID
     * @param amount The expense amount (required, must be > 0)
     * @param currency The currency code (default: TWD)
     * @param description The expense description (required)
     * @param category The expense category
     * @param expenseDate The date of the expense
     * @param payerId The ID of the member who paid
     * @param splitMethod The split method (EQUAL, PERCENTAGE, CUSTOM)
     * @param participantIds The IDs of participants for EQUAL split
     * @param percentages Map of userId to percentage for PERCENTAGE split
     * @param customAmounts Map of userId to custom amount for CUSTOM split
     * @param notes Optional notes for the expense
     * @param principal The authenticated user
     * @param model The Spring MVC model
     * @param redirectAttributes For flash messages
     * @return Redirect to expense list or back to form
     */
    @PostMapping
    public String createExpense(@PathVariable UUID tripId,
                                @RequestParam BigDecimal amount,
                                @RequestParam(defaultValue = "TWD") String currency,
                                @RequestParam String description,
                                @RequestParam(required = false) String category,
                                @RequestParam(required = false) String expenseDate,
                                @RequestParam UUID payerId,
                                @RequestParam(defaultValue = "EQUAL") String splitMethod,
                                @RequestParam(required = false) List<UUID> participantIds,
                                @RequestParam(required = false) Map<String, String> percentages,
                                @RequestParam(required = false) Map<String, String> customAmounts,
                                @RequestParam(required = false) String notes,
                                @RequestParam(required = false) UUID activityId,
                                @CurrentUser UserPrincipal principal,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            // Validate and get trip
            TripResponse trip = tripService.getTrip(tripId, user.getId());
            if (trip == null) {
                return "redirect:/dashboard?error=trip_not_found";
            }

            // Check permission
            TripResponse.MemberSummary currentMember = findCurrentMember(trip, user.getId());
            if (!canEdit(currentMember)) {
                log.warn("User {} has no edit permission for trip {}", user.getId(), tripId);
                redirectAttributes.addFlashAttribute("error", "您沒有權限新增支出");
                return "redirect:/trips/" + tripId + "/expenses";
            }

            // Parse expense date
            LocalDate parsedExpenseDate = null;
            if (expenseDate != null && !expenseDate.isEmpty()) {
                try {
                    parsedExpenseDate = LocalDate.parse(expenseDate);
                } catch (Exception e) {
                    log.warn("Invalid expense date format: {}", expenseDate);
                }
            }

            // Determine split type
            SplitType splitType = expenseViewHelper.parseSplitType(splitMethod);

            // Build the request
            CreateExpenseRequest.CreateExpenseRequestBuilder requestBuilder = CreateExpenseRequest.builder()
                    .description(description)
                    .amount(amount)
                    .currency(currency)
                    .paidBy(payerId)
                    .splitType(splitType)
                    .category(category)
                    .expenseDate(parsedExpenseDate)
                    .activityId(activityId)
                    .note(notes);

            // Build splits based on split type
            List<CreateExpenseRequest.SplitRequest> splits = expenseViewHelper.buildSplits(
                    splitType, participantIds, percentages, customAmounts, tripId, amount);

            if (splitType != SplitType.EQUAL || (participantIds != null && !participantIds.isEmpty())) {
                requestBuilder.splits(splits);
            }

            CreateExpenseRequest request = requestBuilder.build();

            // Create expense
            ExpenseResponse createdExpense = expenseService.createExpense(tripId, request, user.getId());
            log.info("Created expense {} for trip {} by user {}", createdExpense.getId(), tripId, user.getId());

            redirectAttributes.addFlashAttribute("success", "支出新增成功");
            return "redirect:/trips/" + tripId + "/expenses";

        } catch (ForbiddenException e) {
            log.warn("Permission denied creating expense for trip {}: {}", tripId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "您沒有權限新增支出");
            return "redirect:/trips/" + tripId + "/expenses";
        } catch (ResourceNotFoundException e) {
            log.warn("Resource not found creating expense for trip {}: {}", tripId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "找不到行程");
            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("Failed to create expense for trip {}: {}", tripId, e.getMessage(), e);
            // Do not expose internal error messages to users for security
            redirectAttributes.addFlashAttribute("error", "新增支出失敗，請稍後再試");
            return "redirect:/trips/" + tripId + "/expenses/create";
        }
    }

    /**
     * Shows the expense detail page.
     *
     * @contract
     *   - pre: tripId != null, expenseId != null, principal != null
     *   - pre: user has view permission on the trip
     *   - post: Returns expense detail view with expense data
     *   - calls: TripService#getTrip, ExpenseService#getExpensesByTrip
     *   - calledBy: Web browser GET /trips/{tripId}/expenses/{expenseId}
     *
     * @param tripId The trip ID
     * @param expenseId The expense ID
     * @param principal The authenticated user
     * @param model The Spring MVC model
     * @return The expense detail view name
     */
    @GetMapping("/{expenseId}")
    public String showExpenseDetail(@PathVariable UUID tripId,
                                    @PathVariable UUID expenseId,
                                    @CurrentUser UserPrincipal principal,
                                    Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip = loadTrip(tripId, user.getId());
        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        // Get the specific expense directly
        ExpenseResponse expense;
        try {
            expense = expenseService.getExpense(expenseId, user.getId());
        } catch (ResourceNotFoundException e) {
            log.warn("Expense {} not found in trip {}: {}", expenseId, tripId, e.getMessage());
            return "redirect:/trips/" + tripId + "/expenses?error=expense_not_found";
        } catch (ForbiddenException e) {
            log.warn("Expense {} not found in trip {}: {}", expenseId, tripId, e.getMessage());
            return "redirect:/trips/" + tripId + "/expenses?error=expense_not_found";
        }

        // Check if user can edit
        TripResponse.MemberSummary currentMember = findCurrentMember(trip, user.getId());
        boolean canEdit = canEdit(currentMember);
        boolean isOwner = currentMember != null && currentMember.getRole() == Role.OWNER;

        model.addAttribute("trip", trip);
        model.addAttribute("expense", expense);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        return "expense/detail";
    }

    /**
     * Shows the expense edit form.
     *
     * @contract
     *   - pre: tripId != null, expenseId != null, principal != null
     *   - pre: user has OWNER or EDITOR role on the trip
     *   - post: Returns expense create form pre-filled with expense data
     *   - calls: TripService#getTrip, ExpenseService#getExpensesByTrip
     *   - calledBy: Web browser GET /trips/{tripId}/expenses/{expenseId}/edit
     *
     * @param tripId The trip ID
     * @param expenseId The expense ID
     * @param principal The authenticated user
     * @param model The Spring MVC model
     * @return The expense edit view name (reuses create template)
     */
    @GetMapping("/{expenseId}/edit")
    public String showEditForm(@PathVariable UUID tripId,
                               @PathVariable UUID expenseId,
                               @CurrentUser UserPrincipal principal,
                               Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip = loadTrip(tripId, user.getId());
        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        // Check permission
        TripResponse.MemberSummary currentMember = findCurrentMember(trip, user.getId());
        if (!canEdit(currentMember)) {
            log.warn("User {} has no edit permission for trip {}", user.getId(), tripId);
            return "redirect:/trips/" + tripId + "/expenses?error=access_denied";
        }

        // Find the expense
        ExpenseResponse expense;
        try {
            expense = expenseService.getExpense(expenseId, user.getId());
        } catch (ResourceNotFoundException e) {
            log.warn("Expense {} not found in trip {}: {}", expenseId, tripId, e.getMessage());
            return "redirect:/trips/" + tripId + "/expenses?error=expense_not_found";
        } catch (ForbiddenException e) {
            log.warn("Expense {} not found in trip {}: {}", expenseId, tripId, e.getMessage());
            return "redirect:/trips/" + tripId + "/expenses?error=expense_not_found";
        }

        model.addAttribute("trip", trip);
        model.addAttribute("tripId", tripId);
        model.addAttribute("members", trip.getMembers());
        model.addAttribute("currentUserId", user.getId());
        model.addAttribute("expense", expense);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        return "expense/create";
    }

    /**
     * Handles expense edit form submission.
     *
     * @contract
     *   - pre: tripId != null, expenseId != null, principal != null
     *   - pre: user has OWNER or EDITOR role on the trip
     *   - pre: amount > 0, description not blank
     *   - post: Expense is updated
     *   - post: Redirects to expense list on success
     *   - calls: ExpenseService#updateExpense
     *   - calledBy: Web browser POST /trips/{tripId}/expenses/{expenseId}
     *
     * @param tripId The trip ID
     * @param expenseId The expense ID
     * @param amount The expense amount
     * @param currency The currency code
     * @param description The expense description
     * @param category The expense category
     * @param expenseDate The expense date
     * @param payerId The payer's user ID
     * @param splitMethod The split method
     * @param notes Optional notes
     * @param principal The authenticated user
     * @param redirectAttributes For flash messages
     * @return Redirect to expense list or back to form
     */
    @PostMapping("/{expenseId}")
    public String updateExpense(@PathVariable UUID tripId,
                                @PathVariable UUID expenseId,
                                @RequestParam BigDecimal amount,
                                @RequestParam(defaultValue = "TWD") String currency,
                                @RequestParam String description,
                                @RequestParam(required = false) String category,
                                @RequestParam(required = false) String expenseDate,
                                @RequestParam UUID payerId,
                                @RequestParam(defaultValue = "EQUAL") String splitMethod,
                                @RequestParam(required = false) String notes,
                                @CurrentUser UserPrincipal principal,
                                RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            // Parse expense date
            LocalDate parsedExpenseDate = null;
            if (expenseDate != null && !expenseDate.isEmpty()) {
                try {
                    parsedExpenseDate = LocalDate.parse(expenseDate);
                } catch (Exception e) {
                    log.warn("Invalid expense date format: {}", expenseDate);
                }
            }

            SplitType splitType = expenseViewHelper.parseSplitType(splitMethod);

            UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                    .description(description)
                    .amount(amount)
                    .currency(currency)
                    .paidBy(payerId)
                    .splitType(splitType)
                    .category(category)
                    .expenseDate(parsedExpenseDate)
                    .note(notes)
                    .build();

            expenseService.updateExpense(expenseId, request, user.getId());
            log.info("Updated expense {} for trip {} by user {}", expenseId, tripId, user.getId());

            redirectAttributes.addFlashAttribute("success", "支出更新成功");
            return "redirect:/trips/" + tripId + "/expenses";

        } catch (ForbiddenException e) {
            log.warn("Permission denied updating expense {} for trip {}: {}", expenseId, tripId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "您沒有權限編輯支出");
            return "redirect:/trips/" + tripId + "/expenses";
        } catch (ResourceNotFoundException e) {
            log.warn("Resource not found updating expense {}: {}", expenseId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "找不到支出");
            return "redirect:/trips/" + tripId + "/expenses";
        } catch (Exception e) {
            log.error("Failed to update expense {} for trip {}: {}", expenseId, tripId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "更新支出失敗，請稍後再試");
            return "redirect:/trips/" + tripId + "/expenses/" + expenseId + "/edit";
        }
    }

    /**
     * Shows the expense statistics page.
     *
     * @contract
     *   - pre: tripId != null, principal != null
     *   - pre: user has view permission on the trip
     *   - post: Returns statistics page with trip data
     *   - calls: TripService#getTrip
     *   - calledBy: Web browser GET /trips/{tripId}/expenses/statistics
     *
     * @param tripId The trip ID
     * @param principal The authenticated user
     * @param model The Spring MVC model
     * @return The expense statistics view name
     */
    @GetMapping("/statistics")
    public String showStatistics(@PathVariable UUID tripId,
                                 @CurrentUser UserPrincipal principal,
                                 Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip = loadTrip(tripId, user.getId());
        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        model.addAttribute("trip", trip);
        model.addAttribute("tripId", tripId);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        return "expense/statistics";
    }

}
