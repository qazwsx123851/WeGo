package com.wego.service;

import com.wego.dto.request.CreateExpenseRequest;
import com.wego.dto.response.ExpenseResponse;
import com.wego.entity.SplitType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helper component for expense view logic.
 * Contains pure business logic extracted from ExpenseWebController
 * for grouping, calculation, and request building.
 *
 * @contract
 *   - All methods are pure functions (no Spring dependencies)
 *   - No side effects beyond logging
 *   - calledBy: ExpenseWebController
 */
@Component
@Slf4j
public class ExpenseViewHelper {

    /**
     * Groups expenses by date using expenseDate, createdAt fallback, or LocalDate.now().
     * Results are sorted in reverse chronological order.
     *
     * @contract
     *   - pre: expenses != null
     *   - post: Returns TreeMap with reverse-order date keys
     *
     * @param expenses List of expense responses to group
     * @return Map of dates to expense lists, newest first
     */
    public Map<LocalDate, List<ExpenseResponse>> groupExpensesByDate(List<ExpenseResponse> expenses) {
        Function<ExpenseResponse, LocalDate> dateClassifier = e ->
                e.getExpenseDate() != null
                        ? e.getExpenseDate()
                        : (e.getCreatedAt() != null
                                ? e.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate()
                                : LocalDate.now());

        return expenses.stream()
                .collect(Collectors.groupingBy(
                        dateClassifier,
                        () -> new TreeMap<>(Comparator.reverseOrder()),
                        Collectors.toList()
                ));
    }

    /**
     * Calculates per-person average expense.
     *
     * @contract
     *   - pre: totalExpense != null
     *   - post: Returns ZERO if memberCount <= 0 or totalExpense <= 0
     *   - post: Returns totalExpense / memberCount rounded HALF_UP to 0 decimal places
     *
     * @param totalExpense The total expense amount
     * @param memberCount  The number of trip members
     * @return The per-person average, or BigDecimal.ZERO
     */
    public BigDecimal calculatePerPersonAverage(BigDecimal totalExpense, int memberCount) {
        if (memberCount <= 0 || totalExpense.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return totalExpense.divide(BigDecimal.valueOf(memberCount), 0, RoundingMode.HALF_UP);
    }

    /**
     * Parses split method string to SplitType enum.
     *
     * @contract
     *   - post: Returns SplitType.EQUAL for null or unrecognized input
     *
     * @param splitMethod The split method string
     * @return The corresponding SplitType
     */
    public SplitType parseSplitType(String splitMethod) {
        if (splitMethod == null) {
            return SplitType.EQUAL;
        }
        return switch (splitMethod.toUpperCase()) {
            case "PERCENTAGE" -> SplitType.PERCENTAGE;
            case "CUSTOM" -> SplitType.CUSTOM;
            case "SHARES" -> SplitType.SHARES;
            default -> SplitType.EQUAL;
        };
    }

    /**
     * Builds split requests based on split type.
     *
     * @contract
     *   - pre: splitType != null
     *   - post: Returns list of SplitRequest objects
     *   - post: For EQUAL with no participantIds, returns empty list (service uses all members)
     *   - post: For PERCENTAGE/CUSTOM, skips entries with zero or negative values
     *
     * @param splitType     The split type
     * @param participantIds List of participant IDs for EQUAL split
     * @param percentages   Map of userId to percentage for PERCENTAGE split
     * @param customAmounts Map of userId to amount for CUSTOM split
     * @param tripId        The trip ID (for logging context)
     * @param totalAmount   The total expense amount
     * @return List of split requests
     */
    public List<CreateExpenseRequest.SplitRequest> buildSplits(
            SplitType splitType,
            List<UUID> participantIds,
            Map<String, String> percentages,
            Map<String, String> customAmounts,
            UUID tripId,
            BigDecimal totalAmount) {

        List<CreateExpenseRequest.SplitRequest> splits = new ArrayList<>();

        switch (splitType) {
            case EQUAL -> {
                // For EQUAL, we may have specific participants
                if (participantIds != null && !participantIds.isEmpty()) {
                    for (UUID participantId : participantIds) {
                        splits.add(CreateExpenseRequest.SplitRequest.builder()
                                .userId(participantId)
                                .build());
                    }
                }
                // If no participants specified, ExpenseService will use all trip members
            }
            case PERCENTAGE -> {
                if (percentages != null) {
                    for (Map.Entry<String, String> entry : percentages.entrySet()) {
                        String key = entry.getKey();
                        // Handle Spring's map key format: percentages[userId]=value
                        if (key.startsWith("percentages[") && key.endsWith("]")) {
                            key = key.substring(12, key.length() - 1);
                        }
                        try {
                            UUID userId = UUID.fromString(key);
                            BigDecimal percentage = new BigDecimal(entry.getValue());
                            if (percentage.compareTo(BigDecimal.ZERO) > 0) {
                                splits.add(CreateExpenseRequest.SplitRequest.builder()
                                        .userId(userId)
                                        .percentage(percentage)
                                        .build());
                            }
                        } catch (Exception e) {
                            log.warn("Invalid percentage entry: {} = {}", key, entry.getValue());
                        }
                    }
                }
            }
            case CUSTOM -> {
                if (customAmounts != null) {
                    for (Map.Entry<String, String> entry : customAmounts.entrySet()) {
                        String key = entry.getKey();
                        // Handle Spring's map key format: customAmounts[userId]=value
                        if (key.startsWith("customAmounts[") && key.endsWith("]")) {
                            key = key.substring(14, key.length() - 1);
                        }
                        try {
                            UUID userId = UUID.fromString(key);
                            BigDecimal customAmount = new BigDecimal(entry.getValue());
                            if (customAmount.compareTo(BigDecimal.ZERO) > 0) {
                                splits.add(CreateExpenseRequest.SplitRequest.builder()
                                        .userId(userId)
                                        .amount(customAmount)
                                        .build());
                            }
                        } catch (Exception e) {
                            log.warn("Invalid custom amount entry: {} = {}", key, entry.getValue());
                        }
                    }
                }
            }
            case SHARES -> {
                // SHARES not implemented in the form yet, but handle it for completeness
                log.debug("SHARES split type not implemented in form");
            }
        }

        return splits;
    }
}
