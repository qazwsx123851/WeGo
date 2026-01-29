package com.wego.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for settlement calculation results.
 *
 * @contract
 *   - settlements: list of payments needed to settle all debts
 *   - totalExpenses: sum of all expenses in the trip
 *   - baseCurrency: the currency for all settlement amounts
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
