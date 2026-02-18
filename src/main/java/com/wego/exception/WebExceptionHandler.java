package com.wego.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Exception handler for Web controllers (non-API).
 *
 * Redirects to error pages instead of returning JSON.
 * Only applies to controllers in the web package.
 *
 * @contract
 *   - post: All exceptions redirect to error/error template
 *   - calledBy: Spring exception handling mechanism
 */
@Slf4j
@Order(1)
@ControllerAdvice(basePackages = "com.wego.controller.web")
public class WebExceptionHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Handles ResourceNotFoundException for web controllers.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Web resource not found: {}", ex.getMessage());
        return createErrorView(HttpStatus.NOT_FOUND, ex.getErrorCode(), ex.getMessage(), request);
    }

    /**
     * Handles UnauthorizedException for web controllers.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ModelAndView handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        log.warn("Web unauthorized access: {}", ex.getMessage());
        return createErrorView(HttpStatus.UNAUTHORIZED, ex.getErrorCode(), ex.getMessage(), request);
    }

    /**
     * Handles ForbiddenException for web controllers.
     */
    @ExceptionHandler(ForbiddenException.class)
    public ModelAndView handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        log.warn("Web access denied: {}", ex.getMessage());
        return createErrorView(HttpStatus.FORBIDDEN, ex.getErrorCode(), ex.getMessage(), request);
    }

    /**
     * Handles BusinessException for web controllers.
     */
    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Web business error: {} - {}", ex.getErrorCode(), ex.getMessage());
        return createErrorView(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage(), request);
    }

    /**
     * Handles ValidationException for web controllers.
     */
    @ExceptionHandler(ValidationException.class)
    public ModelAndView handleValidationException(ValidationException ex, HttpServletRequest request) {
        log.warn("Web validation error: {}", ex.getMessage());
        return createErrorView(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), request);
    }

    /**
     * Handles all other unexpected exceptions for web controllers.
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Web unexpected error occurred", ex);
        return createErrorView(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "發生非預期錯誤，請稍後再試。", request);
    }

    /**
     * Creates a ModelAndView for the error page.
     *
     * @contract
     *   - pre: status != null
     *   - post: Returns ModelAndView pointing to error/error template
     */
    private ModelAndView createErrorView(HttpStatus status, String errorCode, String details, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("error/error");

        mav.addObject("statusCode", status.value());
        mav.addObject("requestUri", request.getRequestURI());
        mav.addObject("timestamp", LocalDateTime.now().format(FORMATTER));
        mav.addObject("errorCode", errorCode);

        // Set error-specific title and message
        switch (status) {
            case BAD_REQUEST -> {
                mav.addObject("errorTitle", "請求錯誤");
                mav.addObject("errorMessage", "您的請求格式不正確或缺少必要參數。<br/>請檢查後再試。");
            }
            case UNAUTHORIZED -> {
                mav.addObject("errorTitle", "需要登入");
                mav.addObject("errorMessage", "您需要登入才能存取此頁面。<br/>請先登入您的帳號。");
            }
            case FORBIDDEN -> {
                mav.addObject("errorTitle", "無法存取此頁面");
                mav.addObject("errorMessage", "您沒有權限存取這個頁面。<br/>如果您認為這是錯誤，請聯繫行程擁有者。");
            }
            case NOT_FOUND -> {
                mav.addObject("errorTitle", "找不到頁面");
                mav.addObject("errorMessage", "這個頁面可能已被刪除，或是連結有誤。<br/>讓我們帶你回到正確的路線上吧！");
            }
            case INTERNAL_SERVER_ERROR -> {
                mav.addObject("errorTitle", "伺服器錯誤");
                mav.addObject("errorMessage", "伺服器遇到了一些技術問題。<br/>請稍後再試，或聯繫我們的支援團隊。");
            }
            default -> {
                mav.addObject("errorTitle", "發生錯誤");
                mav.addObject("errorMessage", "發生了未預期的錯誤。<br/>請稍後再試。");
            }
        }

        // Add technical details for debugging
        if (details != null && !details.isEmpty()) {
            mav.addObject("errorDetails", details);
        }

        mav.setStatus(status);
        return mav;
    }
}
