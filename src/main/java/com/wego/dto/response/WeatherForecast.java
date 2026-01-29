package com.wego.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO representing a single day's weather forecast.
 *
 * Contains temperature, weather conditions, and precipitation probability.
 *
 * @contract
 *   - date: The date this forecast is for
 *   - tempHigh: Maximum temperature in Celsius
 *   - tempLow: Minimum temperature in Celsius
 *   - rainProbability: Probability of precipitation (0.0 - 1.0)
 *   - icon: Weather icon code (e.g., "01d", "10n")
 *
 * @see com.wego.service.WeatherService
 * @see com.wego.service.external.WeatherClient
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherForecast {

    /**
     * The date this forecast is for.
     */
    private LocalDate date;

    /**
     * Weather condition main category (e.g., "Clear", "Clouds", "Rain").
     */
    private String condition;

    /**
     * Detailed weather description (e.g., "scattered clouds", "light rain").
     */
    private String description;

    /**
     * OpenWeatherMap icon code for displaying weather icon.
     * Format: {id}{d/n} where d=day, n=night.
     * Example: "01d" (clear sky day), "10n" (rain night).
     */
    private String icon;

    /**
     * Maximum temperature for the day in Celsius.
     */
    private double tempHigh;

    /**
     * Minimum temperature for the day in Celsius.
     */
    private double tempLow;

    /**
     * Average temperature for the day in Celsius.
     */
    private double tempAvg;

    /**
     * Humidity percentage (0-100).
     */
    private int humidity;

    /**
     * Probability of precipitation (0.0 - 1.0).
     */
    private double rainProbability;

    /**
     * Wind speed in meters per second.
     */
    private double windSpeed;

    /**
     * Returns the icon URL for OpenWeatherMap icons.
     *
     * @contract
     *   - pre: icon != null
     *   - post: Returns valid URL string
     *
     * @return URL to the weather icon image
     */
    public String getIconUrl() {
        if (icon == null || icon.isEmpty()) {
            return null;
        }
        return String.format("https://openweathermap.org/img/wn/%s@2x.png", icon);
    }

    /**
     * Returns rain probability as a percentage string.
     *
     * @return Rain probability as percentage (e.g., "30%")
     */
    public String getRainProbabilityPercent() {
        return String.format("%.0f%%", rainProbability * 100);
    }

    /**
     * Checks if this forecast indicates rainy weather.
     *
     * @return true if rain probability is >= 50%
     */
    public boolean isRainy() {
        return rainProbability >= 0.5;
    }
}
