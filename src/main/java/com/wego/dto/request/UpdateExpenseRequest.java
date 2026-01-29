package com.wego.dto.request;

import com.wego.entity.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for updating an expense.
 * All fields are optional - only provided fields will be updated.
 *
 * @contract
 *   - description: max 200 characters if provided
 *   - amount: must be > 0 if provided
 *   - currency: 3-letter currency code if provided
 *   - splits: required if splitType changes to CUSTOM
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExpenseRequest {

    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;

    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter code")
    private String currency;

    private UUID paidBy;

    private SplitType splitType;

    @Valid
    private List<CreateExpenseRequest.SplitRequest> splits;

    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    private LocalDate expenseDate;

    private UUID activityId;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;
}
