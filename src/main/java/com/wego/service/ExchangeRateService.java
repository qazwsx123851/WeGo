package com.wego.service;

import com.wego.config.ExchangeRateProperties;
import com.wego.dto.response.ExchangeRateResponse;
import com.wego.service.external.ExchangeRateClient;
import com.wego.service.external.ExchangeRateException;
import com.wego.util.CurrencyConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Service for exchange rate operations with caching.
 *
 * Implements a two-tier caching strategy:
 * - Primary cache: 1 hour TTL (configurable)
 * - Fallback cache: 24 hour TTL when API unavailable
 *
 * @contract
 *   - Thread-safe caching implementation
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

    // Cache for exchange rates: key = "FROM-TO", value = CachedRate
    private final Map<String, CachedRate> rateCache = new ConcurrentHashMap<>();

    // Cache for all rates from a base currency
    private final Map<String, CachedAllRates> allRatesCache = new ConcurrentHashMap<>();

    /**
     * Creates an ExchangeRateService with the specified client and properties.
     *
     * @contract
     *   - pre: exchangeRateClient != null
     *   - pre: properties != null
     *
     * @param exchangeRateClient The exchange rate client (real or mock)
     * @param properties Configuration properties
     */
    public ExchangeRateService(ExchangeRateClient exchangeRateClient, ExchangeRateProperties properties) {
        this.exchangeRateClient = exchangeRateClient;
        this.properties = properties;
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
     *
     * @param fromCurrency Source currency code
     * @param toCurrency Target currency code
     * @return Exchange rate response with cache metadata
     */
    public ExchangeRateResponse getRate(String fromCurrency, String toCurrency) {
        validateCurrencyCode(fromCurrency);
        validateCurrencyCode(toCurrency);

        String cacheKey = fromCurrency + "-" + toCurrency;
        CachedRate cached = rateCache.get(cacheKey);

        // Check if cache is still valid
        if (cached != null && !isCacheExpired(cached.fetchedAt, properties.getCacheTtlHours())) {
            long cacheAgeMs = Duration.between(cached.fetchedAt, Instant.now()).toMillis();
            log.debug("Cache hit for {}: rate={}, age={}ms", cacheKey, cached.rate, cacheAgeMs);
            return ExchangeRateResponse.cached(fromCurrency, toCurrency, cached.rate, cached.fetchedAt, cacheAgeMs);
        }

        // Try to fetch fresh rate
        try {
            BigDecimal rate = exchangeRateClient.getRate(fromCurrency, toCurrency);
            Instant fetchedAt = exchangeRateClient.getLastUpdateTime();

            // Update cache
            rateCache.put(cacheKey, new CachedRate(rate, fetchedAt));

            log.debug("Fresh rate for {}: {}", cacheKey, rate);
            return ExchangeRateResponse.fresh(fromCurrency, toCurrency, rate, fetchedAt);

        } catch (ExchangeRateException e) {
            // Try fallback cache
            if (cached != null && !isCacheExpired(cached.fetchedAt, properties.getFallbackTtlHours())) {
                long cacheAgeMs = Duration.between(cached.fetchedAt, Instant.now()).toMillis();
                log.warn("API failed, using fallback cache for {}: age={}ms, error={}",
                        cacheKey, cacheAgeMs, e.getMessage());
                return ExchangeRateResponse.cached(fromCurrency, toCurrency, cached.rate, cached.fetchedAt, cacheAgeMs);
            }

            // No valid cache available
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
     *
     * @param baseCurrency Base currency code
     * @return Map of currency codes to rates
     */
    public Map<String, BigDecimal> getAllRates(String baseCurrency) {
        validateCurrencyCode(baseCurrency);

        CachedAllRates cached = allRatesCache.get(baseCurrency);

        // Check if cache is still valid
        if (cached != null && !isCacheExpired(cached.fetchedAt, properties.getCacheTtlHours())) {
            log.debug("Cache hit for all rates from {}", baseCurrency);
            return cached.rates;
        }

        // Try to fetch fresh rates
        try {
            Map<String, BigDecimal> rates = exchangeRateClient.getAllRates(baseCurrency);
            Instant fetchedAt = exchangeRateClient.getLastUpdateTime();

            // Update cache
            allRatesCache.put(baseCurrency, new CachedAllRates(rates, fetchedAt));

            log.debug("Fresh rates for {}: {} currencies", baseCurrency, rates.size());
            return rates;

        } catch (ExchangeRateException e) {
            // Try fallback cache
            if (cached != null && !isCacheExpired(cached.fetchedAt, properties.getFallbackTtlHours())) {
                log.warn("API failed, using fallback cache for all rates from {}", baseCurrency);
                return cached.rates;
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
     *
     * @param amount Amount to convert
     * @param fromCurrency Source currency code
     * @param toCurrency Target currency code
     * @return Converted amount
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }

        // Same currency, no conversion needed
        if (fromCurrency.equals(toCurrency)) {
            return CurrencyConverter.roundAmount(amount);
        }

        ExchangeRateResponse rateResponse = getRate(fromCurrency, toCurrency);
        return CurrencyConverter.convert(amount, rateResponse.getRate());
    }

    /**
     * Gets the list of commonly supported currencies.
     *
     * @return Set of common currency codes
     */
    public Set<String> getSupportedCurrencies() {
        return COMMON_CURRENCIES;
    }

    /**
     * Validates that a currency code is in correct format.
     *
     * @param currencyCode The currency code to validate
     * @throws ExchangeRateException if invalid
     */
    private void validateCurrencyCode(String currencyCode) {
        if (currencyCode == null || !CURRENCY_CODE_PATTERN.matcher(currencyCode).matches()) {
            throw ExchangeRateException.invalidCurrency(currencyCode);
        }
    }

    /**
     * Checks if a cache entry has expired.
     *
     * @param fetchedAt When the data was fetched
     * @param ttlHours TTL in hours
     * @return true if expired
     */
    private boolean isCacheExpired(Instant fetchedAt, int ttlHours) {
        Duration age = Duration.between(fetchedAt, Instant.now());
        return age.toHours() >= ttlHours;
    }

    /**
     * Clears all cached rates.
     * Mainly for testing purposes.
     */
    public void clearCache() {
        rateCache.clear();
        allRatesCache.clear();
        log.info("Exchange rate cache cleared");
    }

    /**
     * Internal class to hold cached exchange rate data.
     */
    private record CachedRate(BigDecimal rate, Instant fetchedAt) {}

    /**
     * Internal class to hold cached all-rates data.
     */
    private record CachedAllRates(Map<String, BigDecimal> rates, Instant fetchedAt) {}
}
