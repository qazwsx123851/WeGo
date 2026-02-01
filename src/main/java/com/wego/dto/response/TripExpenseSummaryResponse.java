package com.wego.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for per-trip expense summary in global view.
 *
 * @contract
 *   - tripId: the trip ID
 *   - tripTitle: trip name for display
 *   - coverImageUrl: trip cover image (nullable)
 *   - userBalance: positive = owed to user, negative = user owes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripExpenseSummaryResponse {

    private UUID tripId;
    private String tripTitle;
    private String coverImageUrl;
    private BigDecimal userBalance;

    /**
     * Checks if user owes money in this trip.
     *
     * @return true if user owes money
     */
    public boolean isInDebt() {
        return userBalance != null && userBalance.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Gets absolute balance amount for display.
     *
     * @return absolute value of balance
     */
    public BigDecimal getAbsoluteBalance() {
        return userBalance != null ? userBalance.abs() : BigDecimal.ZERO;
    }
}
