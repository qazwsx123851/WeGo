package com.wego.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.config.ExchangeRateProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Real implementation of ExchangeRateClient that calls ExchangeRate-API.
 *
 * Uses the ExchangeRate-API v6 (https://www.exchangerate-api.com/).
 * Free tier: 1,500 requests/month with daily updates.
 *
 * Features:
 * - Circuit breaker pattern for resilience
 * - URL sanitization to prevent API key leakage in logs
 * - Timeout configuration via properties
 *
 * @contract
 *   - pre: ExchangeRateProperties must be configured with valid apiKey
 *   - post: All API calls include API key in request
 *   - throws: ExchangeRateException on API errors or invalid responses
 *
 * @see ExchangeRateClient
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "wego.external-api.exchangerate.enabled",
        havingValue = "true"
)
public class ExchangeRateApiClient implements ExchangeRateClient {

    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6";
    private static final Pattern CURRENCY_CODE_PATTERN = Pattern.compile("^[A-Z]{3}$");

    private final ExchangeRateProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Circuit breaker state
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong circuitOpenedAt = new AtomicLong(0);

    // Last successful update time
    private volatile Instant lastUpdateTime = Instant.now();

    /**
     * Creates an ExchangeRateApiClient with the specified properties.
     *
     * @contract
     *   - pre: properties != null
     *   - post: Client is ready to make API calls with configured timeouts
     *
     * @param properties ExchangeRate configuration properties
     * @param restTemplate RestTemplate for HTTP calls
     */
    public ExchangeRateApiClient(ExchangeRateProperties properties, RestTemplate restTemplate) {
        if (properties == null) {
            throw new IllegalArgumentException("ExchangeRateProperties cannot be null");
        }
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        log.info("ExchangeRateApiClient initialized (API key configured: {})", properties.hasApiKey());
    }

    /**
     * Default constructor for Spring injection.
     * Configures RestTemplate with timeouts from properties.
     */
    @org.springframework.beans.factory.annotation.Autowired
    public ExchangeRateApiClient(ExchangeRateProperties properties) {
        this(properties, createRestTemplateWithTimeouts(properties));
    }

