package com.wego.service;

import com.wego.domain.permission.PermissionChecker;
import com.wego.domain.statistics.CategoryBreakdown;
import com.wego.domain.statistics.ExpenseAggregator;
import com.wego.domain.statistics.MemberStatistics;
import com.wego.domain.statistics.TrendDataPoint;
import com.wego.dto.response.CategoryBreakdownResponse;
import com.wego.dto.response.MemberStatisticsResponse;
import com.wego.dto.response.TrendResponse;
import com.wego.entity.Expense;
import com.wego.entity.ExpenseSplit;
import com.wego.entity.Trip;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.repository.ExpenseRepository;
import com.wego.repository.ExpenseSplitRepository;
import com.wego.repository.TripMemberRepository;
import com.wego.repository.TripRepository;
import com.wego.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for expense statistics and analytics.
 *
 * @contract
 *   - pre: User must have view permission on trip
 *   - post: Returns aggregated statistics
 *   - calledBy: StatisticsApiController
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserRepository userRepository;
    private final PermissionChecker permissionChecker;
    private final ExpenseAggregator expenseAggregator;
    private final ExchangeRateService exchangeRateService;

    /**
     * Gets category breakdown statistics for a trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - pre: user has view permission on trip
     *   - post: Returns category breakdown sorted by amount desc
     *   - calledBy: StatisticsApiController#getCategoryBreakdown
     *
     * @param tripId The trip ID
     * @param userId The requesting user's ID
     * @return Category breakdown response
     * @throws ForbiddenException if user has no view permission
     * @throws ResourceNotFoundException if trip not found
     */
    @Transactional(readOnly = true)
    public CategoryBreakdownResponse getCategoryBreakdown(UUID tripId, UUID userId) {
        // Authorization check MUST happen before any data access (including cache)
        validateViewPermission(tripId, userId);
        return getCategoryBreakdownCached(tripId);
    }

    /**
     * Internal cached method for category breakdown.
     * Authorization is handled by the public method.
     */
    @Cacheable(value = "statistics-category", key = "#tripId")
    protected CategoryBreakdownResponse getCategoryBreakdownCached(UUID tripId) {
        log.debug("Getting category breakdown for trip {}", tripId);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId.toString()));

        String baseCurrency = trip.getBaseCurrency();
        List<Expense> expenses = expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId);

        // Convert all expenses to base currency for aggregation
        List<Expense> convertedExpenses = convertExpensesToBaseCurrency(expenses, baseCurrency);

        List<CategoryBreakdown> breakdowns = expenseAggregator.aggregateByCategory(convertedExpenses);

        log.debug("Found {} categories for trip {}", breakdowns.size(), tripId);
        return CategoryBreakdownResponse.from(breakdowns, baseCurrency);
    }

    /**
     * Gets expense trend data for a trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - pre: user has view permission on trip
     *   - post: Returns trend data sorted by date ascending
     *   - calledBy: StatisticsApiController#getTrend
     *
     * @param tripId The trip ID
     * @param userId The requesting user's ID
     * @return Trend response
     * @throws ForbiddenException if user has no view permission
     * @throws ResourceNotFoundException if trip not found
     */
    @Transactional(readOnly = true)
    public TrendResponse getTrend(UUID tripId, UUID userId) {
        // Authorization check MUST happen before any data access (including cache)
        validateViewPermission(tripId, userId);
        return getTrendCached(tripId);
    }

    /**
     * Internal cached method for trend data.
     * Authorization is handled by the public method.
     */
    @Cacheable(value = "statistics-trend", key = "#tripId")
    protected TrendResponse getTrendCached(UUID tripId) {
        log.debug("Getting expense trend for trip {}", tripId);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId.toString()));

        String baseCurrency = trip.getBaseCurrency();
        List<Expense> expenses = expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId);

        // Convert all expenses to base currency for aggregation
        List<Expense> convertedExpenses = convertExpensesToBaseCurrency(expenses, baseCurrency);

        List<TrendDataPoint> dataPoints = expenseAggregator.aggregateByDate(convertedExpenses);

        log.debug("Found {} data points for trip {}", dataPoints.size(), tripId);
        return TrendResponse.from(dataPoints, baseCurrency);
    }

    /**
     * Gets member statistics for a trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - pre: user has view permission on trip
     *   - post: Returns member statistics sorted by balance desc
     *   - calledBy: StatisticsApiController#getMemberStatistics
     *
     * @param tripId The trip ID
     * @param userId The requesting user's ID
     * @return Member statistics response
     * @throws ForbiddenException if user has no view permission
     * @throws ResourceNotFoundException if trip not found
     */
    @Transactional(readOnly = true)
    public MemberStatisticsResponse getMemberStatistics(UUID tripId, UUID userId) {
        // Authorization check MUST happen before any data access (including cache)
        validateViewPermission(tripId, userId);
        return getMemberStatisticsCached(tripId);
    }

    /**
     * Internal cached method for member statistics.
     * Authorization is handled by the public method.
     */
    @Cacheable(value = "statistics-members", key = "#tripId")
    protected MemberStatisticsResponse getMemberStatisticsCached(UUID tripId) {
        log.debug("Getting member statistics for trip {}", tripId);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId.toString()));

        String baseCurrency = trip.getBaseCurrency();

        List<Expense> expenses = expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId);
        List<ExpenseSplit> splits = expenseSplitRepository.findByTripId(tripId);

        // Convert amounts to base currency
        List<Expense> convertedExpenses = convertExpensesToBaseCurrency(expenses, baseCurrency);
        List<ExpenseSplit> convertedSplits = convertSplitsToBaseCurrency(splits, expenses, baseCurrency);

        // Get user map for all involved users
        List<UUID> userIds = tripMemberRepository.findByTripId(tripId).stream()
                .map(m -> m.getUserId())
                .collect(Collectors.toList());
        Map<UUID, User> userMap = getUserMap(userIds);

        List<MemberStatistics> statistics = expenseAggregator.aggregateByMember(
                convertedExpenses, convertedSplits, userMap);

        log.debug("Found {} member statistics for trip {}", statistics.size(), tripId);
        return MemberStatisticsResponse.from(statistics, baseCurrency);
    }

    /**
     * Evicts all statistics caches for a trip.
     * Call this when expenses are created, updated, or deleted.
     *
     * @contract
     *   - pre: tripId != null
     *   - post: All statistics caches for trip are cleared
     *   - calledBy: ExpenseService#createExpense, updateExpense, deleteExpense
     *
     * @param tripId The trip ID
     */
    @CacheEvict(value = {"statistics-category", "statistics-trend", "statistics-members"}, key = "#tripId")
    public void evictCaches(UUID tripId) {
        log.debug("Evicting statistics caches for trip {}", tripId);
    }

    // ========== Private Methods ==========

    private void validateViewPermission(UUID tripId, UUID userId) {
        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("No permission to view this trip");
        }
    }

    /**
     * Converts expenses to base currency for aggregation.
     * Creates new Expense objects with converted amounts.
     */
    private List<Expense> convertExpensesToBaseCurrency(List<Expense> expenses, String baseCurrency) {
        return expenses.stream()
                .map(expense -> {
                    if (expense.getCurrency() == null || expense.getCurrency().equals(baseCurrency)) {
                        return expense;
                    }

                    BigDecimal convertedAmount = convertToBaseCurrency(
                            expense.getAmount(), expense.getCurrency(), baseCurrency);

                    // Create a new expense with converted amount (immutable approach)
                    Expense converted = new Expense();
                    converted.setId(expense.getId());
                    converted.setTripId(expense.getTripId());
                    converted.setDescription(expense.getDescription());
                    converted.setAmount(convertedAmount);
                    converted.setCategory(expense.getCategory());
                    converted.setCurrency(baseCurrency);
                    converted.setPaidBy(expense.getPaidBy());
                    converted.setExpenseDate(expense.getExpenseDate());
                    converted.setCreatedAt(expense.getCreatedAt());
                    converted.setCreatedBy(expense.getCreatedBy());
                    return converted;
                })
                .collect(Collectors.toList());
    }

    /**
     * Converts splits to base currency for aggregation.
     */
    private List<ExpenseSplit> convertSplitsToBaseCurrency(
            List<ExpenseSplit> splits,
            List<Expense> expenses,
            String baseCurrency) {

        // Create expense ID to currency map
        Map<UUID, String> expenseCurrencyMap = expenses.stream()
                .collect(Collectors.toMap(Expense::getId, e -> e.getCurrency() != null ? e.getCurrency() : baseCurrency));

        return splits.stream()
                .map(split -> {
                    String splitCurrency = expenseCurrencyMap.getOrDefault(split.getExpenseId(), baseCurrency);
                    if (splitCurrency.equals(baseCurrency)) {
                        return split;
                    }

                    BigDecimal convertedAmount = convertToBaseCurrency(
                            split.getAmount(), splitCurrency, baseCurrency);

                    // Create a new split with converted amount (immutable approach)
                    ExpenseSplit converted = new ExpenseSplit();
                    converted.setId(split.getId());
                    converted.setExpenseId(split.getExpenseId());
                    converted.setUserId(split.getUserId());
                    converted.setAmount(convertedAmount);
                    converted.setSettled(split.isSettled());
                    return converted;
                })
                .collect(Collectors.toList());
    }

    private BigDecimal convertToBaseCurrency(BigDecimal amount, String fromCurrency, String baseCurrency) {
        if (fromCurrency == null || fromCurrency.equals(baseCurrency)) {
            return amount;
        }

        if (exchangeRateService == null) {
            log.warn("ExchangeRateService not available, using original amount");
            return amount;
        }

        try {
            return exchangeRateService.convert(amount, fromCurrency, baseCurrency);
        } catch (Exception e) {
            log.error("Failed to convert {} {} to {}: {}", amount, fromCurrency, baseCurrency, e.getMessage());
            return amount;
        }
    }

    private Map<UUID, User> getUserMap(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
