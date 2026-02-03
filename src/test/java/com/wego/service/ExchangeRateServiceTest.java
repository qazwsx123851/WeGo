package com.wego.service;

import com.wego.config.ExchangeRateProperties;
import com.wego.dto.response.ExchangeRateResponse;
import com.wego.service.external.ExchangeRateClient;
import com.wego.service.external.ExchangeRateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExchangeRateService.
 *
 * Test cases cover:
 * - P3-T-001a: getRate() with cache hit/miss scenarios
 * - P3-T-001b: getAllRates() caching behavior
 * - P3-T-001c: convert() currency conversion
 * - P3-T-001d: Fallback cache when API fails
 * - P3-T-001e: Input validation
 * - P3-T-001f: Supported currencies
 *
 * @see ExchangeRateService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateService")
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateClient exchangeRateClient;

    @Mock
    private ExchangeRateProperties properties;

    private ExchangeRateService service;

    @BeforeEach
    void setUp() {
        // Default property values
        lenient().when(properties.getCacheTtlHours()).thenReturn(1);
        lenient().when(properties.getFallbackTtlHours()).thenReturn(24);

        service = new ExchangeRateService(exchangeRateClient, properties);
    }

    @Nested
    @DisplayName("getRate()")
    class GetRateTests {

        @Test
        @DisplayName("should return fresh rate on cache miss")
        void getRate_cacheMiss_shouldCallClient() {
            // Arrange
            when(exchangeRateClient.getRate("USD", "TWD")).thenReturn(new BigDecimal("31.5"));
            when(exchangeRateClient.getLastUpdateTime()).thenReturn(Instant.now());

            // Act
            ExchangeRateResponse response = service.getRate("USD", "TWD");

            // Assert
            assertThat(response.getRate()).isEqualByComparingTo("31.5");
            assertThat(response.getFrom()).isEqualTo("USD");
            assertThat(response.getTo()).isEqualTo("TWD");
            assertThat(response.isCached()).isFalse();
            assertThat(response.getCacheAgeMs()).isZero();
            verify(exchangeRateClient, times(1)).getRate("USD", "TWD");
        }

        @Test
        @DisplayName("should return cached rate on cache hit")
        void getRate_cacheHit_shouldNotCallClientAgain() {
            // Arrange
            when(exchangeRateClient.getRate("USD", "TWD")).thenReturn(new BigDecimal("31.5"));
            when(exchangeRateClient.getLastUpdateTime()).thenReturn(Instant.now());

            // Act - first call (cache miss)
            service.getRate("USD", "TWD");

            // Act - second call (cache hit)
            ExchangeRateResponse response = service.getRate("USD", "TWD");

            // Assert
            assertThat(response.getRate()).isEqualByComparingTo("31.5");
            assertThat(response.isCached()).isTrue();
            assertThat(response.getCacheAgeMs()).isGreaterThanOrEqualTo(0);
            verify(exchangeRateClient, times(1)).getRate("USD", "TWD");
        }

        @Test
        @DisplayName("should return 1.0 for same currency")
        void getRate_sameCurrency_shouldReturnOne() {
            // Arrange
            when(exchangeRateClient.getRate("USD", "USD")).thenReturn(BigDecimal.ONE);
            when(exchangeRateClient.getLastUpdateTime()).thenReturn(Instant.now());

            // Act
            ExchangeRateResponse response = service.getRate("USD", "USD");

            // Assert
            assertThat(response.getRate()).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("should include fetchedAt from client")
        void getRate_shouldIncludeFetchedAt() {
            // Arrange
            Instant expectedTime = Instant.parse("2026-02-03T10:00:00Z");
            when(exchangeRateClient.getRate("USD", "TWD")).thenReturn(new BigDecimal("31.5"));
            when(exchangeRateClient.getLastUpdateTime()).thenReturn(expectedTime);

            // Act
            ExchangeRateResponse response = service.getRate("USD", "TWD");

            // Assert
            assertThat(response.getFetchedAt()).isEqualTo(expectedTime);
        }

        @Test
        @DisplayName("should cache different currency pairs separately")
        void getRate_differentPairs_shouldCacheSeparately() {
            // Arrange
            when(exchangeRateClient.getRate("USD", "TWD")).thenReturn(new BigDecimal("31.5"));
            when(exchangeRateClient.getRate("USD", "JPY")).thenReturn(new BigDecimal("149.5"));
            when(exchangeRateClient.getLastUpdateTime()).thenReturn(Instant.now());

            // Act
            service.getRate("USD", "TWD");
            service.getRate("USD", "JPY");
            service.getRate("USD", "TWD");
            service.getRate("USD", "JPY");

            // Assert - each pair should only call client once
            verify(exchangeRateClient, times(1)).getRate("USD", "TWD");
            verify(exchangeRateClient, times(1)).getRate("USD", "JPY");
        }
    }

    @Nested
    @DisplayName("getAllRates()")
    class GetAllRatesTests {

        @Test
        @DisplayName("should return rates map on cache miss")
        void getAllRates_cacheMiss_shouldCallClient() {
            // Arrange
            Map<String, BigDecimal> rates = Map.of(
                    "USD", BigDecimal.ONE,
                    "TWD", new BigDecimal("31.5"),
                    "JPY", new BigDecimal("149.5")
            );
            when(exchangeRateClient.getAllRates("USD")).thenReturn(rates);
            when(exchangeRateClient.getLastUpdateTime()).thenReturn(Instant.now());

            // Act
            Map<String, BigDecimal> result = service.getAllRates("USD");

            // Assert
            assertThat(result).hasSize(3);
            assertThat(result.get("TWD")).isEqualByComparingTo("31.5");
            verify(exchangeRateClient, times(1)).getAllRates("USD");
        }

        @Test
        @DisplayName("should return cached rates on cache hit")
        void getAllRates_cacheHit_shouldNotCallClientAgain() {
            // Arrange
            Map<String, BigDecimal> rates = Map.of("USD", BigDecimal.ONE, "TWD", new BigDecimal("31.5"));
            when(exchangeRateClient.getAllRates("USD")).thenReturn(rates);
            when(exchangeRateClient.getLastUpdateTime()).thenReturn(Instant.now());

            // Act - first call
            service.getAllRates("USD");

            // Act - second call
            Map<String, BigDecimal> result = service.getAllRates("USD");

            // Assert
            assertThat(result.get("TWD")).isEqualByComparingTo("31.5");
            verify(exchangeRateClient, times(1)).getAllRates("USD");
        }

        @Test
        @DisplayName("should contain base currency with rate 1")
        void getAllRates_shouldContainBaseCurrency() {
            // Arrange
            Map<String, BigDecimal> rates = Map.of(
                    "USD", BigDecimal.ONE,
                    "TWD", new BigDecimal("31.5")
            );
            when(exchangeRateClient.getAllRates("USD")).thenReturn(rates);
            when(exchangeRateClient.getLastUpdateTime()).thenReturn(Instant.now());

            // Act
            Map<String, BigDecimal> result = service.getAllRates("USD");

            // Assert
            assertThat(result).containsKey("USD");
            assertThat(result.get("USD")).isEqualByComparingTo(BigDecimal.ONE);
        }
    }

    @Nested
    @DisplayName("convert()")
    class ConvertTests {

        @Test
        @DisplayName("should convert amount correctly")
        void convert_validInput_shouldCalculate() {
            // Arrange
            when(exchangeRateClient.getRate("USD", "TWD")).thenReturn(new BigDecimal("31.5"));
            when(exchangeRateClient.getLastUpdateTime()).thenReturn(Instant.now());

            // Act
            BigDecimal result = service.convert(new BigDecimal("100"), "USD", "TWD");

            // Assert - 100 * 31.5 = 3150.00
            assertThat(result).isEqualByComparingTo("3150.00");
        }

        @Test
        @DisplayName("should round without API call for same currency")
        void convert_sameCurrency_shouldNotCallClient() {
            // Act
            BigDecimal result = service.convert(new BigDecimal("100.555"), "USD", "USD");

            // Assert - should round to 2 decimal places
            assertThat(result).isEqualByComparingTo("100.56");
            verify(exchangeRateClient, never()).getRate(anyString(), anyString());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for null amount")
        void convert_nullAmount_shouldThrow() {
            // Act & Assert
            assertThatThrownBy(() -> service.convert(null, "USD", "TWD"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount cannot be null");
        }

        @Test
        @DisplayName("should handle zero amount")
        void convert_zeroAmount_shouldReturnZero() {
            // Arrange
            when(exchangeRateClient.getRate("USD", "TWD")).thenReturn(new BigDecimal("31.5"));
            when(exchangeRateClient.getLastUpdateTime()).thenReturn(Instant.now());

            // Act
            BigDecimal result = service.convert(BigDecimal.ZERO, "USD", "TWD");

            // Assert - 0 * 31.5 = 0.00
            assertThat(result).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("should use cached rate for multiple conversions")
        void convert_multipleCalls_shouldUseCachedRate() {
            // Arrange
            when(exchangeRateClient.getRate("USD", "TWD")).thenReturn(new BigDecimal("31.5"));
            when(exchangeRateClient.getLastUpdateTime()).thenReturn(Instant.now());

            // Act
            service.convert(new BigDecimal("100"), "USD", "TWD");
            service.convert(new BigDecimal("200"), "USD", "TWD");
            service.convert(new BigDecimal("300"), "USD", "TWD");

            // Assert - client should only be called once
            verify(exchangeRateClient, times(1)).getRate("USD", "TWD");
        }
    }

    @Nested
    @DisplayName("Cache Expiration")
    class CacheExpirationTests {

        @Test
        @DisplayName("should use cache within TTL")
        void getRate_withinTtl_shouldUseCache() {
            // Arrange - cache TTL is 1 hour
            when(exchangeRateClient.getRate("USD", "TWD")).thenReturn(new BigDecimal("31.5"));
            when(exchangeRateClient.getLastUpdateTime()).thenReturn(Instant.now());

            // Act - multiple calls within TTL
            service.getRate("USD", "TWD");
            service.getRate("USD", "TWD");
            service.getRate("USD", "TWD");

            // Assert - only one client call
            verify(exchangeRateClient, times(1)).getRate("USD", "TWD");
        }

        @Test
        @DisplayName("clearCache should evict all cached data")
        void clearCache_shouldEvictAll() {
            // Arrange
            when(exchangeRateClient.getRate("USD", "TWD")).thenReturn(new BigDecimal("31.5"));
            when(exchangeRateClient.getLastUpdateTime()).thenReturn(Instant.now());

            // Act - populate cache
            service.getRate("USD", "TWD");

            // Clear cache
            service.clearCache();

            // Call again - should hit client again
            service.getRate("USD", "TWD");

            // Assert - client called twice (before and after clear)
            verify(exchangeRateClient, times(2)).getRate("USD", "TWD");
        }

        @Test
        @DisplayName("clearCache should also clear allRates cache")
        void clearCache_shouldEvictAllRatesCache() {
            // Arrange
            Map<String, BigDecimal> rates = Map.of("USD", BigDecimal.ONE, "TWD", new BigDecimal("31.5"));
            when(exchangeRateClient.getAllRates("USD")).thenReturn(rates);
            when(exchangeRateClient.getLastUpdateTime()).thenReturn(Instant.now());

            // Act - populate cache
            service.getAllRates("USD");

            // Clear cache
            service.clearCache();

            // Call again
            service.getAllRates("USD");

            // Assert - client called twice
            verify(exchangeRateClient, times(2)).getAllRates("USD");
        }
    }

    @Nested
    @DisplayName("Fallback Behavior")
    class FallbackBehaviorTests {

        @Test
        @DisplayName("should return cached rate when cache is valid (within primary TTL)")
        void getRate_cacheValid_shouldReturnCachedRate() {
            // Arrange - first call succeeds, populates cache
            when(exchangeRateClient.getRate("USD", "TWD")).thenReturn(new BigDecimal("31.5"));
            when(exchangeRateClient.getLastUpdateTime()).thenReturn(Instant.now());

            // Act - first call populates cache
            ExchangeRateResponse first = service.getRate("USD", "TWD");
            assertThat(first.isCached()).isFalse();

            // Act - second call should use cache (within primary TTL)
            ExchangeRateResponse second = service.getRate("USD", "TWD");

            // Assert - should return cached value
            assertThat(second.getRate()).isEqualByComparingTo("31.5");
            assertThat(second.isCached()).isTrue();

            // Verify client was only called once
            verify(exchangeRateClient, times(1)).getRate("USD", "TWD");
        }

        @Test
        @DisplayName("should throw exception when API fails and no cache available")
        void getRate_clientFails_noCache_shouldThrow() {
            // Arrange - client fails immediately, no cache
            when(exchangeRateClient.getRate("USD", "TWD"))
                    .thenThrow(ExchangeRateException.apiError("API down"));

            // Act & Assert
            assertThatThrownBy(() -> service.getRate("USD", "TWD"))
                    .isInstanceOf(ExchangeRateException.class)
                    .hasMessageContaining("API");
        }

        @Test
        @DisplayName("should return cached rates when cache is valid")
        void getAllRates_cacheValid_shouldReturnCachedRates() {
            // Arrange - first call succeeds
            Map<String, BigDecimal> rates = Map.of("USD", BigDecimal.ONE, "TWD", new BigDecimal("31.5"));
            when(exchangeRateClient.getAllRates("USD")).thenReturn(rates);
            when(exchangeRateClient.getLastUpdateTime()).thenReturn(Instant.now());

            // Act - first call populates cache
            service.getAllRates("USD");

            // Act - second call should use cache
            Map<String, BigDecimal> result = service.getAllRates("USD");

            // Assert - should return cached value
            assertThat(result.get("TWD")).isEqualByComparingTo("31.5");

            // Verify client was only called once
            verify(exchangeRateClient, times(1)).getAllRates("USD");
        }

        @Test
        @DisplayName("should throw exception for getAllRates when API fails and no cache")
        void getAllRates_clientFails_noCache_shouldThrow() {
            // Arrange
            when(exchangeRateClient.getAllRates("USD"))
                    .thenThrow(ExchangeRateException.apiError("API down"));

            // Act & Assert
            assertThatThrownBy(() -> service.getAllRates("USD"))
                    .isInstanceOf(ExchangeRateException.class);
        }
    }

    @Nested
    @DisplayName("Input Validation")
    class InputValidationTests {

        @Test
        @DisplayName("should throw exception for null from currency")
        void getRate_nullFrom_shouldThrow() {
            assertThatThrownBy(() -> service.getRate(null, "TWD"))
                    .isInstanceOf(ExchangeRateException.class)
                    .hasMessageContaining("Invalid currency code");
        }

        @Test
        @DisplayName("should throw exception for null to currency")
        void getRate_nullTo_shouldThrow() {
            assertThatThrownBy(() -> service.getRate("USD", null))
                    .isInstanceOf(ExchangeRateException.class)
                    .hasMessageContaining("Invalid currency code");
        }

        @Test
        @DisplayName("should throw exception for invalid format (2 letters)")
        void getRate_invalidFormat_twoLetters_shouldThrow() {
            assertThatThrownBy(() -> service.getRate("US", "TWD"))
                    .isInstanceOf(ExchangeRateException.class)
                    .hasMessageContaining("Invalid currency code");
        }

        @Test
        @DisplayName("should throw exception for invalid format (4 letters)")
        void getRate_invalidFormat_fourLetters_shouldThrow() {
            assertThatThrownBy(() -> service.getRate("USDA", "TWD"))
                    .isInstanceOf(ExchangeRateException.class)
                    .hasMessageContaining("Invalid currency code");
        }

        @Test
        @DisplayName("should throw exception for lowercase currency code")
        void getRate_lowercase_shouldThrow() {
            assertThatThrownBy(() -> service.getRate("usd", "TWD"))
                    .isInstanceOf(ExchangeRateException.class)
                    .hasMessageContaining("Invalid currency code");
        }

        @Test
        @DisplayName("should throw exception for invalid getAllRates base currency")
        void getAllRates_invalidBase_shouldThrow() {
            assertThatThrownBy(() -> service.getAllRates("us"))
                    .isInstanceOf(ExchangeRateException.class)
                    .hasMessageContaining("Invalid currency code");
        }

        @Test
        @DisplayName("should throw exception for convert with invalid currencies")
        void convert_invalidCurrency_shouldThrow() {
            assertThatThrownBy(() -> service.convert(new BigDecimal("100"), "123", "TWD"))
                    .isInstanceOf(ExchangeRateException.class)
                    .hasMessageContaining("Invalid currency code");
        }

        @Test
        @DisplayName("should throw exception for empty string currency code")
        void getRate_emptyString_shouldThrow() {
            assertThatThrownBy(() -> service.getRate("", "TWD"))
                    .isInstanceOf(ExchangeRateException.class)
                    .hasMessageContaining("Invalid currency code");
        }
    }

    @Nested
    @DisplayName("getSupportedCurrencies()")
    class SupportedCurrenciesTests {

        @Test
        @DisplayName("should return all 18 common currencies")
        void getSupportedCurrencies_shouldReturnAll() {
            // Act
            Set<String> currencies = service.getSupportedCurrencies();

            // Assert
            assertThat(currencies).hasSize(18);
            assertThat(currencies).contains("USD", "TWD", "JPY", "EUR", "GBP", "CNY");
            assertThat(currencies).contains("KRW", "HKD", "SGD", "THB", "AUD", "CAD");
            assertThat(currencies).contains("CHF", "NZD", "MYR", "PHP", "IDR", "VND");
        }

        @Test
        @DisplayName("should return immutable set")
        void getSupportedCurrencies_shouldBeImmutable() {
            // Act
            Set<String> currencies = service.getSupportedCurrencies();

            // Assert - attempting to modify should throw
            assertThatThrownBy(() -> currencies.add("XYZ"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
