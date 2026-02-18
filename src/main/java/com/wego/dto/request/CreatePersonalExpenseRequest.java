package com.wego.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for creating a personal expense.
 *
 * @contract
 *   - description: 1-200 characters, not blank
 *   - amount: required, must be > 0
 *   - currency: optional 3-letter code; defaults to trip baseCurrency if null
 *   - exchangeRate: optional; null means same currency (1:1)
 *   - category: optional, max 50 characters
 *   - expenseDate: optional
 *   - note: optional, max 500 characters
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePersonalExpenseRequest {

    @NotBlank(message = "Description is required")
    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Size(max = 3, message = "Currency must be a 3-letter code")
    private String currency;  // defaults to trip baseCurrency if null

    private BigDecimal exchangeRate;  // null = same currency (1:1)

    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    private LocalDate expenseDate;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;
}
