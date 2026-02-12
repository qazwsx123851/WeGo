package com.wego.service;

import com.wego.dto.response.WeatherForecast;
import com.wego.dto.response.WeatherResponse;
import com.wego.service.external.WeatherClient;
import com.wego.service.external.WeatherException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for weather forecast operations.
 *
 * Provides cached weather forecasts using OpenWeatherMap API
 * with a 6-hour cache TTL (managed by Spring Cache + Caffeine).
 *
 * @contract
 *   - Uses Spring Cache "weather" for 6-hour TTL caching
 *   - Validates date range (only 5 days from today)
 *   - Returns unavailable response for dates beyond range
 *
 * @see WeatherClient
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private static final int FORECAST_DAYS_LIMIT = 5;
    private static final String CACHE_KEY_PREFIX = "forecast:";

    private final WeatherClient weatherClient;
    private final CacheManager cacheManager;

    /**
     * Gets weather forecast for a specific location and date.
     *
     * @contract
     *   - pre: lat between -90 and 90
     *   - pre: lng between -180 and 180
     *   - pre: date != null
     *   - post: Returns WeatherResponse (may indicate unavailability)
     *   - calls: WeatherClient#get5DayForecast
     *   - calledBy: WeatherApiController#getWeather
     */
    public WeatherResponse getWeatherForDate(double lat, double lng, LocalDate date) {
        log.debug("Getting weather for ({}, {}) on {}", lat, lng, date);

        validateCoordinates(lat, lng);

        if (!isDateWithinForecastRange(date)) {
            log.debug("Date {} is beyond 5-day forecast range", date);
            return WeatherResponse.notAvailable(lat, lng, date);
        }

        List<WeatherForecast> forecasts = getCachedOrFetch(lat, lng);

        Optional<WeatherForecast> forecastForDate = forecasts.stream()
                .filter(f -> f.getDate().equals(date))
                .findFirst();

        if (forecastForDate.isPresent()) {
            return WeatherResponse.forDate(lat, lng, date, forecastForDate.get());
        } else {
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
     *   - calls: WeatherClient#get5DayForecast
     *   - calledBy: WeatherApiController#getWeatherForecast
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
        Cache cache = cacheManager.getCache("weather");

        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper != null) {
                log.debug("Cache hit for weather at ({}, {})", lat, lng);
                return (List<WeatherForecast>) wrapper.get();
            }
        }

        log.debug("Cache miss for weather at ({}, {}), fetching from API", lat, lng);

        List<WeatherForecast> forecasts = weatherClient.get5DayForecast(lat, lng);

        if (cache != null) {
            cache.put(cacheKey, forecasts);
        }

        return forecasts;
    }

    /**
     * Builds cache key for location.
     * Uses 2 decimal places for coordinate precision (about 1.1km accuracy).
     */
    private String buildCacheKey(double lat, double lng) {
        return String.format("%s%.2f:%.2f", CACHE_KEY_PREFIX, lat, lng);
    }

    private boolean isDateWithinForecastRange(LocalDate date) {
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(FORECAST_DAYS_LIMIT);
        return !date.isBefore(today) && date.isBefore(maxDate);
    }

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
     */
    public void evictCache(double lat, double lng) {
        String cacheKey = buildCacheKey(lat, lng);
        Cache cache = cacheManager.getCache("weather");
        if (cache != null) {
            cache.evict(cacheKey);
        }
        log.debug("Evicted cache for weather at ({}, {})", lat, lng);
    }

    /**
     * Evicts all cached weather data.
     */
    public void evictAllCache() {
        Cache cache = cacheManager.getCache("weather");
        if (cache != null) {
            cache.clear();
        }
        log.info("Evicted all weather cache entries");
    }
}
