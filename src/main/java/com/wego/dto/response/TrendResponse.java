package com.wego.dto.response;

import com.wego.domain.statistics.TrendDataPoint;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for expense trend statistics.
 *
 * @contract
 *   - calledBy: StatisticsApiController#getTrend
 */
@Data
@Builder
public class TrendResponse {

    private List<TrendItem> dataPoints;
    private BigDecimal totalAmount;
    private BigDecimal averagePerDay;
    private String currency;
    private LocalDate startDate;
    private LocalDate endDate;

    @Data
    @Builder
    public static class TrendItem {
        private LocalDate date;
        private String dateLabel;
        private BigDecimal amount;
        private int count;

        public static TrendItem from(TrendDataPoint dataPoint) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
            return TrendItem.builder()
                    .date(dataPoint.getDate())
                    .dateLabel(dataPoint.getDate().format(formatter))
                    .amount(dataPoint.getAmount())
                    .count(dataPoint.getCount())
                    .build();
        }
    }

    /**
     * Creates response from domain objects.
     *
     * @param dataPoints List of trend data points
     * @param currency The base currency
     * @return Response DTO
     */
    public static TrendResponse from(List<TrendDataPoint> dataPoints, String currency) {
        if (dataPoints.isEmpty()) {
            return TrendResponse.builder()
                    .dataPoints(List.of())
                    .totalAmount(BigDecimal.ZERO)
                    .averagePerDay(BigDecimal.ZERO)
                    .currency(currency)
                    .build();
        }

        List<TrendItem> items = dataPoints.stream()
                .map(TrendItem::from)
                .collect(Collectors.toList());

        BigDecimal total = dataPoints.stream()
                .map(TrendDataPoint::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = total.divide(
                BigDecimal.valueOf(dataPoints.size()),
                2,
                java.math.RoundingMode.HALF_UP
        );

        LocalDate start = dataPoints.get(0).getDate();
        LocalDate end = dataPoints.get(dataPoints.size() - 1).getDate();

        return TrendResponse.builder()
                .dataPoints(items)
                .totalAmount(total)
                .averagePerDay(average)
                .currency(currency)
                .startDate(start)
                .endDate(end)
                .build();
    }
}
