package com.wego.service;

import com.wego.config.ExchangeRateProperties;
import com.wego.dto.response.ExchangeRateResponse;
import com.wego.service.external.ExchangeRateClient;
import com.wego.service.external.ExchangeRateException;
import com.wego.util.CurrencyConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service for exchange rate operations with caching.
 *
 * Implements a two-tier caching strategy using Spring Cache + Caffeine:
 * - Primary cache ("exchange-rate"): 1 hour TTL
 * - Fallback cache ("exchange-rate-fallback"): 24 hour TTL when API unavailable
 *
 * @contract
 *   - Thread-safe caching via Caffeine (bounded max size)
 *   - Falls back to cached rates on API failure
 *   - Currency codes must be valid 3-letter ISO 4217 codes
 *
 * @see ExchangeRateClient
 * @see CurrencyConverter
 */
@Slf4j
@Service
public class ExchangeRateService {

    private static final Pattern CURRENCY_CODE_PATTERN = Pattern.compile("^[A-Z]{3}$");

    // Common currencies that are always supported
    private static final Set<String> COMMON_CURRENCIES = Set.of(
            "USD", "TWD", "JPY", "EUR", "GBP", "CNY", "KRW", "HKD", "SGD", "THB",
            "AUD", "CAD", "CHF", "NZD", "MYR", "PHP", "IDR", "VND"
    );

    private final ExchangeRateClient exchangeRateClient;
    private final ExchangeRateProperties properties;
    private final CacheManager cacheManager;

    public ExchangeRateService(ExchangeRateClient exchangeRateClient,
                                ExchangeRateProperties properties,
                                CacheManager cacheManager) {
        this.exchangeRateClient = exchangeRateClient;
        this.properties = properties;
        this.cacheManager = cacheManager;
        log.info("ExchangeRateService initialized with cache TTL: {} hours", properties.getCacheTtlHours());
    }

    /**
     * Gets the exchange rate between two currencies.
     *
     * @contract
     *   - pre: fromCurrency != null and matches ^[A-Z]{3}$
     *   - pre: toCurrency != null and matches ^[A-Z]{3}$
     *   - post: returns ExchangeRateResponse with rate and cache info
     *   - throws: ExchangeRateException if rate cannot be obtained
     *   - calledBy: ExchangeRateApiController#getRate
     */
    public ExchangeRateResponse getRate(String fromCurrency, String toCurrency) {
        validateCurrencyCode(fromCurrency);
        validateCurrencyCode(toCurrency);

        String cacheKey = fromCurrency + "-" + toCurrency;

        // Check primary cache
        Cache primaryCache = cacheManager.getCache("exchange-rate");
        CachedRate cached = getCachedRate(primaryCache, cacheKey);

        if (cached != null) {
            long cacheAgeMs = Duration.between(cached.fetchedAt(), Instant.now()).toMillis();
            log.debug("Cache hit for {}: rate={}, age={}ms", cacheKey, cached.rate(), cacheAgeMs);
            return ExchangeRateResponse.cached(fromCurrency, toCurrency, cached.rate(), cached.fetchedAt(), cacheAgeMs);
        }

        // Try to fetch fresh rate
        try {
            BigDecimal rate = exchangeRateClient.getRate(fromCurrency, toCurrency);
            Instant fetchedAt = exchangeRateClient.getLastUpdateTime();
            CachedRate newEntry = new CachedRate(rate, fetchedAt);

            // Update both primary and fallback caches
            putCachedRate(primaryCache, cacheKey, newEntry);
            Cache fallbackCache = cacheManager.getCache("exchange-rate-fallback");
            putCachedRate(fallbackCache, cacheKey, newEntry);

            log.debug("Fresh rate for {}: {}", cacheKey, rate);
            return ExchangeRateResponse.fresh(fromCurrency, toCurrency, rate, fetchedAt);

        } catch (ExchangeRateException e) {
            // Try fallback cache
            Cache fallbackCache = cacheManager.getCache("exchange-rate-fallback");
            CachedRate fallback = getCachedRate(fallbackCache, cacheKey);

            if (fallback != null) {
                long cacheAgeMs = Duration.between(fallback.fetchedAt(), Instant.now()).toMillis();
                log.warn("API failed, using fallback cache for {}: age={}ms, error={}",
                        cacheKey, cacheAgeMs, e.getMessage());
                return ExchangeRateResponse.cached(fromCurrency, toCurrency, fallback.rate(), fallback.fetchedAt(), cacheAgeMs);
            }

            log.error("No valid cache for {} and API failed: {}", cacheKey, e.getMessage());
            throw e;
        }
    }

