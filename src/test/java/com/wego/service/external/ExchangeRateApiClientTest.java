package com.wego.service.external;

import com.wego.config.ExchangeRateProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateApiClient")
class ExchangeRateApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    private ExchangeRateApiClient client;
    private ExchangeRateProperties properties;

    private static final String PAIR_SUCCESS = """
            {
                "result": "success",
                "conversion_rate": "31.25",
                "time_last_update_unix": 1708300800
            }
            """;

    private static final String LATEST_SUCCESS = """
            {
                "result": "success",
                "conversion_rates": {
                    "USD": "1",
                    "TWD": "31.25",
                    "JPY": "149.50"
                },
                "time_last_update_unix": 1708300800
            }
            """;

    private static final String ERROR_INVALID_KEY = """
            {
                "result": "error",
                "error-type": "invalid-key"
            }
            """;

    private static final String ERROR_QUOTA = """
            {
                "result": "error",
                "error-type": "quota-reached"
            }
            """;

    private static final String ERROR_UNSUPPORTED = """
            {
                "result": "error",
                "error-type": "unsupported-code"
            }
            """;

    @BeforeEach
    void setUp() {
        properties = new ExchangeRateProperties();
        properties.setApiKey("test-api-key");
        properties.setEnabled(true);
        properties.setCircuitBreakerFailureThreshold(3);
        properties.setCircuitBreakerCooldownMinutes(5);
        client = new ExchangeRateApiClient(properties, restTemplate);
    }

    @Nested
    @DisplayName("getRate")
    class GetRate {

        @Test
        @DisplayName("should return rate on successful API call")
        void shouldReturnRate() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(PAIR_SUCCESS));

            BigDecimal rate = client.getRate("USD", "TWD");

            assertThat(rate).isEqualByComparingTo("31.25");
        }

        @Test
        @DisplayName("should return 1 for same currency")
        void shouldReturnOneForSameCurrency() {
            BigDecimal rate = client.getRate("USD", "USD");

            assertThat(rate).isEqualByComparingTo("1");
        }

        @Test
        @DisplayName("should throw on invalid currency code")
        void shouldThrowOnInvalidCurrency() {
            assertThatThrownBy(() -> client.getRate("INVALID", "TWD"))
                    .isInstanceOf(ExchangeRateException.class);
        }

        @Test
        @DisplayName("should throw on null currency code")
        void shouldThrowOnNullCurrency() {
            assertThatThrownBy(() -> client.getRate(null, "TWD"))
                    .isInstanceOf(ExchangeRateException.class);
        }

        @Test
        @DisplayName("should throw on invalid API key error")
        void shouldThrowOnInvalidApiKey() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(ERROR_INVALID_KEY));

            assertThatThrownBy(() -> client.getRate("USD", "TWD"))
                    .isInstanceOf(ExchangeRateException.class)
                    .hasMessageContaining("API");
        }

        @Test
        @DisplayName("should throw on quota exceeded")
        void shouldThrowOnQuotaExceeded() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(ERROR_QUOTA));

            assertThatThrownBy(() -> client.getRate("USD", "TWD"))
                    .isInstanceOf(ExchangeRateException.class);
        }

        @Test
        @DisplayName("should throw on unsupported currency")
        void shouldThrowOnUnsupportedCurrency() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(ERROR_UNSUPPORTED));

            assertThatThrownBy(() -> client.getRate("USD", "TWD"))
                    .isInstanceOf(ExchangeRateException.class);
        }

        @Test
        @DisplayName("should throw on network error")
        void shouldThrowOnNetworkError() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenThrow(new ResourceAccessException("Connection refused",
                            new SocketTimeoutException("timeout")));

            assertThatThrownBy(() -> client.getRate("USD", "TWD"))
                    .isInstanceOf(ExchangeRateException.class);
        }
    }

    @Nested
    @DisplayName("getAllRates")
    class GetAllRates {

        @Test
        @DisplayName("should return all rates on success")
        void shouldReturnAllRates() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(LATEST_SUCCESS));

            Map<String, BigDecimal> rates = client.getAllRates("USD");

            assertThat(rates).hasSize(3);
            assertThat(rates.get("TWD")).isEqualByComparingTo("31.25");
            assertThat(rates.get("JPY")).isEqualByComparingTo("149.50");
        }

        @Test
        @DisplayName("should throw on invalid base currency")
        void shouldThrowOnInvalidBaseCurrency() {
            assertThatThrownBy(() -> client.getAllRates("XX"))
                    .isInstanceOf(ExchangeRateException.class);
        }
    }

    @Nested
    @DisplayName("circuit breaker")
    class CircuitBreaker {

        @Test
        @DisplayName("should open circuit breaker after consecutive failures")
        void shouldOpenAfterFailures() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenThrow(new ResourceAccessException("Connection refused"));

            // Trigger failures up to threshold (3)
            for (int i = 0; i < 3; i++) {
                assertThatThrownBy(() -> client.getRate("USD", "TWD"))
                        .isInstanceOf(ExchangeRateException.class);
            }

            // Next call should be rejected by circuit breaker
            assertThatThrownBy(() -> client.getRate("USD", "TWD"))
                    .isInstanceOf(ExchangeRateException.class)
                    .hasMessageContaining("unavailable");
        }

        @Test
        @DisplayName("should reset circuit breaker on success")
        void shouldResetOnSuccess() {
            // First cause some failures (but not enough to open)
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenThrow(new ResourceAccessException("Connection refused"))
                    .thenThrow(new ResourceAccessException("Connection refused"))
                    .thenReturn(ResponseEntity.ok(PAIR_SUCCESS));

            assertThatThrownBy(() -> client.getRate("USD", "TWD"))
                    .isInstanceOf(ExchangeRateException.class);
            assertThatThrownBy(() -> client.getRate("USD", "TWD"))
                    .isInstanceOf(ExchangeRateException.class);

            // Success should reset counter
            BigDecimal rate = client.getRate("USD", "TWD");
            assertThat(rate).isEqualByComparingTo("31.25");

            // Now failures should start counting from 0 again
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenThrow(new ResourceAccessException("Connection refused"));

            assertThatThrownBy(() -> client.getRate("USD", "TWD"))
                    .isInstanceOf(ExchangeRateException.class);
            // Should NOT be circuit breaker error since counter was reset
            assertThatThrownBy(() -> client.getRate("USD", "TWD"))
                    .isInstanceOf(ExchangeRateException.class)
                    .hasMessageNotContaining("unavailable");
        }
    }

    @Nested
    @DisplayName("getLastUpdateTime")
    class GetLastUpdateTime {

        @Test
        @DisplayName("should return last update time after successful call")
        void shouldReturnLastUpdateTime() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(PAIR_SUCCESS));

            client.getRate("USD", "TWD");

            assertThat(client.getLastUpdateTime()).isNotNull();
        }
    }
}
