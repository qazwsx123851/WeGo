package com.wego.service.external;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Interface for exchange rate API operations.
 * Allows swapping between real and mock implementations.
 *
 * @contract
 *   - All methods throw ExchangeRateException on API failures
 *   - Mock implementation uses static rates for development
 *   - Real implementation calls ExchangeRate-API
 *   - Currency codes must be valid 3-letter ISO 4217 codes
 *
 * @see MockExchangeRateClient
 * @see ExchangeRateApiClient
 */
public interface ExchangeRateClient {

    /**
     * Gets the exchange rate between two currencies.
     *
     * @contract
     *   - pre: fromCurrency != null and matches ^[A-Z]{3}$
     *   - pre: toCurrency != null and matches ^[A-Z]{3}$
     *   - post: returns positive BigDecimal rate
     *   - throws: ExchangeRateException if API call fails
     *   - throws: ExchangeRateException if currency is invalid
     *   - calledBy: ExchangeRateService#getRate
     *
     * @param fromCurrency Source currency code (e.g., "USD")
     * @param toCurrency Target currency code (e.g., "TWD")
     * @return Exchange rate (1 fromCurrency = X toCurrency)
     */
    BigDecimal getRate(String fromCurrency, String toCurrency);

    /**
     * Gets all exchange rates from a base currency.
     *
     * @contract
     *   - pre: baseCurrency != null and matches ^[A-Z]{3}$
     *   - post: returns non-empty map of currency code to rate
     *   - throws: ExchangeRateException on failure
     *   - calledBy: ExchangeRateService#getAllRates
     *
     * @param baseCurrency Base currency code (e.g., "USD")
     * @return Map of target currency codes to rates
     */
    Map<String, BigDecimal> getAllRates(String baseCurrency);

    /**
     * Gets the last update timestamp for rates.
     *
     * @contract
     *   - post: returns timestamp of when rates were last updated
     *   - calledBy: ExchangeRateService#getCacheInfo
     *
     * @return Last update timestamp from API
     */
    Instant getLastUpdateTime();
}
