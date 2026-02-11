package com.wego.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global model attributes for all controllers.
 * Provides common data needed across all Thymeleaf templates.
 *
 * @contract
 *   - post: currentPath is available in all templates
 *   - post: googleMapsApiKey is available in all templates (restricted embed-only key)
 *   - calledBy: Spring MVC framework
 */
@ControllerAdvice
public class GlobalModelAttributes {

    @Value("${wego.external-api.google-maps.embed-api-key:}")
    private String googleMapsApiKey;

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

    /**
     * Adds the Google Maps API key to all models.
     * Used for displaying static maps and map links in templates.
     *
     * @return The Google Maps API key (may be empty if not configured)
     */
    @ModelAttribute("googleMapsApiKey")
    public String googleMapsApiKey() {
        return googleMapsApiKey;
    }
}
