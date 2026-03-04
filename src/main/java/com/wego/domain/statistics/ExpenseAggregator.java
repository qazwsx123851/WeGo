package com.wego.domain.statistics;

import com.wego.entity.Expense;
import com.wego.entity.ExpenseSplit;
import com.wego.service.ParticipantResolver.ParticipantInfo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Domain service for aggregating expense statistics.
 * Pure calculation logic without external dependencies.
 *
 * @contract
 *   - pre: Input lists must not be null
 *   - post: Results are sorted appropriately for each method
 *   - calledBy: StatisticsService
 */
@Component
public class ExpenseAggregator {

    private static final String DEFAULT_CATEGORY = "其他";

    /**
     * Aggregates expenses by category.
     *
     * @contract
     *   - pre: expenses != null
     *   - post: Returns list sorted by amount descending
     *   - post: Percentages sum to 100 (with rounding tolerance)
     *   - calledBy: StatisticsService#getCategoryBreakdown
     *
     * @param expenses List of expenses to aggregate
     * @return List of CategoryBreakdown sorted by amount descending
     */
    public List<CategoryBreakdown> aggregateByCategory(List<Expense> expenses) {
        Objects.requireNonNull(expenses, "expenses must not be null");

        if (expenses.isEmpty()) {
            return List.of();
        }

        // Calculate total amount
        BigDecimal totalAmount = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group by category
        Map<String, List<Expense>> byCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory() != null ? e.getCategory() : DEFAULT_CATEGORY
                ));

        // Create breakdowns
        List<CategoryBreakdown> breakdowns = byCategory.entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    List<Expense> categoryExpenses = entry.getValue();
                    BigDecimal amount = categoryExpenses.stream()
                            .map(Expense::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    double percentage = totalAmount.compareTo(BigDecimal.ZERO) > 0
                            ? amount.divide(totalAmount, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .doubleValue()
                            : 0.0;
                    return new CategoryBreakdown(category, amount, percentage, categoryExpenses.size());
                })
                .sorted(Comparator.comparing(CategoryBreakdown::getAmount).reversed())
                .collect(Collectors.toList());

        return breakdowns;
    }

    /**
     * Aggregates expenses by date for trend analysis.
     *
     * @contract
     *   - pre: expenses != null
     *   - post: Returns list sorted by date ascending
     *   - calledBy: StatisticsService#getTrend
     *
     * @param expenses List of expenses to aggregate
     * @return List of TrendDataPoint sorted by date ascending
     */
    public List<TrendDataPoint> aggregateByDate(List<Expense> expenses) {
        Objects.requireNonNull(expenses, "expenses must not be null");

        if (expenses.isEmpty()) {
            return List.of();
        }

        // Group by date (use expenseDate if available, otherwise createdAt)
        Map<LocalDate, List<Expense>> byDate = expenses.stream()
                .collect(Collectors.groupingBy(this::getExpenseDate));

        // Create data points
        List<TrendDataPoint> dataPoints = byDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<Expense> dateExpenses = entry.getValue();
                    BigDecimal amount = dateExpenses.stream()
                            .map(Expense::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new TrendDataPoint(date, amount, dateExpenses.size());
                })
                .sorted(Comparator.comparing(TrendDataPoint::getDate))
                .collect(Collectors.toList());

        return dataPoints;
    }

    /**
     * Aggregates statistics by member.
     *
     * @contract
     *   - pre: All parameters != null
     *   - post: Returns list sorted by balance descending
     *   - calledBy: StatisticsService#getMemberStatistics
     *
     * @param expenses List of expenses
     * @param splits List of expense splits
     * @param participantMap Map of participant ID to ParticipantInfo (real users + ghost members)
     * @return List of MemberStatistics sorted by balance descending
     */
    public List<MemberStatistics> aggregateByMember(
            List<Expense> expenses,
            List<ExpenseSplit> splits,
            Map<UUID, ParticipantInfo> participantMap) {

        Objects.requireNonNull(expenses, "expenses must not be null");
        Objects.requireNonNull(splits, "splits must not be null");
        Objects.requireNonNull(participantMap, "participantMap must not be null");

        if (expenses.isEmpty() && splits.isEmpty()) {
            return List.of();
        }

        // Calculate total paid by each user
        Map<UUID, BigDecimal> paidByUser = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getPaidBy,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        // Calculate expense count by user
        Map<UUID, Long> countByUser = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getPaidBy, Collectors.counting()));

        // Calculate total owed by each user (from splits)
        Map<UUID, BigDecimal> owedByUser = splits.stream()
                .collect(Collectors.groupingBy(
                        ExpenseSplit::getUserId,
                        Collectors.reducing(BigDecimal.ZERO, ExpenseSplit::getAmount, BigDecimal::add)
                ));

        // Calculate unsettled balances (skipping payer's own splits)
        Map<UUID, BigDecimal> unsettledBalances = new HashMap<>();
        Map<UUID, List<ExpenseSplit>> splitsByExpense = splits.stream()
                .collect(Collectors.groupingBy(ExpenseSplit::getExpenseId));

        for (Expense expense : expenses) {
            UUID payer = expense.getPaidBy();
            List<ExpenseSplit> expenseSplits = splitsByExpense.getOrDefault(expense.getId(), List.of());
            BigDecimal payerCredit = BigDecimal.ZERO;
            for (ExpenseSplit split : expenseSplits) {
                if (!split.isSettled() && !split.getUserId().equals(payer)) {
                    unsettledBalances.merge(split.getUserId(), split.getAmount().negate(), BigDecimal::add);
                    payerCredit = payerCredit.add(split.getAmount());
                }
            }
            if (payerCredit.compareTo(BigDecimal.ZERO) > 0) {
                unsettledBalances.merge(payer, payerCredit, BigDecimal::add);
            }
        }

        // Combine all user IDs
        Set<UUID> allUserIds = new HashSet<>();
        allUserIds.addAll(paidByUser.keySet());
        allUserIds.addAll(owedByUser.keySet());

        // Also include participants from the map (e.g. members with 0 expenses)
        allUserIds.addAll(participantMap.keySet());

        // Create member statistics
        List<MemberStatistics> statistics = allUserIds.stream()
                .filter(participantMap::containsKey)
                .map(userId -> {
                    ParticipantInfo info = participantMap.get(userId);
                    BigDecimal totalPaid = paidByUser.getOrDefault(userId, BigDecimal.ZERO);
                    BigDecimal totalOwed = owedByUser.getOrDefault(userId, BigDecimal.ZERO);
                    BigDecimal unsettledBalance = unsettledBalances.getOrDefault(userId, BigDecimal.ZERO);
                    int expenseCount = countByUser.getOrDefault(userId, 0L).intValue();

                    return new MemberStatistics(
                            userId,
                            info.nickname(),
                            info.avatarUrl(),
                            totalPaid,
                            totalOwed,
                            unsettledBalance,
                            expenseCount,
                            info.isGhost()
                    );
                })
                .sorted(Comparator.comparing(MemberStatistics::getBalance).reversed())
                .collect(Collectors.toList());

        return statistics;
    }

    /**
     * Gets the date for an expense.
     * Uses expenseDate if available, otherwise falls back to createdAt.
     */
    private LocalDate getExpenseDate(Expense expense) {
        if (expense.getExpenseDate() != null) {
            return expense.getExpenseDate();
        }
        if (expense.getCreatedAt() != null) {
            return expense.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.now();
    }
}
