package com.wego.dto.response;

import com.wego.entity.Role;
import com.wego.entity.Trip;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for trip information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {

    private UUID id;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private int durationDays;
    private String baseCurrency;
    private String coverImageUrl;
    private UUID ownerId;
    private int memberCount;
    private Role currentUserRole;
    private List<MemberSummary> members;
    private Instant createdAt;
    private Long daysUntil;

    /**
     * Summary of a trip member.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberSummary {
        private UUID userId;
        private String nickname;
        private String avatarUrl;
        private Role role;
        private boolean isGhost;
    }

    /**
     * Creates a TripResponse from a Trip entity.
     *
     * @param trip The trip entity
     * @return TripResponse DTO
     */
    public static TripResponse fromEntity(Trip trip) {
        return TripResponse.builder()
                .id(trip.getId())
                .title(trip.getTitle())
                .description(trip.getDescription())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .durationDays(trip.getDurationDays())
                .baseCurrency(trip.getBaseCurrency())
                .coverImageUrl(trip.getCoverImageUrl())
                .ownerId(trip.getOwnerId())
                .createdAt(trip.getCreatedAt())
                .build();
    }
}
