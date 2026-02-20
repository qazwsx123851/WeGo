package com.wego.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PersonalExpense entity.
 *
 * Covers: builder defaults, getAmountInBaseCurrency, equals/hashCode, toString.
 */
@Tag("fast")
@DisplayName("PersonalExpense Entity Tests")
class PersonalExpenseTest {

    @Nested
    @DisplayName("Builder Defaults")
    class BuilderDefaults {

        @Test
        @DisplayName("Should default currency to TWD")
        void build_shouldDefaultCurrencyToTWD() {
            PersonalExpense expense = PersonalExpense.builder()
                    .userId(UUID.randomUUID())
                    .tripId(UUID.randomUUID())
                    .description("Lunch")
                    .amount(BigDecimal.valueOf(150))
                    .build();

            assertEquals("TWD", expense.getCurrency());
        }

        @Test
        @DisplayName("Should set createdAt on build")
        void build_shouldSetCreatedAt() {
            Instant before = Instant.now();

            PersonalExpense expense = PersonalExpense.builder()
                    .userId(UUID.randomUUID())
                    .tripId(UUID.randomUUID())
                    .description("Lunch")
                    .amount(BigDecimal.valueOf(150))
                    .build();

            Instant after = Instant.now();

            assertNotNull(expense.getCreatedAt());
            assertTrue(expense.getCreatedAt().compareTo(before) >= 0);
            assertTrue(expense.getCreatedAt().compareTo(after) <= 0);
        }

        @Test
        @DisplayName("Should allow overriding currency")
        void build_withCustomCurrency_shouldOverrideDefault() {
            PersonalExpense expense = PersonalExpense.builder()
                    .userId(UUID.randomUUID())
                    .tripId(UUID.randomUUID())
                    .description("Dinner")
                    .amount(BigDecimal.valueOf(50))
                    .currency("USD")
                    .build();

            assertEquals("USD", expense.getCurrency());
        }

