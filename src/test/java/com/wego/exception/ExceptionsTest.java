package com.wego.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for exception classes.
 *
 * Tests input validation and proper error code/message handling.
 */
class ExceptionsTest {

    @Nested
    @DisplayName("BusinessException")
    class BusinessExceptionTests {

        @Test
        @DisplayName("Should store error code and message")
        void businessException_withValidInput_shouldStoreErrorCodeAndMessage() {
            BusinessException ex = new BusinessException("TEST_ERROR", "Test message");

            assertThat(ex.getErrorCode()).isEqualTo("TEST_ERROR");
            assertThat(ex.getMessage()).isEqualTo("Test message");
        }

        @Test
        @DisplayName("Should store cause")
        void businessException_withCause_shouldStoreCause() {
            Throwable cause = new RuntimeException("Original error");
            BusinessException ex = new BusinessException("TEST_ERROR", "Test message", cause);

            assertThat(ex.getErrorCode()).isEqualTo("TEST_ERROR");
            assertThat(ex.getMessage()).isEqualTo("Test message");
            assertThat(ex.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when errorCode is null")
        void businessException_withNullErrorCode_shouldThrowException() {
            assertThatThrownBy(() -> new BusinessException(null, "Test message"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("errorCode must not be null or blank");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when errorCode is blank")
        void businessException_withBlankErrorCode_shouldThrowException() {
            assertThatThrownBy(() -> new BusinessException("   ", "Test message"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("errorCode must not be null or blank");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when errorCode is empty")
        void businessException_withEmptyErrorCode_shouldThrowException() {
            assertThatThrownBy(() -> new BusinessException("", "Test message"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("errorCode must not be null or blank");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException with cause when errorCode is null")
        void businessException_withCauseAndNullErrorCode_shouldThrowException() {
            Throwable cause = new RuntimeException("Original error");
            assertThatThrownBy(() -> new BusinessException(null, "Test message", cause))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("errorCode must not be null or blank");
        }
    }

    @Nested
    @DisplayName("ResourceNotFoundException")
    class ResourceNotFoundExceptionTests {

        @Test
        @DisplayName("Should format message correctly")
        void resourceNotFoundException_withValidInput_shouldFormatMessage() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Trip", "abc-123");

            assertThat(ex.getErrorCode()).isEqualTo("TRIP_NOT_FOUND");
            assertThat(ex.getMessage()).contains("Trip");
            assertThat(ex.getMessage()).contains("abc-123");
        }

        @Test
        @DisplayName("Should accept Long id")
        void resourceNotFoundException_withLongId_shouldFormatMessage() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Activity", 456L);

            assertThat(ex.getErrorCode()).isEqualTo("ACTIVITY_NOT_FOUND");
            assertThat(ex.getMessage()).contains("Activity");
            assertThat(ex.getMessage()).contains("456");
        }

        @Test
        @DisplayName("Should throw NullPointerException when resourceType is null")
        void resourceNotFoundException_withNullResourceType_shouldThrowException() {
            assertThatThrownBy(() -> new ResourceNotFoundException(null, "123"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("resourceType must not be null");
        }

        @Test
        @DisplayName("Should throw NullPointerException when resourceId is null (String)")
        void resourceNotFoundException_withNullResourceId_shouldThrowException() {
            assertThatThrownBy(() -> new ResourceNotFoundException("Trip", (String) null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("resourceId must not be null");
        }

        @Test
        @DisplayName("Should throw NullPointerException when resourceId is null (Long)")
        void resourceNotFoundException_withNullLongId_shouldThrowException() {
            assertThatThrownBy(() -> new ResourceNotFoundException("Trip", (Long) null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("resourceId must not be null");
        }
    }

    @Nested
    @DisplayName("UnauthorizedException")
    class UnauthorizedExceptionTests {

        @Test
        @DisplayName("Should use default message")
        void unauthorizedException_defaultConstructor_shouldUseDefaultMessage() {
            UnauthorizedException ex = new UnauthorizedException();

            assertThat(ex.getErrorCode()).isEqualTo("AUTH_REQUIRED");
            assertThat(ex.getMessage()).isEqualTo("Authentication is required");
        }

        @Test
        @DisplayName("Should accept custom message")
        void unauthorizedException_withMessage_shouldUseCustomMessage() {
            UnauthorizedException ex = new UnauthorizedException("Session expired");

            assertThat(ex.getErrorCode()).isEqualTo("AUTH_REQUIRED");
            assertThat(ex.getMessage()).isEqualTo("Session expired");
        }
    }

    @Nested
    @DisplayName("ForbiddenException")
    class ForbiddenExceptionTests {

        @Test
        @DisplayName("Should use default message")
        void forbiddenException_defaultConstructor_shouldUseDefaultMessage() {
            ForbiddenException ex = new ForbiddenException();

            assertThat(ex.getErrorCode()).isEqualTo("ACCESS_DENIED");
            assertThat(ex.getMessage()).isEqualTo("You do not have permission to perform this action");
        }

        @Test
        @DisplayName("Should accept custom message")
        void forbiddenException_withMessage_shouldUseCustomMessage() {
            ForbiddenException ex = new ForbiddenException("Cannot edit this trip");

            assertThat(ex.getErrorCode()).isEqualTo("ACCESS_DENIED");
            assertThat(ex.getMessage()).isEqualTo("Cannot edit this trip");
        }

        @Test
        @DisplayName("Should format resource type and action")
        void forbiddenException_withResourceAndAction_shouldFormatMessage() {
            ForbiddenException ex = new ForbiddenException("trip", "delete");

            assertThat(ex.getErrorCode()).isEqualTo("ACCESS_DENIED");
            assertThat(ex.getMessage()).isEqualTo("You do not have permission to delete this trip");
        }
    }
}
