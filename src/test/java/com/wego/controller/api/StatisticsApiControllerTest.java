package com.wego.controller.api;

import com.wego.dto.response.CategoryBreakdownResponse;
import com.wego.dto.response.MemberStatisticsResponse;
import com.wego.dto.response.TrendResponse;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.security.UserPrincipal;
import com.wego.service.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
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
@Import(TestSecurityConfig.class)
class StatisticsApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticsService statisticsService;

    private UUID tripId;
    private UserPrincipal userPrincipal;
    private CategoryBreakdownResponse categoryResponse;
    private TrendResponse trendResponse;
    private MemberStatisticsResponse memberResponse;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();

        User testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .nickname("Test User")
                .provider("test")
                .providerId("test-id")
                .build();
        userPrincipal = new UserPrincipal(testUser);

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
        @DisplayName("should return category breakdown when authorized")
        void shouldReturnCategoryBreakdownWhenAuthorized() throws Exception {
            when(statisticsService.getCategoryBreakdown(any(), any())).thenReturn(categoryResponse);

            mockMvc.perform(get("/api/trips/{tripId}/statistics/category", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.categories").isArray())
                    .andExpect(jsonPath("$.data.categories[0].category").value("FOOD"))
                    .andExpect(jsonPath("$.data.totalAmount").value(2000))
                    .andExpect(jsonPath("$.data.currency").value("TWD"))
                    .andExpect(jsonPath("$.data.totalCount").value(10));
        }

        @Test
        @DisplayName("should return 403 when user has no permission")
        void shouldReturn403WhenNoPermission() throws Exception {
            when(statisticsService.getCategoryBreakdown(any(), any()))
                    .thenThrow(new ForbiddenException("No permission to view this trip"));

            mockMvc.perform(get("/api/trips/{tripId}/statistics/category", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should return 404 when trip not found")
        void shouldReturn404WhenTripNotFound() throws Exception {
            when(statisticsService.getCategoryBreakdown(any(), any()))
                    .thenThrow(new ResourceNotFoundException("Trip", tripId.toString()));

            mockMvc.perform(get("/api/trips/{tripId}/statistics/category", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should return empty categories when no expenses")
        void shouldReturnEmptyCategoriesWhenNoExpenses() throws Exception {
            CategoryBreakdownResponse emptyResponse = CategoryBreakdownResponse.builder()
                    .categories(Collections.emptyList())
                    .totalAmount(BigDecimal.ZERO)
                    .currency("TWD")
                    .totalCount(0)
                    .build();
            when(statisticsService.getCategoryBreakdown(any(), any())).thenReturn(emptyResponse);

            mockMvc.perform(get("/api/trips/{tripId}/statistics/category", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.categories").isEmpty())
                    .andExpect(jsonPath("$.data.totalCount").value(0));
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void shouldReturn403WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/trips/{tripId}/statistics/category", tripId))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/trips/{tripId}/statistics/trend")
    class GetTrend {

        @Test
        @DisplayName("should return trend data when authorized")
        void shouldReturnTrendDataWhenAuthorized() throws Exception {
            when(statisticsService.getTrend(any(), any())).thenReturn(trendResponse);

            mockMvc.perform(get("/api/trips/{tripId}/statistics/trend", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.dataPoints").isArray())
                    .andExpect(jsonPath("$.data.dataPoints[0].dateLabel").value("01/01"))
                    .andExpect(jsonPath("$.data.currency").value("TWD"));
        }

        @Test
        @DisplayName("should return 403 when user has no permission")
        void shouldReturn403WhenNoPermission() throws Exception {
            when(statisticsService.getTrend(any(), any()))
                    .thenThrow(new ForbiddenException("No permission to view this trip"));

            mockMvc.perform(get("/api/trips/{tripId}/statistics/trend", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should return empty data points when no expenses")
        void shouldReturnEmptyDataPointsWhenNoExpenses() throws Exception {
            TrendResponse emptyResponse = TrendResponse.builder()
                    .dataPoints(Collections.emptyList())
                    .currency("TWD")
                    .totalAmount(BigDecimal.ZERO)
                    .averagePerDay(BigDecimal.ZERO)
                    .build();
            when(statisticsService.getTrend(any(), any())).thenReturn(emptyResponse);

            mockMvc.perform(get("/api/trips/{tripId}/statistics/trend", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.dataPoints").isEmpty());
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void shouldReturn403WhenNotAuthenticated_trend() throws Exception {
            mockMvc.perform(get("/api/trips/{tripId}/statistics/trend", tripId))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/trips/{tripId}/statistics/members")
    class GetMemberStatistics {

        @Test
        @DisplayName("should return member statistics when authorized")
        void shouldReturnMemberStatisticsWhenAuthorized() throws Exception {
            when(statisticsService.getMemberStatistics(any(), any())).thenReturn(memberResponse);

            mockMvc.perform(get("/api/trips/{tripId}/statistics/members", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.members").isArray())
                    .andExpect(jsonPath("$.data.members[0].nickname").value("Alice"))
                    .andExpect(jsonPath("$.data.members[0].balance").value(1000))
                    .andExpect(jsonPath("$.data.currency").value("TWD"));
        }

        @Test
        @DisplayName("should return 403 when user has no permission")
        void shouldReturn403WhenNoPermission() throws Exception {
            when(statisticsService.getMemberStatistics(any(), any()))
                    .thenThrow(new ForbiddenException("No permission to view this trip"));

            mockMvc.perform(get("/api/trips/{tripId}/statistics/members", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should return empty members when no trip members")
        void shouldReturnEmptyMembersWhenNoTripMembers() throws Exception {
            MemberStatisticsResponse emptyResponse = MemberStatisticsResponse.builder()
                    .members(Collections.emptyList())
                    .currency("TWD")
                    .build();
            when(statisticsService.getMemberStatistics(any(), any())).thenReturn(emptyResponse);

            mockMvc.perform(get("/api/trips/{tripId}/statistics/members", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.members").isEmpty());
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void shouldReturn403WhenNotAuthenticated_members() throws Exception {
            mockMvc.perform(get("/api/trips/{tripId}/statistics/members", tripId))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Invalid Trip ID")
    class InvalidTripId {

        @Test
        @DisplayName("should return 400 when trip ID is invalid")
        void shouldReturn400WhenTripIdIsInvalid() throws Exception {
            mockMvc.perform(get("/api/trips/{tripId}/statistics/category", "invalid-uuid")
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest());
        }
    }
}
