package com.wego.domain.settlement;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * Domain service for simplifying debt relationships.
 *
 * Uses a greedy algorithm to minimize the number of transactions
 * needed to settle all debts by pairing the largest creditor
 * with the largest debtor.
 *
 * @contract
 *   - pre: balances sum to zero (or very close due to rounding)
 *   - post: settlements list minimizes number of transactions
 *   - post: sum of all settlement amounts equals sum of positive balances
 *   - thread-safe (stateless)
 *
 * @see Settlement
 */
@Component
public class DebtSimplifier {

    /**
     * Internal balance record for priority queue.
     */
    private record Balance(UUID userId, BigDecimal amount) {}

    /**
     * Simplifies debt relationships to minimize transactions.
     *
     * Algorithm:
     * 1. Separate users into creditors (positive balance) and debtors (negative balance)
     * 2. Use priority queues to always pair largest creditor with largest debtor
     * 3. Create settlement for the minimum of their amounts
     * 4. Put back any remainder into the appropriate queue
     * 5. Repeat until all debts are settled
     *
     * Time complexity: O(n log n) where n is number of users
     *
     * @contract
     *   - pre: balances != null
     *   - post: returns list of settlements, may be empty if no debts
     *   - calledBy: SettlementService#calculateSettlement
     *
     * @param balances Map of userId to net balance (positive = owed money, negative = owes money)
     * @return List of settlements needed to clear all debts
     */
    public List<Settlement> simplify(Map<UUID, BigDecimal> balances) {
        if (balances == null || balances.isEmpty()) {
            return new ArrayList<>();
        }

        // Separate into creditors (positive) and debtors (negative)
        PriorityQueue<Balance> creditors = new PriorityQueue<>(
                Comparator.comparing(Balance::amount).reversed()
        );
        PriorityQueue<Balance> debtors = new PriorityQueue<>(
                Comparator.comparing(b -> b.amount().abs(), Comparator.reverseOrder())
        );

        for (Map.Entry<UUID, BigDecimal> entry : balances.entrySet()) {
            BigDecimal amount = entry.getValue();
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new Balance(entry.getKey(), amount));
            } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(new Balance(entry.getKey(), amount.abs()));
            }
        }

        List<Settlement> settlements = new ArrayList<>();

        // Greedy matching: pair largest creditor with largest debtor
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Balance creditor = creditors.poll();
            Balance debtor = debtors.poll();

            // Settlement amount is minimum of both
            BigDecimal settlementAmount = creditor.amount().min(debtor.amount());

            settlements.add(Settlement.builder()
                    .fromUserId(debtor.userId())
                    .toUserId(creditor.userId())
                    .amount(settlementAmount)
                    .build());

            // Calculate remainders
            BigDecimal creditorRemaining = creditor.amount().subtract(settlementAmount);
            BigDecimal debtorRemaining = debtor.amount().subtract(settlementAmount);

            // Put back any remaining balance
            if (creditorRemaining.compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new Balance(creditor.userId(), creditorRemaining));
            }
            if (debtorRemaining.compareTo(BigDecimal.ZERO) > 0) {
                debtors.add(new Balance(debtor.userId(), debtorRemaining));
            }
        }

        return settlements;
    }
}
