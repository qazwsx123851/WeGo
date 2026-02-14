package com.wego.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExpenseSplit entity.
 */
@Tag("fast")
@DisplayName("ExpenseSplit Entity Tests")
class ExpenseSplitTest {

    @Nested
    @DisplayName("ExpenseSplit Creation")
    class ExpenseSplitCreation {

        @Test
        @DisplayName("Should default to unsettled")
        void createSplit_shouldDefaultToUnsettled() {
            ExpenseSplit split = ExpenseSplit.builder()
                    .expenseId(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .amount(new BigDecimal("500"))
                    .build();

            assertFalse(split.isSettled());
            assertNull(split.getSettledAt());
        }
    }

    @Nested
    @DisplayName("Settlement Management")
    class SettlementManagement {

        @Test
        @DisplayName("E-040: Should mark as settled")
        void markAsSettled_shouldSetFlagAndTimestamp() {
            ExpenseSplit split = ExpenseSplit.builder()
                    .expenseId(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .amount(new BigDecimal("500"))
                    .build();

            split.markAsSettled();

            assertTrue(split.isSettled());
            assertNotNull(split.getSettledAt());
        }

        @Test
        @DisplayName("E-041: Should mark as unsettled")
        void markAsUnsettled_shouldClearFlagAndTimestamp() {
            ExpenseSplit split = ExpenseSplit.builder()
                    .expenseId(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .amount(new BigDecimal("500"))
                    .build();

            split.markAsSettled();
            split.markAsUnsettled();

            assertFalse(split.isSettled());
            assertNull(split.getSettledAt());
        }
    }

}
