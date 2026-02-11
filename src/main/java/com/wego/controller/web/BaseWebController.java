package com.wego.controller.web;

import com.wego.dto.response.TripResponse;
import com.wego.entity.Role;
import com.wego.entity.User;
import com.wego.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.UUID;

/**
 * Base controller providing common utilities for all Web controllers.
 *
 * Consolidates repeated patterns:
 * - getCurrentUser: resolves OAuth2User to User entity
 * - findCurrentMember: finds current user's membership in a trip
 * - canEdit/isOwner: permission checks from member summary
 *
 * @contract
 *   - pre: UserService must be injected
 *   - post: Provides shared utility methods to subclasses
 *   - calledBy: All Web Controllers
 */
@Slf4j
public abstract class BaseWebController {

    @Autowired
    protected UserService userService;

    /**
     * Gets the current user from OAuth2 principal.
     *
     * @param principal The OAuth2 user principal
     * @return The user entity or null if not found
     */
    protected User getCurrentUser(OAuth2User principal) {
        if (principal == null) {
            return null;
        }
        String email = principal.getAttribute("email");
        try {
            return userService.getUserByEmail(email);
        } catch (Exception e) {
            log.warn("Failed to get user by email {}: {}", email, e.getMessage());
            return null;
        }
    }

    /**
     * Finds the current member in the trip response.
     *
     * @param trip The trip response
     * @param userId The user ID
     * @return The member summary or null if not found
     */
    protected TripResponse.MemberSummary findCurrentMember(TripResponse trip, UUID userId) {
        if (trip == null || trip.getMembers() == null) {
            return null;
        }
        return trip.getMembers().stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if the member has edit permission (OWNER or EDITOR).
     *
     * @param member The member summary
     * @return true if the member can edit
     */
    protected boolean canEdit(TripResponse.MemberSummary member) {
        return member != null &&
                (member.getRole() == Role.OWNER || member.getRole() == Role.EDITOR);
    }

    /**
     * Checks if the member is the trip owner.
     *
     * @param member The member summary
     * @return true if the member is OWNER
     */
    protected boolean isOwner(TripResponse.MemberSummary member) {
        return member != null && member.getRole() == Role.OWNER;
    }
}
