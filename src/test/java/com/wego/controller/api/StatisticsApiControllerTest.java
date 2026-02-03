package com.wego.controller.api;

import com.wego.dto.response.CategoryBreakdownResponse;
import com.wego.dto.response.MemberStatisticsResponse;
import com.wego.dto.response.TrendResponse;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.service.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for StatisticsApiController.
 *
 * @contract
 *   - Tests all API endpoints
 *   - Verifies authorization behavior
 *   - Tests error handling
 */
@WebMvcTest(StatisticsApiController.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatisticsApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticsService statisticsService;

    private UUID tripId;
    private CategoryBreakdownResponse categoryResponse;
    private TrendResponse trendResponse;
    private MemberStatisticsResponse memberResponse;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();

        // Setup category breakdown response
        CategoryBreakdownResponse.CategoryItem foodItem = CategoryBreakdownResponse.CategoryItem.builder()
                .category("FOOD")
                .amount(new BigDecimal("1000"))
                .percentage(50.0)
                .count(5)
                .color("#F97316")
                .build();

        categoryResponse = CategoryBreakdownResponse.builder()
                .categories(List.of(foodItem))
                .totalAmount(new BigDecimal("2000"))
                .currency("TWD")
                .totalCount(10)
                .build();

        // Setup trend response
        TrendResponse.TrendItem trendItem = TrendResponse.TrendItem.builder()
                .date(LocalDate.of(2026, 1, 1))
                .dateLabel("01/01")
                .amount(new BigDecimal("500"))
                .count(2)
                .build();

        trendResponse = TrendResponse.builder()
                .dataPoints(List.of(trendItem))
                .currency("TWD")
                .totalAmount(new BigDecimal("500"))
                .averagePerDay(new BigDecimal("500"))
                .build();

        // Setup member statistics response
        MemberStatisticsResponse.MemberItem memberItem = MemberStatisticsResponse.MemberItem.builder()
                .userId(UUID.randomUUID())
                .nickname("Alice")
                .totalPaid(new BigDecimal("2000"))
                .totalOwed(new BigDecimal("1000"))
                .balance(new BigDecimal("1000"))
                .expenseCount(5)
                .paidPercentage(50.0)
                .build();

        memberResponse = MemberStatisticsResponse.builder()
                .members(List.of(memberItem))
                .currency("TWD")
                .totalExpenses(new BigDecimal("2000"))
                .memberCount(1)
                .build();
    }

    @Nested
    @DisplayName("GET /api/trips/{tripId}/statistics/category")
    class GetCategoryBreakdown {

        @Test
        @WithMockUser
        @DisplayName("should return category breakdown when authorized")
        void shouldReturnCategoryBreakdownWhenAuthorized() throws Exception {
            // Arrange
            when(statisticsService.getCategoryBreakdown(any(), any())).thenReturn(categoryResponse);

            // Act & Assert
            mockMvc.perform(get("/api/trips/{tripId}/statistics/category", tripId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.categories").isArray())
                    .andExpect(jsonPath("$.data.categories[0].category").value("FOOD"))
                    .andExpect(jsonPath("$.data.totalAmount").value(2000))
                    .andExpect(jsonPath("$.data.currency").value("TWD"))
                    .andExpect(jsonPath("$.data.totalCount").value(10));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 403 when user has no permission")
        void shouldReturn403WhenNoPermission() throws Exception {
            // Arrange
            when(statisticsService.getCategoryBreakdown(any(), any()))
                    .thenThrow(new ForbiddenException("No permission to view this trip"));

            // Act & Assert
            mockMvc.perform(get("/api/trips/{tripId}/statistics/category", tripId))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 404 when trip not found")
        void shouldReturn404WhenTripNotFound() throws Exception {
            // Arrange
            when(statisticsService.getCategoryBreakdown(any(), any()))
                    .thenThrow(new ResourceNotFoundException("Trip", tripId.toString()));

            // Act & Assert
            mockMvc.perform(get("/api/trips/{tripId}/statistics/category", tripId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @WithMockUser
        @DisplayName("should return empty categories when no expenses")
        void shouldReturnEmptyCategoriesWhenNoExpenses() throws Exception {
            // Arrange
            CategoryBreakdownResponse emptyResponse = CategoryBreakdownResponse.builder()
                    .categories(Collections.emptyList())
                    .totalAmount(BigDecimal.ZERO)
                    .currency("TWD")
                    .totalCount(0)
                    .build();
            when(statisticsService.getCategoryBreakdown(any(), any())).thenReturn(emptyResponse);

            // Act & Assert
            mockMvc.perform(get("/api/trips/{tripId}/statistics/category", tripId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.categories").isEmpty())
                    .andExpect(jsonPath("$.data.totalCount").value(0));
        }

        @Test
        @DisplayName("should redirect when not authenticated")
        void shouldRedirectWhenNotAuthenticated() throws Exception {
            // Act & Assert - Spring Security redirects to login page
            mockMvc.perform(get("/api/trips/{tripId}/statistics/category", tripId))
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Nested
    @DisplayName("GET /api/trips/{tripId}/statistics/trend")
    class GetTrend {

        @Test
        @WithMockUser
        @DisplayName("should return trend data when authorized")
        void shouldReturnTrendDataWhenAuthorized() throws Exception {
            // Arrange
            when(statisticsService.getTrend(any(), any())).thenReturn(trendResponse);

            // Act & Assert
            mockMvc.perform(get("/api/trips/{tripId}/statistics/trend", tripId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.dataPoints").isArray())
                    .andExpect(jsonPath("$.data.dataPoints[0].dateLabel").value("01/01"))
                    .andExpect(jsonPath("$.data.currency").value("TWD"));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 403 when user has no permission")
        void shouldReturn403WhenNoPermission() throws Exception {
            // Arrange
            when(statisticsService.getTrend(any(), any()))
                    .thenThrow(new ForbiddenException("No permission to view this trip"));

            // Act & Assert
            mockMvc.perform(get("/api/trips/{tripId}/statistics/trend", tripId))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @WithMockUser
        @DisplayName("should return empty data points when no expenses")
        void shouldReturnEmptyDataPointsWhenNoExpenses() throws Exception {
            // Arrange
            TrendResponse emptyResponse = TrendResponse.builder()
                    .dataPoints(Collections.emptyList())
                    .currency("TWD")
                    .totalAmount(BigDecimal.ZERO)
                    .averagePerDay(BigDecimal.ZERO)
                    .build();
            when(statisticsService.getTrend(any(), any())).thenReturn(emptyResponse);

            // Act & Assert
            mockMvc.perform(get("/api/trips/{tripId}/statistics/trend", tripId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.dataPoints").isEmpty());
        }

        @Test
        @DisplayName("should redirect when not authenticated")
        void shouldRedirectWhenNotAuthenticated() throws Exception {
            // Act & Assert - Spring Security redirects to login page
            mockMvc.perform(get("/api/trips/{tripId}/statistics/trend", tripId))
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Nested
    @DisplayName("GET /api/trips/{tripId}/statistics/members")
    class GetMemberStatistics {

        @Test
        @WithMockUser
        @DisplayName("should return member statistics when authorized")
        void shouldReturnMemberStatisticsWhenAuthorized() throws Exception {
            // Arrange
            when(statisticsService.getMemberStatistics(any(), any())).thenReturn(memberResponse);

            // Act & Assert
            mockMvc.perform(get("/api/trips/{tripId}/statistics/members", tripId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.members").isArray())
                    .andExpect(jsonPath("$.data.members[0].nickname").value("Alice"))
                    .andExpect(jsonPath("$.data.members[0].balance").value(1000))
                    .andExpect(jsonPath("$.data.currency").value("TWD"));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 403 when user has no permission")
        void shouldReturn403WhenNoPermission() throws Exception {
            // Arrange
            when(statisticsService.getMemberStatistics(any(), any()))
                    .thenThrow(new ForbiddenException("No permission to view this trip"));

            // Act & Assert
            mockMvc.perform(get("/api/trips/{tripId}/statistics/members", tripId))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @WithMockUser
        @DisplayName("should return empty members when no trip members")
        void shouldReturnEmptyMembersWhenNoTripMembers() throws Exception {
            // Arrange
            MemberStatisticsResponse emptyResponse = MemberStatisticsResponse.builder()
                    .members(Collections.emptyList())
                    .currency("TWD")
                    .build();
            when(statisticsService.getMemberStatistics(any(), any())).thenReturn(emptyResponse);

            // Act & Assert
            mockMvc.perform(get("/api/trips/{tripId}/statistics/members", tripId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.members").isEmpty());
        }

        @Test
        @DisplayName("should redirect when not authenticated")
        void shouldRedirectWhenNotAuthenticated() throws Exception {
            // Act & Assert - Spring Security redirects to login page
            mockMvc.perform(get("/api/trips/{tripId}/statistics/members", tripId))
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Nested
    @DisplayName("Invalid Trip ID")
    class InvalidTripId {

        @Test
        @WithMockUser
        @DisplayName("should return 400 when trip ID is invalid")
        void shouldReturn400WhenTripIdIsInvalid() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/trips/{tripId}/statistics/category", "invalid-uuid"))
                    .andExpect(status().isBadRequest());
        }
    }
}
