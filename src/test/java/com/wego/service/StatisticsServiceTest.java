package com.wego.service;

import com.wego.domain.permission.PermissionChecker;
import com.wego.domain.statistics.CategoryBreakdown;
import com.wego.domain.statistics.MemberStatistics;
import com.wego.domain.statistics.TrendDataPoint;
import com.wego.dto.response.CategoryBreakdownResponse;
import com.wego.dto.response.MemberStatisticsResponse;
import com.wego.dto.response.TrendResponse;
import com.wego.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for StatisticsService.
 * Tests permission checking and delegation to StatisticsCacheDelegate.
 */
@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private PermissionChecker permissionChecker;

    @Mock
    private StatisticsCacheDelegate cacheDelegate;

    private StatisticsService statisticsService;

    private UUID tripId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        statisticsService = new StatisticsService(permissionChecker, cacheDelegate);
        tripId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("getCategoryBreakdown")
    class GetCategoryBreakdown {

        @Test
        @DisplayName("should return category breakdown when authorized")
        void shouldReturnCategoryBreakdownWhenAuthorized() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);

            List<CategoryBreakdown> breakdowns = List.of(
                    new CategoryBreakdown("FOOD", new BigDecimal("1000"), 50.0, 5),
                    new CategoryBreakdown("TRANSPORT", new BigDecimal("1000"), 50.0, 3)
            );
            CategoryBreakdownResponse expected = CategoryBreakdownResponse.from(breakdowns, "TWD");
            when(cacheDelegate.getCategoryBreakdown(tripId)).thenReturn(expected);

            CategoryBreakdownResponse response = statisticsService.getCategoryBreakdown(tripId, userId);

            assertThat(response).isNotNull();
            assertThat(response.getCategories()).hasSize(2);
            assertThat(response.getCurrency()).isEqualTo("TWD");
            verify(permissionChecker).canView(tripId, userId);
            verify(cacheDelegate).getCategoryBreakdown(tripId);
        }

        @Test
        @DisplayName("should throw ForbiddenException when not authorized")
        void shouldThrowForbiddenExceptionWhenNotAuthorized() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            assertThatThrownBy(() -> statisticsService.getCategoryBreakdown(tripId, userId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("No permission to view this trip");

            verify(cacheDelegate, never()).getCategoryBreakdown(any());
        }

        @Test
        @DisplayName("should return empty categories when no expenses")
        void shouldReturnEmptyCategoriesWhenNoExpenses() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            CategoryBreakdownResponse empty = CategoryBreakdownResponse.from(List.of(), "TWD");
            when(cacheDelegate.getCategoryBreakdown(tripId)).thenReturn(empty);

            CategoryBreakdownResponse response = statisticsService.getCategoryBreakdown(tripId, userId);

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
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);

            List<TrendDataPoint> dataPoints = List.of(
                    new TrendDataPoint(LocalDate.of(2026, 1, 1), new BigDecimal("500"), 2),
                    new TrendDataPoint(LocalDate.of(2026, 1, 2), new BigDecimal("800"), 3)
            );
            TrendResponse expected = TrendResponse.from(dataPoints, "TWD");
            when(cacheDelegate.getTrend(tripId)).thenReturn(expected);

            TrendResponse response = statisticsService.getTrend(tripId, userId);

            assertThat(response).isNotNull();
            assertThat(response.getDataPoints()).hasSize(2);
            assertThat(response.getCurrency()).isEqualTo("TWD");
            verify(permissionChecker).canView(tripId, userId);
            verify(cacheDelegate).getTrend(tripId);
        }

        @Test
        @DisplayName("should throw ForbiddenException when not authorized")
        void shouldThrowForbiddenExceptionWhenNotAuthorized() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            assertThatThrownBy(() -> statisticsService.getTrend(tripId, userId))
                    .isInstanceOf(ForbiddenException.class);

            verify(cacheDelegate, never()).getTrend(any());
        }

        @Test
        @DisplayName("should return empty data points when no expenses")
        void shouldReturnEmptyDataPointsWhenNoExpenses() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            TrendResponse empty = TrendResponse.from(List.of(), "TWD");
            when(cacheDelegate.getTrend(tripId)).thenReturn(empty);

            TrendResponse response = statisticsService.getTrend(tripId, userId);

            assertThat(response.getDataPoints()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getMemberStatistics")
    class GetMemberStatistics {

        @Test
        @DisplayName("should return member statistics when authorized")
        void shouldReturnMemberStatisticsWhenAuthorized() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);

            UUID user2Id = UUID.randomUUID();
            List<MemberStatistics> stats = List.of(
                    new MemberStatistics(userId, "Alice", null,
                            new BigDecimal("2000"), new BigDecimal("1000"), new BigDecimal("1000"), 5, false),
                    new MemberStatistics(user2Id, "Bob", null,
                            new BigDecimal("1000"), new BigDecimal("2000"), new BigDecimal("-1000"), 3, false)
            );
            MemberStatisticsResponse expected = MemberStatisticsResponse.from(stats, "TWD");
            when(cacheDelegate.getMemberStatistics(tripId)).thenReturn(expected);

            MemberStatisticsResponse response = statisticsService.getMemberStatistics(tripId, userId);

            assertThat(response).isNotNull();
            assertThat(response.getMembers()).hasSize(2);
            assertThat(response.getCurrency()).isEqualTo("TWD");
            verify(permissionChecker).canView(tripId, userId);
            verify(cacheDelegate).getMemberStatistics(tripId);
        }

        @Test
        @DisplayName("should throw ForbiddenException when not authorized")
        void shouldThrowForbiddenExceptionWhenNotAuthorized() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            assertThatThrownBy(() -> statisticsService.getMemberStatistics(tripId, userId))
                    .isInstanceOf(ForbiddenException.class);

            verify(cacheDelegate, never()).getMemberStatistics(any());
        }

        @Test
        @DisplayName("should return empty members when no trip members")
        void shouldReturnEmptyMembersWhenNoTripMembers() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            MemberStatisticsResponse empty = MemberStatisticsResponse.from(List.of(), "TWD");
            when(cacheDelegate.getMemberStatistics(tripId)).thenReturn(empty);

            MemberStatisticsResponse response = statisticsService.getMemberStatistics(tripId, userId);

            assertThat(response.getMembers()).isEmpty();
        }
    }

    @Nested
    @DisplayName("evictCaches")
    class EvictCaches {

        @Test
        @DisplayName("should evict caches without errors")
        void shouldEvictCachesWithoutErrors() {
            statisticsService.evictCaches(tripId);
            // No assertions needed - just verify no exception
        }
    }

    @Nested
    @DisplayName("Authorization First Principle")
    class AuthorizationFirstPrinciple {

        @Test
        @DisplayName("getCategoryBreakdown should check authorization before delegation")
        void getCategoryBreakdownShouldCheckAuthorizationBeforeDelegation() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            assertThatThrownBy(() -> statisticsService.getCategoryBreakdown(tripId, userId))
                    .isInstanceOf(ForbiddenException.class);

            verify(cacheDelegate, never()).getCategoryBreakdown(any());
        }

        @Test
        @DisplayName("getTrend should check authorization before delegation")
        void getTrendShouldCheckAuthorizationBeforeDelegation() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            assertThatThrownBy(() -> statisticsService.getTrend(tripId, userId))
                    .isInstanceOf(ForbiddenException.class);

            verify(cacheDelegate, never()).getTrend(any());
        }

        @Test
        @DisplayName("getMemberStatistics should check authorization before delegation")
        void getMemberStatisticsShouldCheckAuthorizationBeforeDelegation() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            assertThatThrownBy(() -> statisticsService.getMemberStatistics(tripId, userId))
                    .isInstanceOf(ForbiddenException.class);

            verify(cacheDelegate, never()).getMemberStatistics(any());
        }
    }
}
