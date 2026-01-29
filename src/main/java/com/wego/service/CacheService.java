package com.wego.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory cache service with TTL support.
 *
 * @contract
 *   - pre: key != null, value != null for put operations
 *   - post: Cached values expire after TTL
 *   - thread-safe: All operations are thread-safe
 */
@Service
public class CacheService {

    private static final long DEFAULT_TTL_MS = 5 * 60 * 1000L; // 5 minutes

    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();

    /**
     * Get a cached value.
     *
     * @param key The cache key
     * @param type The expected type of the cached value
     * @return Optional containing the value if present and not expired
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        CacheEntry<?> entry = cache.get(key);
        if (entry == null) {
            return Optional.empty();
        }

        if (entry.isExpired()) {
            cache.remove(key);
            return Optional.empty();
        }

        if (!type.isInstance(entry.value)) {
            return Optional.empty();
        }

        return Optional.of((T) entry.value);
    }

    /**
     * Put a value in the cache with default TTL.
     *
     * @param key The cache key
     * @param value The value to cache
     */
    public <T> void put(String key, T value) {
        put(key, value, DEFAULT_TTL_MS);
    }

    /**
     * Put a value in the cache with custom TTL.
     *
     * @param key The cache key
     * @param value The value to cache
     * @param ttlMs Time-to-live in milliseconds
     */
    public <T> void put(String key, T value, long ttlMs) {
        if (key == null || value == null) {
            return;
        }
        cache.put(key, new CacheEntry<>(value, ttlMs));
    }

    /**
     * Remove a value from the cache.
     *
     * @param key The cache key
     */
    public void evict(String key) {
        cache.remove(key);
    }

    /**
     * Remove all values matching a key prefix.
     *
     * @param keyPrefix The key prefix to match
     */
    public void evictByPrefix(String keyPrefix) {
        cache.keySet().removeIf(key -> key.startsWith(keyPrefix));
    }

    /**
     * Clear all cached values.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Get cache statistics.
     *
     * @return Current number of entries in cache
     */
    public int size() {
        // Clean up expired entries first
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        return cache.size();
    }

    /**
     * Cache entry with expiration time.
     */
    private static class CacheEntry<T> {
        final T value;
        final long expiresAt;

        CacheEntry(T value, long ttlMs) {
            this.value = value;
            this.expiresAt = System.currentTimeMillis() + ttlMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
