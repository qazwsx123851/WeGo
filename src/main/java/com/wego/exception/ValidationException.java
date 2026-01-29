package com.wego.exception;

/**
 * Exception for validation errors.
 *
 * Thrown when input validation fails, such as invalid date ranges
 * or business rule violations.
 */
public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }

    public ValidationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
