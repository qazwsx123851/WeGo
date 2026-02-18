package com.wego.controller.api;

import com.wego.dto.ApiResponse;
import com.wego.dto.request.CreatePersonalExpenseRequest;
import com.wego.dto.request.SetPersonalBudgetRequest;
import com.wego.dto.request.UpdatePersonalExpenseRequest;
import com.wego.dto.response.PersonalExpenseItemResponse;
import com.wego.dto.response.PersonalExpenseSummaryResponse;
import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
import com.wego.service.PersonalExpenseService;
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
 * REST API controller for personal expense operations within a trip.
 *
 * Personal expenses include two sources:
 * - AUTO: expense splits derived from shared trip expenses (read-only)
 * - MANUAL: user-entered private expenses
 *
 * @contract
 *   - All endpoints require authentication
 *   - All endpoints enforce trip membership via PersonalExpenseService
 *   - Returns ApiResponse wrapper for all responses
 *   - Validates request bodies with @Valid
 *
 * @see PersonalExpenseService
 */
@Slf4j
@RestController
@RequestMapping("/api/trips/{tripId}/personal-expenses")
@RequiredArgsConstructor
public class PersonalExpenseApiController {

    private final PersonalExpenseService personalExpenseService;

    /**
     * Lists all personal expenses for the current user within a trip.
     *
     * Merges AUTO (shared expense splits) and MANUAL (user-entered) sources,
     * sorted by expenseDate ASC, nulls last.
     *
     * @contract
     *   - pre: user is authenticated and is a trip member
     *   - post: returns 200 with merged list of personal expense items
     *   - calls: PersonalExpenseService#getPersonalExpenses
     *
     * @param tripId    The trip ID
     * @param principal The current user
     * @return Response with list of personal expense items
     *
     * GET /api/trips/{tripId}/personal-expenses
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PersonalExpenseItemResponse>>> listPersonalExpenses(
            @PathVariable UUID tripId,
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
        log.debug("GET /api/trips/{}/personal-expenses by user {}", tripId, userId);

        List<PersonalExpenseItemResponse> items =
                personalExpenseService.getPersonalExpenses(userId, tripId);

        return ResponseEntity.ok(ApiResponse.success(items));
    }

    /**
     * Creates a new manual personal expense for the current user within a trip.
     *
     * @contract
     *   - pre: user is authenticated and is a trip member
     *   - pre: request body is valid
     *   - post: returns 201 with the created personal expense item
     *   - calls: PersonalExpenseService#createPersonalExpense
     *
     * @param tripId    The trip ID
     * @param request   The create request
     * @param principal The current user
     * @return Response with the created personal expense item
     *
     * POST /api/trips/{tripId}/personal-expenses
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PersonalExpenseItemResponse>> createPersonalExpense(
            @PathVariable UUID tripId,
            @Valid @RequestBody CreatePersonalExpenseRequest request,
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
        log.debug("POST /api/trips/{}/personal-expenses by user {}", tripId, userId);

        PersonalExpenseItemResponse response =
                personalExpenseService.createPersonalExpense(userId, tripId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Personal expense created successfully"));
    }

    /**
     * Updates an existing manual personal expense.
     *
     * Applies PATCH semantics: only non-null fields in the request are applied.
     *
     * @contract
     *   - pre: user is authenticated and is the owner of the expense
     *   - pre: request body is valid
     *   - post: returns 200 with the updated personal expense item
     *   - calls: PersonalExpenseService#updatePersonalExpense
     *
     * @param tripId    The trip ID (used for route consistency; ownership verified in service)
     * @param id        The personal expense ID
     * @param request   The update request
     * @param principal The current user
     * @return Response with the updated personal expense item
     *
     * PUT /api/trips/{tripId}/personal-expenses/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PersonalExpenseItemResponse>> updatePersonalExpense(
            @PathVariable UUID tripId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePersonalExpenseRequest request,
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
        log.debug("PUT /api/trips/{}/personal-expenses/{} by user {}", tripId, id, userId);

        PersonalExpenseItemResponse response =
                personalExpenseService.updatePersonalExpense(id, userId, request);

        return ResponseEntity.ok(ApiResponse.success(response, "Personal expense updated successfully"));
    }

    /**
     * Deletes a manual personal expense by ID.
     *
     * @contract
     *   - pre: user is authenticated and is the owner of the expense
     *   - post: returns 204 No Content on success
     *   - calls: PersonalExpenseService#deletePersonalExpense
     *
     * @param tripId    The trip ID (used for route consistency; ownership verified in service)
     * @param id        The personal expense ID
     * @param principal The current user
     * @return Empty 204 response
     *
     * DELETE /api/trips/{tripId}/personal-expenses/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePersonalExpense(
            @PathVariable UUID tripId,
            @PathVariable UUID id,
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
        log.debug("DELETE /api/trips/{}/personal-expenses/{} by user {}", tripId, id, userId);

        personalExpenseService.deletePersonalExpense(id, userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Returns a summary of personal expenses for the current user within a trip.
     *
     * Includes total amount, daily average, category breakdown, daily amounts
     * (zero-filled for all trip dates), and budget status.
     *
     * @contract
     *   - pre: user is authenticated and is a trip member
     *   - post: returns 200 with summary response
     *   - post: budgetStatus is NONE when no budget has been set
     *   - calls: PersonalExpenseService#getPersonalSummary
     *
     * @param tripId    The trip ID
     * @param principal The current user
     * @return Response with personal expense summary
     *
     * GET /api/trips/{tripId}/personal-expenses/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<PersonalExpenseSummaryResponse>> getPersonalSummary(
            @PathVariable UUID tripId,
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
        log.debug("GET /api/trips/{}/personal-expenses/summary by user {}", tripId, userId);

        PersonalExpenseSummaryResponse response =
                personalExpenseService.getPersonalSummary(userId, tripId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Sets or updates the personal budget for the current user within a trip.
     *
     * @contract
     *   - pre: user is authenticated and is a trip member
     *   - pre: request body is valid (budget > 0)
     *   - post: returns 200 on success
     *   - post: TripMember.personalBudget is persisted
     *   - calls: PersonalExpenseService#setPersonalBudget
     *
     * @param tripId    The trip ID
     * @param request   The budget request
     * @param principal The current user
     * @return Success response
     *
     * PUT /api/trips/{tripId}/personal-expenses/budget
     */
    @PutMapping("/budget")
    public ResponseEntity<ApiResponse<Void>> setPersonalBudget(
            @PathVariable UUID tripId,
            @Valid @RequestBody SetPersonalBudgetRequest request,
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
        log.debug("PUT /api/trips/{}/personal-expenses/budget by user {}", tripId, userId);

        personalExpenseService.setPersonalBudget(tripId, userId, request);

        return ResponseEntity.ok(ApiResponse.success(null, "Personal budget updated successfully"));
    }
}
