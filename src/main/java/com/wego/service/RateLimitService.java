package com.wego.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiting service using a sliding window algorithm.
 * Uses Caffeine cache for automatic eviction to prevent unbounded memory growth.
 *
 * @contract
 *   - pre: key != null
 *   - post: Returns true if request is allowed, false if rate limited
 *   - throws: None (fails open on errors)
 */
@Service
public class RateLimitService {

    private static final int DEFAULT_REQUESTS_PER_MINUTE = 60;
    private static final long WINDOW_SIZE_MS = 60_000L; // 1 minute

    private final Cache<String, RateLimitBucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(2))
            .maximumSize(10_000)
            .build();

    /**
     * Check if a request is allowed for the given key.
     *
     * @param key The rate limit key (e.g., "places:search:userId")
     * @return true if allowed, false if rate limited
     */
    public boolean isAllowed(String key) {
        return isAllowed(key, DEFAULT_REQUESTS_PER_MINUTE);
    }

    /**
     * Check if a request is allowed for the given key with custom limit.
     *
     * @param key The rate limit key
     * @param maxRequestsPerMinute Maximum requests allowed per minute
     * @return true if allowed, false if rate limited
     */
    public boolean isAllowed(String key, int maxRequestsPerMinute) {
        RateLimitBucket bucket = buckets.get(key,
            k -> new RateLimitBucket(maxRequestsPerMinute));
        return bucket.tryAcquire();
    }

    /**
     * Get remaining requests for a key.
     *
     * @param key The rate limit key
     * @return Number of remaining requests in current window
     */
    public int getRemainingRequests(String key) {
        RateLimitBucket bucket = buckets.getIfPresent(key);
        if (bucket == null) {
            return DEFAULT_REQUESTS_PER_MINUTE;
        }
        return bucket.getRemainingRequests();
    }

    /**
     * Simple token bucket implementation for rate limiting.
     */
    static class RateLimitBucket {
        private final int maxRequests;
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private volatile long windowStart;

        RateLimitBucket(int maxRequests) {
            this.maxRequests = maxRequests;
            this.windowStart = System.currentTimeMillis();
        }

        synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();

            // Reset window if expired
            if (now - windowStart >= WINDOW_SIZE_MS) {
                windowStart = now;
                requestCount.set(0);
            }

            // Check if under limit
            if (requestCount.get() < maxRequests) {
                requestCount.incrementAndGet();
                return true;
            }

            return false;
        }

        int getRemainingRequests() {
            long now = System.currentTimeMillis();
            if (now - windowStart >= WINDOW_SIZE_MS) {
                return maxRequests;
            }
            return Math.max(0, maxRequests - requestCount.get());
        }
    }
}