        @Test
        @DisplayName("Should set optional fields to null by default")
        void build_shouldHaveNullOptionalFields() {
            PersonalExpense expense = PersonalExpense.builder()
                    .userId(UUID.randomUUID())
                    .tripId(UUID.randomUUID())
                    .description("Test")
                    .amount(BigDecimal.TEN)
                    .build();

            assertNull(expense.getExchangeRate());
            assertNull(expense.getCategory());
            assertNull(expense.getExpenseDate());
            assertNull(expense.getNote());
            assertNull(expense.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("getAmountInBaseCurrency")
    class GetAmountInBaseCurrency {

        @Test
        @DisplayName("Should return amount as-is when exchangeRate is null")
        void getAmountInBaseCurrency_nullRate_shouldReturnAmount() {
            PersonalExpense expense = PersonalExpense.builder()
                    .amount(BigDecimal.valueOf(100))
                    .exchangeRate(null)
                    .build();

            assertEquals(BigDecimal.valueOf(100), expense.getAmountInBaseCurrency());
        }

        @Test
        @DisplayName("Should return amount as-is when exchangeRate is zero")
        void getAmountInBaseCurrency_zeroRate_shouldReturnAmount() {
            PersonalExpense expense = PersonalExpense.builder()
                    .amount(BigDecimal.valueOf(100))
                    .exchangeRate(BigDecimal.ZERO)
                    .build();

            assertEquals(BigDecimal.valueOf(100), expense.getAmountInBaseCurrency());
        }

        @Test
        @DisplayName("Should multiply amount by exchangeRate when rate is set")
        void getAmountInBaseCurrency_withRate_shouldMultiply() {
            PersonalExpense expense = PersonalExpense.builder()
                    .amount(BigDecimal.valueOf(50))
                    .exchangeRate(BigDecimal.valueOf(30.5))
                    .build();

            // 50 * 30.5 = 1525.0
            assertEquals(0, BigDecimal.valueOf(1525.0).compareTo(expense.getAmountInBaseCurrency()));
        }

        @Test
        @DisplayName("Should handle decimal exchange rates correctly")
        void getAmountInBaseCurrency_decimalRate_shouldMultiplyCorrectly() {
            PersonalExpense expense = PersonalExpense.builder()
                    .amount(new BigDecimal("33.33"))
                    .exchangeRate(new BigDecimal("4.500000"))
                    .build();

            // 33.33 * 4.5 = 149.985000
            BigDecimal result = expense.getAmountInBaseCurrency();
            assertEquals(0, new BigDecimal("149.985000").compareTo(result));
        }
    }

    @Nested
    @DisplayName("Equals and HashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("Should be equal when same ID")
        void equals_sameId_shouldBeEqual() {
            UUID id = UUID.randomUUID();
            PersonalExpense a = PersonalExpense.builder().id(id).description("A").amount(BigDecimal.ONE).build();
            PersonalExpense b = PersonalExpense.builder().id(id).description("B").amount(BigDecimal.TEN).build();

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when different ID")
        void equals_differentId_shouldNotBeEqual() {
            PersonalExpense a = PersonalExpense.builder().id(UUID.randomUUID()).description("A").amount(BigDecimal.ONE).build();
            PersonalExpense b = PersonalExpense.builder().id(UUID.randomUUID()).description("A").amount(BigDecimal.ONE).build();

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("Should not be equal when ID is null")
        void equals_nullId_shouldNotBeEqual() {
            PersonalExpense a = PersonalExpense.builder().description("A").amount(BigDecimal.ONE).build();
            PersonalExpense b = PersonalExpense.builder().description("A").amount(BigDecimal.ONE).build();

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("Should be equal to self")
        void equals_self_shouldBeEqual() {
            PersonalExpense a = PersonalExpense.builder().id(UUID.randomUUID()).description("A").amount(BigDecimal.ONE).build();
            assertEquals(a, a);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void equals_null_shouldNotBeEqual() {
            PersonalExpense a = PersonalExpense.builder().id(UUID.randomUUID()).description("A").amount(BigDecimal.ONE).build();
            assertNotEquals(null, a);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void equals_differentType_shouldNotBeEqual() {
            PersonalExpense a = PersonalExpense.builder().id(UUID.randomUUID()).description("A").amount(BigDecimal.ONE).build();
            assertNotEquals("not an expense", a);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("Should contain key fields in toString")
        void toString_shouldContainKeyFields() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID tripId = UUID.randomUUID();

            PersonalExpense expense = PersonalExpense.builder()
                    .id(id)
                    .userId(userId)
                    .tripId(tripId)
                    .description("Coffee")
                    .amount(BigDecimal.valueOf(120))
                    .currency("TWD")
                    .build();

            String result = expense.toString();
            assertTrue(result.contains(id.toString()));
            assertTrue(result.contains(userId.toString()));
            assertTrue(result.contains(tripId.toString()));
            assertTrue(result.contains("Coffee"));
            assertTrue(result.contains("120"));
            assertTrue(result.contains("TWD"));
        }
    }

    @Nested
    @DisplayName("All Fields")
    class AllFields {

        @Test
        @DisplayName("Should set all fields via builder")
        void build_allFields_shouldSetCorrectly() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID tripId = UUID.randomUUID();
            Instant now = Instant.now();
            LocalDate date = LocalDate.of(2026, 2, 15);

            PersonalExpense expense = PersonalExpense.builder()
                    .id(id)
                    .userId(userId)
                    .tripId(tripId)
                    .description("Shopping")
                    .amount(BigDecimal.valueOf(500))
                    .currency("JPY")
                    .exchangeRate(BigDecimal.valueOf(0.22))
                    .category("SHOPPING")
                    .expenseDate(date)
                    .note("Souvenirs")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            assertEquals(id, expense.getId());
            assertEquals(userId, expense.getUserId());
            assertEquals(tripId, expense.getTripId());
            assertEquals("Shopping", expense.getDescription());
            assertEquals(BigDecimal.valueOf(500), expense.getAmount());
            assertEquals("JPY", expense.getCurrency());
            assertEquals(BigDecimal.valueOf(0.22), expense.getExchangeRate());
            assertEquals("SHOPPING", expense.getCategory());
            assertEquals(date, expense.getExpenseDate());
            assertEquals("Souvenirs", expense.getNote());
            assertEquals(now, expense.getCreatedAt());
            assertEquals(now, expense.getUpdatedAt());
        }
    }
}
