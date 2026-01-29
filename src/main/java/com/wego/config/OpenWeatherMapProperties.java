package com.wego.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for OpenWeatherMap API integration.
 *
 * @contract
 *   - apiKey: OpenWeatherMap API Key for authentication
 *   - enabled: Whether the OpenWeatherMap integration is enabled
 *   - connectTimeoutMs: Connection timeout in milliseconds (default: 5000)
 *   - readTimeoutMs: Read timeout in milliseconds (default: 10000)
 *
 * @see com.wego.service.external.OpenWeatherMapClient
 * @see com.wego.service.WeatherService
 */
@Configuration
@ConfigurationProperties(prefix = "wego.external-api.openweathermap")
@Getter
@Setter
public class OpenWeatherMapProperties {

    /**
     * OpenWeatherMap API Key.
     * Required for all OpenWeatherMap API calls.
     */
    private String apiKey;

    /**
     * Whether OpenWeatherMap integration is enabled.
     * When disabled, the service should use mock/fallback data.
     */
    private boolean enabled = false;

    /**
     * Connection timeout in milliseconds.
     * Time to wait for establishing a connection.
     */
    private int connectTimeoutMs = 5000;

    /**
     * Read timeout in milliseconds.
     * Time to wait for reading data after connection is established.
     */
    private int readTimeoutMs = 10000;
}
