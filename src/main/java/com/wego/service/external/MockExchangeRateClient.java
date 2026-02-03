package com.wego.service.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Mock implementation of ExchangeRateClient for testing and development.
 * Uses static exchange rates based on approximate real-world values.
 *
 * @contract
 *   - Only active when exchangerate.enabled is false (default)
 *   - Returns consistent static rates for predictable testing
 *   - Supports common currencies: USD, TWD, JPY, EUR, GBP, CNY, KRW, HKD, SGD, THB
 *
 * @see ExchangeRateClient
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "wego.external-api.exchangerate.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class MockExchangeRateClient implements ExchangeRateClient {

    private static final Pattern CURRENCY_CODE_PATTERN = Pattern.compile("^[A-Z]{3}$");

    // Static rates: 1 USD = X currency (approximate real-world values as of 2026)
    private static final Map<String, BigDecimal> USD_RATES = Map.ofEntries(
            Map.entry("USD", new BigDecimal("1.000000")),
            Map.entry("TWD", new BigDecimal("31.500000")),
            Map.entry("JPY", new BigDecimal("149.500000")),
            Map.entry("EUR", new BigDecimal("0.920000")),
            Map.entry("GBP", new BigDecimal("0.790000")),
            Map.entry("CNY", new BigDecimal("7.250000")),
            Map.entry("KRW", new BigDecimal("1320.000000")),
            Map.entry("HKD", new BigDecimal("7.800000")),
            Map.entry("SGD", new BigDecimal("1.340000")),
            Map.entry("THB", new BigDecimal("35.500000")),
            Map.entry("AUD", new BigDecimal("1.530000")),
            Map.entry("CAD", new BigDecimal("1.360000")),
            Map.entry("CHF", new BigDecimal("0.880000")),
            Map.entry("NZD", new BigDecimal("1.650000")),
            Map.entry("MYR", new BigDecimal("4.700000")),
            Map.entry("PHP", new BigDecimal("56.500000")),
            Map.entry("IDR", new BigDecimal("15800.000000")),
            Map.entry("VND", new BigDecimal("24500.000000"))
    );

    private static final Set<String> SUPPORTED_CURRENCIES = USD_RATES.keySet();

    private final Instant lastUpdateTime;

    public MockExchangeRateClient() {
        // Set last update time to current time on initialization
        this.lastUpdateTime = Instant.now();
        log.info("[MOCK] MockExchangeRateClient initialized with {} currencies", SUPPORTED_CURRENCIES.size());
    }

    /**
     * {@inheritDoc}
     *
     * Returns a static exchange rate based on pre-defined USD rates.
     * Uses cross-rate calculation for non-USD pairs.
     */
    @Override
    public BigDecimal getRate(String fromCurrency, String toCurrency) {
        validateCurrencyCode(fromCurrency);
        validateCurrencyCode(toCurrency);
        validateSupportedCurrency(fromCurrency);
        validateSupportedCurrency(toCurrency);

        log.debug("[MOCK] Getting rate from {} to {}", fromCurrency, toCurrency);

        // Same currency = 1.0
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }

        // Get USD rate for both currencies
        BigDecimal fromUsdRate = USD_RATES.get(fromCurrency);
        BigDecimal toUsdRate = USD_RATES.get(toCurrency);

        // Calculate cross rate: (1 / fromUsdRate) * toUsdRate
        // Example: EUR -> JPY: (1 / 0.92) * 149.5 = 162.5
        BigDecimal rate = toUsdRate.divide(fromUsdRate, 6, RoundingMode.HALF_UP);

        log.info("[MOCK] Rate {} -> {}: {}", fromCurrency, toCurrency, rate);
        return rate;
    }

    /**
     * {@inheritDoc}
     *
     * Returns all exchange rates from the specified base currency.
     */
    @Override
    public Map<String, BigDecimal> getAllRates(String baseCurrency) {
        validateCurrencyCode(baseCurrency);
        validateSupportedCurrency(baseCurrency);

        log.debug("[MOCK] Getting all rates for base currency {}", baseCurrency);

        Map<String, BigDecimal> rates = new HashMap<>();
        BigDecimal baseUsdRate = USD_RATES.get(baseCurrency);

        for (Map.Entry<String, BigDecimal> entry : USD_RATES.entrySet()) {
            String targetCurrency = entry.getKey();
            BigDecimal targetUsdRate = entry.getValue();

            // Calculate cross rate
            BigDecimal rate = targetUsdRate.divide(baseUsdRate, 6, RoundingMode.HALF_UP);
            rates.put(targetCurrency, rate);
        }

        log.info("[MOCK] Retrieved {} rates for base currency {}", rates.size(), baseCurrency);
        return rates;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instant getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * Gets the set of supported currency codes.
     *
     * @return Set of supported currency codes
     */
    public static Set<String> getSupportedCurrencies() {
        return SUPPORTED_CURRENCIES;
    }

    /**
     * Validates that a currency code matches the ISO 4217 format.
     *
     * @param currencyCode The currency code to validate
     * @throws ExchangeRateException if the code is invalid
     */
    private void validateCurrencyCode(String currencyCode) {
        if (currencyCode == null || !CURRENCY_CODE_PATTERN.matcher(currencyCode).matches()) {
            throw ExchangeRateException.invalidCurrency(currencyCode);
        }
    }

    /**
     * Validates that a currency code is supported.
     *
     * @param currencyCode The currency code to validate
     * @throws ExchangeRateException if the currency is not supported
     */
    private void validateSupportedCurrency(String currencyCode) {
        if (!SUPPORTED_CURRENCIES.contains(currencyCode)) {
            throw ExchangeRateException.unsupportedCurrency(currencyCode);
        }
    }
}
