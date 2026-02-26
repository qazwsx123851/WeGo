package com.wego.dto.response;

import com.wego.entity.GhostMember;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for ghost member data.
 *
 * @param id The ghost member UUID
 * @param tripId The trip UUID
 * @param displayName The display name
 * @param note Optional note
 * @param createdAt When created
 */
public record GhostMemberResponse(
        UUID id,
        UUID tripId,
        String displayName,
        String note,
        Instant createdAt
) {
    public static GhostMemberResponse fromEntity(GhostMember ghost) {
        return new GhostMemberResponse(
                ghost.getId(),
                ghost.getTripId(),
                ghost.getDisplayName(),
                ghost.getNote(),
                ghost.getCreatedAt()
        );
    }
}
