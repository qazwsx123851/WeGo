package com.wego.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for exchange rate queries.
 *
 * @contract
 *   - from: Source currency code (3-letter ISO 4217)
 *   - to: Target currency code (3-letter ISO 4217)
 *   - rate: Exchange rate (1 from = X to)
 *   - fetchedAt: When the rate was fetched from API
 *   - cached: Whether this rate came from cache
 *   - cacheAgeMs: Age of cached data in milliseconds (0 if not cached)
 *
 * @see com.wego.controller.api.ExchangeRateApiController
 */
@Getter
@Builder
public class ExchangeRateResponse {

    /**
     * Source currency code (e.g., "USD").
     */
    private final String from;

    /**
     * Target currency code (e.g., "TWD").
     */
    private final String to;

    /**
     * Exchange rate (1 from = X to).
     */
    private final BigDecimal rate;

    /**
     * When the rate was last updated from the API.
     */
    private final Instant fetchedAt;

    /**
     * Whether this rate was served from cache.
     */
    private final boolean cached;

    /**
     * Age of the cached data in milliseconds.
     * 0 if not cached.
     */
    private final long cacheAgeMs;

    /**
     * Creates a response for a fresh (non-cached) rate.
     *
     * @param from Source currency code
     * @param to Target currency code
     * @param rate The exchange rate
     * @param fetchedAt When the rate was fetched
     * @return A new ExchangeRateResponse
     */
    public static ExchangeRateResponse fresh(String from, String to, BigDecimal rate, Instant fetchedAt) {
        return ExchangeRateResponse.builder()
                .from(from)
                .to(to)
                .rate(rate)
                .fetchedAt(fetchedAt)
                .cached(false)
                .cacheAgeMs(0)
                .build();
    }

    /**
     * Creates a response for a cached rate.
     *
     * @param from Source currency code
     * @param to Target currency code
     * @param rate The exchange rate
     * @param fetchedAt When the rate was originally fetched
     * @param cacheAgeMs Age of the cache in milliseconds
     * @return A new ExchangeRateResponse
     */
    public static ExchangeRateResponse cached(String from, String to, BigDecimal rate,
                                              Instant fetchedAt, long cacheAgeMs) {
        return ExchangeRateResponse.builder()
                .from(from)
                .to(to)
                .rate(rate)
                .fetchedAt(fetchedAt)
                .cached(true)
                .cacheAgeMs(cacheAgeMs)
                .build();
    }
}
