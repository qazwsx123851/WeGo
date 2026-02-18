package com.wego.service;

import com.wego.dto.response.GlobalExpenseOverviewResponse;
import com.wego.dto.response.TripExpenseSummaryResponse;
import com.wego.entity.Trip;
import com.wego.entity.TripMember;
import com.wego.repository.ExpenseRepository;
import com.wego.repository.ExpenseSplitRepository;
import com.wego.repository.TripMemberRepository;
import com.wego.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests for GlobalExpenseService.
 *
 * @contract
 *   - Tests global expense overview across all trips
 *   - Tests unsettled trips listing
 *   - Verifies null safety for aggregate queries
 */
@ExtendWith(MockitoExtension.class)
class GlobalExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseSplitRepository expenseSplitRepository;

    @Mock
    private TripMemberRepository tripMemberRepository;

    @Mock
    private TripRepository tripRepository;

    @InjectMocks
    private GlobalExpenseService globalExpenseService;

    private UUID userId;
    private UUID tripId1;
    private UUID tripId2;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tripId1 = UUID.randomUUID();
        tripId2 = UUID.randomUUID();
    }

    private TripMember createMember(UUID tripId) {
        return TripMember.builder()
                .userId(userId)
                .tripId(tripId)
                .build();
    }

    @Nested
    @DisplayName("getOverview")
    class GetOverviewTests {

        @Test
        @DisplayName("should return empty overview when user has no trips")
        void getOverview_noTrips_shouldReturnEmpty() {
            when(tripMemberRepository.findByUserId(userId)).thenReturn(List.of());

            GlobalExpenseOverviewResponse result = globalExpenseService.getOverview(userId);

            assertThat(result).isNotNull();
            assertThat(result.getTripCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return correct overview with expense data")
        void getOverview_withExpenses_shouldReturnAggregates() {
            List<UUID> tripIds = List.of(tripId1, tripId2);
            when(tripMemberRepository.findByUserId(userId))
                    .thenReturn(List.of(createMember(tripId1), createMember(tripId2)));
            when(expenseRepository.sumAmountPaidByUser(eq(userId), any()))
                    .thenReturn(new BigDecimal("5000"));
            when(expenseSplitRepository.sumUnsettledAmountByUserIdAndTripIds(eq(userId), any()))
                    .thenReturn(new BigDecimal("1200"));
            when(expenseSplitRepository.sumUnsettledAmountOwedToUser(eq(userId), any()))
                    .thenReturn(new BigDecimal("800"));

            GlobalExpenseOverviewResponse result = globalExpenseService.getOverview(userId);

            assertThat(result.getTotalPaid()).isEqualByComparingTo("5000");
            assertThat(result.getTotalOwed()).isEqualByComparingTo("1200");
            assertThat(result.getTotalOwedToUser()).isEqualByComparingTo("800");
            assertThat(result.getNetBalance()).isEqualByComparingTo("-400");
            assertThat(result.getTripCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should handle null aggregate results gracefully")
        void getOverview_nullAggregates_shouldDefaultToZero() {
            when(tripMemberRepository.findByUserId(userId))
                    .thenReturn(List.of(createMember(tripId1)));
            when(expenseRepository.sumAmountPaidByUser(eq(userId), any())).thenReturn(null);
            when(expenseSplitRepository.sumUnsettledAmountByUserIdAndTripIds(eq(userId), any())).thenReturn(null);
            when(expenseSplitRepository.sumUnsettledAmountOwedToUser(eq(userId), any())).thenReturn(null);

            GlobalExpenseOverviewResponse result = globalExpenseService.getOverview(userId);

            assertThat(result.getTotalPaid()).isEqualByComparingTo("0");
            assertThat(result.getTotalOwed()).isEqualByComparingTo("0");
            assertThat(result.getTotalOwedToUser()).isEqualByComparingTo("0");
            assertThat(result.getNetBalance()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("getUnsettledTrips")
    class GetUnsettledTripsTests {

        @Test
        @DisplayName("should return empty list when no unsettled trips")
        void getUnsettledTrips_none_shouldReturnEmpty() {
            when(expenseSplitRepository.findUnsettledTripIdsByUserId(userId))
                    .thenReturn(List.of());

            List<TripExpenseSummaryResponse> result = globalExpenseService.getUnsettledTrips(userId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return unsettled trips sorted by absolute balance")
        void getUnsettledTrips_withData_shouldReturnSortedByAbsBalance() {
            Trip trip1 = Trip.builder().id(tripId1).title("Trip A").build();
            Trip trip2 = Trip.builder().id(tripId2).title("Trip B").build();

            when(expenseSplitRepository.findUnsettledTripIdsByUserId(userId))
                    .thenReturn(List.of(tripId1, tripId2));
            when(tripRepository.findAllById(List.of(tripId1, tripId2)))
                    .thenReturn(List.of(trip1, trip2));

            // Batch query: Trip 1 owedToUser=500, owedByUser=200 -> balance=300
            //              Trip 2 owedToUser=100, owedByUser=1000 -> balance=-900
            when(expenseSplitRepository.sumBalancesByUserAndTripIds(userId, List.of(tripId1, tripId2)))
                    .thenReturn(List.of(
                            new Object[]{tripId1, new BigDecimal("500"), new BigDecimal("200")},
                            new Object[]{tripId2, new BigDecimal("100"), new BigDecimal("1000")}
                    ));

            List<TripExpenseSummaryResponse> result = globalExpenseService.getUnsettledTrips(userId);

            assertThat(result).hasSize(2);
            // Trip B has |balance| = 900 > Trip A |balance| = 300, so Trip B first
            assertThat(result.get(0).getTripTitle()).isEqualTo("Trip B");
            assertThat(result.get(0).getUserBalance()).isEqualByComparingTo("-900");
            assertThat(result.get(1).getTripTitle()).isEqualTo("Trip A");
            assertThat(result.get(1).getUserBalance()).isEqualByComparingTo("300");
        }

        @Test
        @DisplayName("should handle trips missing from batch query gracefully")
        void getUnsettledTrips_missingFromBatch_shouldDefaultToZero() {
            Trip trip = Trip.builder().id(tripId1).title("Trip A").build();

            when(expenseSplitRepository.findUnsettledTripIdsByUserId(userId))
                    .thenReturn(List.of(tripId1));
            when(tripRepository.findAllById(List.of(tripId1)))
                    .thenReturn(List.of(trip));
            // Batch query returns empty (no matching rows)
            when(expenseSplitRepository.sumBalancesByUserAndTripIds(userId, List.of(tripId1)))
                    .thenReturn(List.of());

            List<TripExpenseSummaryResponse> result = globalExpenseService.getUnsettledTrips(userId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserBalance()).isEqualByComparingTo("0");
        }
    }
}
