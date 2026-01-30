package com.wego.controller.web;

import com.wego.dto.request.CreateExpenseRequest;
import com.wego.dto.response.ExpenseResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.Role;
import com.wego.entity.SplitType;
import com.wego.entity.TripMember;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.repository.TripMemberRepository;
import com.wego.service.ExpenseService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
 *   - calls: ExpenseService, TripService, UserService
 *   - calledBy: Web browser requests, HTML form submissions
 */
@Controller
@RequestMapping("/trips/{tripId}/expenses")
@RequiredArgsConstructor
@Slf4j
public class ExpenseWebController {

    private final ExpenseService expenseService;
    private final TripService tripService;
    private final UserService userService;
    private final TripMemberRepository tripMemberRepository;

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
    @GetMapping("/create")
    public String showCreateForm(@PathVariable UUID tripId,
                                 @AuthenticationPrincipal OAuth2User principal,
                                 Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(tripId, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", tripId, e.getMessage());
            return "redirect:/dashboard?error=trip_not_found";
        }

        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        // Check permission - only OWNER or EDITOR can create expenses
        TripResponse.MemberSummary currentMember = findCurrentMember(trip, user.getId());
        if (!canEdit(currentMember)) {
            log.warn("User {} has no edit permission for trip {}", user.getId(), tripId);
            return "redirect:/trips/" + tripId + "/expenses?error=access_denied";
        }

        // Get members for the payer and participant selection
        List<TripMember> members = tripMemberRepository.findByTripId(tripId);

        model.addAttribute("trip", trip);
        model.addAttribute("tripId", tripId);
        model.addAttribute("members", members);
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
                                @AuthenticationPrincipal OAuth2User principal,
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
            SplitType splitType = parseSplitType(splitMethod);

            // Build the request
            CreateExpenseRequest.CreateExpenseRequestBuilder requestBuilder = CreateExpenseRequest.builder()
                    .description(description)
                    .amount(amount)
                    .currency(currency)
                    .paidBy(payerId)
                    .splitType(splitType)
                    .category(category)
                    .expenseDate(parsedExpenseDate)
                    .note(notes);

            // Build splits based on split type
            List<CreateExpenseRequest.SplitRequest> splits = buildSplits(
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
                                    @AuthenticationPrincipal OAuth2User principal,
                                    Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(tripId, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", tripId, e.getMessage());
            return "redirect:/dashboard?error=trip_not_found";
        }

        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        // Get all expenses and find the target one
        List<ExpenseResponse> expenses = expenseService.getExpensesByTrip(tripId, user.getId());
        ExpenseResponse expense = expenses.stream()
                .filter(e -> e.getId().equals(expenseId))
                .findFirst()
                .orElse(null);

        if (expense == null) {
            log.warn("Expense {} not found in trip {}", expenseId, tripId);
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
     * Parses split method string to SplitType enum.
     *
     * @param splitMethod The split method string
     * @return The corresponding SplitType
     */
    private SplitType parseSplitType(String splitMethod) {
        if (splitMethod == null) {
            return SplitType.EQUAL;
        }
        return switch (splitMethod.toUpperCase()) {
            case "PERCENTAGE" -> SplitType.PERCENTAGE;
            case "CUSTOM" -> SplitType.CUSTOM;
            case "SHARES" -> SplitType.SHARES;
            default -> SplitType.EQUAL;
        };
    }

    /**
     * Builds split requests based on split type.
     *
     * @param splitType The split type
     * @param participantIds List of participant IDs for EQUAL split
     * @param percentages Map of userId to percentage for PERCENTAGE split
     * @param customAmounts Map of userId to amount for CUSTOM split
     * @param tripId The trip ID (for getting all members if needed)
     * @param totalAmount The total expense amount
     * @return List of split requests
     */
    private List<CreateExpenseRequest.SplitRequest> buildSplits(
            SplitType splitType,
            List<UUID> participantIds,
            Map<String, String> percentages,
            Map<String, String> customAmounts,
            UUID tripId,
            BigDecimal totalAmount) {

        List<CreateExpenseRequest.SplitRequest> splits = new ArrayList<>();

        switch (splitType) {
            case EQUAL -> {
                // For EQUAL, we may have specific participants
                if (participantIds != null && !participantIds.isEmpty()) {
                    for (UUID participantId : participantIds) {
                        splits.add(CreateExpenseRequest.SplitRequest.builder()
                                .userId(participantId)
                                .build());
                    }
                }
                // If no participants specified, ExpenseService will use all trip members
            }
            case PERCENTAGE -> {
                if (percentages != null) {
                    for (Map.Entry<String, String> entry : percentages.entrySet()) {
                        String key = entry.getKey();
                        // Handle Spring's map key format: percentages[userId]=value
                        if (key.startsWith("percentages[") && key.endsWith("]")) {
                            key = key.substring(12, key.length() - 1);
                        }
                        try {
                            UUID userId = UUID.fromString(key);
                            BigDecimal percentage = new BigDecimal(entry.getValue());
                            if (percentage.compareTo(BigDecimal.ZERO) > 0) {
                                splits.add(CreateExpenseRequest.SplitRequest.builder()
                                        .userId(userId)
                                        .percentage(percentage)
                                        .build());
                            }
                        } catch (Exception e) {
                            log.warn("Invalid percentage entry: {} = {}", key, entry.getValue());
                        }
                    }
                }
            }
            case CUSTOM -> {
                if (customAmounts != null) {
                    for (Map.Entry<String, String> entry : customAmounts.entrySet()) {
                        String key = entry.getKey();
                        // Handle Spring's map key format: customAmounts[userId]=value
                        if (key.startsWith("customAmounts[") && key.endsWith("]")) {
                            key = key.substring(14, key.length() - 1);
                        }
                        try {
                            UUID userId = UUID.fromString(key);
                            BigDecimal customAmount = new BigDecimal(entry.getValue());
                            if (customAmount.compareTo(BigDecimal.ZERO) > 0) {
                                splits.add(CreateExpenseRequest.SplitRequest.builder()
                                        .userId(userId)
                                        .amount(customAmount)
                                        .build());
                            }
                        } catch (Exception e) {
                            log.warn("Invalid custom amount entry: {} = {}", key, entry.getValue());
                        }
                    }
                }
            }
            case SHARES -> {
                // SHARES not implemented in the form yet, but handle it for completeness
                log.debug("SHARES split type not implemented in form");
            }
        }

        return splits;
    }

    /**
     * Finds the current member in the trip.
     *
     * @param trip The trip response
     * @param userId The user ID
     * @return The member summary or null if not found
     */
    private TripResponse.MemberSummary findCurrentMember(TripResponse trip, UUID userId) {
        if (trip == null || trip.getMembers() == null) {
            return null;
        }
        return trip.getMembers().stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if the member can edit the trip.
     *
     * @param member The member summary
     * @return true if the member is OWNER or EDITOR
     */
    private boolean canEdit(TripResponse.MemberSummary member) {
        return member != null &&
                (member.getRole() == Role.OWNER || member.getRole() == Role.EDITOR);
    }

    /**
     * Gets the current user from OAuth2 principal.
     *
     * @param principal The OAuth2 principal
     * @return The user or null if not found
     */
    private User getCurrentUser(OAuth2User principal) {
        if (principal == null) {
            return null;
        }
        String email = principal.getAttribute("email");
        return userService.getUserByEmail(email);
    }
}
