package com.wego.exception;

import com.wego.dto.ApiResponse;
import com.wego.service.external.GoogleMapsException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Global exception handler for REST controllers.
 *
 * Converts exceptions to standardized ApiResponse format.
 *
 * @contract
 *   - post: All exceptions return ApiResponse with appropriate HTTP status
 *   - calledBy: Spring exception handling mechanism
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles ResourceNotFoundException.
     *
     * @contract
     *   - post: Returns 404 NOT_FOUND with error details
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * Handles UnauthorizedException.
     *
     * @contract
     *   - post: Returns 401 UNAUTHORIZED with error details
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * Handles ForbiddenException.
     *
     * @contract
     *   - post: Returns 403 FORBIDDEN with error details
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * Handles BusinessException.
     *
     * @contract
     *   - post: Returns 400 BAD_REQUEST with error details
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business error: {} - {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * Handles validation errors from @Valid annotations.
     *
     * @contract
     *   - post: Returns 400 BAD_REQUEST with validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));

        log.warn("Validation error: {}", message);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("VALIDATION_ERROR", message));
    }

    /**
     * Handles constraint violation errors.
     *
     * @contract
     *   - post: Returns 400 BAD_REQUEST with constraint violation details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .collect(Collectors.joining(", "));

        log.warn("Constraint violation: {}", message);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("VALIDATION_ERROR", message));
    }

    /**
     * Handles missing request parameter errors.
     *
     * @contract
     *   - post: Returns 400 BAD_REQUEST with parameter name
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        String message = "Required parameter '" + ex.getParameterName() + "' is missing";
        log.warn("Missing parameter: {}", message);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("VALIDATION_ERROR", message));
    }

    /**
     * Handles method argument type mismatch errors.
     *
     * @contract
     *   - post: Returns 400 BAD_REQUEST with type information
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Parameter '" + ex.getName() + "' has invalid value: " + ex.getValue();
        log.warn("Type mismatch: {}", message);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("VALIDATION_ERROR", message));
    }

    /**
     * Handles Google Maps API exceptions.
     *
     * @contract
     *   - PLACE_NOT_FOUND, NO_ROUTE_FOUND -> 404 NOT_FOUND
     *   - Other errors -> 502 BAD_GATEWAY
     */
    @ExceptionHandler(GoogleMapsException.class)
    public ResponseEntity<ApiResponse<Void>> handleGoogleMapsException(GoogleMapsException ex) {
        String errorCode = ex.getErrorCode();
        log.error("Google Maps API error: {} - {}", errorCode, ex.getMessage());

        HttpStatus status;
        if ("PLACE_NOT_FOUND".equals(errorCode) || "NO_ROUTE_FOUND".equals(errorCode)) {
            status = HttpStatus.NOT_FOUND;
        } else {
            status = HttpStatus.BAD_GATEWAY;
        }

        return ResponseEntity
            .status(status)
            .body(ApiResponse.error(errorCode, ex.getMessage()));
    }

    /**
     * Handles static resource not found exceptions.
     * Silently returns 404 for browser-initiated requests (e.g., Chrome DevTools).
     *
     * @contract
     *   - post: Returns 404 NOT_FOUND without logging as error
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException ex) {
        // Silently return 404 for missing static resources (e.g., .well-known/*)
        log.debug("Static resource not found: {}", ex.getResourcePath());
        return ResponseEntity.notFound().build();
    }

    /**
     * Handles all other unexpected exceptions.
     *
     * @contract
     *   - post: Returns 500 INTERNAL_SERVER_ERROR with generic message
     *   - post: Logs full stack trace for debugging
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
