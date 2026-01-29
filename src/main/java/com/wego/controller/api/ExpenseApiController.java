package com.wego.controller.api;

import com.wego.dto.ApiResponse;
import com.wego.dto.request.CreateExpenseRequest;
import com.wego.dto.request.UpdateExpenseRequest;
import com.wego.dto.response.ExpenseResponse;
import com.wego.dto.response.SettlementResponse;
import com.wego.service.ExpenseService;
import com.wego.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST API controller for expense and settlement operations.
 *
 * @contract
 *   - All endpoints require authentication
 *   - Returns ApiResponse wrapper for all responses
 *   - Validates request bodies
 *
 * @see ExpenseService
 * @see SettlementService
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExpenseApiController {

    private final ExpenseService expenseService;
    private final SettlementService settlementService;

    /**
     * Creates a new expense for a trip.
     *
     * @contract
     *   - pre: user is authenticated
     *   - pre: request is valid
     *   - post: returns 201 with created expense
     *   - calls: ExpenseService#createExpense
     *
     * POST /api/trips/{tripId}/expenses
     */
    @PostMapping("/trips/{tripId}/expenses")
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @PathVariable UUID tripId,
            @Valid @RequestBody CreateExpenseRequest request,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("POST /api/trips/{}/expenses by user {}", tripId, userId);

        ExpenseResponse response = expenseService.createExpense(tripId, request, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Expense created successfully"));
    }

    /**
     * Gets all expenses for a trip.
     *
     * @contract
     *   - pre: user is authenticated
     *   - post: returns 200 with list of expenses
     *   - calls: ExpenseService#getExpensesByTrip
     *
     * GET /api/trips/{tripId}/expenses
     */
    @GetMapping("/trips/{tripId}/expenses")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getExpensesByTrip(
            @PathVariable UUID tripId,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("GET /api/trips/{}/expenses by user {}", tripId, userId);

        List<ExpenseResponse> expenses = expenseService.getExpensesByTrip(tripId, userId);

        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    /**
     * Updates an expense.
     *
     * @contract
     *   - pre: user is authenticated
     *   - pre: request is valid
     *   - post: returns 200 with updated expense
     *   - calls: ExpenseService#updateExpense
     *
     * PUT /api/expenses/{expenseId}
     */
    @PutMapping("/expenses/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
            @PathVariable UUID expenseId,
            @Valid @RequestBody UpdateExpenseRequest request,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("PUT /api/expenses/{} by user {}", expenseId, userId);

        ExpenseResponse response = expenseService.updateExpense(expenseId, request, userId);

        return ResponseEntity.ok(ApiResponse.success(response, "Expense updated successfully"));
    }

    /**
     * Deletes an expense.
     *
     * @contract
     *   - pre: user is authenticated
     *   - post: returns 200 on success
     *   - calls: ExpenseService#deleteExpense
     *
     * DELETE /api/expenses/{expenseId}
     */
    @DeleteMapping("/expenses/{expenseId}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("DELETE /api/expenses/{} by user {}", expenseId, userId);

        expenseService.deleteExpense(expenseId, userId);

        return ResponseEntity.ok(ApiResponse.success(null, "Expense deleted successfully"));
    }

    /**
     * Gets the settlement calculation for a trip.
     *
     * @contract
     *   - pre: user is authenticated
     *   - post: returns 200 with settlement details
     *   - calls: SettlementService#calculateSettlement
     *
     * GET /api/trips/{tripId}/settlement
     */
    @GetMapping("/trips/{tripId}/settlement")
    public ResponseEntity<ApiResponse<SettlementResponse>> getSettlement(
            @PathVariable UUID tripId,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("GET /api/trips/{}/settlement by user {}", tripId, userId);

        SettlementResponse response = settlementService.calculateSettlement(tripId, userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Marks an expense split as settled.
     *
     * @contract
     *   - pre: user is authenticated
     *   - post: returns 200 on success
     *   - calls: SettlementService#markAsSettled
     *
     * PUT /api/expense-splits/{splitId}/settle
     */
    @PutMapping("/expense-splits/{splitId}/settle")
    public ResponseEntity<ApiResponse<Void>> markAsSettled(
            @PathVariable UUID splitId,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("PUT /api/expense-splits/{}/settle by user {}", splitId, userId);

        settlementService.markAsSettled(splitId, userId);

        return ResponseEntity.ok(ApiResponse.success(null, "Split marked as settled"));
    }

    /**
     * Marks an expense split as unsettled.
     *
     * @contract
     *   - pre: user is authenticated
     *   - post: returns 200 on success
     *   - calls: SettlementService#markAsUnsettled
     *
     * PUT /api/expense-splits/{splitId}/unsettle
     */
    @PutMapping("/expense-splits/{splitId}/unsettle")
    public ResponseEntity<ApiResponse<Void>> markAsUnsettled(
            @PathVariable UUID splitId,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("PUT /api/expense-splits/{}/unsettle by user {}", splitId, userId);

        settlementService.markAsUnsettled(splitId, userId);

        return ResponseEntity.ok(ApiResponse.success(null, "Split marked as unsettled"));
    }

    /**
     * Gets the current user ID from the OAuth2 principal.
     * For testing, returns a random UUID if principal is null.
     */
    private UUID getCurrentUserId(OAuth2User principal) {
        if (principal == null) {
            // For testing purposes, generate a consistent UUID
            return UUID.fromString("00000000-0000-0000-0000-000000000001");
        }
        // In a real implementation, this would extract the user ID from the principal
        // For now, we use a placeholder that should be replaced with actual user lookup
        String sub = principal.getAttribute("sub");
        if (sub != null) {
            try {
                return UUID.fromString(sub);
            } catch (IllegalArgumentException e) {
                // If sub is not a valid UUID, generate one based on the sub hash
                return UUID.nameUUIDFromBytes(sub.getBytes());
            }
        }
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }
}
