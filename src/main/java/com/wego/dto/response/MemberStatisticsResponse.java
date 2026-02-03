package com.wego.dto.response;

import com.wego.domain.statistics.MemberStatistics;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response DTO for member expense statistics.
 *
 * @contract
 *   - calledBy: StatisticsApiController#getMemberStatistics
 */
@Data
@Builder
public class MemberStatisticsResponse {

    private List<MemberItem> members;
    private BigDecimal totalExpenses;
    private String currency;
    private int memberCount;

    @Data
    @Builder
    public static class MemberItem {
        private UUID userId;
        private String nickname;
        private String avatarUrl;
        private BigDecimal totalPaid;
        private BigDecimal totalOwed;
        private BigDecimal balance;
        private int expenseCount;
        private double paidPercentage;

        public static MemberItem from(MemberStatistics stats, BigDecimal totalExpenses) {
            double paidPct = totalExpenses.compareTo(BigDecimal.ZERO) > 0
                    ? stats.getTotalPaid()
                            .divide(totalExpenses, 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue()
                    : 0.0;

            return MemberItem.builder()
                    .userId(stats.getUserId())
                    .nickname(stats.getNickname())
                    .avatarUrl(stats.getAvatarUrl())
                    .totalPaid(stats.getTotalPaid())
                    .totalOwed(stats.getTotalOwed())
                    .balance(stats.getBalance())
                    .expenseCount(stats.getExpenseCount())
                    .paidPercentage(paidPct)
                    .build();
        }
    }

    /**
     * Creates response from domain objects.
     *
     * @param statistics List of member statistics
     * @param currency The base currency
     * @return Response DTO
     */
    public static MemberStatisticsResponse from(List<MemberStatistics> statistics, String currency) {
        BigDecimal total = statistics.stream()
                .map(MemberStatistics::getTotalPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<MemberItem> items = statistics.stream()
                .map(stats -> MemberItem.from(stats, total))
                .collect(Collectors.toList());

        return MemberStatisticsResponse.builder()
                .members(items)
                .totalExpenses(total)
                .currency(currency)
                .memberCount(statistics.size())
                .build();
    }
}
