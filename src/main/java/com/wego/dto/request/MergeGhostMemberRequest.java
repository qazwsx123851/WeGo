package com.wego.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for merging a ghost member into a real user.
 *
 * @param targetUserId The real user's UUID to merge the ghost into (required)
 */
public record MergeGhostMemberRequest(
        @NotNull(message = "目標用戶 ID 不可為空")
        UUID targetUserId
) {}
