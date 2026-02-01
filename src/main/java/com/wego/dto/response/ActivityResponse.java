package com.wego.dto.response;

import com.wego.entity.Activity;
import com.wego.entity.Place;
import com.wego.entity.TransportMode;
import com.wego.entity.TransportSource;
import com.wego.entity.TransportWarning;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Response DTO for activity information.
 *
 * @contract
 *   - id: always present
 *   - tripId: always present
 *   - day: always present, >= 1
 *   - sortOrder: always present, >= 0
 *   - place may be null if placeId is null
 *   - transportSource: source of transport calculation (GOOGLE_API, HAVERSINE, MANUAL, NOT_APPLICABLE)
 *   - transportWarning: warning about transport data accuracy
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponse {

    private UUID id;
    private UUID tripId;
    private PlaceResponse place;
    private int day;
    private int sortOrder;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private String note;
    private TransportMode transportMode;
    private Integer transportDurationMinutes;
    private Integer transportDistanceMeters;
    private TransportSource transportSource;
    private TransportWarning transportWarning;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Creates an ActivityResponse from an Activity entity and Place entity.
     *
     * @contract
     *   - pre: activity != null
     *   - post: returns ActivityResponse with all fields mapped
     *   - calledBy: ActivityService
     *
     * @param activity The activity entity
     * @param place The place entity (may be null)
     * @return ActivityResponse DTO
     */
    public static ActivityResponse fromEntity(Activity activity, Place place) {
        if (activity == null) {
            return null;
        }
        return ActivityResponse.builder()
                .id(activity.getId())
                .tripId(activity.getTripId())
                .place(PlaceResponse.fromEntity(place))
                .day(activity.getDay())
                .sortOrder(activity.getSortOrder())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .durationMinutes(activity.getDurationMinutes())
                .note(activity.getNote())
                .transportMode(activity.getTransportMode())
                .transportDurationMinutes(activity.getTransportDurationMinutes())
                .transportDistanceMeters(activity.getTransportDistanceMeters())
                .transportSource(activity.getTransportSource())
                .transportWarning(activity.getTransportWarning())
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .build();
    }

    /**
     * Checks if the transport data is estimated (not from Google API).
     *
     * @return true if source is HAVERSINE or MANUAL
     */
    public boolean isEstimated() {
        return transportSource == TransportSource.HAVERSINE ||
                transportSource == TransportSource.MANUAL;
    }

    /**
     * Checks if there's a warning that should be displayed.
     *
     * @return true if warning is not NONE
     */
    public boolean hasWarning() {
        return transportWarning != null && transportWarning != TransportWarning.NONE;
    }

    /**
     * Gets the display message for the transport warning.
     *
     * @return Warning message in Traditional Chinese, or null if no warning
     */
    public String getWarningMessage() {
        return transportWarning != null ? transportWarning.getDisplayMessage() : null;
    }

    /**
     * Gets the severity level for the warning (for UI styling).
     *
     * @return "info" or "warning", or null if no warning
     */
    public String getWarningSeverity() {
        return transportWarning != null ? transportWarning.getSeverity() : null;
    }

    /**
     * Gets the display name for the transport mode.
     *
     * @return Mode name in Traditional Chinese
     */
    public String getTransportModeDisplayName() {
        return transportMode != null ? transportMode.getDisplayName() : null;
    }
}
