package com.wego.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Expense entity.
 *
 * Covers test cases: E-001 to E-006
 */
@Tag("fast")
@DisplayName("Expense Entity Tests")
class ExpenseTest {

    @Nested
    @DisplayName("Expense Creation")
    class ExpenseCreation {

        @Test
        @DisplayName("E-001: Should create expense with valid input")
        void createExpense_withValidInput_shouldCreateExpense() {
            UUID tripId = UUID.randomUUID();
            UUID paidBy = UUID.randomUUID();
            UUID createdBy = UUID.randomUUID();

            Expense expense = Expense.builder()
                    .tripId(tripId)
                    .description("午餐")
                    .amount(new BigDecimal("3000"))
                    .currency("JPY")
                    .paidBy(paidBy)
                    .createdBy(createdBy)
                    .splitType(SplitType.EQUAL)
                    .expenseDate(LocalDate.now())
                    .build();

            assertNotNull(expense);
            assertEquals(tripId, expense.getTripId());
            assertEquals("午餐", expense.getDescription());
            assertEquals(new BigDecimal("3000"), expense.getAmount());
            assertEquals("JPY", expense.getCurrency());
            assertEquals(paidBy, expense.getPaidBy());
            assertEquals(SplitType.EQUAL, expense.getSplitType());
        }

        @Test
        @DisplayName("Should default to TWD currency")
        void createExpense_shouldDefaultToTWD() {
            Expense expense = Expense.builder()
                    .tripId(UUID.randomUUID())
                    .description("Test")
                    .amount(new BigDecimal("100"))
                    .paidBy(UUID.randomUUID())
                    .createdBy(UUID.randomUUID())
                    .build();

            assertEquals("TWD", expense.getCurrency());
        }

        @Test
        @DisplayName("Should default to EQUAL split type")
        void createExpense_shouldDefaultToEqualSplit() {
            Expense expense = Expense.builder()
                    .tripId(UUID.randomUUID())
                    .description("Test")
                    .amount(new BigDecimal("100"))
                    .paidBy(UUID.randomUUID())
                    .createdBy(UUID.randomUUID())
                    .build();

            assertEquals(SplitType.EQUAL, expense.getSplitType());
        }
    }

    @Nested
    @DisplayName("Currency Conversion")
    class CurrencyConversion {

        @Test
        @DisplayName("Should convert to base currency with exchange rate")
        void getAmountInBaseCurrency_withExchangeRate_shouldConvert() {
            Expense expense = Expense.builder()
                    .tripId(UUID.randomUUID())
                    .description("Dinner")
                    .amount(new BigDecimal("10000"))
                    .currency("JPY")
                    .exchangeRate(new BigDecimal("0.22"))
                    .paidBy(UUID.randomUUID())
                    .createdBy(UUID.randomUUID())
                    .build();

            BigDecimal converted = expense.getAmountInBaseCurrency();

            assertEquals(new BigDecimal("2200.00"), converted);
        }

        @Test
        @DisplayName("Should return original amount when no exchange rate")
        void getAmountInBaseCurrency_withoutExchangeRate_shouldReturnOriginal() {
            Expense expense = Expense.builder()
                    .tripId(UUID.randomUUID())
                    .description("Lunch")
                    .amount(new BigDecimal("500"))
                    .currency("TWD")
                    .paidBy(UUID.randomUUID())
                    .createdBy(UUID.randomUUID())
                    .build();

            assertEquals(new BigDecimal("500"), expense.getAmountInBaseCurrency());
        }
    }

    @Nested
    @DisplayName("Expense Equality")
    class ExpenseEquality {

        @Test
        @DisplayName("Same ID should be equal")
        void equals_sameId_shouldBeEqual() {
            UUID expenseId = UUID.randomUUID();

            Expense expense1 = new Expense();
            expense1.setId(expenseId);

            Expense expense2 = new Expense();
            expense2.setId(expenseId);

            assertEquals(expense1, expense2);
            assertEquals(expense1.hashCode(), expense2.hashCode());
        }
    }
}
