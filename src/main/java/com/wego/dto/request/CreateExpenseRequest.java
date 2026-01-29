package com.wego.dto.request;

import com.wego.entity.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
 * Request DTO for creating a new expense.
 *
 * @contract
 *   - description: 1-200 characters, not blank
 *   - amount: required, must be > 0
 *   - currency: 3-letter currency code
 *   - paidBy: required, must be a valid user ID
 *   - splitType: required, determines how to split the expense
 *   - splits: required for CUSTOM split type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExpenseRequest {

    @NotBlank(message = "Description is required")
    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter code")
    @Builder.Default
    private String currency = "TWD";

    @NotNull(message = "Paid by user ID is required")
    private UUID paidBy;

    @NotNull(message = "Split type is required")
    @Builder.Default
    private SplitType splitType = SplitType.EQUAL;

    @Valid
    private List<SplitRequest> splits;

    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    private LocalDate expenseDate;

    private UUID activityId;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;

    /**
     * Individual split request for custom split type.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SplitRequest {

        @NotNull(message = "User ID is required for split")
        private UUID userId;

        @DecimalMin(value = "0", message = "Split amount must be >= 0")
        private BigDecimal amount;

        /**
         * Used for PERCENTAGE split type (0-100).
         */
        private BigDecimal percentage;

        /**
         * Used for SHARES split type.
         */
        private Integer shares;
    }
}
