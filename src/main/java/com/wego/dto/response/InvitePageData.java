package com.wego.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * DTO for invite page data, encapsulating all information needed
 * to render the invite acceptance page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitePageData {

    private String token;
    private String tripTitle;
    private UUID tripId;
    private LocalDate tripStartDate;
    private LocalDate tripEndDate;
    private String inviteRole;
    private ZonedDateTime expiresAt;
    private long memberCount;
    private boolean expiresWithin24h;
    private boolean alreadyMember;
    private String error;
}
