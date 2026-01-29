package com.wego.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO representing the complete weather response for a location.
 *
 * Contains location information and a list of daily forecasts.
 *
 * @contract
 *   - latitude/longitude: Location coordinates
 *   - forecasts: List of daily forecasts (up to 5 days)
 *   - available: Whether forecast data is available for the requested date
 *
 * @see WeatherForecast
 * @see com.wego.service.WeatherService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherResponse {

    /**
     * Latitude of the forecast location.
     */
    private double latitude;

    /**
     * Longitude of the forecast location.
     */
    private double longitude;

    /**
     * Location name (if available from API).
     */
    private String locationName;

    /**
     * Country code (if available from API).
     */
    private String country;

    /**
     * The specific date requested (if single date requested).
     */
    private LocalDate requestedDate;

    /**
     * Whether forecast is available for the requested date.
     * Will be false if the date is beyond the 5-day forecast range.
     */
    private boolean available;

    /**
     * Message explaining availability status.
     * e.g., "Weather forecast not available" for dates beyond 5 days.
     */
    private String message;

    /**
     * Single day forecast for the requested date.
     * Null if date is beyond forecast range.
     */
    private WeatherForecast forecast;

    /**
     * List of all available daily forecasts.
     */
    private List<WeatherForecast> forecasts;

    /**
     * Creates an unavailable response for dates beyond the forecast range.
     *
     * @contract
     *   - pre: lat, lng are valid coordinates
     *   - pre: date is beyond 5-day forecast range
     *   - post: Returns response with available=false
     *
     * @param lat Latitude
     * @param lng Longitude
     * @param date The requested date
     * @return WeatherResponse indicating unavailability
     */
    public static WeatherResponse notAvailable(double lat, double lng, LocalDate date) {
        return WeatherResponse.builder()
                .latitude(lat)
                .longitude(lng)
                .requestedDate(date)
                .available(false)
                .message("Weather forecast not available for dates beyond 5 days from today")
                .build();
    }

    /**
     * Creates a successful response for a single date.
     *
     * @contract
     *   - pre: forecast != null
     *   - post: Returns response with available=true
     *
     * @param lat Latitude
     * @param lng Longitude
     * @param date The requested date
     * @param forecast The weather forecast for the date
     * @return WeatherResponse with the forecast data
     */
    public static WeatherResponse forDate(double lat, double lng, LocalDate date, WeatherForecast forecast) {
        return WeatherResponse.builder()
                .latitude(lat)
                .longitude(lng)
                .requestedDate(date)
                .available(true)
                .forecast(forecast)
                .build();
    }
}
