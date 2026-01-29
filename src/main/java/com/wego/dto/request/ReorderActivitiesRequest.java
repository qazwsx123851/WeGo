package com.wego.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for reordering activities within a day.
 *
 * @contract
 *   - day: required, must be >= 1
 *   - activityIds: required, non-empty list of activity IDs in desired order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderActivitiesRequest {

    @NotNull(message = "天數不可為空")
    @Min(value = 1, message = "天數必須大於等於1")
    private Integer day;

    @NotEmpty(message = "景點ID列表不可為空")
    private List<UUID> activityIds;
}
