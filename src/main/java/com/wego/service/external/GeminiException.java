package com.wego.service.external;

/**
 * Exception thrown when Gemini API operations fail.
 *
 * @contract
 *   - errorCode: Specific error code for categorizing the failure
 *   - message: Human-readable error description
 *
 * @see GeminiClient
 */
public class GeminiException extends RuntimeException {

    private final String errorCode;

    public GeminiException(String message) {
        super(message);
        this.errorCode = "GEMINI_ERROR";
    }

    public GeminiException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public GeminiException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static GeminiException apiError(String reason) {
        return new GeminiException("API_ERROR", "Gemini API error: " + reason);
    }

    public static GeminiException networkError(Throwable cause) {
        return new GeminiException("NETWORK_ERROR",
                "Failed to connect to Gemini API: " + cause.getMessage(), cause);
    }

    public static GeminiException invalidApiKey() {
        return new GeminiException("INVALID_API_KEY",
                "Invalid or missing Gemini API key. Please check your configuration.");
    }

    public static GeminiException timeout() {
        return new GeminiException("TIMEOUT",
                "Gemini API request timed out. Please try again.");
    }
}
