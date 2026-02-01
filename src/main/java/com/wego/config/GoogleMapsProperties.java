package com.wego.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for Google Maps API integration.
 *
 * @contract
 *   - apiKey: Google Maps API Key for authentication
 *   - enabled: Whether the Google Maps integration is enabled
 *   - connectTimeoutMs: Connection timeout in milliseconds (default: 5000)
 *   - readTimeoutMs: Read timeout in milliseconds (default: 10000)
 *   - useRoutesApi: Whether to use new Routes API instead of Distance Matrix
 *   - routesApi: Configuration for Routes API specific settings
 *
 * @see com.wego.service.external.GoogleMapsClientImpl
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

    /**
     * Whether to use new Routes API instead of legacy Distance Matrix API.
     * When true, uses computeRouteMatrix endpoint.
     * When false, uses legacy distancematrix endpoint.
     */
    private boolean useRoutesApi = false;

    /**
     * Routes API specific configuration.
     */
    private RoutesApiConfig routesApi = new RoutesApiConfig();

    /**
     * Configuration for Google Routes API.
     */
    @Getter
    @Setter
    public static class RoutesApiConfig {

        /**
         * Default transit travel modes to allow.
         * Options: BUS, SUBWAY, TRAIN, LIGHT_RAIL, RAIL
         */
        private List<String> defaultTransitModes = List.of("BUS", "SUBWAY", "TRAIN", "RAIL");

        /**
         * Default routing preference for transit.
         * Options: LESS_WALKING, FEWER_TRANSFERS
         */
        private String defaultRoutingPreference = "LESS_WALKING";

        /**
         * Whether to fall back to DRIVING mode when TRANSIT returns no results.
         */
        private boolean fallbackToDriving = true;

        /**
         * Whether to fall back to Haversine estimation when all API calls fail.
         */
        private boolean fallbackToHaversine = true;
    }
}
