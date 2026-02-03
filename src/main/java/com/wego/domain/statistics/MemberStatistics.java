package com.wego.domain.statistics;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing expense statistics for a trip member.
 *
 * @contract
 *   - immutable: All fields are final
 *   - pre: userId != null, amounts != null and >= 0
 *   - post: balance = totalPaid - totalOwed
 *   - calledBy: ExpenseAggregator#aggregateByMember
 */
public final class MemberStatistics {
    private final UUID userId;
    private final String nickname;
    private final String avatarUrl;
    private final BigDecimal totalPaid;
    private final BigDecimal totalOwed;
    private final BigDecimal balance;
    private final int expenseCount;

    /**
     * Creates a new MemberStatistics.
     *
     * @param userId The user's ID
     * @param nickname The user's display name
     * @param avatarUrl The user's avatar URL (nullable)
     * @param totalPaid Total amount paid by this user
     * @param totalOwed Total amount this user owes (from splits)
     * @param expenseCount Number of expenses created by this user
     */
    public MemberStatistics(UUID userId, String nickname, String avatarUrl,
                            BigDecimal totalPaid, BigDecimal totalOwed, int expenseCount) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.nickname = Objects.requireNonNull(nickname, "nickname must not be null");
        this.avatarUrl = avatarUrl;
        this.totalPaid = Objects.requireNonNull(totalPaid, "totalPaid must not be null");
        this.totalOwed = Objects.requireNonNull(totalOwed, "totalOwed must not be null");
        if (totalPaid.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("totalPaid must be non-negative");
        }
        if (totalOwed.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("totalOwed must be non-negative");
        }
        this.balance = totalPaid.subtract(totalOwed);
        this.expenseCount = expenseCount;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }

    public BigDecimal getTotalOwed() {
        return totalOwed;
    }

    /**
     * Returns the balance (totalPaid - totalOwed).
     * Positive means user is owed money, negative means user owes money.
     *
     * @return The balance
     */
    public BigDecimal getBalance() {
        return balance;
    }

    public int getExpenseCount() {
        return expenseCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberStatistics that = (MemberStatistics) o;
        return expenseCount == that.expenseCount &&
               Objects.equals(userId, that.userId) &&
               Objects.equals(nickname, that.nickname) &&
               Objects.equals(avatarUrl, that.avatarUrl) &&
               Objects.equals(totalPaid, that.totalPaid) &&
               Objects.equals(totalOwed, that.totalOwed) &&
               Objects.equals(balance, that.balance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, nickname, avatarUrl, totalPaid, totalOwed, balance, expenseCount);
    }

    @Override
    public String toString() {
        return "MemberStatistics{" +
               "userId=" + userId +
               ", nickname='" + nickname + '\'' +
               ", totalPaid=" + totalPaid +
               ", totalOwed=" + totalOwed +
               ", balance=" + balance +
               ", expenseCount=" + expenseCount +
               '}';
    }
}
