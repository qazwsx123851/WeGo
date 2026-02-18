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
import com.wego.entity.TripMember;
import com.wego.entity.User;
import com.wego.exception.ResourceNotFoundException;
import com.wego.repository.ExpenseRepository;
import com.wego.repository.ExpenseSplitRepository;
import com.wego.repository.TripMemberRepository;
import com.wego.repository.TripRepository;
import com.wego.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for StatisticsCacheDelegate.
 * Tests cached statistics computation and currency conversion logic.
 */
@ExtendWith(MockitoExtension.class)
class StatisticsCacheDelegateTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseSplitRepository expenseSplitRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripMemberRepository tripMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExpenseAggregator expenseAggregator;

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private StatisticsCacheDelegate cacheDelegate;

    private UUID tripId;
    private Trip trip;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        trip = Trip.builder()
                .id(tripId)
                .title("Test Trip")
                .baseCurrency("TWD")
                .build();
    }

    @Nested
    @DisplayName("getCategoryBreakdown")
    class GetCategoryBreakdown {

        @Test
        @DisplayName("should return category breakdown for trip with expenses")
        void getCategoryBreakdown_withExpenses_shouldReturnBreakdown() {
            Expense expense = new Expense();
            expense.setId(UUID.randomUUID());
            expense.setTripId(tripId);
            expense.setAmount(new BigDecimal("500"));
            expense.setCurrency("TWD");
            expense.setCategory("FOOD");

            List<CategoryBreakdown> breakdowns = List.of(
                    new CategoryBreakdown("FOOD", new BigDecimal("500"), 100.0, 1));

            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(List.of(expense));
            when(expenseAggregator.aggregateByCategory(anyList())).thenReturn(breakdowns);

            CategoryBreakdownResponse response = cacheDelegate.getCategoryBreakdown(tripId);

            assertThat(response).isNotNull();
            assertThat(response.getCategories()).hasSize(1);
            assertThat(response.getCurrency()).isEqualTo("TWD");
        }

        @Test
        @DisplayName("should return empty breakdown when trip has no expenses")
        void getCategoryBreakdown_noExpenses_shouldReturnEmpty() {
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(List.of());
            when(expenseAggregator.aggregateByCategory(anyList())).thenReturn(List.of());

            CategoryBreakdownResponse response = cacheDelegate.getCategoryBreakdown(tripId);

            assertThat(response).isNotNull();
            assertThat(response.getCategories()).isEmpty();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when trip not found")
        void getCategoryBreakdown_tripNotFound_shouldThrow() {
            when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cacheDelegate.getCategoryBreakdown(tripId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should convert foreign currency expenses to base currency")
        void getCategoryBreakdown_foreignCurrency_shouldConvert() {
            Expense foreignExpense = new Expense();
            foreignExpense.setId(UUID.randomUUID());
            foreignExpense.setTripId(tripId);
            foreignExpense.setAmount(new BigDecimal("100"));
            foreignExpense.setCurrency("USD");

            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(List.of(foreignExpense));
            when(exchangeRateService.convert(new BigDecimal("100"), "USD", "TWD"))
                    .thenReturn(new BigDecimal("3100"));
            when(expenseAggregator.aggregateByCategory(anyList())).thenReturn(List.of());

            cacheDelegate.getCategoryBreakdown(tripId);

            verify(exchangeRateService).convert(new BigDecimal("100"), "USD", "TWD");
        }

        @Test
        @DisplayName("should not convert expense with same currency as base")
        void getCategoryBreakdown_sameCurrency_shouldNotConvert() {
            Expense sameExpense = new Expense();
            sameExpense.setId(UUID.randomUUID());
            sameExpense.setTripId(tripId);
            sameExpense.setAmount(new BigDecimal("500"));
            sameExpense.setCurrency("TWD");

            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(List.of(sameExpense));
            when(expenseAggregator.aggregateByCategory(anyList())).thenReturn(List.of());

            cacheDelegate.getCategoryBreakdown(tripId);

            verify(exchangeRateService, never()).convert(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getTrend")
    class GetTrend {

        @Test
        @DisplayName("should return trend data for trip")
        void getTrend_withExpenses_shouldReturnTrend() {
            List<TrendDataPoint> dataPoints = List.of(
                    new TrendDataPoint(LocalDate.of(2026, 1, 1), new BigDecimal("500"), 2));

            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(List.of());
            when(expenseAggregator.aggregateByDate(anyList())).thenReturn(dataPoints);

            TrendResponse response = cacheDelegate.getTrend(tripId);

            assertThat(response).isNotNull();
            assertThat(response.getCurrency()).isEqualTo("TWD");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when trip not found")
        void getTrend_tripNotFound_shouldThrow() {
            when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cacheDelegate.getTrend(tripId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getMemberStatistics")
    class GetMemberStatistics {

        @Test
        @DisplayName("should return member statistics with user data")
        void getMemberStatistics_withMembers_shouldReturn() {
            UUID userId = UUID.randomUUID();
            TripMember member = TripMember.builder()
                    .tripId(tripId)
                    .userId(userId)
                    .build();
            User user = User.builder()
                    .id(userId)
                    .nickname("Alice")
                    .build();

            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(List.of());
            when(expenseSplitRepository.findByTripId(tripId)).thenReturn(List.of());
            when(tripMemberRepository.findByTripId(tripId)).thenReturn(List.of(member));
            when(userRepository.findAllById(List.of(userId))).thenReturn(List.of(user));
            when(expenseAggregator.aggregateByMember(anyList(), anyList(), anyMap()))
                    .thenReturn(List.of());

            MemberStatisticsResponse response = cacheDelegate.getMemberStatistics(tripId);

            assertThat(response).isNotNull();
            assertThat(response.getCurrency()).isEqualTo("TWD");
        }

        @Test
        @DisplayName("should handle empty member list")
        void getMemberStatistics_noMembers_shouldReturnEmpty() {
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(List.of());
            when(expenseSplitRepository.findByTripId(tripId)).thenReturn(List.of());
            when(tripMemberRepository.findByTripId(tripId)).thenReturn(List.of());
            when(expenseAggregator.aggregateByMember(anyList(), anyList(), anyMap()))
                    .thenReturn(List.of());

            MemberStatisticsResponse response = cacheDelegate.getMemberStatistics(tripId);

            assertThat(response.getMembers()).isEmpty();
        }

        @Test
        @DisplayName("should convert splits with foreign currency")
        void getMemberStatistics_foreignCurrencySplits_shouldConvert() {
            UUID expenseId = UUID.randomUUID();
            Expense foreignExpense = new Expense();
            foreignExpense.setId(expenseId);
            foreignExpense.setTripId(tripId);
            foreignExpense.setAmount(new BigDecimal("100"));
            foreignExpense.setCurrency("JPY");

            ExpenseSplit split = new ExpenseSplit();
            split.setId(UUID.randomUUID());
            split.setExpenseId(expenseId);
            split.setUserId(UUID.randomUUID());
            split.setAmount(new BigDecimal("50"));

            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(List.of(foreignExpense));
            when(expenseSplitRepository.findByTripId(tripId)).thenReturn(List.of(split));
            when(tripMemberRepository.findByTripId(tripId)).thenReturn(List.of());
            when(exchangeRateService.convert(new BigDecimal("100"), "JPY", "TWD"))
                    .thenReturn(new BigDecimal("22"));
            when(exchangeRateService.convert(new BigDecimal("50"), "JPY", "TWD"))
                    .thenReturn(new BigDecimal("11"));
            when(expenseAggregator.aggregateByMember(anyList(), anyList(), anyMap()))
                    .thenReturn(List.of());

            cacheDelegate.getMemberStatistics(tripId);

            verify(exchangeRateService).convert(new BigDecimal("100"), "JPY", "TWD");
            verify(exchangeRateService).convert(new BigDecimal("50"), "JPY", "TWD");
        }

        @Test
        @DisplayName("should handle currency conversion failure gracefully")
        void getMemberStatistics_conversionFailure_shouldUseOriginalAmount() {
            Expense foreignExpense = new Expense();
            foreignExpense.setId(UUID.randomUUID());
            foreignExpense.setTripId(tripId);
            foreignExpense.setAmount(new BigDecimal("100"));
            foreignExpense.setCurrency("USD");

            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(List.of(foreignExpense));
            when(expenseSplitRepository.findByTripId(tripId)).thenReturn(List.of());
            when(tripMemberRepository.findByTripId(tripId)).thenReturn(List.of());
            when(exchangeRateService.convert(any(), any(), any()))
                    .thenThrow(new RuntimeException("API error"));
            when(expenseAggregator.aggregateByMember(anyList(), anyList(), anyMap()))
                    .thenReturn(List.of());

            // Should not throw - graceful fallback
            MemberStatisticsResponse response = cacheDelegate.getMemberStatistics(tripId);

            assertThat(response).isNotNull();
        }
    }

    @Nested
    @DisplayName("Currency Conversion Edge Cases")
    class CurrencyConversionEdgeCases {

        @Test
        @DisplayName("should handle null currency on expense")
        void nullCurrency_shouldNotConvert() {
            Expense nullCurrencyExpense = new Expense();
            nullCurrencyExpense.setId(UUID.randomUUID());
            nullCurrencyExpense.setTripId(tripId);
            nullCurrencyExpense.setAmount(new BigDecimal("100"));
            nullCurrencyExpense.setCurrency(null);

            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(List.of(nullCurrencyExpense));
            when(expenseAggregator.aggregateByCategory(anyList())).thenReturn(List.of());

            cacheDelegate.getCategoryBreakdown(tripId);

            verify(exchangeRateService, never()).convert(any(), any(), any());
        }
    }
}