    /**
     * Creates a RestTemplate with configured timeouts.
     */
    private static RestTemplate createRestTemplateWithTimeouts(ExchangeRateProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        return new RestTemplate(factory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal getRate(String fromCurrency, String toCurrency) {
        validateCurrencyCode(fromCurrency);
        validateCurrencyCode(toCurrency);

        // Same currency = 1.0
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }

        checkCircuitBreaker();

        log.debug("Getting rate from {} to {}", fromCurrency, toCurrency);

        // Use pair endpoint: /v6/{apiKey}/pair/{from}/{to}
        String url = String.format("%s/%s/pair/%s/%s",
                BASE_URL, properties.getApiKey(), fromCurrency, toCurrency);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            String result = root.path("result").asText();
            if (!"success".equals(result)) {
                handleApiError(root);
            }

            BigDecimal rate = new BigDecimal(root.path("conversion_rate").asText());
            lastUpdateTime = Instant.ofEpochSecond(root.path("time_last_update_unix").asLong());

            recordSuccess();
            log.info("Rate {} -> {}: {}", fromCurrency, toCurrency, rate);
            return rate;

        } catch (ExchangeRateException e) {
            recordFailure();
            throw e;
        } catch (RestClientException e) {
            recordFailure();
            log.error("HTTP error getting exchange rate: {}", sanitizeError(e.getMessage()));
            throw ExchangeRateException.networkError(e);
        } catch (Exception e) {
            recordFailure();
            log.error("Error getting exchange rate: {}", sanitizeError(e.getMessage()), e);
            throw ExchangeRateException.apiError("Failed to get exchange rate: " + sanitizeError(e.getMessage()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, BigDecimal> getAllRates(String baseCurrency) {
        validateCurrencyCode(baseCurrency);
        checkCircuitBreaker();

        log.debug("Getting all rates for base currency {}", baseCurrency);

        // Use latest endpoint: /v6/{apiKey}/latest/{base}
        String url = String.format("%s/%s/latest/%s",
                BASE_URL, properties.getApiKey(), baseCurrency);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            String result = root.path("result").asText();
            if (!"success".equals(result)) {
                handleApiError(root);
            }

            Map<String, BigDecimal> rates = new HashMap<>();
            JsonNode conversionRates = root.path("conversion_rates");

            Iterator<Map.Entry<String, JsonNode>> fields = conversionRates.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                rates.put(entry.getKey(), new BigDecimal(entry.getValue().asText()));
            }

            lastUpdateTime = Instant.ofEpochSecond(root.path("time_last_update_unix").asLong());

            recordSuccess();
            log.info("Retrieved {} rates for base currency {}", rates.size(), baseCurrency);
            return rates;

        } catch (ExchangeRateException e) {
            recordFailure();
            throw e;
        } catch (RestClientException e) {
            recordFailure();
            log.error("HTTP error getting all exchange rates: {}", sanitizeError(e.getMessage()));
            throw ExchangeRateException.networkError(e);
        } catch (Exception e) {
            recordFailure();
            log.error("Error getting all exchange rates: {}", sanitizeError(e.getMessage()), e);
            throw ExchangeRateException.apiError("Failed to get exchange rates: " + sanitizeError(e.getMessage()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instant getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * Handles API error responses.
     */
    private void handleApiError(JsonNode root) {
        String errorType = root.path("error-type").asText("unknown-error");
        log.error("ExchangeRate API error: {}", errorType);

        switch (errorType) {
            case "invalid-key":
                throw ExchangeRateException.invalidApiKey();
            case "inactive-account":
                throw ExchangeRateException.invalidApiKey();
            case "quota-reached":
                throw ExchangeRateException.rateLimitExceeded();
            case "unsupported-code":
                throw ExchangeRateException.unsupportedCurrency("Unknown");
            case "malformed-request":
                throw ExchangeRateException.apiError("Malformed request");
            default:
                throw ExchangeRateException.apiError("API returned error: " + errorType);
        }
    }

    /**
     * Validates that a currency code matches the ISO 4217 format.
     */
    private void validateCurrencyCode(String currencyCode) {
        if (currencyCode == null || !CURRENCY_CODE_PATTERN.matcher(currencyCode).matches()) {
            throw ExchangeRateException.invalidCurrency(currencyCode);
        }
    }

    /**
     * Checks if the circuit breaker is open.
     * Throws an exception if too many recent failures.
     */
    private void checkCircuitBreaker() {
        long openedAt = circuitOpenedAt.get();
        if (openedAt > 0) {
            long cooldownMs = properties.getCircuitBreakerCooldownMinutes() * 60 * 1000L;
            if (System.currentTimeMillis() - openedAt < cooldownMs) {
                log.warn("Circuit breaker is open, rejecting request");
                throw ExchangeRateException.serviceUnavailable();
            } else {
                // Cooldown period passed, allow one request (half-open state)
                log.info("Circuit breaker cooldown passed, attempting request");
            }
        }
    }

    /**
     * Records a successful API call.
     * Resets failure counter and closes circuit breaker.
     */
    private void recordSuccess() {
        consecutiveFailures.set(0);
        circuitOpenedAt.set(0);
    }

    /**
     * Records a failed API call.
     * Opens circuit breaker if threshold is reached.
     */
    private void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= properties.getCircuitBreakerFailureThreshold()) {
            long now = System.currentTimeMillis();
            if (circuitOpenedAt.compareAndSet(0, now)) {
                log.warn("Circuit breaker opened after {} consecutive failures", failures);
            }
        }
    }

    /**
     * Sanitizes error messages to remove API key if present.
     * Prevents accidental logging of sensitive information.
     */
    private String sanitizeError(String message) {
        if (message == null) {
            return "null";
        }
        if (properties.hasApiKey()) {
            return message.replace(properties.getApiKey(), "[REDACTED]");
        }
        return message;
    }
}
