package com.wego.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for exception classes.
 */
class ExceptionsTest {

    @Test
    @DisplayName("BusinessException should store error code and message")
    void businessException_shouldStoreErrorCodeAndMessage() {
        BusinessException ex = new BusinessException("TEST_ERROR", "Test message");

        assertThat(ex.getErrorCode()).isEqualTo("TEST_ERROR");
        assertThat(ex.getMessage()).isEqualTo("Test message");
    }

    @Test
    @DisplayName("BusinessException should store cause")
    void businessException_shouldStoreCause() {
        Throwable cause = new RuntimeException("Original error");
        BusinessException ex = new BusinessException("TEST_ERROR", "Test message", cause);

        assertThat(ex.getErrorCode()).isEqualTo("TEST_ERROR");
        assertThat(ex.getMessage()).isEqualTo("Test message");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("ResourceNotFoundException should format message correctly")
    void resourceNotFoundException_shouldFormatMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Trip", "abc-123");

        assertThat(ex.getErrorCode()).isEqualTo("TRIP_NOT_FOUND");
        assertThat(ex.getMessage()).contains("Trip");
        assertThat(ex.getMessage()).contains("abc-123");
    }

    @Test
    @DisplayName("ResourceNotFoundException should accept Long id")
    void resourceNotFoundException_withLongId_shouldFormatMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Activity", 456L);

        assertThat(ex.getErrorCode()).isEqualTo("ACTIVITY_NOT_FOUND");
        assertThat(ex.getMessage()).contains("Activity");
        assertThat(ex.getMessage()).contains("456");
    }

    @Test
    @DisplayName("UnauthorizedException should use default message")
    void unauthorizedException_defaultConstructor_shouldUseDefaultMessage() {
        UnauthorizedException ex = new UnauthorizedException();

        assertThat(ex.getErrorCode()).isEqualTo("AUTH_REQUIRED");
        assertThat(ex.getMessage()).isEqualTo("Authentication is required");
    }

    @Test
    @DisplayName("UnauthorizedException should accept custom message")
    void unauthorizedException_withMessage_shouldUseCustomMessage() {
        UnauthorizedException ex = new UnauthorizedException("Session expired");

        assertThat(ex.getErrorCode()).isEqualTo("AUTH_REQUIRED");
        assertThat(ex.getMessage()).isEqualTo("Session expired");
    }

    @Test
    @DisplayName("ForbiddenException should use default message")
    void forbiddenException_defaultConstructor_shouldUseDefaultMessage() {
        ForbiddenException ex = new ForbiddenException();

        assertThat(ex.getErrorCode()).isEqualTo("ACCESS_DENIED");
        assertThat(ex.getMessage()).isEqualTo("You do not have permission to perform this action");
    }

    @Test
    @DisplayName("ForbiddenException should accept custom message")
    void forbiddenException_withMessage_shouldUseCustomMessage() {
        ForbiddenException ex = new ForbiddenException("Cannot edit this trip");

        assertThat(ex.getErrorCode()).isEqualTo("ACCESS_DENIED");
        assertThat(ex.getMessage()).isEqualTo("Cannot edit this trip");
    }

    @Test
    @DisplayName("ForbiddenException should format resource type and action")
    void forbiddenException_withResourceAndAction_shouldFormatMessage() {
        ForbiddenException ex = new ForbiddenException("trip", "delete");

        assertThat(ex.getErrorCode()).isEqualTo("ACCESS_DENIED");
        assertThat(ex.getMessage()).isEqualTo("You do not have permission to delete this trip");
    }
}
