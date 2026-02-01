package com.wego.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for global expense overview across all trips.
 *
 * @contract
 *   - totalPaid: total amount user has paid across all trips
 *   - totalOwed: total amount user owes to others (unsettled)
 *   - totalOwedToUser: total amount others owe to user (unsettled)
 *   - netBalance: positive = owed to user, negative = user owes
 *   - tripCount: number of trips user is member of
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalExpenseOverviewResponse {

    private BigDecimal totalPaid;
    private BigDecimal totalOwed;
    private BigDecimal totalOwedToUser;
    private BigDecimal netBalance;
    private int tripCount;

    /**
     * Creates an empty response for users with no trips.
     *
     * @return Empty GlobalExpenseOverviewResponse
     */
    public static GlobalExpenseOverviewResponse empty() {
        return GlobalExpenseOverviewResponse.builder()
                .totalPaid(BigDecimal.ZERO)
                .totalOwed(BigDecimal.ZERO)
                .totalOwedToUser(BigDecimal.ZERO)
                .netBalance(BigDecimal.ZERO)
                .tripCount(0)
                .build();
    }

    /**
     * Checks if user has any unsettled balances.
     *
     * @return true if there are unsettled balances
     */
    public boolean hasUnsettledBalance() {
        return netBalance != null && netBalance.compareTo(BigDecimal.ZERO) != 0;
    }

    /**
     * Checks if user owes money to others.
     *
     * @return true if user owes money
     */
    public boolean isInDebt() {
        return netBalance != null && netBalance.compareTo(BigDecimal.ZERO) < 0;
    }
}
