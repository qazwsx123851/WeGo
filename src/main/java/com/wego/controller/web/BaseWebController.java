package com.wego.controller.web;

import com.wego.dto.response.TripResponse;
import com.wego.entity.Role;
import com.wego.entity.User;
import com.wego.security.UserPrincipal;
import com.wego.service.TripService;
import com.wego.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

/**
 * Base controller providing common utilities for all Web controllers.
 *
 * Consolidates repeated patterns:
 * - getCurrentUser: resolves UserPrincipal to User entity (zero DB query)
 * - loadTrip: fetches a trip with error handling
 * - findCurrentMember: finds current user's membership in a trip
 * - canEdit/isOwner: permission checks from member summary
 *
 * @contract
 *   - pre: UserService and TripService must be injected
 *   - post: Provides shared utility methods to subclasses
 *   - calledBy: All Web Controllers
 */
@Slf4j
public abstract class BaseWebController {

    @Autowired
    protected UserService userService;

    @Autowired
    protected TripService tripService;

    /**
     * Gets the current user from UserPrincipal (zero DB query).
     *
     * @param principal The authenticated user principal
     * @return The user entity or null if not authenticated
     */
    protected User getCurrentUser(UserPrincipal principal) {
        return principal == null ? null : principal.getUser();
    }

    /**
     * Loads a trip by ID with error handling.
     * Returns null if the trip is not found or access is denied.
     *
     * @param tripId The trip UUID
     * @param userId The requesting user's UUID
     * @return The trip response, or null if not found/forbidden
     */
    protected TripResponse loadTrip(UUID tripId, UUID userId) {
        try {
            TripResponse trip = tripService.getTrip(tripId, userId);
            if (trip == null) {
                log.warn("Trip {} returned null for user {}", tripId, userId);
            }
            return trip;
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", tripId, e.getMessage());
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
