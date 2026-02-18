package com.wego.controller.web;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ErrorController.
 * Tests error page model attributes for various HTTP status codes.
 */
class ErrorControllerTest {

    private ErrorController errorController;
    private Model model;

    @BeforeEach
    void setUp() {
        errorController = new ErrorController();
        model = new ConcurrentModel();
    }

    @ParameterizedTest
    @CsvSource({
            "400, BAD_REQUEST, 請求錯誤",
            "401, UNAUTHORIZED, 需要登入",
            "403, FORBIDDEN, 無法存取此頁面",
            "404, NOT_FOUND, 找不到頁面",
            "405, METHOD_NOT_ALLOWED, 不支援的操作",
            "500, INTERNAL_ERROR, 伺服器錯誤",
            "502, BAD_GATEWAY, 服務暫時無法使用",
            "503, SERVICE_UNAVAILABLE, 服務暫時維護中"
    })
    @DisplayName("should set correct error attributes for known status codes")
    void handleError_knownStatusCodes_shouldSetCorrectAttributes(
            int statusCode, String expectedCode, String expectedTitle) {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, statusCode);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/test-path");

        String viewName = errorController.handleError(request, model);

        assertThat(viewName).isEqualTo("error/error");
        assertThat(model.getAttribute("statusCode")).isEqualTo(statusCode);
        assertThat(model.getAttribute("errorCode")).isEqualTo(expectedCode);
        assertThat(model.getAttribute("errorTitle")).isEqualTo(expectedTitle);
        assertThat(model.getAttribute("requestUri")).isEqualTo("/test-path");
        assertThat(model.getAttribute("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("should default to 500 when status code attribute is null")
    void handleError_nullStatusCode_shouldDefaultTo500() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        String viewName = errorController.handleError(request, model);

        assertThat(viewName).isEqualTo("error/error");
        assertThat(model.getAttribute("statusCode")).isEqualTo(500);
        assertThat(model.getAttribute("errorCode")).isEqualTo("INTERNAL_ERROR");
        assertThat(model.getAttribute("errorTitle")).isEqualTo("伺服器錯誤");
    }

    @Test
    @DisplayName("should return generic error for unknown status code")
    void handleError_unknownStatusCode_shouldReturnGenericError() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 418);

        String viewName = errorController.handleError(request, model);

        assertThat(viewName).isEqualTo("error/error");
        assertThat(model.getAttribute("errorCode")).isEqualTo("UNKNOWN_ERROR");
        assertThat(model.getAttribute("errorTitle")).isEqualTo("發生錯誤");
    }

    @Test
    @DisplayName("should include error details from exception")
    void handleError_withException_shouldIncludeDetails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION,
                new RuntimeException("Database connection failed"));

        errorController.handleError(request, model);

        assertThat(model.getAttribute("errorDetails")).isEqualTo("Database connection failed");
    }

    @Test
    @DisplayName("should include error details from message when no exception")
    void handleError_withMessage_shouldIncludeDetails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Resource not found");

        errorController.handleError(request, model);

        assertThat(model.getAttribute("errorDetails")).isEqualTo("Resource not found");
    }

    @Test
    @DisplayName("should not set error details when both exception and message are null")
    void handleError_noExceptionOrMessage_shouldNotSetDetails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);

        errorController.handleError(request, model);

        assertThat(model.getAttribute("errorDetails")).isNull();
    }

    @Test
    @DisplayName("should not set error details when message is empty string")
    void handleError_emptyMessage_shouldNotSetDetails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "");

        errorController.handleError(request, model);

        assertThat(model.getAttribute("errorDetails")).isNull();
    }
}
