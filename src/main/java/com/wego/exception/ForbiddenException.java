package com.wego.exception;

/**
 * Exception thrown when user lacks permission for an operation.
 *
 * @contract
 *   - post: errorCode is "ACCESS_DENIED"
 */
public class ForbiddenException extends BusinessException {

    private static final String ERROR_CODE = "ACCESS_DENIED";

    public ForbiddenException() {
        super(ERROR_CODE, "You do not have permission to perform this action");
    }

    public ForbiddenException(String message) {
        super(ERROR_CODE, message);
    }

    public ForbiddenException(String resourceType, String action) {
        super(ERROR_CODE, String.format("You do not have permission to %s this %s", action, resourceType));
    }
}
