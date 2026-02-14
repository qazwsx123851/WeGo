package com.wego.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ApiResponse.
 */
class ApiResponseTest {

    @Test
    @DisplayName("Should create success response with data")
    void success_withData_shouldReturnSuccessResponse() {
        String data = "test data";

        ApiResponse<String> response = ApiResponse.success(data);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("test data");
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should create error response")
    void error_shouldReturnErrorResponse() {
        ApiResponse<Object> response = ApiResponse.error("TEST_ERROR", "Test error message");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getErrorCode()).isEqualTo("TEST_ERROR");
        assertThat(response.getMessage()).isEqualTo("Test error message");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should create success response with null data")
    void success_withNullData_shouldReturnSuccessResponse() {
        ApiResponse<Object> response = ApiResponse.success(null);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNull();
    }

}
