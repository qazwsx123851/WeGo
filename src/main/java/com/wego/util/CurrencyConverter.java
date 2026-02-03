package com.wego.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for currency conversion operations.
 * All methods are immutable and thread-safe.
 *
 * @contract
 *   - All amounts use BigDecimal for precision
 *   - Amount scale: 2 decimal places (cents)
 *   - Rate scale: 6 decimal places
 *   - Rounding mode: HALF_UP
 *   - All methods are pure functions (no side effects)
 *
 * @see com.wego.service.ExchangeRateService
 */
public final class CurrencyConverter {

    /**
     * Scale for monetary amounts (2 decimal places).
     */
    public static final int AMOUNT_SCALE = 2;

    /**
     * Scale for exchange rates (6 decimal places).
     */
    public static final int RATE_SCALE = 6;

    /**
     * Rounding mode for all calculations.
     */
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Minimum valid exchange rate.
     */
    public static final BigDecimal MIN_RATE = new BigDecimal("0.0001");

    /**
     * Maximum valid exchange rate.
     */
    public static final BigDecimal MAX_RATE = new BigDecimal("1000000");

    // Private constructor to prevent instantiation
    private CurrencyConverter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Converts an amount from one currency to another using the given rate.
     *
     * @contract
     *   - pre: amount != null
     *   - pre: rate != null and rate > 0
     *   - post: returns amount * rate, rounded to 2 decimal places
     *   - calledBy: ExchangeRateService#convert
     *
     * @param amount The amount to convert
     * @param rate The exchange rate (1 source = X target)
     * @return The converted amount
     * @throws IllegalArgumentException if amount or rate is null or rate is not positive
     */
    public static BigDecimal convert(BigDecimal amount, BigDecimal rate) {
        validateAmount(amount);
        validateRate(rate);

        return amount.multiply(rate).setScale(AMOUNT_SCALE, ROUNDING_MODE);
    }

    /**
     * Converts an amount using from and to rates (cross-rate calculation).
     *
     * @contract
     *   - pre: amount != null
     *   - pre: fromRate != null and fromRate > 0 (source currency to USD)
     *   - pre: toRate != null and toRate > 0 (target currency to USD)
     *   - post: returns (amount / fromRate) * toRate
     *   - calledBy: ExchangeRateService#convertCrossRate
     *
     * @param amount The amount to convert
     * @param fromRate Rate of source currency to USD (1 USD = X source)
     * @param toRate Rate of target currency to USD (1 USD = X target)
     * @return The converted amount
     */
    public static BigDecimal convertCrossRate(BigDecimal amount, BigDecimal fromRate, BigDecimal toRate) {
        validateAmount(amount);
        validateRate(fromRate);
        validateRate(toRate);

        // Cross rate = toRate / fromRate
        BigDecimal crossRate = toRate.divide(fromRate, RATE_SCALE, ROUNDING_MODE);
        return amount.multiply(crossRate).setScale(AMOUNT_SCALE, ROUNDING_MODE);
    }

    /**
     * Calculates the cross rate between two currencies.
     *
     * @contract
     *   - pre: fromRate != null and fromRate > 0
     *   - pre: toRate != null and toRate > 0
     *   - post: returns toRate / fromRate
     *   - calledBy: ExchangeRateService#getCrossRate
     *
     * @param fromRate Rate of source currency to base (1 base = X source)
     * @param toRate Rate of target currency to base (1 base = X target)
     * @return The cross rate
     */
    public static BigDecimal calculateCrossRate(BigDecimal fromRate, BigDecimal toRate) {
        validateRate(fromRate);
        validateRate(toRate);

        return toRate.divide(fromRate, RATE_SCALE, ROUNDING_MODE);
    }

    /**
     * Rounds an amount to the standard scale (2 decimal places).
     *
     * @contract
     *   - pre: amount != null
     *   - post: returns amount rounded to 2 decimal places
     *
     * @param amount The amount to round
     * @return The rounded amount
     */
    public static BigDecimal roundAmount(BigDecimal amount) {
        validateAmount(amount);
        return amount.setScale(AMOUNT_SCALE, ROUNDING_MODE);
    }

    /**
     * Rounds a rate to the standard scale (6 decimal places).
     *
     * @contract
     *   - pre: rate != null
     *   - post: returns rate rounded to 6 decimal places
     *
     * @param rate The rate to round
     * @return The rounded rate
     */
    public static BigDecimal roundRate(BigDecimal rate) {
        if (rate == null) {
            throw new IllegalArgumentException("Rate cannot be null");
        }
        return rate.setScale(RATE_SCALE, ROUNDING_MODE);
    }

    /**
     * Validates that an exchange rate is within acceptable bounds.
     *
     * @contract
     *   - pre: rate != null
     *   - post: returns true if MIN_RATE <= rate <= MAX_RATE
     *
     * @param rate The rate to validate
     * @return true if the rate is valid
     */
    public static boolean isValidRate(BigDecimal rate) {
        if (rate == null) {
            return false;
        }
        return rate.compareTo(MIN_RATE) >= 0 && rate.compareTo(MAX_RATE) <= 0;
    }

    /**
     * Validates an amount.
     *
     * @param amount The amount to validate
     * @throws IllegalArgumentException if amount is null
     */
    private static void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
    }

    /**
     * Validates an exchange rate.
     *
     * @param rate The rate to validate
     * @throws IllegalArgumentException if rate is null, zero, negative, or out of bounds
     */
    private static void validateRate(BigDecimal rate) {
        if (rate == null) {
            throw new IllegalArgumentException("Rate cannot be null");
        }
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Rate must be positive");
        }
        if (!isValidRate(rate)) {
            throw new IllegalArgumentException(
                    String.format("Rate %s is out of valid range [%s, %s]", rate, MIN_RATE, MAX_RATE));
        }
    }
}
