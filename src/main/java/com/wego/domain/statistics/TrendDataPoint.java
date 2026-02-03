package com.wego.domain.statistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Value object representing a single data point in expense trend analysis.
 *
 * @contract
 *   - immutable: All fields are final
 *   - pre: date != null, amount != null and >= 0
 *   - calledBy: ExpenseAggregator#aggregateByDate
 */
public final class TrendDataPoint {
    private final LocalDate date;
    private final BigDecimal amount;
    private final int count;

    /**
     * Creates a new TrendDataPoint.
     *
     * @param date The date for this data point
     * @param amount Total amount for this date
     * @param count Number of expenses on this date
     */
    public TrendDataPoint(LocalDate date, BigDecimal amount, int count) {
        this.date = Objects.requireNonNull(date, "date must not be null");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        this.count = count;
    }

    public LocalDate getDate() {
        return date;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrendDataPoint that = (TrendDataPoint) o;
        return count == that.count &&
               Objects.equals(date, that.date) &&
               Objects.equals(amount, that.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, amount, count);
    }

    @Override
    public String toString() {
        return "TrendDataPoint{" +
               "date=" + date +
               ", amount=" + amount +
               ", count=" + count +
               '}';
    }
}
