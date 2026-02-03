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
import com.wego.entity.TripMember;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
 * Unit tests for StatisticsService.
 *
 * @contract
 *   - Tests follow TDD methodology
 *   - Covers all public methods
 *   - Tests authorization and edge cases
 */
@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

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
    private PermissionChecker permissionChecker;

    @Mock
    private ExpenseAggregator expenseAggregator;

    @Mock
    private ExchangeRateService exchangeRateService;

    private StatisticsService statisticsService;

    private UUID tripId;
    private UUID userId;
    private UUID user2Id;
    private Trip testTrip;
    private User testUser;
    private User testUser2;

    @BeforeEach
    void setUp() {
        statisticsService = new StatisticsService(
                expenseRepository,
                expenseSplitRepository,
                tripRepository,
                tripMemberRepository,
                userRepository,
                permissionChecker,
                expenseAggregator,
                exchangeRateService
        );

        tripId = UUID.randomUUID();
        userId = UUID.randomUUID();
        user2Id = UUID.randomUUID();

        testTrip = new Trip();
        testTrip.setId(tripId);
        testTrip.setTitle("Test Trip");
        testTrip.setBaseCurrency("TWD");

        testUser = new User();
        testUser.setId(userId);
        testUser.setNickname("Alice");
        testUser.setEmail("alice@test.com");

        testUser2 = new User();
        testUser2.setId(user2Id);
        testUser2.setNickname("Bob");
        testUser2.setEmail("bob@test.com");
    }

    @Nested
    @DisplayName("getCategoryBreakdown")
    class GetCategoryBreakdown {

        @Test
        @DisplayName("should return category breakdown when authorized")
        void shouldReturnCategoryBreakdownWhenAuthorized() {
            // Arrange
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(Collections.emptyList());

            List<CategoryBreakdown> breakdowns = List.of(
                    new CategoryBreakdown("FOOD", new BigDecimal("1000"), 50.0, 5),
                    new CategoryBreakdown("TRANSPORT", new BigDecimal("1000"), 50.0, 3)
            );
            when(expenseAggregator.aggregateByCategory(anyList())).thenReturn(breakdowns);

            // Act
            CategoryBreakdownResponse response = statisticsService.getCategoryBreakdown(tripId, userId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getCategories()).hasSize(2);
            assertThat(response.getCurrency()).isEqualTo("TWD");
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("2000"));
            assertThat(response.getTotalCount()).isEqualTo(8);

            verify(permissionChecker).canView(tripId, userId);
            verify(tripRepository).findById(tripId);
        }

        @Test
        @DisplayName("should throw ForbiddenException when not authorized")
        void shouldThrowForbiddenExceptionWhenNotAuthorized() {
            // Arrange
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> statisticsService.getCategoryBreakdown(tripId, userId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("No permission to view this trip");

            verify(tripRepository, never()).findById(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when trip not found")
        void shouldThrowResourceNotFoundExceptionWhenTripNotFound() {
            // Arrange
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> statisticsService.getCategoryBreakdown(tripId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should return empty categories when no expenses")
        void shouldReturnEmptyCategoriesWhenNoExpenses() {
            // Arrange
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(Collections.emptyList());
            when(expenseAggregator.aggregateByCategory(anyList())).thenReturn(Collections.emptyList());

            // Act
            CategoryBreakdownResponse response = statisticsService.getCategoryBreakdown(tripId, userId);

            // Assert
            assertThat(response.getCategories()).isEmpty();
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getTotalCount()).isZero();
        }
    }

    @Nested
    @DisplayName("getTrend")
    class GetTrend {

        @Test
        @DisplayName("should return trend data when authorized")
        void shouldReturnTrendDataWhenAuthorized() {
            // Arrange
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(Collections.emptyList());

            List<TrendDataPoint> dataPoints = List.of(
                    new TrendDataPoint(LocalDate.of(2026, 1, 1), new BigDecimal("500"), 2),
                    new TrendDataPoint(LocalDate.of(2026, 1, 2), new BigDecimal("800"), 3)
            );
            when(expenseAggregator.aggregateByDate(anyList())).thenReturn(dataPoints);

            // Act
            TrendResponse response = statisticsService.getTrend(tripId, userId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getDataPoints()).hasSize(2);
            assertThat(response.getCurrency()).isEqualTo("TWD");

            verify(permissionChecker).canView(tripId, userId);
        }

        @Test
        @DisplayName("should throw ForbiddenException when not authorized")
        void shouldThrowForbiddenExceptionWhenNotAuthorized() {
            // Arrange
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> statisticsService.getTrend(tripId, userId))
                    .isInstanceOf(ForbiddenException.class);

            verify(tripRepository, never()).findById(any());
        }

        @Test
        @DisplayName("should return empty data points when no expenses")
        void shouldReturnEmptyDataPointsWhenNoExpenses() {
            // Arrange
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(Collections.emptyList());
            when(expenseAggregator.aggregateByDate(anyList())).thenReturn(Collections.emptyList());

            // Act
            TrendResponse response = statisticsService.getTrend(tripId, userId);

            // Assert
            assertThat(response.getDataPoints()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getMemberStatistics")
    class GetMemberStatistics {

        @Test
        @DisplayName("should return member statistics when authorized")
        void shouldReturnMemberStatisticsWhenAuthorized() {
            // Arrange
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(Collections.emptyList());
            when(expenseSplitRepository.findByTripId(tripId)).thenReturn(Collections.emptyList());

            TripMember member1 = new TripMember();
            member1.setUserId(userId);
            TripMember member2 = new TripMember();
            member2.setUserId(user2Id);
            when(tripMemberRepository.findByTripId(tripId)).thenReturn(List.of(member1, member2));
            when(userRepository.findAllById(List.of(userId, user2Id)))
                    .thenReturn(List.of(testUser, testUser2));

            List<MemberStatistics> stats = List.of(
                    new MemberStatistics(userId, "Alice", null,
                            new BigDecimal("2000"), new BigDecimal("1000"), 5),
                    new MemberStatistics(user2Id, "Bob", null,
                            new BigDecimal("1000"), new BigDecimal("2000"), 3)
            );
            when(expenseAggregator.aggregateByMember(anyList(), anyList(), anyMap())).thenReturn(stats);

            // Act
            MemberStatisticsResponse response = statisticsService.getMemberStatistics(tripId, userId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getMembers()).hasSize(2);
            assertThat(response.getCurrency()).isEqualTo("TWD");

            verify(permissionChecker).canView(tripId, userId);
            verify(tripMemberRepository).findByTripId(tripId);
        }

        @Test
        @DisplayName("should throw ForbiddenException when not authorized")
        void shouldThrowForbiddenExceptionWhenNotAuthorized() {
            // Arrange
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> statisticsService.getMemberStatistics(tripId, userId))
                    .isInstanceOf(ForbiddenException.class);

            verify(tripRepository, never()).findById(any());
        }

        @Test
        @DisplayName("should return empty members when no trip members")
        void shouldReturnEmptyMembersWhenNoTripMembers() {
            // Arrange
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(Collections.emptyList());
            when(expenseSplitRepository.findByTripId(tripId)).thenReturn(Collections.emptyList());
            when(tripMemberRepository.findByTripId(tripId)).thenReturn(Collections.emptyList());
            when(expenseAggregator.aggregateByMember(anyList(), anyList(), anyMap()))
                    .thenReturn(Collections.emptyList());

            // Act
            MemberStatisticsResponse response = statisticsService.getMemberStatistics(tripId, userId);

            // Assert
            assertThat(response.getMembers()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Currency Conversion")
    class CurrencyConversion {

        @Test
        @DisplayName("should convert expenses to base currency")
        void shouldConvertExpensesToBaseCurrency() {
            // Arrange
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

            Expense usdExpense = new Expense();
            usdExpense.setId(UUID.randomUUID());
            usdExpense.setTripId(tripId);
            usdExpense.setAmount(new BigDecimal("100"));
            usdExpense.setCurrency("USD");
            usdExpense.setCategory("FOOD");
            usdExpense.setPaidBy(userId);

            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(List.of(usdExpense));

            // Mock exchange rate conversion: 100 USD -> 3150 TWD
            when(exchangeRateService.convert(
                    new BigDecimal("100"), "USD", "TWD"))
                    .thenReturn(new BigDecimal("3150"));

            when(expenseAggregator.aggregateByCategory(anyList()))
                    .thenReturn(Collections.emptyList());

            // Act
            statisticsService.getCategoryBreakdown(tripId, userId);

            // Assert
            verify(exchangeRateService).convert(new BigDecimal("100"), "USD", "TWD");
        }

        @Test
        @DisplayName("should not convert when currency matches base currency")
        void shouldNotConvertWhenCurrencyMatchesBaseCurrency() {
            // Arrange
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

            Expense twdExpense = new Expense();
            twdExpense.setId(UUID.randomUUID());
            twdExpense.setTripId(tripId);
            twdExpense.setAmount(new BigDecimal("1000"));
            twdExpense.setCurrency("TWD");
            twdExpense.setCategory("FOOD");
            twdExpense.setPaidBy(userId);

            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(List.of(twdExpense));
            when(expenseAggregator.aggregateByCategory(anyList()))
                    .thenReturn(Collections.emptyList());

            // Act
            statisticsService.getCategoryBreakdown(tripId, userId);

            // Assert - exchangeRateService should not be called
            verify(exchangeRateService, never()).convert(any(), any(), any());
        }

        @Test
        @DisplayName("should use original amount when conversion fails")
        void shouldUseOriginalAmountWhenConversionFails() {
            // Arrange
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

            Expense usdExpense = new Expense();
            usdExpense.setId(UUID.randomUUID());
            usdExpense.setTripId(tripId);
            usdExpense.setAmount(new BigDecimal("100"));
            usdExpense.setCurrency("USD");
            usdExpense.setCategory("FOOD");
            usdExpense.setPaidBy(userId);

            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(List.of(usdExpense));

            // Mock conversion failure
            when(exchangeRateService.convert(any(), any(), any()))
                    .thenThrow(new RuntimeException("API unavailable"));

            when(expenseAggregator.aggregateByCategory(anyList()))
                    .thenReturn(Collections.emptyList());

            // Act - should not throw
            CategoryBreakdownResponse response = statisticsService.getCategoryBreakdown(tripId, userId);

            // Assert
            assertThat(response).isNotNull();
        }
    }

    @Nested
    @DisplayName("evictCaches")
    class EvictCaches {

        @Test
        @DisplayName("should evict caches without errors")
        void shouldEvictCachesWithoutErrors() {
            // Act - should not throw
            statisticsService.evictCaches(tripId);

            // No assertions needed - just verify no exception
        }
    }

    @Nested
    @DisplayName("Authorization First Principle")
    class AuthorizationFirstPrinciple {

        @Test
        @DisplayName("getCategoryBreakdown should check authorization before data access")
        void getCategoryBreakdownShouldCheckAuthorizationBeforeDataAccess() {
            // Arrange
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> statisticsService.getCategoryBreakdown(tripId, userId))
                    .isInstanceOf(ForbiddenException.class);

            // Verify no data access happened
            verify(tripRepository, never()).findById(any());
            verify(expenseRepository, never()).findByTripIdOrderByCreatedAtDesc(any());
        }

        @Test
        @DisplayName("getTrend should check authorization before data access")
        void getTrendShouldCheckAuthorizationBeforeDataAccess() {
            // Arrange
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> statisticsService.getTrend(tripId, userId))
                    .isInstanceOf(ForbiddenException.class);

            // Verify no data access happened
            verify(tripRepository, never()).findById(any());
            verify(expenseRepository, never()).findByTripIdOrderByCreatedAtDesc(any());
        }

        @Test
        @DisplayName("getMemberStatistics should check authorization before data access")
        void getMemberStatisticsShouldCheckAuthorizationBeforeDataAccess() {
            // Arrange
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> statisticsService.getMemberStatistics(tripId, userId))
                    .isInstanceOf(ForbiddenException.class);

            // Verify no data access happened
            verify(tripRepository, never()).findById(any());
            verify(expenseRepository, never()).findByTripIdOrderByCreatedAtDesc(any());
            verify(tripMemberRepository, never()).findByTripId(any());
        }
    }
}
