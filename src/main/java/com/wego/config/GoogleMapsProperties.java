package com.wego.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Google Maps API integration.
 *
 * @contract
 *   - apiKey: Google Maps API Key for authentication
 *   - enabled: Whether the Google Maps integration is enabled
 *   - connectTimeoutMs: Connection timeout in milliseconds (default: 5000)
 *   - readTimeoutMs: Read timeout in milliseconds (default: 10000)
 *
 * @see com.wego.service.external.GoogleMapsService
 */
@Configuration
@ConfigurationProperties(prefix = "wego.external-api.google-maps")
@Getter
@Setter
public class GoogleMapsProperties {

    /**
     * Google Maps API Key.
     * Required for all Google Maps API calls.
     */
    private String apiKey;

    /**
     * Whether Google Maps integration is enabled.
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
