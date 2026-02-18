package com.wego.service;

import com.wego.domain.TripConstants;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 *   - Supports multi-currency conversion using ExchangeRateService
 *
 * @see DebtSimplifier
 * @see Settlement
 * @see ExchangeRateService
 */
@Slf4j
@Service
public class SettlementService {


    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final PermissionChecker permissionChecker;
    private final DebtSimplifier debtSimplifier;
    private final ExchangeRateService exchangeRateService;
    private final StatisticsService statisticsService;

    /**
     * Creates a SettlementService with all dependencies.
     *
     * @contract
     *   - pre: all repository and service dependencies are non-null
     *   - pre: exchangeRateService can be null (fallback to no conversion)
     *
     * @param expenseRepository Expense repository
     * @param expenseSplitRepository ExpenseSplit repository
     * @param tripRepository Trip repository
     * @param userRepository User repository
     * @param permissionChecker Permission checker
     * @param debtSimplifier Debt simplifier algorithm
     * @param exchangeRateService Exchange rate service (optional, can be null)
     * @param statisticsService Statistics service for cache eviction
     */
    public SettlementService(
            ExpenseRepository expenseRepository,
            ExpenseSplitRepository expenseSplitRepository,
            TripRepository tripRepository,
            UserRepository userRepository,
            PermissionChecker permissionChecker,
            DebtSimplifier debtSimplifier,
            @Nullable ExchangeRateService exchangeRateService,
            @Nullable StatisticsService statisticsService) {
        this.expenseRepository = expenseRepository;
        this.expenseSplitRepository = expenseSplitRepository;
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.permissionChecker = permissionChecker;
        this.debtSimplifier = debtSimplifier;
        this.exchangeRateService = exchangeRateService;
        this.statisticsService = statisticsService;
    }

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

        String baseCurrency = trip.getBaseCurrency();