    /**
     * Gets all exchange rates from a base currency.
     *
     * @contract
     *   - pre: baseCurrency != null and matches ^[A-Z]{3}$
     *   - post: returns map of currency codes to rates
     *   - throws: ExchangeRateException if rates cannot be obtained
     *   - calledBy: ExchangeRateApiController#getAllRates
     */
    @SuppressWarnings("unchecked")
    public Map<String, BigDecimal> getAllRates(String baseCurrency) {
        validateCurrencyCode(baseCurrency);

        // Check primary cache
        Cache primaryCache = cacheManager.getCache("exchange-rate-all");
        if (primaryCache != null) {
            Cache.ValueWrapper wrapper = primaryCache.get(baseCurrency);
            if (wrapper != null) {
                log.debug("Cache hit for all rates from {}", baseCurrency);
                return (Map<String, BigDecimal>) wrapper.get();
            }
        }

        // Try to fetch fresh rates
        try {
            Map<String, BigDecimal> rates = exchangeRateClient.getAllRates(baseCurrency);

            // Update both primary and fallback caches
            if (primaryCache != null) {
                primaryCache.put(baseCurrency, rates);
            }
            Cache fallbackCache = cacheManager.getCache("exchange-rate-all-fallback");
            if (fallbackCache != null) {
                fallbackCache.put(baseCurrency, rates);
            }

            log.debug("Fresh rates for {}: {} currencies", baseCurrency, rates.size());
            return rates;

        } catch (ExchangeRateException e) {
            // Try fallback cache
            Cache fallbackCache = cacheManager.getCache("exchange-rate-all-fallback");
            if (fallbackCache != null) {
                Cache.ValueWrapper wrapper = fallbackCache.get(baseCurrency);
                if (wrapper != null) {
                    log.warn("API failed, using fallback cache for all rates from {}", baseCurrency);
                    return (Map<String, BigDecimal>) wrapper.get();
                }
            }

            log.error("No valid cache for all rates from {} and API failed: {}", baseCurrency, e.getMessage());
            throw e;
        }
    }

    /**
     * Converts an amount from one currency to another.
     *
     * @contract
     *   - pre: amount != null
     *   - pre: fromCurrency != null and matches ^[A-Z]{3}$
     *   - pre: toCurrency != null and matches ^[A-Z]{3}$
     *   - post: returns converted amount with 2 decimal places
     *   - throws: ExchangeRateException if rate cannot be obtained
     *   - calledBy: ExpenseService#convertToBaseCurrency, SettlementService#calculateSettlement
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }

        if (fromCurrency.equals(toCurrency)) {
            return CurrencyConverter.roundAmount(amount);
        }

        ExchangeRateResponse rateResponse = getRate(fromCurrency, toCurrency);
        return CurrencyConverter.convert(amount, rateResponse.getRate());
    }

    /**
     * Gets the list of commonly supported currencies.
     */
    public Set<String> getSupportedCurrencies() {
        return COMMON_CURRENCIES;
    }

    /**
     * Clears all cached rates.
     */
    public void clearCache() {
        clearCacheByName("exchange-rate");
        clearCacheByName("exchange-rate-fallback");
        clearCacheByName("exchange-rate-all");
        clearCacheByName("exchange-rate-all-fallback");
        log.info("Exchange rate cache cleared");
    }

    private void validateCurrencyCode(String currencyCode) {
        if (currencyCode == null || !CURRENCY_CODE_PATTERN.matcher(currencyCode).matches()) {
            throw ExchangeRateException.invalidCurrency(currencyCode);
        }
    }

    private CachedRate getCachedRate(Cache cache, String key) {
        if (cache == null) return null;
        Cache.ValueWrapper wrapper = cache.get(key);
        if (wrapper == null) return null;
        return (CachedRate) wrapper.get();
    }

    private void putCachedRate(Cache cache, String key, CachedRate value) {
        if (cache != null) {
            cache.put(key, value);
        }
    }

    private void clearCacheByName(String name) {
        Cache cache = cacheManager.getCache(name);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Internal record to hold cached exchange rate data.
     */
    private record CachedRate(BigDecimal rate, Instant fetchedAt) {}
}
