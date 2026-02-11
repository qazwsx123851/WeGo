package com.wego.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * ExpenseSplit entity representing one person's share of an expense.
 *
 * Each expense has one or more splits, representing how the expense
 * is divided among trip members.
 *
 * @contract
 *   - invariant: expenseId is never null
 *   - invariant: userId is never null
 *   - invariant: amount >= 0
 *
 * @see Expense
 */
@Entity
@Table(name = "expense_splits", indexes = {
    @Index(name = "idx_split_expense_id", columnList = "expense_id"),
    @Index(name = "idx_split_user_id", columnList = "user_id"),
    @Index(name = "idx_split_user_settled", columnList = "user_id, is_settled")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "expense_id", nullable = false)
    private UUID expenseId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "is_settled", nullable = false)
    @Builder.Default
    private boolean isSettled = false;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Marks this split as settled.
     *
     * @contract
     *   - post: isSettled == true, settledAt is set to now
     */
    public void markAsSettled() {
        this.isSettled = true;
        this.settledAt = Instant.now();
    }

    /**
     * Marks this split as unsettled.
     *
     * @contract
     *   - post: isSettled == false, settledAt is cleared
     */
    public void markAsUnsettled() {
        this.isSettled = false;
        this.settledAt = null;
    }

    /**
     * Equality based on ID for JPA entity identity.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpenseSplit that = (ExpenseSplit) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ExpenseSplit{" +
                "id=" + id +
                ", expenseId=" + expenseId +
                ", userId=" + userId +
                ", amount=" + amount +
                ", isSettled=" + isSettled +
                '}';
    }
}
