package com.wego.dto.response;

import com.wego.entity.InviteLink;
import com.wego.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for invite link information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteLinkResponse {

    private UUID id;
    private String token;
    private UUID tripId;
    private Role role;
    private Instant expiresAt;
    private int useCount;
    private Instant createdAt;
    private String inviteUrl;

    /**
     * Creates an InviteLinkResponse from an InviteLink entity.
     *
     * @param link The invite link entity
     * @param baseUrl The base URL for the invite link
     * @return InviteLinkResponse DTO
     */
    public static InviteLinkResponse fromEntity(InviteLink link, String baseUrl) {
        return InviteLinkResponse.builder()
                .id(link.getId())
                .token(link.getToken())
                .tripId(link.getTripId())
                .role(link.getRole())
                .expiresAt(link.getExpiresAt())
                .useCount(link.getUseCount())
                .createdAt(link.getCreatedAt())
                .inviteUrl(baseUrl + "/invite/" + link.getToken())
                .build();
    }
}
