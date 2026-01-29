package com.wego.service.external;

/**
 * Exception thrown when weather API operations fail.
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
 *   - invalidLocation(): Invalid coordinates
 *   - dateOutOfRange(): Date beyond forecast range
 *
 * @see com.wego.service.WeatherService
 * @see com.wego.service.external.WeatherClient
 */
public class WeatherException extends RuntimeException {

    private final String errorCode;

    /**
     * Creates a WeatherException with a message only.
     *
     * @param message The error message
     */
    public WeatherException(String message) {
        super(message);
        this.errorCode = "WEATHER_ERROR";
    }

    /**
     * Creates a WeatherException with an error code and message.
     *
     * @param errorCode The specific error code
     * @param message The error message
     */
    public WeatherException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Creates a WeatherException with a message and cause.
     *
     * @param message The error message
     * @param cause The underlying exception
     */
    public WeatherException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "WEATHER_ERROR";
    }

    /**
     * Creates a WeatherException with an error code, message, and cause.
     *
     * @param errorCode The specific error code
     * @param message The error message
     * @param cause The underlying exception
     */
    public WeatherException(String errorCode, String message, Throwable cause) {
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
     * Creates a WeatherException for a general API error.
     *
     * @param reason The reason for the API error
     * @return A new WeatherException
     */
    public static WeatherException apiError(String reason) {
        return new WeatherException("API_ERROR", "OpenWeatherMap API error: " + reason);
    }

    /**
     * Creates a WeatherException for rate limit exceeded.
     *
     * @return A new WeatherException
     */
    public static WeatherException rateLimitExceeded() {
        return new WeatherException("RATE_LIMIT_EXCEEDED",
                "OpenWeatherMap API rate limit exceeded. Please try again later.");
    }

    /**
     * Creates a WeatherException for invalid API key.
     *
     * @return A new WeatherException
     */
    public static WeatherException invalidApiKey() {
        return new WeatherException("INVALID_API_KEY",
                "Invalid or missing OpenWeatherMap API key. Please check your configuration.");
    }

    /**
     * Creates a WeatherException for invalid location coordinates.
     *
     * @param lat The invalid latitude
     * @param lng The invalid longitude
     * @return A new WeatherException
     */
    public static WeatherException invalidLocation(double lat, double lng) {
        return new WeatherException("INVALID_LOCATION",
                String.format("Invalid location coordinates: lat=%.6f, lng=%.6f", lat, lng));
    }

    /**
     * Creates a WeatherException for date out of forecast range.
     *
     * @return A new WeatherException
     */
    public static WeatherException dateOutOfRange() {
        return new WeatherException("DATE_OUT_OF_RANGE",
                "Requested date is beyond the 5-day forecast range.");
    }

    /**
     * Creates a WeatherException for network/connection issues.
     *
     * @param cause The underlying exception
     * @return A new WeatherException
     */
    public static WeatherException networkError(Throwable cause) {
        return new WeatherException("NETWORK_ERROR",
                "Failed to connect to OpenWeatherMap API: " + cause.getMessage(), cause);
    }
}
