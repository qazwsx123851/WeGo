package com.wego.controller.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for handling error pages.
 *
 * @contract
 *   - Handles all HTTP error codes
 *   - Returns unified error template with dynamic content
 *   - calledBy: Spring error handling mechanism
 */
@Controller
@Slf4j
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        int statusCode = 500;
        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
        }

        // Log error details
        log.warn("Error occurred: status={}, uri={}, message={}",
                statusCode, requestUri, message);

        // Set common model attributes
        model.addAttribute("statusCode", statusCode);
        model.addAttribute("requestUri", requestUri);
        model.addAttribute("timestamp", LocalDateTime.now().format(FORMATTER));

        // Set error-specific attributes based on status code
        configureErrorAttributes(model, statusCode, message, exception);

        return "error/error";
    }

    /**
     * Configure error-specific model attributes.
     *
     * @contract
     *   - pre: model != null, statusCode is valid HTTP status
     *   - post: model contains errorTitle, errorMessage, errorCode
     */
    private void configureErrorAttributes(Model model, int statusCode, Object message, Object exception) {
        switch (statusCode) {
            case 400 -> {
                model.addAttribute("errorTitle", "請求錯誤");
                model.addAttribute("errorMessage", "您的請求格式不正確或缺少必要參數。<br/>請檢查後再試。");
                model.addAttribute("errorCode", "BAD_REQUEST");
            }
            case 401 -> {
                model.addAttribute("errorTitle", "需要登入");
                model.addAttribute("errorMessage", "您需要登入才能存取此頁面。<br/>請先登入您的帳號。");
                model.addAttribute("errorCode", "UNAUTHORIZED");
            }
            case 403 -> {
                model.addAttribute("errorTitle", "無法存取此頁面");
                model.addAttribute("errorMessage", "您沒有權限存取這個頁面。<br/>如果您認為這是錯誤，請聯繫行程擁有者。");
                model.addAttribute("errorCode", "FORBIDDEN");
            }
            case 404 -> {
                model.addAttribute("errorTitle", "找不到頁面");
                model.addAttribute("errorMessage", "這個頁面可能已被刪除，或是連結有誤。<br/>讓我們帶你回到正確的路線上吧！");
                model.addAttribute("errorCode", "NOT_FOUND");
            }
            case 405 -> {
                model.addAttribute("errorTitle", "不支援的操作");
                model.addAttribute("errorMessage", "此頁面不支援您請求的操作方式。<br/>請確認您的操作是否正確。");
                model.addAttribute("errorCode", "METHOD_NOT_ALLOWED");
            }
            case 500 -> {
                model.addAttribute("errorTitle", "伺服器錯誤");
                model.addAttribute("errorMessage", "伺服器遇到了一些技術問題。<br/>請稍後再試，或聯繫我們的支援團隊。");
                model.addAttribute("errorCode", "INTERNAL_ERROR");
            }
            case 502 -> {
                model.addAttribute("errorTitle", "服務暫時無法使用");
                model.addAttribute("errorMessage", "外部服務目前無法回應。<br/>請稍後再試。");
                model.addAttribute("errorCode", "BAD_GATEWAY");
            }
            case 503 -> {
                model.addAttribute("errorTitle", "服務暫時維護中");
                model.addAttribute("errorMessage", "我們正在進行系統維護，很快就會恢復。<br/>請稍後再試。");
                model.addAttribute("errorCode", "SERVICE_UNAVAILABLE");
            }
            default -> {
                model.addAttribute("errorTitle", "發生錯誤");
                model.addAttribute("errorMessage", "發生了未預期的錯誤。<br/>請稍後再試。");
                model.addAttribute("errorCode", "UNKNOWN_ERROR");
            }
        }

        // Add exception details for development debugging
        if (exception != null) {
            Throwable ex = (Throwable) exception;
            model.addAttribute("errorDetails", ex.getMessage());
        } else if (message != null && !message.toString().isEmpty()) {
            model.addAttribute("errorDetails", message.toString());
        }
    }
}
