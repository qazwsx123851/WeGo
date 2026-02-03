package com.wego.domain.statistics;

import com.wego.entity.Expense;
import com.wego.entity.ExpenseSplit;
import com.wego.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ExpenseAggregator.
 * Tests pure domain logic without external dependencies.
 */
@DisplayName("ExpenseAggregator")
class ExpenseAggregatorTest {

    private ExpenseAggregator aggregator;
    private UUID tripId;
    private UUID user1Id;
    private UUID user2Id;
    private UUID user3Id;

    @BeforeEach
    void setUp() {
        aggregator = new ExpenseAggregator();
        tripId = UUID.randomUUID();
        user1Id = UUID.randomUUID();
        user2Id = UUID.randomUUID();
        user3Id = UUID.randomUUID();
    }

    @Nested
    @DisplayName("aggregateByCategory")
    class AggregateByCategory {

        @Test
        @DisplayName("should return empty list for empty expenses")
        void emptyExpenses_shouldReturnEmptyList() {
            List<CategoryBreakdown> result = aggregator.aggregateByCategory(List.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should aggregate single category correctly")
        void singleCategory_shouldCalculateCorrectly() {
            List<Expense> expenses = List.of(
                createExpense("餐飲", new BigDecimal("100")),
                createExpense("餐飲", new BigDecimal("200"))
            );

            List<CategoryBreakdown> result = aggregator.aggregateByCategory(expenses);

            assertThat(result).hasSize(1);
            CategoryBreakdown breakdown = result.get(0);
            assertThat(breakdown.getCategory()).isEqualTo("餐飲");
            assertThat(breakdown.getAmount()).isEqualByComparingTo(new BigDecimal("300"));
            assertThat(breakdown.getPercentage()).isEqualTo(100.0);
            assertThat(breakdown.getCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should aggregate multiple categories and calculate percentages")
        void multipleCategories_shouldCalculatePercentages() {
            List<Expense> expenses = List.of(
                createExpense("餐飲", new BigDecimal("400")),
                createExpense("交通", new BigDecimal("300")),
                createExpense("住宿", new BigDecimal("200")),
                createExpense("其他", new BigDecimal("100"))
            );

            List<CategoryBreakdown> result = aggregator.aggregateByCategory(expenses);

            assertThat(result).hasSize(4);
            // Should be sorted by amount descending
            assertThat(result.get(0).getCategory()).isEqualTo("餐飲");
            assertThat(result.get(0).getPercentage()).isEqualTo(40.0);
            assertThat(result.get(1).getCategory()).isEqualTo("交通");
            assertThat(result.get(1).getPercentage()).isEqualTo(30.0);
            assertThat(result.get(2).getCategory()).isEqualTo("住宿");
            assertThat(result.get(2).getPercentage()).isEqualTo(20.0);
            assertThat(result.get(3).getCategory()).isEqualTo("其他");
            assertThat(result.get(3).getPercentage()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("should handle null category as '其他'")
        void nullCategory_shouldTreatAsOther() {
            Expense expense = createExpense(null, new BigDecimal("100"));

            List<CategoryBreakdown> result = aggregator.aggregateByCategory(List.of(expense));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo("其他");
        }
    }

    @Nested
    @DisplayName("aggregateByDate")
    class AggregateByDate {

        @Test
        @DisplayName("should return empty list for empty expenses")
        void emptyExpenses_shouldReturnEmptyList() {
            List<TrendDataPoint> result = aggregator.aggregateByDate(List.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should aggregate by expense date")
        void multipleExpenses_shouldAggregateByDate() {
            LocalDate date1 = LocalDate.of(2024, 1, 15);
            LocalDate date2 = LocalDate.of(2024, 1, 16);

            List<Expense> expenses = List.of(
                createExpenseWithDate("餐飲", new BigDecimal("100"), date1),
                createExpenseWithDate("交通", new BigDecimal("200"), date1),
                createExpenseWithDate("住宿", new BigDecimal("500"), date2)
            );

            List<TrendDataPoint> result = aggregator.aggregateByDate(expenses);

            assertThat(result).hasSize(2);
            // Should be sorted by date ascending
            assertThat(result.get(0).getDate()).isEqualTo(date1);
            assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("300"));
            assertThat(result.get(0).getCount()).isEqualTo(2);
            assertThat(result.get(1).getDate()).isEqualTo(date2);
            assertThat(result.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("500"));
            assertThat(result.get(1).getCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should use createdAt when expenseDate is null")
        void nullExpenseDate_shouldUseCreatedAt() {
            Expense expense = createExpense("餐飲", new BigDecimal("100"));
            // expense has no expenseDate but has createdAt

            List<TrendDataPoint> result = aggregator.aggregateByDate(List.of(expense));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("aggregateByMember")
    class AggregateByMember {

        @Test
        @DisplayName("should return empty list for empty inputs")
        void emptyInputs_shouldReturnEmptyList() {
            List<MemberStatistics> result = aggregator.aggregateByMember(
                List.of(), List.of(), Map.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should calculate paid amounts correctly")
        void singlePayer_shouldCalculatePaidAmount() {
            User user = createUser(user1Id, "Alice");
            Expense expense = createExpenseWithPayer("餐飲", new BigDecimal("300"), user1Id);

            List<MemberStatistics> result = aggregator.aggregateByMember(
                List.of(expense),
                List.of(),
                Map.of(user1Id, user)
            );

            assertThat(result).hasSize(1);
            MemberStatistics stats = result.get(0);
            assertThat(stats.getUserId()).isEqualTo(user1Id);
            assertThat(stats.getNickname()).isEqualTo("Alice");
            assertThat(stats.getTotalPaid()).isEqualByComparingTo(new BigDecimal("300"));
        }

        @Test
        @DisplayName("should calculate owed amounts from splits")
        void withSplits_shouldCalculateOwedAmounts() {
            User user1 = createUser(user1Id, "Alice");
            User user2 = createUser(user2Id, "Bob");

            Expense expense = createExpenseWithPayer("餐飲", new BigDecimal("300"), user1Id);
            UUID expenseId = expense.getId();

            ExpenseSplit split1 = createSplit(expenseId, user1Id, new BigDecimal("100"));
            ExpenseSplit split2 = createSplit(expenseId, user2Id, new BigDecimal("200"));

            List<MemberStatistics> result = aggregator.aggregateByMember(
                List.of(expense),
                List.of(split1, split2),
                Map.of(user1Id, user1, user2Id, user2)
            );

            assertThat(result).hasSize(2);

            // Alice: paid 300, owes 100, balance = +200
            MemberStatistics alice = result.stream()
                .filter(s -> s.getUserId().equals(user1Id))
                .findFirst().orElseThrow();
            assertThat(alice.getTotalPaid()).isEqualByComparingTo(new BigDecimal("300"));
            assertThat(alice.getTotalOwed()).isEqualByComparingTo(new BigDecimal("100"));
            assertThat(alice.getBalance()).isEqualByComparingTo(new BigDecimal("200"));

            // Bob: paid 0, owes 200, balance = -200
            MemberStatistics bob = result.stream()
                .filter(s -> s.getUserId().equals(user2Id))
                .findFirst().orElseThrow();
            assertThat(bob.getTotalPaid()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(bob.getTotalOwed()).isEqualByComparingTo(new BigDecimal("200"));
            assertThat(bob.getBalance()).isEqualByComparingTo(new BigDecimal("-200"));
        }

        @Test
        @DisplayName("should sort by balance descending")
        void multipleMembers_shouldSortByBalanceDescending() {
            User user1 = createUser(user1Id, "Alice");
            User user2 = createUser(user2Id, "Bob");
            User user3 = createUser(user3Id, "Charlie");

            Expense expense1 = createExpenseWithPayer("餐飲", new BigDecimal("300"), user1Id);
            Expense expense2 = createExpenseWithPayer("交通", new BigDecimal("600"), user2Id);

            ExpenseSplit split1 = createSplit(expense1.getId(), user1Id, new BigDecimal("100"));
            ExpenseSplit split2 = createSplit(expense1.getId(), user2Id, new BigDecimal("100"));
            ExpenseSplit split3 = createSplit(expense1.getId(), user3Id, new BigDecimal("100"));
            ExpenseSplit split4 = createSplit(expense2.getId(), user1Id, new BigDecimal("200"));
            ExpenseSplit split5 = createSplit(expense2.getId(), user2Id, new BigDecimal("200"));
            ExpenseSplit split6 = createSplit(expense2.getId(), user3Id, new BigDecimal("200"));

            List<MemberStatistics> result = aggregator.aggregateByMember(
                List.of(expense1, expense2),
                List.of(split1, split2, split3, split4, split5, split6),
                Map.of(user1Id, user1, user2Id, user2, user3Id, user3)
            );

            assertThat(result).hasSize(3);
            // Bob: paid 600, owes 300, balance = +300 (highest)
            assertThat(result.get(0).getNickname()).isEqualTo("Bob");
            // Alice: paid 300, owes 300, balance = 0
            assertThat(result.get(1).getNickname()).isEqualTo("Alice");
            // Charlie: paid 0, owes 300, balance = -300 (lowest)
            assertThat(result.get(2).getNickname()).isEqualTo("Charlie");
        }
    }

    // ========== Helper Methods ==========

    private Expense createExpense(String category, BigDecimal amount) {
        Expense expense = new Expense();
        expense.setId(UUID.randomUUID());
        expense.setTripId(tripId);
        expense.setDescription("Test expense");
        expense.setAmount(amount);
        expense.setCategory(category);
        expense.setCurrency("TWD");
        expense.setPaidBy(user1Id);
        expense.setCreatedAt(Instant.now());
        return expense;
    }

    private Expense createExpenseWithDate(String category, BigDecimal amount, LocalDate date) {
        Expense expense = createExpense(category, amount);
        expense.setExpenseDate(date);
        return expense;
    }

    private Expense createExpenseWithPayer(String category, BigDecimal amount, UUID payerId) {
        Expense expense = createExpense(category, amount);
        expense.setPaidBy(payerId);
        return expense;
    }

    private ExpenseSplit createSplit(UUID expenseId, UUID userId, BigDecimal amount) {
        ExpenseSplit split = new ExpenseSplit();
        split.setId(UUID.randomUUID());
        split.setExpenseId(expenseId);
        split.setUserId(userId);
        split.setAmount(amount);
        split.setSettled(false);
        return split;
    }

    private User createUser(UUID userId, String nickname) {
        User user = new User();
        user.setId(userId);
        user.setNickname(nickname);
        user.setEmail(nickname.toLowerCase() + "@test.com");
        return user;
    }
}
