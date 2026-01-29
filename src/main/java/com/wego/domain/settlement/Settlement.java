package com.wego.domain.settlement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Value object representing a settlement transaction.
 *
 * A settlement describes that one user (fromUserId) should pay
 * a specific amount to another user (toUserId).
 *
 * @contract
 *   - invariant: fromUserId != toUserId
 *   - invariant: amount > 0
 *   - immutable
 */
@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Settlement {

    /**
     * The user who owes money (debtor).
     */
    private final UUID fromUserId;

    /**
     * The user who is owed money (creditor).
     */
    private final UUID toUserId;

    /**
     * The amount to be paid.
     */
    private final BigDecimal amount;
}
