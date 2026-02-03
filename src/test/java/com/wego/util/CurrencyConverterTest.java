package com.wego.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for CurrencyConverter utility class.
 *
 * Test cases:
 * - P3-T-002a: convert() with valid inputs
 * - P3-T-002b: convert() with null inputs
 * - P3-T-002c: convertCrossRate() calculation
 * - P3-T-002d: calculateCrossRate() precision
 * - P3-T-002e: roundAmount() and roundRate()
 * - P3-T-002f: isValidRate() boundary conditions
 */
@DisplayName("CurrencyConverter")
class CurrencyConverterTest {

    @Nested
    @DisplayName("convert()")
    class ConvertTests {

        @Test
        @DisplayName("should convert amount with valid rate")
        void convert_withValidInputs_shouldReturnConvertedAmount() {
            // Given
            BigDecimal amount = new BigDecimal("100.00");
            BigDecimal rate = new BigDecimal("31.5");

            // When
            BigDecimal result = CurrencyConverter.convert(amount, rate);

            // Then
            assertThat(result).isEqualByComparingTo("3150.00");
        }

        @Test
        @DisplayName("should round to 2 decimal places")
        void convert_withFractionalResult_shouldRoundToTwoDecimals() {
            // Given
            BigDecimal amount = new BigDecimal("100.00");
            BigDecimal rate = new BigDecimal("31.567");

            // When
            BigDecimal result = CurrencyConverter.convert(amount, rate);

            // Then - 100 * 31.567 = 3156.7, rounded to 3156.70
            assertThat(result).isEqualByComparingTo("3156.70");
        }

        @Test
        @DisplayName("should use HALF_UP rounding")
        void convert_withHalfUpRounding_shouldRoundCorrectly() {
            // Given
            BigDecimal amount = new BigDecimal("1.00");
            BigDecimal rate = new BigDecimal("1.555"); // Result = 1.555, should round to 1.56

            // When
            BigDecimal result = CurrencyConverter.convert(amount, rate);

            // Then
            assertThat(result).isEqualByComparingTo("1.56");
        }

