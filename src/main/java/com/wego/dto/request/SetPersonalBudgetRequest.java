package com.wego.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for setting a personal budget for a trip member.
 *
 * @contract
 *   - budget: required, must be > 0
 *   - budget is stored in the trip's baseCurrency
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetPersonalBudgetRequest {

    @NotNull(message = "Budget is required")
    @DecimalMin(value = "0.01", message = "Budget must be greater than 0")
    private BigDecimal budget;
}
