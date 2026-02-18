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
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * PersonalExpense entity representing an individual expense tracked by a trip member.
 *
 * Personal expenses are private to the user and are not shared with other trip members.
 * They allow each member to track their own spending independently of team expenses.
 *
 * @contract
 *   - invariant: userId is never null
 *   - invariant: tripId is never null
 *   - invariant: description is never null
 *   - invariant: amount > 0
 *   - invariant: currency is a valid 3-letter code
 *   - invariant: createdAt is never null
 *
 * @see Expense
 * @see TripMember
 */
@Entity
@Table(name = "personal_expenses", indexes = {
    @Index(name = "idx_personal_expense_user_id", columnList = "user_id"),
    @Index(name = "idx_personal_expense_user_trip", columnList = "user_id, trip_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "TWD";

    /**
     * Exchange rate relative to the trip's base currency.
     * Null indicates same currency (1:1 ratio, no conversion needed).
     */
    @Column(name = "exchange_rate", precision = 12, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "expense_date")
    private LocalDate expenseDate;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Converts the amount to the trip's base currency using the stored exchange rate.
     *
     * @contract
     *   - pre: exchangeRate != null if currency differs from base currency
     *   - post: returns amount in base currency; if exchangeRate is null or zero, returns amount as-is
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
        PersonalExpense that = (PersonalExpense) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PersonalExpense{" +
                "id=" + id +
                ", userId=" + userId +
                ", tripId=" + tripId +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                '}';
    }
}
