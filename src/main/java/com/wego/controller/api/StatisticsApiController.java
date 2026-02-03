package com.wego.controller.api;

import com.wego.dto.ApiResponse;
import com.wego.dto.response.CategoryBreakdownResponse;
import com.wego.dto.response.MemberStatisticsResponse;
import com.wego.dto.response.TrendResponse;
import com.wego.security.CurrentUser;
import com.wego.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API controller for expense statistics.
 *
 * @contract
 *   - pre: User must be authenticated
 *   - pre: User must have view permission on trip
 *   - calledBy: Frontend JavaScript (expense-statistics.js)
 */
@Slf4j
@RestController
@RequestMapping("/api/trips/{tripId}/statistics")
@RequiredArgsConstructor
public class StatisticsApiController {

    private final StatisticsService statisticsService;

    /**
     * Gets category breakdown statistics for a trip.
     *
     * @contract
     *   - pre: tripId is valid UUID
     *   - pre: user has view permission on trip
     *   - post: Returns category breakdown with percentages
     *   - calledBy: expense-statistics.js#loadCategoryChart
     *
     * @param tripId The trip ID
     * @param currentUser The authenticated user
     * @return Category breakdown response
     */
    @GetMapping("/category")
    public ResponseEntity<ApiResponse<CategoryBreakdownResponse>> getCategoryBreakdown(
            @PathVariable UUID tripId,
            @CurrentUser UUID currentUser) {

        log.debug("GET /api/trips/{}/statistics/category by user {}", tripId, currentUser);

        CategoryBreakdownResponse response = statisticsService.getCategoryBreakdown(tripId, currentUser);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Gets expense trend data for a trip.
     *
     * @contract
     *   - pre: tripId is valid UUID
     *   - pre: user has view permission on trip
     *   - post: Returns daily expense trend
     *   - calledBy: expense-statistics.js#loadTrendChart
     *
     * @param tripId The trip ID
     * @param currentUser The authenticated user
     * @return Trend response
     */
    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<TrendResponse>> getTrend(
            @PathVariable UUID tripId,
            @CurrentUser UUID currentUser) {

        log.debug("GET /api/trips/{}/statistics/trend by user {}", tripId, currentUser);

        TrendResponse response = statisticsService.getTrend(tripId, currentUser);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Gets member statistics for a trip.
     *
     * @contract
     *   - pre: tripId is valid UUID
     *   - pre: user has view permission on trip
     *   - post: Returns member statistics with balances
     *   - calledBy: expense-statistics.js#loadMemberChart
     *
     * @param tripId The trip ID
     * @param currentUser The authenticated user
     * @return Member statistics response
     */
    @GetMapping("/members")
    public ResponseEntity<ApiResponse<MemberStatisticsResponse>> getMemberStatistics(
            @PathVariable UUID tripId,
            @CurrentUser UUID currentUser) {

        log.debug("GET /api/trips/{}/statistics/members by user {}", tripId, currentUser);

        MemberStatisticsResponse response = statisticsService.getMemberStatistics(tripId, currentUser);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
