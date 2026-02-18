package com.wego.service;

import com.wego.domain.permission.PermissionChecker;
import com.wego.dto.response.CategoryBreakdownResponse;
import com.wego.dto.response.MemberStatisticsResponse;
import com.wego.dto.response.TrendResponse;
import com.wego.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for expense statistics and analytics.
 * Delegates cached computations to StatisticsCacheDelegate
 * to avoid Spring AOP proxy bypass on self-invocation.
 *
 * @contract
 *   - pre: User must have view permission on trip
 *   - post: Returns aggregated statistics
 *   - calledBy: StatisticsApiController
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final PermissionChecker permissionChecker;
    private final StatisticsCacheDelegate cacheDelegate;

    @Transactional(readOnly = true)
    public CategoryBreakdownResponse getCategoryBreakdown(UUID tripId, UUID userId) {
        validateViewPermission(tripId, userId);
        return cacheDelegate.getCategoryBreakdown(tripId);
    }

    @Transactional(readOnly = true)
    public TrendResponse getTrend(UUID tripId, UUID userId) {
        validateViewPermission(tripId, userId);
        return cacheDelegate.getTrend(tripId);
    }

    @Transactional(readOnly = true)
    public MemberStatisticsResponse getMemberStatistics(UUID tripId, UUID userId) {
        validateViewPermission(tripId, userId);
        return cacheDelegate.getMemberStatistics(tripId);
    }

    /**
     * Evicts all statistics caches for a trip.
     * Call this when expenses are created, updated, or deleted.
     */
    @CacheEvict(value = {"statistics-category", "statistics-trend", "statistics-members"}, key = "#tripId")
    public void evictCaches(UUID tripId) {
        log.debug("Evicting statistics caches for trip {}", tripId);
    }

    private void validateViewPermission(UUID tripId, UUID userId) {
        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("No permission to view this trip");
        }
    }
}