        // Get all expenses and splits
        List<Expense> expenses = expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId);
        List<ExpenseSplit> splits = expenseSplitRepository.findByTripId(tripId);

        // Calculate currency breakdown (original amounts by currency)
        Map<String, BigDecimal> currencyBreakdown = calculateCurrencyBreakdown(expenses);

        // Track currency conversion warnings
        Set<String> conversionWarnings = new HashSet<>();

        // Calculate balances with currency conversion
        // Positive balance = owed money (creditor)
        // Negative balance = owes money (debtor)
        Map<UUID, BigDecimal> balances = calculateBalances(expenses, splits, baseCurrency, conversionWarnings);

        // Get simplified settlements
        List<Settlement> settlements = debtSimplifier.simplify(balances);

        // Calculate total expenses (converted to base currency)
        BigDecimal totalExpenses = calculateTotalInBaseCurrency(expenses, baseCurrency, conversionWarnings);

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
                            .fromUserName(fromUser != null ? fromUser.getNickname() : TripConstants.UNKNOWN_USER_NAME)
                            .fromUserAvatarUrl(fromUser != null ? fromUser.getAvatarUrl() : null)
                            .toUserId(settlement.getToUserId())
                            .toUserName(toUser != null ? toUser.getNickname() : TripConstants.UNKNOWN_USER_NAME)
                            .toUserAvatarUrl(toUser != null ? toUser.getAvatarUrl() : null)
                            .amount(settlement.getAmount())
                            .build();
                })
                .collect(Collectors.toList());

        log.debug("Calculated {} settlements for trip {} with {} currencies",
                settlements.size(), tripId, currencyBreakdown.size());

        return SettlementResponse.builder()
                .settlements(settlementItems)
                .totalExpenses(totalExpenses)
                .baseCurrency(baseCurrency)
                .expenseCount(expenses.size())
                .currencyBreakdown(currencyBreakdown)
                .conversionWarnings(new ArrayList<>(conversionWarnings))
                .userBalances(balances)
                .build();
    }

    /**
     * Calculates a single user's balance in a trip with proper multi-currency conversion.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - pre: user has view permission on trip
     *   - post: Returns positive if owed to user, negative if user owes, in base currency
     *   - calls: calculateBalances, convertToBaseCurrency
     *   - calledBy: ExpenseWebController#showExpenses
     *
     * @param tripId The trip ID
     * @param userId The user to calculate balance for
     * @return Balance in base currency (positive = owed to user, negative = user owes)
     */
    @Transactional(readOnly = true)
    public BigDecimal getUserBalance(UUID tripId, UUID userId) {
        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("No permission to view this trip");
        }

        var trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId.toString()));

        String baseCurrency = trip.getBaseCurrency();
        List<Expense> expenses = expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId);
        List<ExpenseSplit> splits = expenseSplitRepository.findByTripId(tripId);

        Set<String> conversionWarnings = new HashSet<>();
        Map<UUID, BigDecimal> balances = calculateBalances(expenses, splits, baseCurrency, conversionWarnings);

        return balances.getOrDefault(userId, BigDecimal.ZERO);
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
        evictStatisticsCache(expense.getTripId());

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
        evictStatisticsCache(expense.getTripId());

        log.info("Marked split {} as unsettled", splitId);
    }

    /**
     * Settles all unsettled splits between two users in a trip.
     * Used by the settlement page to settle a simplified transfer.
     *
     * @contract
     *   - pre: tripId, fromUserId, toUserId, currentUserId != null
     *   - pre: current user has edit permission on trip
     *   - post: all unsettled splits between both users (both directions) are marked settled
     *   - calledBy: ExpenseApiController#settleByUsers
     *
     * @param tripId The trip ID
     * @param fromUserId The debtor user ID (who owes)
     * @param toUserId The creditor user ID (who is owed)
     * @param currentUserId The user performing the action
     */
    @Transactional
    public void settleAllBetweenUsers(UUID tripId, UUID fromUserId, UUID toUserId, UUID currentUserId) {
        if (!permissionChecker.canEdit(tripId, currentUserId)) {
            throw new ForbiddenException("No permission to settle");
        }
        // Forward direction: toUser paid, fromUser owes
        List<ExpenseSplit> splits = expenseSplitRepository
                .findUnsettledByTripIdAndUsers(tripId, toUserId, fromUserId);
        splits.forEach(ExpenseSplit::markAsSettled);
        expenseSplitRepository.saveAll(splits);

        // Reverse direction: fromUser paid, toUser owes
        List<ExpenseSplit> reverseSplits = expenseSplitRepository
                .findUnsettledByTripIdAndUsers(tripId, fromUserId, toUserId);
        reverseSplits.forEach(ExpenseSplit::markAsSettled);
        expenseSplitRepository.saveAll(reverseSplits);

        evictStatisticsCache(tripId);
        log.info("Settled {} + {} reverse splits between users {} <-> {} in trip {}",
                splits.size(), reverseSplits.size(), fromUserId, toUserId, tripId);
    }

    /**
     * Unsettles all settled splits between two users in a trip.
     *
     * @contract
     *   - pre: tripId, fromUserId, toUserId, currentUserId != null
     *   - pre: current user has edit permission on trip
     *   - post: all settled splits between both users (both directions) are marked unsettled
     *   - calledBy: ExpenseApiController#unsettleByUsers
     *
     * @param tripId The trip ID
     * @param fromUserId The debtor user ID
     * @param toUserId The creditor user ID
     * @param currentUserId The user performing the action
     */
    @Transactional
    public void unsettleAllBetweenUsers(UUID tripId, UUID fromUserId, UUID toUserId, UUID currentUserId) {
        if (!permissionChecker.canEdit(tripId, currentUserId)) {
            throw new ForbiddenException("No permission to unsettle");
        }
        // Forward direction: toUser paid, fromUser owes
        List<ExpenseSplit> splits = expenseSplitRepository
                .findSettledByTripIdAndUsers(tripId, toUserId, fromUserId);
        splits.forEach(ExpenseSplit::markAsUnsettled);
        expenseSplitRepository.saveAll(splits);

        // Reverse direction: fromUser paid, toUser owes
        List<ExpenseSplit> reverseSplits = expenseSplitRepository
                .findSettledByTripIdAndUsers(tripId, fromUserId, toUserId);
        reverseSplits.forEach(ExpenseSplit::markAsUnsettled);
        expenseSplitRepository.saveAll(reverseSplits);

        evictStatisticsCache(tripId);
        log.info("Unsettled {} + {} reverse splits between users {} <-> {} in trip {}",
                splits.size(), reverseSplits.size(), fromUserId, toUserId, tripId);
    }

    private void evictStatisticsCache(UUID tripId) {
        if (statisticsService != null) {
            statisticsService.evictCaches(tripId);
        }
    }

    /**
     * Calculates net balances for all users involved in expenses.
     * Converts all amounts to base currency for consistent settlement calculation.
     *
     * Only unsettled non-payer splits contribute to balances. The payer's own split
     * is always skipped — a payer never owes themselves. This prevents phantom
     * positive balances after partial settlement.
     *
     * @param expenses List of expenses
     * @param splits List of expense splits
     * @param baseCurrency The base currency to convert to
     * @return Map of user ID to net balance (positive = creditor, negative = debtor)
     */
    private Map<UUID, BigDecimal> calculateBalances(List<Expense> expenses, List<ExpenseSplit> splits, String baseCurrency,
                                                     Set<String> conversionWarnings) {
        Map<UUID, BigDecimal> balances = new HashMap<>();

        // Group splits by expense for easy lookup
        Map<UUID, List<ExpenseSplit>> splitsByExpense = splits.stream()
                .collect(Collectors.groupingBy(ExpenseSplit::getExpenseId));

        for (Expense expense : expenses) {
            UUID payer = expense.getPaidBy();
            String expenseCurrency = expense.getCurrency();

            // Credit the payer only for unsettled non-payer splits.
            // The payer's own split is skipped entirely — you never owe yourself.
            // This prevents phantom balances after partial settlement.
            List<ExpenseSplit> expenseSplits = splitsByExpense.getOrDefault(expense.getId(), List.of());
            BigDecimal payerCredit = BigDecimal.ZERO;
            for (ExpenseSplit split : expenseSplits) {
                if (!split.isSettled() && !split.getUserId().equals(payer)) {
                    BigDecimal splitAmountInBaseCurrency = convertToBaseCurrency(
                            split.getAmount(), expenseCurrency, baseCurrency, conversionWarnings);
                    balances.merge(split.getUserId(), splitAmountInBaseCurrency.negate(), BigDecimal::add);
                    payerCredit = payerCredit.add(splitAmountInBaseCurrency);
                }
            }
            if (payerCredit.compareTo(BigDecimal.ZERO) > 0) {
                balances.merge(payer, payerCredit, BigDecimal::add);
            }
        }

        // Remove zero balances
        balances.entrySet().removeIf(entry ->
                entry.getValue().compareTo(BigDecimal.ZERO) == 0);

        return balances;
    }

    /**
     * Converts an amount from one currency to base currency.
     *
     * @contract
     *   - pre: amount != null, fromCurrency != null, baseCurrency != null
     *   - post: returns amount in base currency
     *   - calledBy: calculateBalances, calculateTotalInBaseCurrency
     *
     * @param amount The amount to convert
     * @param fromCurrency The source currency
     * @param baseCurrency The target base currency
     * @return Amount in base currency
     */
    private BigDecimal convertToBaseCurrency(BigDecimal amount, String fromCurrency, String baseCurrency,
                                               Set<String> conversionWarnings) {
        if (fromCurrency == null || fromCurrency.equals(baseCurrency)) {
            return amount;
        }

        if (exchangeRateService == null) {
            log.warn("ExchangeRateService not available, using original amount for {} -> {}", fromCurrency, baseCurrency);
            conversionWarnings.add(String.format("%s → %s 匯率服務不可用，使用原始金額", fromCurrency, baseCurrency));
            return amount;
        }

        try {
            return exchangeRateService.convert(amount, fromCurrency, baseCurrency);
        } catch (Exception e) {
            log.error("Failed to convert {} {} to {}: {}", amount, fromCurrency, baseCurrency, e.getMessage());
            conversionWarnings.add(String.format("%s → %s 匯率取得失敗，使用原始金額", fromCurrency, baseCurrency));
            return amount;
        }
    }

    /**
     * Calculates the total expenses converted to base currency.
     *
     * @param expenses List of expenses
     * @param baseCurrency The base currency to convert to
     * @return Total amount in base currency
     */
    private BigDecimal calculateTotalInBaseCurrency(List<Expense> expenses, String baseCurrency,
                                                     Set<String> conversionWarnings) {
        return expenses.stream()
                .map(expense -> convertToBaseCurrency(expense.getAmount(), expense.getCurrency(), baseCurrency, conversionWarnings))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates the breakdown of expenses by original currency.
     *
     * @param expenses List of expenses
     * @return Map of currency code to total amount in that currency
     */
    private Map<String, BigDecimal> calculateCurrencyBreakdown(List<Expense> expenses) {
        return expenses.stream()
                .collect(Collectors.groupingBy(
                        expense -> expense.getCurrency() != null ? expense.getCurrency() : "TWD",
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));
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
