package com.wego.domain.settlement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DebtSimplifier.
 *
 * Covers test cases: D-001 to D-013
 */
@Tag("fast")
@DisplayName("DebtSimplifier Unit Tests")
class DebtSimplifierTest {

    private DebtSimplifier debtSimplifier;
    private UUID userA;
    private UUID userB;
    private UUID userC;
    private UUID userD;

    @BeforeEach
    void setUp() {
        debtSimplifier = new DebtSimplifier();
        userA = UUID.randomUUID();
        userB = UUID.randomUUID();
        userC = UUID.randomUUID();
        userD = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Basic Scenarios")
    class BasicScenarios {

        @Test
        @DisplayName("D-001: Two person simple debt")
        void simplify_twoPersonSimpleDebt_shouldReturnOneSettlement() {
            // A is owed 100, B owes 100
            Map<UUID, BigDecimal> balances = new HashMap<>();
            balances.put(userA, new BigDecimal("100"));
            balances.put(userB, new BigDecimal("-100"));

            List<Settlement> settlements = debtSimplifier.simplify(balances);

            assertEquals(1, settlements.size());
            Settlement s = settlements.get(0);
            assertEquals(userB, s.getFromUserId());  // B pays
            assertEquals(userA, s.getToUserId());    // A receives
            assertEquals(new BigDecimal("100"), s.getAmount());
        }

        @Test
        @DisplayName("D-002: Three person chain debt should simplify")
        void simplify_threePersonChainDebt_shouldSimplify() {
            // A→B 100, B→C 100 should simplify to A→C 100
            // Net: A owes 100, B neutral, C owed 100
            Map<UUID, BigDecimal> balances = new HashMap<>();
            balances.put(userA, new BigDecimal("-100"));
            balances.put(userB, BigDecimal.ZERO);
            balances.put(userC, new BigDecimal("100"));

            List<Settlement> settlements = debtSimplifier.simplify(balances);

            assertEquals(1, settlements.size());
            Settlement s = settlements.get(0);
            assertEquals(userA, s.getFromUserId());  // A pays
            assertEquals(userC, s.getToUserId());    // C receives
            assertEquals(new BigDecimal("100"), s.getAmount());
        }

        @Test
        @DisplayName("D-003: Circular debt should cancel out")
        void simplify_circularDebt_shouldCancelOut() {
            // A→B 100, B→C 100, C→A 100 = everyone nets to 0
            Map<UUID, BigDecimal> balances = new HashMap<>();
            balances.put(userA, BigDecimal.ZERO);
            balances.put(userB, BigDecimal.ZERO);
            balances.put(userC, BigDecimal.ZERO);

            List<Settlement> settlements = debtSimplifier.simplify(balances);

            assertTrue(settlements.isEmpty());
        }

        @Test
        @DisplayName("D-004: Complex four person debt")
        void simplify_complexFourPersonDebt_shouldMinimizeTransactions() {
            // A paid 1000 for everyone (split 4 ways: 250 each)
            // B paid 400 for A and B (split 2 ways: 200 each)
            // Net balances:
            // A: paid 1000, owes 250+200=450, net=+550
            // B: paid 400, owes 250+200=450, net=-50
            // C: paid 0, owes 250, net=-250
            // D: paid 0, owes 250, net=-250
            Map<UUID, BigDecimal> balances = new HashMap<>();
            balances.put(userA, new BigDecimal("550"));
            balances.put(userB, new BigDecimal("-50"));
            balances.put(userC, new BigDecimal("-250"));
            balances.put(userD, new BigDecimal("-250"));

            List<Settlement> settlements = debtSimplifier.simplify(balances);

            // Should minimize transactions: 3 transactions at most
            assertTrue(settlements.size() <= 3);

            // Verify total amounts
            BigDecimal totalToA = settlements.stream()
                    .filter(s -> s.getToUserId().equals(userA))
                    .map(Settlement::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(new BigDecimal("550"), totalToA);
        }

        @Test
        @DisplayName("D-005: Fractional amounts should round correctly")
        void simplify_fractionalAmounts_shouldRoundCorrectly() {
            // 1000 / 3 = 333.33...
            // A paid, B and C each owe 333.33
            Map<UUID, BigDecimal> balances = new HashMap<>();
            balances.put(userA, new BigDecimal("666.66"));
            balances.put(userB, new BigDecimal("-333.33"));
            balances.put(userC, new BigDecimal("-333.33"));

            List<Settlement> settlements = debtSimplifier.simplify(balances);

            // Should produce valid settlements
            assertFalse(settlements.isEmpty());

            // Total paid should equal total received
            BigDecimal totalPaid = settlements.stream()
                    .map(Settlement::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalOwed = balances.values().stream()
                    .filter(b -> b.compareTo(BigDecimal.ZERO) < 0)
                    .map(BigDecimal::abs)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertEquals(0, totalPaid.compareTo(totalOwed));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("D-010: Empty input should return empty list")
        void simplify_emptyInput_shouldReturnEmptyList() {
            Map<UUID, BigDecimal> balances = new HashMap<>();

            List<Settlement> settlements = debtSimplifier.simplify(balances);

            assertTrue(settlements.isEmpty());
        }

        @Test
        @DisplayName("D-011: Single person expense (everyone owes payer)")
        void simplify_singlePersonPaid_shouldCreateSettlements() {
            // A paid 300, B and C each owe 100
            Map<UUID, BigDecimal> balances = new HashMap<>();
            balances.put(userA, new BigDecimal("200"));
            balances.put(userB, new BigDecimal("-100"));
            balances.put(userC, new BigDecimal("-100"));

            List<Settlement> settlements = debtSimplifier.simplify(balances);

            assertEquals(2, settlements.size());

            // All payments should go to A
            assertTrue(settlements.stream().allMatch(s -> s.getToUserId().equals(userA)));

            // Total should be 200
            BigDecimal total = settlements.stream()
                    .map(Settlement::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(new BigDecimal("200"), total);
        }

        @Test
        @DisplayName("D-012: Zero amount should not generate debt")
        void simplify_zeroAmount_shouldNotGenerateDebt() {
            Map<UUID, BigDecimal> balances = new HashMap<>();
            balances.put(userA, BigDecimal.ZERO);
            balances.put(userB, BigDecimal.ZERO);

            List<Settlement> settlements = debtSimplifier.simplify(balances);

            assertTrue(settlements.isEmpty());
        }

        @Test
        @DisplayName("D-013: Large amounts should not overflow")
        void simplify_largeAmounts_shouldNotOverflow() {
            // Test with millions
            Map<UUID, BigDecimal> balances = new HashMap<>();
            balances.put(userA, new BigDecimal("5000000.00"));
            balances.put(userB, new BigDecimal("-2500000.00"));
            balances.put(userC, new BigDecimal("-2500000.00"));

            List<Settlement> settlements = debtSimplifier.simplify(balances);

            assertFalse(settlements.isEmpty());

            BigDecimal totalPaid = settlements.stream()
                    .map(Settlement::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(new BigDecimal("5000000.00"), totalPaid);
        }
    }

    @Nested
    @DisplayName("Balance Validation")
    class BalanceValidation {

        @Test
        @DisplayName("Balances should sum to zero for valid input")
        void simplify_validInput_balancesShouldSumToZero() {
            Map<UUID, BigDecimal> balances = new HashMap<>();
            balances.put(userA, new BigDecimal("300"));
            balances.put(userB, new BigDecimal("-100"));
            balances.put(userC, new BigDecimal("-100"));
            balances.put(userD, new BigDecimal("-100"));

            // Sum should be zero
            BigDecimal sum = balances.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, sum.compareTo(BigDecimal.ZERO));

            List<Settlement> settlements = debtSimplifier.simplify(balances);

            // All settlements should be valid
            for (Settlement s : settlements) {
                assertTrue(s.getAmount().compareTo(BigDecimal.ZERO) > 0);
                assertNotNull(s.getFromUserId());
                assertNotNull(s.getToUserId());
                assertNotEquals(s.getFromUserId(), s.getToUserId());
            }
        }
    }

    @Nested
    @DisplayName("Settlement Properties")
    class SettlementProperties {

        @Test
        @DisplayName("Settlement should have all required fields")
        void settlement_shouldHaveAllRequiredFields() {
            Map<UUID, BigDecimal> balances = new HashMap<>();
            balances.put(userA, new BigDecimal("100"));
            balances.put(userB, new BigDecimal("-100"));

            List<Settlement> settlements = debtSimplifier.simplify(balances);
            Settlement s = settlements.get(0);

            assertNotNull(s.getFromUserId());
            assertNotNull(s.getToUserId());
            assertNotNull(s.getAmount());
            assertTrue(s.getAmount().compareTo(BigDecimal.ZERO) > 0);
        }
    }
}
