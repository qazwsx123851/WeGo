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
        @DisplayName("Should create split with valid input")
        void createSplit_withValidInput_shouldCreateSplit() {
            UUID expenseId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            ExpenseSplit split = ExpenseSplit.builder()
                    .expenseId(expenseId)
                    .userId(userId)
                    .amount(new BigDecimal("1000"))
                    .build();

            assertNotNull(split);
            assertEquals(expenseId, split.getExpenseId());
            assertEquals(userId, split.getUserId());
            assertEquals(new BigDecimal("1000"), split.getAmount());
            assertFalse(split.isSettled());
        }

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

    @Nested
    @DisplayName("ExpenseSplit Equality")
    class ExpenseSplitEquality {

        @Test
        @DisplayName("Same ID should be equal")
        void equals_sameId_shouldBeEqual() {
            UUID splitId = UUID.randomUUID();

            ExpenseSplit split1 = new ExpenseSplit();
            split1.setId(splitId);

            ExpenseSplit split2 = new ExpenseSplit();
            split2.setId(splitId);

            assertEquals(split1, split2);
            assertEquals(split1.hashCode(), split2.hashCode());
        }
    }
}
