package com.wego.dto.response;

import com.wego.entity.Expense;
import com.wego.entity.SplitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for expense information.
 *
 * @contract
 *   - id: the expense ID
 *   - tripId: the trip this expense belongs to
 *   - description: expense description
 *   - amount: total expense amount
 *   - splits: list of expense splits with user details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {

    private UUID id;
    private UUID tripId;
    private String description;
    private BigDecimal amount;
    private String currency;
    private BigDecimal exchangeRate;
    private UUID paidBy;
    private String paidByName;
    private String paidByAvatarUrl;
    private boolean paidByIsGhost;
    private SplitType splitType;
    private List<ExpenseSplitResponse> splits;
    private String category;
    private LocalDate expenseDate;
    private UUID activityId;
    private String receiptUrl;
    private String note;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Creates an ExpenseResponse from an Expense entity.
     * Note: splits and paidByName must be set separately.
     *
     * @param expense The expense entity
     * @return ExpenseResponse DTO
     */
    public static ExpenseResponse fromEntity(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .tripId(expense.getTripId())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .currency(expense.getCurrency())
                .exchangeRate(expense.getExchangeRate())
                .paidBy(expense.getPaidBy())
                .splitType(expense.getSplitType())
                .category(expense.getCategory())
                .expenseDate(expense.getExpenseDate())
                .activityId(expense.getActivityId())
                .receiptUrl(expense.getReceiptUrl())
                .note(expense.getNote())
                .createdBy(expense.getCreatedBy())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}
