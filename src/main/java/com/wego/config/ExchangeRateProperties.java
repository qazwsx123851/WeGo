package com.wego.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for ExchangeRate API integration.
 *
 * @contract
 *   - apiKey: ExchangeRate-API key for authentication
 *   - enabled: Whether the ExchangeRate integration is enabled
 *   - connectTimeoutMs: Connection timeout in milliseconds (default: 5000)
 *   - readTimeoutMs: Read timeout in milliseconds (default: 10000)
 *   - cacheTtlHours: Primary cache TTL in hours (default: 1)
 *   - fallbackTtlHours: Fallback cache TTL in hours when API unavailable (default: 24)
 *   - circuitBreakerFailureThreshold: Number of failures before circuit opens (default: 5)
 *   - circuitBreakerCooldownMinutes: Cooldown period before retry (default: 5)
 *
 * @see com.wego.service.external.ExchangeRateApiClient
 * @see com.wego.service.ExchangeRateService
 */
@Configuration
@ConfigurationProperties(prefix = "wego.external-api.exchangerate")
@Getter
@Setter
public class ExchangeRateProperties {

    /**
     * ExchangeRate-API key.
     * Required for all API calls.
     * Obtain from: https://www.exchangerate-api.com/
     */
    private String apiKey;

    /**
     * Whether ExchangeRate integration is enabled.
     * When disabled, the service uses mock/static rates.
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
     * Primary cache TTL in hours.
     * How long to cache rates before refreshing.
     */
    private int cacheTtlHours = 1;

    /**
     * Fallback cache TTL in hours.
     * Maximum age of cached rates to use when API is unavailable.
     */
    private int fallbackTtlHours = 24;

    /**
     * Number of consecutive failures before circuit breaker opens.
     */
    private int circuitBreakerFailureThreshold = 5;

    /**
     * Cooldown period in minutes before retrying after circuit opens.
     */
    private int circuitBreakerCooldownMinutes = 5;

    /**
     * Checks if the API key is configured.
     *
     * @return true if API key is non-null and not empty
     */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
