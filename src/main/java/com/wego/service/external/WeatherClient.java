package com.wego.service.external;

import com.wego.dto.response.WeatherForecast;

import java.util.List;

/**
 * Interface for weather API operations.
 * Allows swapping between real and mock implementations.
 *
 * @contract
 *   - All methods throw WeatherException on API failures
 *   - Mock implementation uses randomized realistic data
 *   - Real implementation calls OpenWeatherMap 5-day forecast API
 *
 * @see MockWeatherClient
 * @see OpenWeatherMapClient
 */
public interface WeatherClient {

    /**
     * Gets 5-day weather forecast for a location.
     *
     * @contract
     *   - pre: latitude between -90 and 90
     *   - pre: longitude between -180 and 180
     *   - post: Returns list of daily forecasts (up to 5 days)
     *   - throws: WeatherException if API call fails
     *   - throws: WeatherException if location is invalid
     *
     * @param lat Latitude of the location
     * @param lng Longitude of the location
     * @return List of WeatherForecast for the next 5 days
     */
    List<WeatherForecast> get5DayForecast(double lat, double lng);
}
