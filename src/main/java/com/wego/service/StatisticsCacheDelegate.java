package com.wego.service;

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
import com.wego.exception.ResourceNotFoundException;
import com.wego.repository.ExpenseRepository;
import com.wego.repository.ExpenseSplitRepository;
import com.wego.repository.TripMemberRepository;
import com.wego.repository.TripRepository;
import com.wego.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Delegate bean for statistics cache operations.
 * Extracted from StatisticsService to fix Spring AOP proxy bypass
 * on self-invoked @Cacheable methods.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class StatisticsCacheDelegate {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserRepository userRepository;
    private final ExpenseAggregator expenseAggregator;
    private final ExchangeRateService exchangeRateService;

    @Cacheable(value = "statistics-category", key = "#tripId")
    @Transactional(readOnly = true)
    public CategoryBreakdownResponse getCategoryBreakdown(UUID tripId) {
        log.debug("Getting category breakdown for trip {}", tripId);

        Trip trip = findTrip(tripId);
        String baseCurrency = trip.getBaseCurrency();
        List<Expense> expenses = expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId);
        List<Expense> convertedExpenses = convertExpensesToBaseCurrency(expenses, baseCurrency);
        List<CategoryBreakdown> breakdowns = expenseAggregator.aggregateByCategory(convertedExpenses);

        log.debug("Found {} categories for trip {}", breakdowns.size(), tripId);
        return CategoryBreakdownResponse.from(breakdowns, baseCurrency);
    }

    @Cacheable(value = "statistics-trend", key = "#tripId")
    @Transactional(readOnly = true)
    public TrendResponse getTrend(UUID tripId) {
        log.debug("Getting expense trend for trip {}", tripId);

        Trip trip = findTrip(tripId);
        String baseCurrency = trip.getBaseCurrency();
        List<Expense> expenses = expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId);
        List<Expense> convertedExpenses = convertExpensesToBaseCurrency(expenses, baseCurrency);
        List<TrendDataPoint> dataPoints = expenseAggregator.aggregateByDate(convertedExpenses);

        log.debug("Found {} data points for trip {}", dataPoints.size(), tripId);
        return TrendResponse.from(dataPoints, baseCurrency);
    }

    @Cacheable(value = "statistics-members", key = "#tripId")
    @Transactional(readOnly = true)
    public MemberStatisticsResponse getMemberStatistics(UUID tripId) {
        log.debug("Getting member statistics for trip {}", tripId);

        Trip trip = findTrip(tripId);
        String baseCurrency = trip.getBaseCurrency();

        List<Expense> expenses = expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId);
        List<ExpenseSplit> splits = expenseSplitRepository.findByTripId(tripId);

        List<Expense> convertedExpenses = convertExpensesToBaseCurrency(expenses, baseCurrency);
        List<ExpenseSplit> convertedSplits = convertSplitsToBaseCurrency(splits, expenses, baseCurrency);

        List<UUID> userIds = tripMemberRepository.findByTripId(tripId).stream()
                .map(m -> m.getUserId())
                .collect(Collectors.toList());
        Map<UUID, User> userMap = getUserMap(userIds);

        List<MemberStatistics> statistics = expenseAggregator.aggregateByMember(
                convertedExpenses, convertedSplits, userMap);

        log.debug("Found {} member statistics for trip {}", statistics.size(), tripId);
        return MemberStatisticsResponse.from(statistics, baseCurrency);
    }

    // ========== Private Methods ==========

    private Trip findTrip(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId.toString()));
    }

    private List<Expense> convertExpensesToBaseCurrency(List<Expense> expenses, String baseCurrency) {
        return expenses.stream()
                .map(expense -> {
                    if (expense.getCurrency() == null || expense.getCurrency().equals(baseCurrency)) {
                        return expense;
                    }
                    BigDecimal convertedAmount = convertToBaseCurrency(
                            expense.getAmount(), expense.getCurrency(), baseCurrency);
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

    private List<ExpenseSplit> convertSplitsToBaseCurrency(
            List<ExpenseSplit> splits, List<Expense> expenses, String baseCurrency) {
        Map<UUID, String> expenseCurrencyMap = expenses.stream()
                .collect(Collectors.toMap(Expense::getId,
                        e -> e.getCurrency() != null ? e.getCurrency() : baseCurrency));
        return splits.stream()
                .map(split -> {
                    String splitCurrency = expenseCurrencyMap.getOrDefault(split.getExpenseId(), baseCurrency);
                    if (splitCurrency.equals(baseCurrency)) {
                        return split;
                    }
                    BigDecimal convertedAmount = convertToBaseCurrency(
                            split.getAmount(), splitCurrency, baseCurrency);
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
