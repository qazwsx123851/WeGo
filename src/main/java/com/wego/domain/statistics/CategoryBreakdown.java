package com.wego.domain.statistics;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing expense breakdown by category.
 *
 * @contract
 *   - immutable: All fields are final
 *   - pre: category != null, amount != null and >= 0, percentage >= 0 and <= 100
 *   - calledBy: ExpenseAggregator#aggregateByCategory
 */
public final class CategoryBreakdown {
    private final String category;
    private final BigDecimal amount;
    private final double percentage;
    private final int count;

    /**
     * Creates a new CategoryBreakdown.
     *
     * @param category The expense category name
     * @param amount Total amount in this category
     * @param percentage Percentage of total expenses
     * @param count Number of expenses in this category
     */
    public CategoryBreakdown(String category, BigDecimal amount, double percentage, int count) {
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("percentage must be between 0 and 100");
        }
        this.percentage = percentage;
        this.count = count;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public double getPercentage() {
        return percentage;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryBreakdown that = (CategoryBreakdown) o;
        return Double.compare(that.percentage, percentage) == 0 &&
               count == that.count &&
               Objects.equals(category, that.category) &&
               Objects.equals(amount, that.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, amount, percentage, count);
    }

    @Override
    public String toString() {
        return "CategoryBreakdown{" +
               "category='" + category + '\'' +
               ", amount=" + amount +
               ", percentage=" + percentage +
               ", count=" + count +
               '}';
    }
}
