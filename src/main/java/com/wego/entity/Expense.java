package com.wego.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Expense entity representing a payment made during a trip.
 *
 * Expenses track who paid, how much, and how it should be split.
 * The actual split details are stored in ExpenseSplit entities.
 *
 * @contract
 *   - invariant: tripId is never null
 *   - invariant: amount > 0
 *   - invariant: currency is a valid 3-letter code
 *   - invariant: paidBy (userId) is never null
 *   - invariant: createdBy (userId) is never null
 *
 * @see ExpenseSplit
 * @see Trip
 */
@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "TWD";

    @Column(name = "exchange_rate", precision = 12, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "expense_date")
    private LocalDate expenseDate;

    @Column(name = "paid_by", nullable = false)
    private UUID paidBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 20)
    @Builder.Default
    private SplitType splitType = SplitType.EQUAL;

    @Column(name = "activity_id")
    private UUID activityId;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Converts the amount to the trip's base currency.
     *
     * @contract
     *   - pre: exchangeRate != null if currency != baseCurrency
     *   - post: returns amount in base currency
     *
     * @return The amount in base currency
     */
    public BigDecimal getAmountInBaseCurrency() {
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) == 0) {
            return amount;
        }
        return amount.multiply(exchangeRate);
    }

    /**
     * Equality based on ID for JPA entity identity.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Expense expense = (Expense) o;
        return id != null && Objects.equals(id, expense.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Expense{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", paidBy=" + paidBy +
                '}';
    }
}
