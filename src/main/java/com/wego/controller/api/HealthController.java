package com.wego.controller.api;

import com.wego.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health check endpoint for monitoring and load balancers.
 *
 * @contract
 *   - post: Returns status "healthy" when application is running
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * Returns application health status.
     *
     * @contract
     *   - pre: Application is running
     *   - post: Returns success response with status "healthy"
     *   - calledBy: Load balancers, monitoring systems
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of(
            "status", "healthy",
            "application", "WeGo"
        ));
    }
}
