package com.wego.controller.api;

import com.wego.dto.ApiResponse;
import com.wego.dto.response.WeatherResponse;
import com.wego.service.RateLimitService;
import com.wego.service.WeatherService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST API controller for weather forecast operations.
 *
 * Provides endpoints for getting weather forecasts by location and date.
 *
 * @contract
 *   - pre: User may or may not be authenticated (public endpoint)
 *   - post: Returns standardized ApiResponse format
 *   - calls: WeatherService
 *   - calledBy: Frontend API calls
 *
 * @see WeatherService
 */
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
@Slf4j
public class WeatherApiController {

    private final WeatherService weatherService;
    private final RateLimitService rateLimitService;

    private static final int WEATHER_RATE_LIMIT_PER_MINUTE = 30;

    /**
     * Gets weather forecast for a specific location and date.
     *
     * @contract
     *   - pre: lat between -90 and 90
     *   - pre: lng between -180 and 180
     *   - pre: date is valid ISO date format (YYYY-MM-DD)
     *   - post: Returns WeatherResponse (may indicate unavailability if date > 5 days)
     *   - calls: WeatherService#getWeatherForDate
     *
     * @param lat Latitude of the location
     * @param lng Longitude of the location
     * @param date The date to get forecast for (ISO format: YYYY-MM-DD)
     * @return Weather forecast for the specified location and date
     */
    @GetMapping
    public ResponseEntity<ApiResponse<WeatherResponse>> getWeather(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest httpRequest) {

        if (!rateLimitService.isAllowed("weather:" + getClientIp(httpRequest), WEATHER_RATE_LIMIT_PER_MINUTE)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("RATE_LIMIT_EXCEEDED", "Too many requests"));
        }

        log.debug("Weather request: lat={}, lng={}, date={}", lat, lng, date);

        WeatherResponse response = weatherService.getWeatherForDate(lat, lng, date);

        if (response.isAvailable()) {
            return ResponseEntity.ok(ApiResponse.success(response));
        } else {
            return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
        }
    }

    /**
     * Gets full 5-day weather forecast for a location.
     *
     * @contract
     *   - pre: lat between -90 and 90
     *   - pre: lng between -180 and 180
     *   - post: Returns list of forecasts for next 5 days
     *   - calls: WeatherService#getFullForecast
     *
     * @param lat Latitude of the location
     * @param lng Longitude of the location
     * @return Weather forecast for the next 5 days
     */
    @GetMapping("/forecast")
    public ResponseEntity<ApiResponse<WeatherResponse>> getFullForecast(
            @RequestParam double lat,
            @RequestParam double lng,
            HttpServletRequest httpRequest) {

        if (!rateLimitService.isAllowed("weather:" + getClientIp(httpRequest), WEATHER_RATE_LIMIT_PER_MINUTE)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("RATE_LIMIT_EXCEEDED", "Too many requests"));
        }

        log.debug("Full forecast request: lat={}, lng={}", lat, lng);

        WeatherResponse response = weatherService.getFullForecast(lat, lng);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null) ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
