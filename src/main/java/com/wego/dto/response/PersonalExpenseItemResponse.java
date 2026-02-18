package com.wego.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for a single personal expense item.
 *
 * @contract
 *   - source: AUTO (derived from shared trip expense split) or MANUAL (user-entered)
 *   - id: present for MANUAL entries; null for AUTO entries
 *   - amount: always in trip baseCurrency after conversion
 *   - paidByName: present for AUTO entries; null for MANUAL entries
 *   - tripExpenseId: present for AUTO entries; null for MANUAL entries
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalExpenseItemResponse {

    public enum Source { AUTO, MANUAL }

    private Source source;
    private UUID id;              // MANUAL only; null for AUTO
    private String description;
    private BigDecimal amount;    // converted to baseCurrency
    private BigDecimal originalAmount;
    private String originalCurrency;
    private BigDecimal exchangeRate; // nullable; null means same currency (1:1)
    private String category;
    private LocalDate expenseDate; // nullable
    private String paidByName;   // AUTO only; null for MANUAL
    private UUID tripExpenseId;  // AUTO only; null for MANUAL
}
