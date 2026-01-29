package com.wego.controller.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for handling error pages.
 *
 * @contract
 *   - Handles 404, 500 and other HTTP error codes
 *   - Returns appropriate error templates
 */
@Controller
@Slf4j
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            // Log error details
            log.warn("Error occurred: status={}, uri={}", statusCode,
                    request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));

            // Add exception to model for dev mode
            if (exception != null) {
                model.addAttribute("exception", exception);
            }

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "error/404";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                return "error/403";
            } else if (statusCode >= 500) {
                return "error/500";
            }
        }

        // Default to 500 error page
        return "error/500";
    }
}
