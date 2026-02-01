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
 *   - manualTransportMinutes: optional, for manual transport time input
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

    /**
     * Manual transport duration in minutes.
     * Used when transportMode is FLIGHT, HIGH_SPEED_RAIL, or user wants to override auto-calculation.
     * Maximum 2880 minutes (48 hours).
     */
    @Min(value = 0, message = "交通時間必須大於等於0")
    @Max(value = 2880, message = "交通時間不可超過48小時")
    private Integer manualTransportMinutes;

    /**
     * Checks if this request has manual transport input.
     *
     * @return true if manualTransportMinutes is provided
     */
    public boolean hasManualTransport() {
        return manualTransportMinutes != null && manualTransportMinutes > 0;
    }

    /**
     * Checks if the transport mode requires manual input.
     *
     * @return true if mode is FLIGHT or HIGH_SPEED_RAIL
     */
    public boolean requiresManualTransportInput() {
        return transportMode != null && transportMode.requiresManualInput();
    }
}
