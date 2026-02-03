package com.wego.service.external;

/**
 * Exception thrown when exchange rate API operations fail.
 *
 * @contract
 *   - errorCode: Specific error code for categorizing the failure
 *   - message: Human-readable error description
 *   - cause: Optional underlying exception
 *
 * Factory methods:
 *   - apiError(reason): General API error
 *   - rateLimitExceeded(): API rate limit exceeded
 *   - invalidApiKey(): Invalid or missing API key
 *   - invalidCurrency(code): Invalid currency code
 *   - networkError(cause): Network/connection issues
 *   - cacheExpired(): Cache expired and API unavailable
 *
 * @see ExchangeRateClient
 * @see com.wego.service.ExchangeRateService
 */
public class ExchangeRateException extends RuntimeException {

    private final String errorCode;

    /**
     * Creates an ExchangeRateException with a message only.
     *
     * @param message The error message
     */
    public ExchangeRateException(String message) {
        super(message);
        this.errorCode = "EXCHANGE_RATE_ERROR";
    }

    /**
     * Creates an ExchangeRateException with an error code and message.
     *
     * @param errorCode The specific error code
     * @param message The error message
     */
    public ExchangeRateException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Creates an ExchangeRateException with a message and cause.
     *
     * @param message The error message
     * @param cause The underlying exception
     */
    public ExchangeRateException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "EXCHANGE_RATE_ERROR";
    }

    /**
     * Creates an ExchangeRateException with an error code, message, and cause.
     *
     * @param errorCode The specific error code
     * @param message The error message
     * @param cause The underlying exception
     */
    public ExchangeRateException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code for this exception.
     *
     * @return The error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Creates an ExchangeRateException for a general API error.
     *
     * @param reason The reason for the API error
     * @return A new ExchangeRateException
     */
    public static ExchangeRateException apiError(String reason) {
        return new ExchangeRateException("API_ERROR", "ExchangeRate API error: " + reason);
    }

    /**
     * Creates an ExchangeRateException for rate limit exceeded.
     *
     * @return A new ExchangeRateException
     */
    public static ExchangeRateException rateLimitExceeded() {
        return new ExchangeRateException("RATE_LIMIT_EXCEEDED",
                "ExchangeRate API rate limit exceeded. Please try again later.");
    }

    /**
     * Creates an ExchangeRateException for invalid API key.
     *
     * @return A new ExchangeRateException
     */
    public static ExchangeRateException invalidApiKey() {
        return new ExchangeRateException("INVALID_API_KEY",
                "Invalid or missing ExchangeRate API key. Please check your configuration.");
    }

    /**
     * Creates an ExchangeRateException for invalid currency code.
     *
     * @param currencyCode The invalid currency code
     * @return A new ExchangeRateException
     */
    public static ExchangeRateException invalidCurrency(String currencyCode) {
        return new ExchangeRateException("INVALID_CURRENCY",
                String.format("Invalid currency code: %s. Must be a valid 3-letter ISO 4217 code.", currencyCode));
    }

    /**
     * Creates an ExchangeRateException for unsupported currency.
     *
     * @param currencyCode The unsupported currency code
     * @return A new ExchangeRateException
     */
    public static ExchangeRateException unsupportedCurrency(String currencyCode) {
        return new ExchangeRateException("UNSUPPORTED_CURRENCY",
                String.format("Currency not supported: %s", currencyCode));
    }

    /**
     * Creates an ExchangeRateException for network/connection issues.
     *
     * @param cause The underlying exception
     * @return A new ExchangeRateException
     */
    public static ExchangeRateException networkError(Throwable cause) {
        return new ExchangeRateException("NETWORK_ERROR",
                "Failed to connect to ExchangeRate API: " + cause.getMessage(), cause);
    }

    /**
     * Creates an ExchangeRateException for cache expired and API unavailable.
     *
     * @return A new ExchangeRateException
     */
    public static ExchangeRateException cacheExpired() {
        return new ExchangeRateException("CACHE_EXPIRED",
                "Exchange rate cache has expired and API is unavailable. Please try again later.");
    }

    /**
     * Creates an ExchangeRateException for service unavailable.
     *
     * @return A new ExchangeRateException
     */
    public static ExchangeRateException serviceUnavailable() {
        return new ExchangeRateException("SERVICE_UNAVAILABLE",
                "Exchange rate service is temporarily unavailable. Please try again later.");
    }
}
