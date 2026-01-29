package com.wego.service.external;

/**
 * Exception thrown when Google Maps API operations fail.
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
 *   - noResults(query): No results found for query
 *
 * @see com.wego.service.external.GoogleMapsService
 */
public class GoogleMapsException extends RuntimeException {

    private final String errorCode;

    /**
     * Creates a GoogleMapsException with a message only.
     *
     * @param message The error message
     */
    public GoogleMapsException(String message) {
        super(message);
        this.errorCode = "GOOGLE_MAPS_ERROR";
    }

    /**
     * Creates a GoogleMapsException with an error code and message.
     *
     * @param errorCode The specific error code
     * @param message The error message
     */
    public GoogleMapsException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Creates a GoogleMapsException with a message and cause.
     *
     * @param message The error message
     * @param cause The underlying exception
     */
    public GoogleMapsException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GOOGLE_MAPS_ERROR";
    }

    /**
     * Creates a GoogleMapsException with an error code, message, and cause.
     *
     * @param errorCode The specific error code
     * @param message The error message
     * @param cause The underlying exception
     */
    public GoogleMapsException(String errorCode, String message, Throwable cause) {
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
     * Creates a GoogleMapsException for a general API error.
     *
     * @param reason The reason for the API error
     * @return A new GoogleMapsException
     */
    public static GoogleMapsException apiError(String reason) {
        return new GoogleMapsException("API_ERROR", "Google Maps API error: " + reason);
    }

    /**
     * Creates a GoogleMapsException for rate limit exceeded.
     *
     * @return A new GoogleMapsException
     */
    public static GoogleMapsException rateLimitExceeded() {
        return new GoogleMapsException("RATE_LIMIT_EXCEEDED",
                "Google Maps API rate limit exceeded. Please try again later.");
    }

    /**
     * Creates a GoogleMapsException for invalid API key.
     *
     * @return A new GoogleMapsException
     */
    public static GoogleMapsException invalidApiKey() {
        return new GoogleMapsException("INVALID_API_KEY",
                "Invalid or missing Google Maps API key. Please check your configuration.");
    }

    /**
     * Creates a GoogleMapsException for no results found.
     *
     * @param query The search query that returned no results
     * @return A new GoogleMapsException
     */
    public static GoogleMapsException noResults(String query) {
        return new GoogleMapsException("NO_RESULTS",
                "No results found for query: " + query);
    }
}
