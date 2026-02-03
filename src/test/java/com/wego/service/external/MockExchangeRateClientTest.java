package com.wego.service.external;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for MockExchangeRateClient.
 *
 * Test cases:
 * - P3-T-001a: getRate() with valid currency pairs
 * - P3-T-001b: getRate() with invalid currency codes
 * - P3-T-001c: getAllRates() returns all currencies
 * - P3-T-001d: Cross-rate calculation correctness
 */
@DisplayName("MockExchangeRateClient")
class MockExchangeRateClientTest {

    private MockExchangeRateClient client;

    @BeforeEach
    void setUp() {
        client = new MockExchangeRateClient();
    }

    @Nested
    @DisplayName("getRate()")
    class GetRateTests {

        @Test
        @DisplayName("should return 1.0 for same currency")
        void getRate_sameCurrency_shouldReturnOne() {
            BigDecimal rate = client.getRate("USD", "USD");
            assertThat(rate).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("should return correct rate for USD to TWD")
        void getRate_usdToTwd_shouldReturnCorrectRate() {
            BigDecimal rate = client.getRate("USD", "TWD");
            assertThat(rate).isEqualByComparingTo("31.5");
        }

        @Test
        @DisplayName("should return correct rate for TWD to USD")
        void getRate_twdToUsd_shouldReturnCorrectRate() {
            BigDecimal rate = client.getRate("TWD", "USD");
            // 1 / 31.5 ≈ 0.031746
            assertThat(rate).isGreaterThan(new BigDecimal("0.031"))
                    .isLessThan(new BigDecimal("0.032"));
        }

        @Test
        @DisplayName("should calculate cross rate for EUR to JPY")
        void getRate_eurToJpy_shouldCalculateCrossRate() {
            BigDecimal rate = client.getRate("EUR", "JPY");
            // EUR->USD: 1/0.92, USD->JPY: 149.5
            // Cross rate: 149.5 / 0.92 ≈ 162.5
            assertThat(rate).isGreaterThan(new BigDecimal("160"))
                    .isLessThan(new BigDecimal("165"));
        }

        @Test
        @DisplayName("should throw exception for null currency code")
        void getRate_nullCurrency_shouldThrowException() {
            assertThatThrownBy(() -> client.getRate(null, "TWD"))
                    .isInstanceOf(ExchangeRateException.class)
                    .hasMessageContaining("Invalid currency code");
        }

        @Test
        @DisplayName("should throw exception for invalid currency format")
        void getRate_invalidFormat_shouldThrowException() {
            assertThatThrownBy(() -> client.getRate("US", "TWD"))
                    .isInstanceOf(ExchangeRateException.class)
                    .hasMessageContaining("Invalid currency code");

            assertThatThrownBy(() -> client.getRate("USDT", "TWD"))
                    .isInstanceOf(ExchangeRateException.class)
                    .hasMessageContaining("Invalid currency code");

            assertThatThrownBy(() -> client.getRate("usd", "TWD"))
                    .isInstanceOf(ExchangeRateException.class)
                    .hasMessageContaining("Invalid currency code");
        }

        @Test
        @DisplayName("should throw exception for unsupported currency")
        void getRate_unsupportedCurrency_shouldThrowException() {
            assertThatThrownBy(() -> client.getRate("XYZ", "TWD"))
                    .isInstanceOf(ExchangeRateException.class)
                    .hasMessageContaining("Currency not supported");
        }
    }

    @Nested
    @DisplayName("getAllRates()")
    class GetAllRatesTests {

        @Test
        @DisplayName("should return all supported currencies")
        void getAllRates_shouldReturnAllCurrencies() {
            Map<String, BigDecimal> rates = client.getAllRates("USD");

            assertThat(rates).isNotEmpty();
            assertThat(rates).containsKey("TWD");
            assertThat(rates).containsKey("JPY");
            assertThat(rates).containsKey("EUR");
            assertThat(rates).containsKey("GBP");
        }

        @Test
        @DisplayName("should return 1.0 for base currency")
        void getAllRates_shouldReturnOneForBaseCurrency() {
            Map<String, BigDecimal> rates = client.getAllRates("USD");

            assertThat(rates.get("USD")).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("should return consistent rates")
        void getAllRates_shouldBeConsistentWithGetRate() {
            Map<String, BigDecimal> allRates = client.getAllRates("USD");
            BigDecimal singleRate = client.getRate("USD", "TWD");

            assertThat(allRates.get("TWD")).isEqualByComparingTo(singleRate);
        }

        @Test
        @DisplayName("should throw exception for invalid base currency")
        void getAllRates_invalidCurrency_shouldThrowException() {
            assertThatThrownBy(() -> client.getAllRates("XYZ"))
                    .isInstanceOf(ExchangeRateException.class);
        }
    }

    @Nested
    @DisplayName("getLastUpdateTime()")
    class GetLastUpdateTimeTests {

        @Test
        @DisplayName("should return recent timestamp")
        void getLastUpdateTime_shouldReturnRecentTime() {
            Instant before = Instant.now().minusSeconds(1);
            MockExchangeRateClient newClient = new MockExchangeRateClient();
            Instant updateTime = newClient.getLastUpdateTime();
            Instant after = Instant.now().plusSeconds(1);

            assertThat(updateTime).isAfterOrEqualTo(before);
            assertThat(updateTime).isBeforeOrEqualTo(after);
        }
    }

    @Nested
    @DisplayName("getSupportedCurrencies()")
    class GetSupportedCurrenciesTests {

        @Test
        @DisplayName("should include common currencies")
        void getSupportedCurrencies_shouldIncludeCommon() {
            var currencies = MockExchangeRateClient.getSupportedCurrencies();

            assertThat(currencies).contains("USD", "TWD", "JPY", "EUR", "GBP", "CNY");
        }

        @Test
        @DisplayName("should have at least 10 currencies")
        void getSupportedCurrencies_shouldHaveAtLeast10() {
            var currencies = MockExchangeRateClient.getSupportedCurrencies();

            assertThat(currencies).hasSizeGreaterThanOrEqualTo(10);
        }
    }
}
