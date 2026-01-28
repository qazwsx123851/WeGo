package com.wego.exception;

import lombok.Getter;

/**
 * Base exception for business logic errors.
 *
 * All domain-specific exceptions should extend this class.
 *
 * @contract
 *   - invariant: errorCode is never null or empty
 *   - calledBy: ResourceNotFoundException, UnauthorizedException, ForbiddenException
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;

    /**
     * Creates a new BusinessException with error code and message.
     *
     * @contract
     *   - pre: errorCode != null && !errorCode.isBlank()
     *   - post: exception is created with valid errorCode
     *
     * @param errorCode The error code (must not be null or blank)
     * @param message The error message
     * @throws IllegalArgumentException if errorCode is null or blank
     */
    public BusinessException(String errorCode, String message) {
        super(message);
        if (errorCode == null || errorCode.isBlank()) {
            throw new IllegalArgumentException("errorCode must not be null or blank");
        }
        this.errorCode = errorCode;
    }

    /**
     * Creates a new BusinessException with error code, message and cause.
     *
     * @contract
     *   - pre: errorCode != null && !errorCode.isBlank()
     *   - post: exception is created with valid errorCode and cause chain
     *
     * @param errorCode The error code (must not be null or blank)
     * @param message The error message
     * @param cause The underlying cause
     * @throws IllegalArgumentException if errorCode is null or blank
     */
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        if (errorCode == null || errorCode.isBlank()) {
            throw new IllegalArgumentException("errorCode must not be null or blank");
        }
        this.errorCode = errorCode;
    }
}
