package com.wego.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for updating a personal expense (PATCH semantics).
 *
 * @contract
 *   - All fields are optional; only provided (non-null) fields are applied
 *   - description: max 200 characters if provided
 *   - amount: must be > 0 if provided
 *   - currency: max 3-letter code if provided
 *   - category: max 50 characters if provided
 *   - note: max 500 characters if provided
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePersonalExpenseRequest {

    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;

    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Size(max = 3, message = "Currency must be a 3-letter code")
    private String currency;

    private BigDecimal exchangeRate;

    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    private LocalDate expenseDate;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;
}
