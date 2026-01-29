package com.wego.service.external;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for GoogleMapsException.
 *
 * @see GoogleMapsException
 */
@DisplayName("GoogleMapsException")
class GoogleMapsExceptionTest {

    @Nested
    @DisplayName("Constructors")
    class Constructors {

        @Test
        @DisplayName("should create with message only")
        void shouldCreateWithMessageOnly() {
            GoogleMapsException exception = new GoogleMapsException("Test error message");

            assertThat(exception.getMessage()).isEqualTo("Test error message");
            assertThat(exception.getErrorCode()).isEqualTo("GOOGLE_MAPS_ERROR");
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("should create with error code and message")
        void shouldCreateWithErrorCodeAndMessage() {
            GoogleMapsException exception = new GoogleMapsException("CUSTOM_ERROR", "Custom error message");

            assertThat(exception.getMessage()).isEqualTo("Custom error message");
            assertThat(exception.getErrorCode()).isEqualTo("CUSTOM_ERROR");
        }

        @Test
        @DisplayName("should create with message and cause")
        void shouldCreateWithMessageAndCause() {
            RuntimeException cause = new RuntimeException("Root cause");
            GoogleMapsException exception = new GoogleMapsException("Error occurred", cause);

            assertThat(exception.getMessage()).isEqualTo("Error occurred");
            assertThat(exception.getErrorCode()).isEqualTo("GOOGLE_MAPS_ERROR");
            assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("should create with error code, message, and cause")
        void shouldCreateWithErrorCodeMessageAndCause() {
            RuntimeException cause = new RuntimeException("Root cause");
            GoogleMapsException exception = new GoogleMapsException("API_TIMEOUT", "Request timed out", cause);

            assertThat(exception.getMessage()).isEqualTo("Request timed out");
            assertThat(exception.getErrorCode()).isEqualTo("API_TIMEOUT");
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("apiError should create exception with API_ERROR code")
        void apiErrorShouldCreateExceptionWithApiErrorCode() {
            GoogleMapsException exception = GoogleMapsException.apiError("Invalid request");

            assertThat(exception.getErrorCode()).isEqualTo("API_ERROR");
            assertThat(exception.getMessage()).contains("Invalid request");
        }

        @Test
        @DisplayName("rateLimitExceeded should create exception with RATE_LIMIT_EXCEEDED code")
        void rateLimitExceededShouldCreateExceptionWithRateLimitCode() {
            GoogleMapsException exception = GoogleMapsException.rateLimitExceeded();

            assertThat(exception.getErrorCode()).isEqualTo("RATE_LIMIT_EXCEEDED");
            assertThat(exception.getMessage()).contains("rate limit");
        }

        @Test
        @DisplayName("invalidApiKey should create exception with INVALID_API_KEY code")
        void invalidApiKeyShouldCreateExceptionWithInvalidKeyCode() {
            GoogleMapsException exception = GoogleMapsException.invalidApiKey();

            assertThat(exception.getErrorCode()).isEqualTo("INVALID_API_KEY");
            assertThat(exception.getMessage()).contains("API key");
        }

        @Test
        @DisplayName("noResults should create exception with NO_RESULTS code")
        void noResultsShouldCreateExceptionWithNoResultsCode() {
            GoogleMapsException exception = GoogleMapsException.noResults("Tokyo Station");

            assertThat(exception.getErrorCode()).isEqualTo("NO_RESULTS");
            assertThat(exception.getMessage()).contains("Tokyo Station");
        }
    }

    @Nested
    @DisplayName("Exception Behavior")
    class ExceptionBehavior {

        @Test
        @DisplayName("should be throwable")
        void shouldBeThrowable() {
            assertThatThrownBy(() -> {
                throw GoogleMapsException.apiError("Test error");
            })
            .isInstanceOf(GoogleMapsException.class)
            .hasMessageContaining("Test error");
        }

        @Test
        @DisplayName("should be catchable as RuntimeException")
        void shouldBeCatchableAsRuntimeException() {
            assertThatThrownBy(() -> {
                throw GoogleMapsException.invalidApiKey();
            })
            .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should preserve stack trace")
        void shouldPreserveStackTrace() {
            // Use constructor directly to test from this class
            GoogleMapsException exception = new GoogleMapsException("Test");

            assertThat(exception.getStackTrace()).isNotEmpty();
            // Stack trace should contain the test class somewhere
            boolean containsTestClass = false;
            for (StackTraceElement element : exception.getStackTrace()) {
                if (element.getClassName().contains("GoogleMapsExceptionTest")) {
                    containsTestClass = true;
                    break;
                }
            }
            assertThat(containsTestClass).isTrue();
        }
    }

    @Nested
    @DisplayName("Error Code Getter")
    class ErrorCodeGetter {

        @Test
        @DisplayName("getErrorCode should return correct code")
        void getErrorCodeShouldReturnCorrectCode() {
            GoogleMapsException exception = new GoogleMapsException("CUSTOM_CODE", "Message");

            assertThat(exception.getErrorCode()).isEqualTo("CUSTOM_CODE");
        }
    }

    @Nested
    @DisplayName("Chained Exceptions")
    class ChainedExceptions {

        @Test
        @DisplayName("should support exception chaining")
        void shouldSupportExceptionChaining() {
            Exception rootCause = new IllegalArgumentException("Invalid parameter");
            Exception intermediateCause = new RuntimeException("Processing failed", rootCause);
            GoogleMapsException exception = new GoogleMapsException(
                    "MAPS_ERROR",
                    "Google Maps call failed",
                    intermediateCause
            );

            assertThat(exception.getCause()).isEqualTo(intermediateCause);
            assertThat(exception.getCause().getCause()).isEqualTo(rootCause);
        }
    }
}
