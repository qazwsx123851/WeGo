package com.wego.controller.api;

import com.wego.dto.ApiResponse;
import com.wego.dto.request.CreateExpenseRequest;
import com.wego.dto.request.SettleUsersRequest;
import com.wego.dto.request.UpdateExpenseRequest;
import com.wego.dto.response.ExpenseResponse;
import com.wego.dto.response.SettlementResponse;
import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
import com.wego.service.ExpenseService;
import com.wego.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
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
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
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
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
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
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
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
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
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
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
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
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
        log.debug("PUT /api/expense-splits/{}/unsettle by user {}", splitId, userId);

        settlementService.markAsUnsettled(splitId, userId);

        return ResponseEntity.ok(ApiResponse.success(null, "Split marked as unsettled"));
    }

    /**
     * Settles all splits between two users in a trip.
     *
     * @contract
     *   - pre: tripId != null, request body contains fromUserId and toUserId
     *   - pre: user has edit permission on trip
     *   - post: all unsettled splits between the users are marked settled
     *   - calls: SettlementService#settleAllBetweenUsers
     *
     * PUT /api/trips/{tripId}/settlement/settle
     */
    @PutMapping("/trips/{tripId}/settlement/settle")
    public ResponseEntity<ApiResponse<Void>> settleByUsers(
            @PathVariable UUID tripId,
            @Valid @RequestBody SettleUsersRequest request,
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
        log.debug("PUT /api/trips/{}/settlement/settle from={} to={} by user {}", tripId, request.getFromUserId(), request.getToUserId(), userId);

        settlementService.settleAllBetweenUsers(tripId, request.getFromUserId(), request.getToUserId(), userId);

        return ResponseEntity.ok(ApiResponse.success(null, "已結清"));
    }

    /**
     * Unsettles all splits between two users in a trip.
     *
     * @contract
     *   - pre: tripId != null, request body contains fromUserId and toUserId
     *   - pre: user has edit permission on trip
     *   - post: all settled splits between the users are marked unsettled
     *   - calls: SettlementService#unsettleAllBetweenUsers
     *
     * PUT /api/trips/{tripId}/settlement/unsettle
     */
    @PutMapping("/trips/{tripId}/settlement/unsettle")
    public ResponseEntity<ApiResponse<Void>> unsettleByUsers(
            @PathVariable UUID tripId,
            @Valid @RequestBody SettleUsersRequest request,
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
        log.debug("PUT /api/trips/{}/settlement/unsettle from={} to={} by user {}", tripId, request.getFromUserId(), request.getToUserId(), userId);

        settlementService.unsettleAllBetweenUsers(tripId, request.getFromUserId(), request.getToUserId(), userId);

        return ResponseEntity.ok(ApiResponse.success(null, "已取消結清"));
    }

}