        @Test
        @DisplayName("should throw exception for null amount")
        void convert_withNullAmount_shouldThrowException() {
            // Given
            BigDecimal rate = new BigDecimal("31.5");

            // When & Then
            assertThatThrownBy(() -> CurrencyConverter.convert(null, rate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Amount cannot be null");
        }

        @Test
        @DisplayName("should throw exception for null rate")
        void convert_withNullRate_shouldThrowException() {
            // Given
            BigDecimal amount = new BigDecimal("100.00");

            // When & Then
            assertThatThrownBy(() -> CurrencyConverter.convert(amount, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Rate cannot be null");
        }

        @Test
        @DisplayName("should throw exception for zero rate")
        void convert_withZeroRate_shouldThrowException() {
            // Given
            BigDecimal amount = new BigDecimal("100.00");
            BigDecimal rate = BigDecimal.ZERO;

            // When & Then
            assertThatThrownBy(() -> CurrencyConverter.convert(amount, rate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Rate must be positive");
        }

        @Test
        @DisplayName("should throw exception for negative rate")
        void convert_withNegativeRate_shouldThrowException() {
            // Given
            BigDecimal amount = new BigDecimal("100.00");
            BigDecimal rate = new BigDecimal("-1.5");

            // When & Then
            assertThatThrownBy(() -> CurrencyConverter.convert(amount, rate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Rate must be positive");
        }
    }

    @Nested
    @DisplayName("convertCrossRate()")
    class ConvertCrossRateTests {

        @Test
        @DisplayName("should convert using cross rate")
        void convertCrossRate_withValidInputs_shouldConvert() {
            // Given: Convert 100 EUR to JPY
            // 1 USD = 0.92 EUR -> 1 EUR = 1/0.92 USD
            // 1 USD = 149.5 JPY
            // Cross rate: 149.5 / 0.92 = 162.5
            BigDecimal amount = new BigDecimal("100.00");
            BigDecimal eurToUsd = new BigDecimal("0.92");  // 1 USD = 0.92 EUR
            BigDecimal jpyToUsd = new BigDecimal("149.5"); // 1 USD = 149.5 JPY

            // When
            BigDecimal result = CurrencyConverter.convertCrossRate(amount, eurToUsd, jpyToUsd);

            // Then: 100 * (149.5 / 0.92) = 100 * 162.5 = 16250
            assertThat(result).isEqualByComparingTo("16250.00");
        }
    }

    @Nested
    @DisplayName("calculateCrossRate()")
    class CalculateCrossRateTests {

        @Test
        @DisplayName("should calculate cross rate correctly")
        void calculateCrossRate_shouldReturnCorrectRate() {
            // Given: EUR to JPY cross rate
            BigDecimal eurToUsd = new BigDecimal("0.92");  // 1 USD = 0.92 EUR
            BigDecimal jpyToUsd = new BigDecimal("149.5"); // 1 USD = 149.5 JPY

            // When
            BigDecimal crossRate = CurrencyConverter.calculateCrossRate(eurToUsd, jpyToUsd);

            // Then: 149.5 / 0.92 = 162.5
            assertThat(crossRate).isEqualByComparingTo("162.5");
        }

        @Test
        @DisplayName("should return 1.0 for same rates")
        void calculateCrossRate_withSameRates_shouldReturnOne() {
            // Given
            BigDecimal rate = new BigDecimal("31.5");

            // When
            BigDecimal crossRate = CurrencyConverter.calculateCrossRate(rate, rate);

            // Then
            assertThat(crossRate).isEqualByComparingTo("1.0");
        }
    }

    @Nested
    @DisplayName("roundAmount()")
    class RoundAmountTests {

        @Test
        @DisplayName("should round to 2 decimal places")
        void roundAmount_shouldRoundToTwoDecimals() {
            // Given
            BigDecimal amount = new BigDecimal("123.456789");

            // When
            BigDecimal result = CurrencyConverter.roundAmount(amount);

            // Then
            assertThat(result).isEqualByComparingTo("123.46");
        }

        @Test
        @DisplayName("should throw exception for null")
        void roundAmount_withNull_shouldThrowException() {
            assertThatThrownBy(() -> CurrencyConverter.roundAmount(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("roundRate()")
    class RoundRateTests {

        @Test
        @DisplayName("should round to 6 decimal places")
        void roundRate_shouldRoundToSixDecimals() {
            // Given
            BigDecimal rate = new BigDecimal("31.567891234");

            // When
            BigDecimal result = CurrencyConverter.roundRate(rate);

            // Then
            assertThat(result).isEqualByComparingTo("31.567891");
        }
    }

    @Nested
    @DisplayName("isValidRate()")
    class IsValidRateTests {

        @Test
        @DisplayName("should return true for valid rate")
        void isValidRate_withValidRate_shouldReturnTrue() {
            assertThat(CurrencyConverter.isValidRate(new BigDecimal("31.5"))).isTrue();
            assertThat(CurrencyConverter.isValidRate(new BigDecimal("0.001"))).isTrue();
            assertThat(CurrencyConverter.isValidRate(new BigDecimal("999999"))).isTrue();
        }

        @Test
        @DisplayName("should return true for boundary values")
        void isValidRate_withBoundaryValues_shouldReturnTrue() {
            // Minimum valid rate
            assertThat(CurrencyConverter.isValidRate(new BigDecimal("0.0001"))).isTrue();
            // Maximum valid rate
            assertThat(CurrencyConverter.isValidRate(new BigDecimal("1000000"))).isTrue();
        }

        @Test
        @DisplayName("should return false for rate below minimum")
        void isValidRate_withRateBelowMinimum_shouldReturnFalse() {
            assertThat(CurrencyConverter.isValidRate(new BigDecimal("0.00009"))).isFalse();
        }

        @Test
        @DisplayName("should return false for rate above maximum")
        void isValidRate_withRateAboveMaximum_shouldReturnFalse() {
            assertThat(CurrencyConverter.isValidRate(new BigDecimal("1000001"))).isFalse();
        }

        @Test
        @DisplayName("should return false for null")
        void isValidRate_withNull_shouldReturnFalse() {
            assertThat(CurrencyConverter.isValidRate(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Constants")
    class ConstantsTests {

        @Test
        @DisplayName("should have correct scale constants")
        void constants_shouldHaveCorrectValues() {
            assertThat(CurrencyConverter.AMOUNT_SCALE).isEqualTo(2);
            assertThat(CurrencyConverter.RATE_SCALE).isEqualTo(6);
        }
    }
}
