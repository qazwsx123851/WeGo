package com.wego.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Cache configuration using Caffeine.
 *
 * <p>Cache definitions:
 * <ul>
 *   <li>statistics-category: Category breakdown stats, 5 min TTL</li>
 *   <li>statistics-trend: Expense trend data, 5 min TTL</li>
 *   <li>statistics-members: Member statistics, 5 min TTL</li>
 *   <li>exchange-rate: Exchange rates, 1 hour TTL</li>
 *   <li>exchange-rate-fallback: Fallback rates, 24 hour TTL</li>
 *   <li>settlement: Settlement calculation cache, 1 min TTL</li>
 * </ul>
 *
 * @contract
 *   - pre: none
 *   - post: CacheManager configured with Caffeine caches
 *   - calledBy: Spring auto-configuration
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Creates a cache manager with multiple caches, each with different TTL.
     *
     * @contract
     *   - pre: none
     *   - post: Returns CacheManager with all configured caches
     *   - calledBy: Spring auto-configuration
     *
     * @return Configured CacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                // Statistics caches - 5 minute TTL
                buildCache("statistics-category", 5, TimeUnit.MINUTES, 500),
                buildCache("statistics-trend", 5, TimeUnit.MINUTES, 500),
                buildCache("statistics-members", 5, TimeUnit.MINUTES, 500),

                // Exchange rate caches
                buildCache("exchange-rate", 1, TimeUnit.HOURS, 200),
                buildCache("exchange-rate-fallback", 24, TimeUnit.HOURS, 200),
                buildCache("exchange-rate-all", 1, TimeUnit.HOURS, 50),
                buildCache("exchange-rate-all-fallback", 24, TimeUnit.HOURS, 50),

                // Weather cache - 6 hour TTL
                buildCache("weather", 6, TimeUnit.HOURS, 200),

                // Place search/details cache - 5 minute TTL
                buildCache("places", 5, TimeUnit.MINUTES, 500),

                // Direction cache - 10 minute TTL
                buildCache("directions", 10, TimeUnit.MINUTES, 200),

                // Settlement cache - 1 minute TTL (evicted on expense changes)
                buildCache("settlement", 1, TimeUnit.MINUTES, 200),

                // Permission check cache - 5 second TTL (request-level dedup)
                buildCache("permission-check", 5, TimeUnit.SECONDS, 500)
        ));
        return cacheManager;
    }

    /**
     * Builds a Caffeine cache with specified TTL.
     *
     * @param name Cache name
     * @param duration TTL duration
     * @param unit TTL time unit
     * @param maxSize Maximum cache size
     * @return Configured CaffeineCache
     */
    private CaffeineCache buildCache(String name, long duration, TimeUnit unit, int maxSize) {
        Cache<Object, Object> cache = Caffeine.newBuilder()
                .expireAfterWrite(duration, unit)
                .maximumSize(maxSize)
                .recordStats()
                .build();
        return new CaffeineCache(name, cache);
    }
}
