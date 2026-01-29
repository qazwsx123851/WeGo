package com.wego.service;

import com.wego.domain.permission.PermissionChecker;
import com.wego.domain.settlement.DebtSimplifier;
import com.wego.domain.settlement.Settlement;
import com.wego.dto.response.SettlementResponse;
import com.wego.entity.Expense;
import com.wego.entity.ExpenseSplit;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.repository.ExpenseRepository;
import com.wego.repository.ExpenseSplitRepository;
import com.wego.repository.TripRepository;
import com.wego.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for calculating and managing settlements.
 *
 * @contract
 *   - Calculates balances from expenses and splits
 *   - Uses DebtSimplifier to minimize transactions
 *   - Manages settlement status of splits
 *
 * @see DebtSimplifier
 * @see Settlement
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final PermissionChecker permissionChecker;
    private final DebtSimplifier debtSimplifier;

    /**
     * Calculates the settlement for a trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - pre: user has view permission on trip
     *   - post: returns simplified settlement transactions
     *   - calledBy: ExpenseApiController#getSettlement
     *
     * @param tripId The trip ID
     * @param userId The ID of the user requesting
     * @return The settlement response with all transactions
     * @throws ForbiddenException if user has no view permission
     * @throws ResourceNotFoundException if trip not found
     */
    @Transactional(readOnly = true)
    public SettlementResponse calculateSettlement(UUID tripId, UUID userId) {
        log.debug("Calculating settlement for trip {} by user {}", tripId, userId);

        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("No permission to view this trip");
        }

        var trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId.toString()));

        // Get all expenses and splits
        List<Expense> expenses = expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId);
        List<ExpenseSplit> splits = expenseSplitRepository.findByTripId(tripId);

        // Calculate balances
        // Positive balance = owed money (creditor)
        // Negative balance = owes money (debtor)
        Map<UUID, BigDecimal> balances = calculateBalances(expenses, splits);

        // Get simplified settlements
        List<Settlement> settlements = debtSimplifier.simplify(balances);

        // Calculate total expenses
        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get user info for settlements
        List<UUID> userIds = settlements.stream()
                .flatMap(s -> java.util.stream.Stream.of(s.getFromUserId(), s.getToUserId()))
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, User> userMap = getUserMap(userIds);

        // Build response
        List<SettlementResponse.SettlementItemResponse> settlementItems = settlements.stream()
                .map(settlement -> {
                    User fromUser = userMap.get(settlement.getFromUserId());
                    User toUser = userMap.get(settlement.getToUserId());

                    return SettlementResponse.SettlementItemResponse.builder()
                            .fromUserId(settlement.getFromUserId())
                            .fromUserName(fromUser != null ? fromUser.getNickname() : "Unknown")
                            .fromUserAvatarUrl(fromUser != null ? fromUser.getAvatarUrl() : null)
                            .toUserId(settlement.getToUserId())
                            .toUserName(toUser != null ? toUser.getNickname() : "Unknown")
                            .toUserAvatarUrl(toUser != null ? toUser.getAvatarUrl() : null)
                            .amount(settlement.getAmount())
                            .build();
                })
                .collect(Collectors.toList());

        log.debug("Calculated {} settlements for trip {}", settlements.size(), tripId);

        return SettlementResponse.builder()
                .settlements(settlementItems)
                .totalExpenses(totalExpenses)
                .baseCurrency(trip.getBaseCurrency())
                .expenseCount(expenses.size())
                .build();
    }

    /**
     * Marks an expense split as settled.
     *
     * @contract
     *   - pre: splitId != null, userId != null
     *   - pre: user has edit permission on the trip
     *   - post: split is marked as settled with timestamp
     *   - calledBy: ExpenseApiController#markAsSettled
     *
     * @param splitId The split ID
     * @param userId The ID of the user marking as settled
     * @throws ResourceNotFoundException if split not found
     * @throws ForbiddenException if user has no permission
     */
    @Transactional
    public void markAsSettled(UUID splitId, UUID userId) {
        log.debug("Marking split {} as settled by user {}", splitId, userId);

        ExpenseSplit split = expenseSplitRepository.findById(splitId)
                .orElseThrow(() -> new ResourceNotFoundException("ExpenseSplit", splitId.toString()));

        Expense expense = expenseRepository.findById(split.getExpenseId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense", split.getExpenseId().toString()));

        if (!permissionChecker.canEdit(expense.getTripId(), userId)) {
            throw new ForbiddenException("No permission to mark settlement");
        }

        split.markAsSettled();
        expenseSplitRepository.save(split);

        log.info("Marked split {} as settled", splitId);
    }

    /**
     * Marks an expense split as unsettled.
     *
     * @contract
     *   - pre: splitId != null, userId != null
     *   - pre: user has edit permission on the trip
     *   - post: split is marked as unsettled
     *   - calledBy: ExpenseApiController#markAsUnsettled
     *
     * @param splitId The split ID
     * @param userId The ID of the user marking as unsettled
     * @throws ResourceNotFoundException if split not found
     * @throws ForbiddenException if user has no permission
     */
    @Transactional
    public void markAsUnsettled(UUID splitId, UUID userId) {
        log.debug("Marking split {} as unsettled by user {}", splitId, userId);

        ExpenseSplit split = expenseSplitRepository.findById(splitId)
                .orElseThrow(() -> new ResourceNotFoundException("ExpenseSplit", splitId.toString()));

        Expense expense = expenseRepository.findById(split.getExpenseId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense", split.getExpenseId().toString()));

        if (!permissionChecker.canEdit(expense.getTripId(), userId)) {
            throw new ForbiddenException("No permission to mark settlement");
        }

        split.markAsUnsettled();
        expenseSplitRepository.save(split);

        log.info("Marked split {} as unsettled", splitId);
    }

    /**
     * Calculates net balances for all users involved in expenses.
     *
     * @param expenses List of expenses
     * @param splits List of expense splits
     * @return Map of user ID to net balance (positive = creditor, negative = debtor)
     */
    private Map<UUID, BigDecimal> calculateBalances(List<Expense> expenses, List<ExpenseSplit> splits) {
        Map<UUID, BigDecimal> balances = new HashMap<>();

        // Group splits by expense for easy lookup
        Map<UUID, List<ExpenseSplit>> splitsByExpense = splits.stream()
                .collect(Collectors.groupingBy(ExpenseSplit::getExpenseId));

        for (Expense expense : expenses) {
            UUID payer = expense.getPaidBy();
            BigDecimal amount = expense.getAmount();

            // Payer is owed the expense amount
            balances.merge(payer, amount, BigDecimal::add);

            // Each split participant owes their share
            List<ExpenseSplit> expenseSplits = splitsByExpense.getOrDefault(expense.getId(), List.of());
            for (ExpenseSplit split : expenseSplits) {
                // Only count unsettled splits
                if (!split.isSettled()) {
                    balances.merge(split.getUserId(), split.getAmount().negate(), BigDecimal::add);
                }
            }
        }

        // Remove zero balances
        balances.entrySet().removeIf(entry ->
                entry.getValue().compareTo(BigDecimal.ZERO) == 0);

        return balances;
    }

    /**
     * Gets a map of user IDs to User entities.
     */
    private Map<UUID, User> getUserMap(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
