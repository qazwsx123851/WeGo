package com.wego.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for settlement calculation results.
 *
 * @contract
 *   - settlements: list of payments needed to settle all debts
 *   - totalExpenses: sum of all expenses in the trip (converted to base currency)
 *   - baseCurrency: the currency for all settlement amounts
 *   - currencyBreakdown: original amounts by currency before conversion
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResponse {

    private List<SettlementItemResponse> settlements;
    private BigDecimal totalExpenses;
    private String baseCurrency;
    private int expenseCount;

    /**
     * Breakdown of total expenses by original currency.
     * Key: currency code (e.g., "USD", "TWD")
     * Value: total amount in that currency before conversion
     */
    private Map<String, BigDecimal> currencyBreakdown;

    /**
     * Warnings about failed currency conversions.
     * Each entry describes a conversion that fell back to using the original amount.
     */
    @Builder.Default
    private List<String> conversionWarnings = new ArrayList<>();

    /**
     * Individual settlement item showing who pays whom.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementItemResponse {
        private UUID fromUserId;
        private String fromUserName;
        private String fromUserAvatarUrl;
        private UUID toUserId;
        private String toUserName;
        private String toUserAvatarUrl;
        private BigDecimal amount;
    }
}
