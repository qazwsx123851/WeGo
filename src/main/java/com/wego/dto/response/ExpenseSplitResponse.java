package com.wego.dto.response;

import com.wego.entity.ExpenseSplit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for expense split information.
 *
 * @contract
 *   - id: the split ID
 *   - userId: the user responsible for this split
 *   - userNickname: display name of the user
 *   - amount: the split amount
 *   - isSettled: whether this split has been paid
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSplitResponse {

    private UUID id;
    private UUID userId;
    private String userNickname;
    private String userAvatarUrl;
    private BigDecimal amount;
    private boolean isSettled;
    private Instant settledAt;

    /**
     * Creates an ExpenseSplitResponse from an ExpenseSplit entity.
     * Note: User nickname must be set separately.
     *
     * @param split The expense split entity
     * @return ExpenseSplitResponse DTO
     */
    public static ExpenseSplitResponse fromEntity(ExpenseSplit split) {
        return ExpenseSplitResponse.builder()
                .id(split.getId())
                .userId(split.getUserId())
                .amount(split.getAmount())
                .isSettled(split.isSettled())
                .settledAt(split.getSettledAt())
                .build();
    }
}
