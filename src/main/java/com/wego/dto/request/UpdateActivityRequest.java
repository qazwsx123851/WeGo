package com.wego.dto.request;

import com.wego.entity.TransportMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Request DTO for updating an activity.
 *
 * All fields are optional - only provided fields will be updated.
 *
 * @contract
 *   - placeId: optional, must reference existing Place if provided
 *   - day: optional, must be >= 1 if provided
 *   - startTime: optional, format HH:mm
 *   - durationMinutes: optional, must be >= 0 if provided
 *   - note: optional, max 1000 characters
 *   - transportMode: optional
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateActivityRequest {

    private UUID placeId;

    @Min(value = 1, message = "天數必須大於等於1")
    private Integer day;

    private LocalTime startTime;

    @Min(value = 0, message = "停留時間必須大於等於0")
    @Max(value = 1440, message = "停留時間不可超過24小時")
    private Integer durationMinutes;

    @Size(max = 1000, message = "備註不可超過1000字")
    private String note;

    private TransportMode transportMode;
}
