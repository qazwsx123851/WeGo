package com.wego.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Response DTO for personal expense summary within a trip.
 *
 * @contract
 *   - totalAmount: sum of all personal expenses in baseCurrency
 *   - dailyAverage: null if trip.endDate is null (open-ended trip)
 *   - categoryBreakdown: aggregated amounts per category in baseCurrency
 *   - dailyAmounts: all trip dates zero-filled, keyed by LocalDate
 *   - budget: null when no budget has been set
 *   - budgetStatus: NONE (no budget), GREEN (<80%), YELLOW (80-100%), RED (>100%)
 *   - budgetOverage: positive amount over budget when RED; null otherwise
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalExpenseSummaryResponse {

    public enum BudgetStatus { NONE, GREEN, YELLOW, RED }

    private BigDecimal totalAmount;
    private BigDecimal dailyAverage;     // null if trip.endDate == null
    private Map<String, BigDecimal> categoryBreakdown;
    private Map<LocalDate, BigDecimal> dailyAmounts;  // all trip dates, zero-filled
    private BigDecimal budget;           // nullable (null = no budget set)
    private BudgetStatus budgetStatus;   // NONE/GREEN/YELLOW/RED
    private BigDecimal budgetOverage;    // positive when RED, null otherwise
}
