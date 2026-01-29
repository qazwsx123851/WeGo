package com.wego.domain.permission;

import com.wego.entity.Role;
import com.wego.entity.TripMember;
import com.wego.repository.TripMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain service for checking user permissions on trips.
 *
 * Centralizes all permission logic to ensure consistent authorization
 * across the application. All permission checks delegate to Role methods.
 *
 * @contract
 *   - All methods return false for non-members
 *   - Permission hierarchy: OWNER > EDITOR > VIEWER
 *   - Thread-safe (stateless)
 *
 * @see Role
 * @see TripMember
 */
@Component
@RequiredArgsConstructor
public class PermissionChecker {

    private final TripMemberRepository tripMemberRepository;

    /**
     * Checks if a user can edit trip content (activities, expenses, etc.).
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - post: returns true for OWNER and EDITOR roles
     *   - calledBy: ActivityService, ExpenseService, DocumentService
     *
     * @param tripId The trip ID
     * @param userId The user ID
     * @return true if user can edit
     */
    public boolean canEdit(UUID tripId, UUID userId) {
        return getMember(tripId, userId)
                .map(TripMember::canEdit)
                .orElse(false);
    }

    /**
     * Checks if a user can delete the trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - post: returns true only for OWNER role
     *   - calledBy: TripService#deleteTrip
     *
     * @param tripId The trip ID
     * @param userId The user ID
     * @return true if user can delete
     */
    public boolean canDelete(UUID tripId, UUID userId) {
        return getMember(tripId, userId)
                .map(TripMember::canDelete)
                .orElse(false);
    }

    /**
     * Checks if a user can manage members (add, remove, change roles).
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - post: returns true only for OWNER role
     *   - calledBy: TripService#addMember, TripService#removeMember
     *
     * @param tripId The trip ID
     * @param userId The user ID
     * @return true if user can manage members
     */
    public boolean canManageMembers(UUID tripId, UUID userId) {
        return getMember(tripId, userId)
                .map(TripMember::canManageMembers)
                .orElse(false);
    }

    /**
     * Checks if a user can generate invite links.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - post: returns true for OWNER and EDITOR roles
     *   - calledBy: InviteLinkService#createInviteLink
     *
     * @param tripId The trip ID
     * @param userId The user ID
     * @return true if user can invite
     */
    public boolean canInvite(UUID tripId, UUID userId) {
        return getMember(tripId, userId)
                .map(TripMember::canInvite)
                .orElse(false);
    }

    /**
     * Checks if a user can view trip content.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - post: returns true for any member (OWNER, EDITOR, VIEWER)
     *   - calledBy: TripService#getTrip, all read operations
     *
     * @param tripId The trip ID
     * @param userId The user ID
     * @return true if user can view
     */
    public boolean canView(UUID tripId, UUID userId) {
        return tripMemberRepository.existsByTripIdAndUserId(tripId, userId);
    }

    /**
     * Checks if a user is a member of a trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - post: returns true if membership exists
     *
     * @param tripId The trip ID
     * @param userId The user ID
     * @return true if user is a member
     */
    public boolean isMember(UUID tripId, UUID userId) {
        return tripMemberRepository.existsByTripIdAndUserId(tripId, userId);
    }

    /**
     * Gets the role of a user in a trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - post: returns Optional containing role if member, empty otherwise
     *
     * @param tripId The trip ID
     * @param userId The user ID
     * @return Optional containing the role if found
     */
    public Optional<Role> getRole(UUID tripId, UUID userId) {
        return getMember(tripId, userId).map(TripMember::getRole);
    }

    /**
     * Gets the membership for a user in a trip.
     *
     * @param tripId The trip ID
     * @param userId The user ID
     * @return Optional containing the membership if found
     */
    private Optional<TripMember> getMember(UUID tripId, UUID userId) {
        return tripMemberRepository.findByTripIdAndUserId(tripId, userId);
    }
}
