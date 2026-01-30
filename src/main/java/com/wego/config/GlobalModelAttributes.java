package com.wego.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global model attributes for all controllers.
 * Provides common data needed across all Thymeleaf templates.
 *
 * @contract
 *   - post: currentPath is available in all templates
 *   - calledBy: Spring MVC framework
 */
@ControllerAdvice
public class GlobalModelAttributes {

    /**
     * Adds the current request URI to all models.
     * This enables active navigation state detection in templates.
     *
     * @param request The HTTP request
     * @return The current request URI (e.g., "/dashboard", "/trips/123")
     */
    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
