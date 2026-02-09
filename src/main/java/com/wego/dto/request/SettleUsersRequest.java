package com.wego.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for settling/unsettling all splits between two users.
 *
 * @contract
 *   - pre: fromUserId != null
 *   - pre: toUserId != null
 *   - calledBy: ExpenseApiController#settleByUsers, ExpenseApiController#unsettleByUsers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettleUsersRequest {

    @NotNull(message = "fromUserId is required")
    private UUID fromUserId;

    @NotNull(message = "toUserId is required")
    private UUID toUserId;
}
