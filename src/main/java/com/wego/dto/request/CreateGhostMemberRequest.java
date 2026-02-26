package com.wego.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a ghost member in a trip.
 *
 * @param displayName The display name (required, max 50 chars)
 * @param note Optional note about the ghost member (max 200 chars)
 */
public record CreateGhostMemberRequest(
        @NotBlank(message = "名稱不可為空")
        @Size(max = 50, message = "名稱長度不可超過 50 字")
        String displayName,

        @Size(max = 200, message = "備註長度不可超過 200 字")
        String note
) {}
