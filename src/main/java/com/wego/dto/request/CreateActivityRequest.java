package com.wego.dto.request;

import com.wego.entity.TransportMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Request DTO for creating a new activity.
 *
 * @contract
 *   - placeId: required, must reference existing Place
 *   - day: required, must be >= 1
 *   - startTime: optional, format HH:mm
 *   - durationMinutes: optional, must be >= 0 if provided
 *   - note: optional, max 1000 characters
 *   - transportMode: optional, defaults to WALKING
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateActivityRequest {

    @NotNull(message = "地點ID不可為空")
    private UUID placeId;

    @NotNull(message = "天數不可為空")
    @Min(value = 1, message = "天數必須大於等於1")
    private Integer day;

    private LocalTime startTime;

    @Min(value = 0, message = "停留時間必須大於等於0")
    @Max(value = 1440, message = "停留時間不可超過24小時")
    private Integer durationMinutes;

    @Size(max = 1000, message = "備註不可超過1000字")
    private String note;

    @Builder.Default
    private TransportMode transportMode = TransportMode.WALKING;
}
