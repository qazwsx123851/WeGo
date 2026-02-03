package com.wego.dto.response;

import com.wego.domain.statistics.CategoryBreakdown;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for category breakdown statistics.
 *
 * @contract
 *   - calledBy: StatisticsApiController#getCategoryBreakdown
 */
@Data
@Builder
public class CategoryBreakdownResponse {

    private List<CategoryItem> categories;
    private BigDecimal totalAmount;
    private String currency;
    private int totalCount;

    @Data
    @Builder
    public static class CategoryItem {
        private String category;
        private BigDecimal amount;
        private double percentage;
        private int count;
        private String color;

        public static CategoryItem from(CategoryBreakdown breakdown, String color) {
            return CategoryItem.builder()
                    .category(breakdown.getCategory())
                    .amount(breakdown.getAmount())
                    .percentage(breakdown.getPercentage())
                    .count(breakdown.getCount())
                    .color(color)
                    .build();
        }
    }

    /**
     * Creates response from domain objects.
     *
     * @param breakdowns List of category breakdowns
     * @param currency The base currency
     * @return Response DTO
     */
    public static CategoryBreakdownResponse from(List<CategoryBreakdown> breakdowns, String currency) {
        // Default colors for chart (max 10)
        String[] colors = {
            "#F97316", // Orange
            "#0EA5E9", // Sky Blue
            "#22C55E", // Green
            "#A855F7", // Purple
            "#EF4444", // Red
            "#F59E0B", // Amber
            "#06B6D4", // Cyan
            "#EC4899", // Pink
            "#6366F1", // Indigo
            "#84CC16"  // Lime
        };

        List<CategoryItem> items = breakdowns.stream()
                .map((breakdown) -> {
                    int index = breakdowns.indexOf(breakdown) % colors.length;
                    return CategoryItem.from(breakdown, colors[index]);
                })
                .collect(Collectors.toList());

        BigDecimal total = breakdowns.stream()
                .map(CategoryBreakdown::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int count = breakdowns.stream()
                .mapToInt(CategoryBreakdown::getCount)
                .sum();

        return CategoryBreakdownResponse.builder()
                .categories(items)
                .totalAmount(total)
                .currency(currency)
                .totalCount(count)
                .build();
    }
}
