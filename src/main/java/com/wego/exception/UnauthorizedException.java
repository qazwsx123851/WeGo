package com.wego.exception;

/**
 * Exception thrown when authentication is required but not provided.
 *
 * @contract
 *   - post: errorCode is "AUTH_REQUIRED"
 */
public class UnauthorizedException extends BusinessException {

    private static final String ERROR_CODE = "AUTH_REQUIRED";

    public UnauthorizedException() {
        super(ERROR_CODE, "Authentication is required");
    }

    public UnauthorizedException(String message) {
        super(ERROR_CODE, message);
    }
}
