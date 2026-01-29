package com.wego.dto.response;

import com.wego.entity.Activity;
import com.wego.entity.Place;
import com.wego.entity.TransportMode;
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
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .build();
    }
}
