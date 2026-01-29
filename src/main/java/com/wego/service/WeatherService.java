package com.wego.service;

import com.wego.dto.response.WeatherForecast;
import com.wego.dto.response.WeatherResponse;
import com.wego.service.external.WeatherClient;
import com.wego.service.external.WeatherException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for weather forecast operations.
 *
 * Provides cached weather forecasts using OpenWeatherMap API
 * with a 6-hour cache TTL to minimize API calls.
 *
 * @contract
 *   - Uses CacheService for 6-hour TTL caching
 *   - Validates date range (only 5 days from today)
 *   - Returns unavailable response for dates beyond range
 *
 * @see WeatherClient
 * @see CacheService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private static final long CACHE_TTL_MS = 6 * 60 * 60 * 1000L; // 6 hours
    private static final int FORECAST_DAYS_LIMIT = 5;
    private static final String CACHE_KEY_PREFIX = "weather:forecast:";

    private final WeatherClient weatherClient;
    private final CacheService cacheService;

    /**
     * Gets weather forecast for a specific location and date.
     *
     * @contract
     *   - pre: lat between -90 and 90
     *   - pre: lng between -180 and 180
     *   - pre: date != null
     *   - post: Returns WeatherResponse (may indicate unavailability)
     *   - calls: WeatherClient#get5DayForecast, CacheService
     *   - calledBy: WeatherApiController#getWeather
     *
     * @param lat Latitude of the location
     * @param lng Longitude of the location
     * @param date The date to get forecast for
     * @return WeatherResponse with forecast or unavailability message
     */
    public WeatherResponse getWeatherForDate(double lat, double lng, LocalDate date) {
        log.debug("Getting weather for ({}, {}) on {}", lat, lng, date);

        // Validate coordinates
        validateCoordinates(lat, lng);

        // Check if date is within forecast range (5 days from today)
        if (!isDateWithinForecastRange(date)) {
            log.debug("Date {} is beyond 5-day forecast range", date);
            return WeatherResponse.notAvailable(lat, lng, date);
        }

        // Try to get from cache first
        List<WeatherForecast> forecasts = getCachedOrFetch(lat, lng);

        // Find forecast for the requested date
        Optional<WeatherForecast> forecastForDate = forecasts.stream()
                .filter(f -> f.getDate().equals(date))
                .findFirst();

        if (forecastForDate.isPresent()) {
            return WeatherResponse.forDate(lat, lng, date, forecastForDate.get());
        } else {
            // Date within range but no data available (shouldn't happen normally)
            log.warn("No forecast data for {} at ({}, {})", date, lat, lng);
            return WeatherResponse.notAvailable(lat, lng, date);
        }
    }

    /**
     * Gets all available forecasts for a location (up to 5 days).
     *
     * @contract
     *   - pre: lat between -90 and 90
     *   - pre: lng between -180 and 180
     *   - post: Returns WeatherResponse with list of forecasts
     *   - calls: WeatherClient#get5DayForecast, CacheService
     *   - calledBy: WeatherApiController#getWeatherForecast
     *
     * @param lat Latitude of the location
     * @param lng Longitude of the location
     * @return WeatherResponse with all available forecasts
     */
    public WeatherResponse getFullForecast(double lat, double lng) {
        log.debug("Getting full forecast for ({}, {})", lat, lng);

        validateCoordinates(lat, lng);

        List<WeatherForecast> forecasts = getCachedOrFetch(lat, lng);

        return WeatherResponse.builder()
                .latitude(lat)
                .longitude(lng)
                .available(true)
                .forecasts(forecasts)
                .build();
    }

    /**
     * Gets forecasts from cache or fetches from API.
     */
    @SuppressWarnings("unchecked")
    private List<WeatherForecast> getCachedOrFetch(double lat, double lng) {
        String cacheKey = buildCacheKey(lat, lng);

        // Try cache first
        Optional<List> cached = cacheService.get(cacheKey, List.class);
        if (cached.isPresent()) {
            log.debug("Cache hit for weather at ({}, {})", lat, lng);
            return (List<WeatherForecast>) cached.get();
        }

        log.debug("Cache miss for weather at ({}, {}), fetching from API", lat, lng);

        // Fetch from API
        List<WeatherForecast> forecasts = weatherClient.get5DayForecast(lat, lng);

        // Cache the result
        cacheService.put(cacheKey, forecasts, CACHE_TTL_MS);

        return forecasts;
    }

    /**
     * Builds cache key for location.
     * Uses 2 decimal places for coordinate precision (about 1.1km accuracy).
     */
    private String buildCacheKey(double lat, double lng) {
        return String.format("%s%.2f:%.2f", CACHE_KEY_PREFIX, lat, lng);
    }

    /**
     * Checks if the date is within the 5-day forecast range.
     */
    private boolean isDateWithinForecastRange(LocalDate date) {
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(FORECAST_DAYS_LIMIT);

        return !date.isBefore(today) && date.isBefore(maxDate);
    }

    /**
     * Validates latitude and longitude values.
     */
    private void validateCoordinates(double lat, double lng) {
        if (lat < -90 || lat > 90) {
            throw WeatherException.invalidLocation(lat, lng);
        }
        if (lng < -180 || lng > 180) {
            throw WeatherException.invalidLocation(lat, lng);
        }
    }

    /**
     * Evicts cached weather data for a location.
     * Can be used when cache needs to be refreshed.
     *
     * @contract
     *   - pre: lat between -90 and 90
     *   - pre: lng between -180 and 180
     *   - post: Cache entry is removed
     *
     * @param lat Latitude of the location
     * @param lng Longitude of the location
     */
    public void evictCache(double lat, double lng) {
        String cacheKey = buildCacheKey(lat, lng);
        cacheService.evict(cacheKey);
        log.debug("Evicted cache for weather at ({}, {})", lat, lng);
    }

    /**
     * Evicts all cached weather data.
     *
     * @contract
     *   - post: All weather cache entries are removed
     */
    public void evictAllCache() {
        cacheService.evictByPrefix(CACHE_KEY_PREFIX);
        log.info("Evicted all weather cache entries");
    }
}
