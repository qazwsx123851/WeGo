package com.wego.service;

import com.wego.dto.request.CreateExpenseRequest;
import com.wego.dto.response.ExpenseResponse;
import com.wego.entity.SplitType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseViewHelperTest {

    private final ExpenseViewHelper helper = new ExpenseViewHelper();

    @Nested
    @DisplayName("groupExpensesByDate")
    class GroupExpensesByDate {

        @Test
        @DisplayName("empty list returns empty map")
        void emptyList() {
            Map<LocalDate, List<ExpenseResponse>> result =
                    helper.groupExpensesByDate(Collections.emptyList());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("multiple expenses on same date grouped together")
        void sameDate() {
            LocalDate date = LocalDate.of(2025, 3, 1);
            ExpenseResponse e1 = ExpenseResponse.builder().expenseDate(date).build();
            ExpenseResponse e2 = ExpenseResponse.builder().expenseDate(date).build();

            Map<LocalDate, List<ExpenseResponse>> result =
                    helper.groupExpensesByDate(List.of(e1, e2));

            assertThat(result).hasSize(1);
            assertThat(result.get(date)).containsExactly(e1, e2);
        }

        @Test
        @DisplayName("multiple dates ordered newest first (reverse chronological)")
        void newestFirst() {
            LocalDate older = LocalDate.of(2025, 1, 1);
            LocalDate newer = LocalDate.of(2025, 2, 1);
            ExpenseResponse e1 = ExpenseResponse.builder().expenseDate(older).build();
            ExpenseResponse e2 = ExpenseResponse.builder().expenseDate(newer).build();

            Map<LocalDate, List<ExpenseResponse>> result =
                    helper.groupExpensesByDate(List.of(e1, e2));

            List<LocalDate> keys = List.copyOf(result.keySet());
            assertThat(keys).containsExactly(newer, older);
        }

        @Test
        @DisplayName("null expenseDate falls back to createdAt")
        void fallbackToCreatedAt() {
            Instant createdAt = Instant.parse("2025-04-15T10:00:00Z");
            ExpenseResponse expense = ExpenseResponse.builder()
                    .expenseDate(null)
                    .createdAt(createdAt)
                    .build();

            Map<LocalDate, List<ExpenseResponse>> result =
                    helper.groupExpensesByDate(List.of(expense));

            // The date depends on system timezone, but should not be null
            assertThat(result).hasSize(1);
            assertThat(result.values().iterator().next()).containsExactly(expense);
        }

        @Test
        @DisplayName("null expenseDate and null createdAt uses today")
        void fallbackToToday() {
            ExpenseResponse expense = ExpenseResponse.builder()
                    .expenseDate(null)
                    .createdAt(null)
                    .build();

            Map<LocalDate, List<ExpenseResponse>> result =
                    helper.groupExpensesByDate(List.of(expense));

            assertThat(result).hasSize(1);
            assertThat(result).containsKey(LocalDate.now());
            assertThat(result.get(LocalDate.now())).containsExactly(expense);
        }
    }

    @Nested
    @DisplayName("calculatePerPersonAverage")
    class CalculatePerPersonAverage {

        @Test
        @DisplayName("normal division: 300 / 3 = 100")
        void normalDivision() {
            BigDecimal result = helper.calculatePerPersonAverage(new BigDecimal("300"), 3);

            assertThat(result).isEqualByComparingTo(new BigDecimal("100"));
        }

        @Test
        @DisplayName("zero members returns ZERO")
        void zeroMembers() {
            BigDecimal result = helper.calculatePerPersonAverage(new BigDecimal("300"), 0);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("zero total returns ZERO")
        void zeroTotal() {
            BigDecimal result = helper.calculatePerPersonAverage(BigDecimal.ZERO, 3);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("non-even division: 100 / 3 = 33 (HALF_UP rounding)")
        void nonEvenDivision() {
            BigDecimal result = helper.calculatePerPersonAverage(new BigDecimal("100"), 3);

            assertThat(result).isEqualByComparingTo(new BigDecimal("33"));
        }
    }

    @Nested
    @DisplayName("parseSplitType")
    class ParseSplitType {

        @Test
        @DisplayName("null returns EQUAL")
        void nullReturnsEqual() {
            assertThat(helper.parseSplitType(null)).isEqualTo(SplitType.EQUAL);
        }

        @Test
        @DisplayName("PERCENTAGE returns PERCENTAGE")
        void percentage() {
            assertThat(helper.parseSplitType("PERCENTAGE")).isEqualTo(SplitType.PERCENTAGE);
        }

        @Test
        @DisplayName("CUSTOM returns CUSTOM")
        void custom() {
            assertThat(helper.parseSplitType("CUSTOM")).isEqualTo(SplitType.CUSTOM);
        }

        @Test
        @DisplayName("SHARES returns SHARES")
        void shares() {
            assertThat(helper.parseSplitType("SHARES")).isEqualTo(SplitType.SHARES);
        }

        @Test
        @DisplayName("lowercase equal returns EQUAL")
        void lowercaseEqual() {
            assertThat(helper.parseSplitType("equal")).isEqualTo(SplitType.EQUAL);
        }

        @Test
        @DisplayName("UNKNOWN returns EQUAL")
        void unknownReturnsEqual() {
            assertThat(helper.parseSplitType("UNKNOWN")).isEqualTo(SplitType.EQUAL);
        }
    }

    @Nested
    @DisplayName("buildSplits")
    class BuildSplits {

        private final UUID tripId = UUID.randomUUID();
        private final BigDecimal totalAmount = new BigDecimal("300");

        @Test
        @DisplayName("EQUAL with participants returns splits for each participant")
        void equalWithParticipants() {
            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();
            List<UUID> participants = List.of(user1, user2);

            List<CreateExpenseRequest.SplitRequest> result =
                    helper.buildSplits(SplitType.EQUAL, participants, null, null, tripId, totalAmount);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getUserId()).isEqualTo(user1);
            assertThat(result.get(1).getUserId()).isEqualTo(user2);
        }

        @Test
        @DisplayName("EQUAL with null participants returns empty list")
        void equalWithNullParticipants() {
            List<CreateExpenseRequest.SplitRequest> result =
                    helper.buildSplits(SplitType.EQUAL, null, null, null, tripId, totalAmount);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("PERCENTAGE with valid entries returns splits with percentages")
        void percentageWithValidEntries() {
            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();
            Map<String, String> percentages = Map.of(
                    user1.toString(), "60",
                    user2.toString(), "40"
            );

            List<CreateExpenseRequest.SplitRequest> result =
                    helper.buildSplits(SplitType.PERCENTAGE, null, percentages, null, tripId, totalAmount);

            assertThat(result).hasSize(2);
            assertThat(result).allSatisfy(split -> {
                assertThat(split.getUserId()).isNotNull();
                assertThat(split.getPercentage()).isNotNull();
            });
        }

        @Test
        @DisplayName("PERCENTAGE with zero percentage skips that entry")
        void percentageWithZeroSkipped() {
            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();
            Map<String, String> percentages = Map.of(
                    user1.toString(), "100",
                    user2.toString(), "0"
            );

            List<CreateExpenseRequest.SplitRequest> result =
                    helper.buildSplits(SplitType.PERCENTAGE, null, percentages, null, tripId, totalAmount);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(user1);
        }

        @Test
        @DisplayName("CUSTOM with valid amounts returns splits with amounts")
        void customWithValidAmounts() {
            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();
            Map<String, String> customAmounts = Map.of(
                    user1.toString(), "200",
                    user2.toString(), "100"
            );

            List<CreateExpenseRequest.SplitRequest> result =
                    helper.buildSplits(SplitType.CUSTOM, null, null, customAmounts, tripId, totalAmount);

            assertThat(result).hasSize(2);
            assertThat(result).allSatisfy(split -> {
                assertThat(split.getUserId()).isNotNull();
                assertThat(split.getAmount()).isNotNull();
            });
        }

        @Test
        @DisplayName("CUSTOM with invalid UUID key skips that entry")
        void customWithInvalidUuidSkipped() {
            UUID validUser = UUID.randomUUID();
            Map<String, String> customAmounts = Map.of(
                    validUser.toString(), "200",
                    "not-a-uuid", "100"
            );

            List<CreateExpenseRequest.SplitRequest> result =
                    helper.buildSplits(SplitType.CUSTOM, null, null, customAmounts, tripId, totalAmount);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(validUser);
        }

        @Test
        @DisplayName("SHARES returns empty list (not implemented)")
        void sharesReturnsEmptyList() {
            UUID user1 = UUID.randomUUID();
            List<UUID> participants = List.of(user1);

            List<CreateExpenseRequest.SplitRequest> result =
                    helper.buildSplits(SplitType.SHARES, participants, null, null, tripId, totalAmount);

            assertThat(result).isEmpty();
        }
    }
}
