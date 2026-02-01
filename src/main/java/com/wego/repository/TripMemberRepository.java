package com.wego.repository;

import com.wego.entity.Role;
import com.wego.entity.TripMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TripMember entity operations.
 *
 * @contract
 *   - pre: All parameters must be non-null unless marked Optional
 *   - post: Returns Optional.empty() when no match found
 *   - calledBy: TripService, PermissionChecker
 */
@Repository
public interface TripMemberRepository extends JpaRepository<TripMember, UUID> {

    /**
     * Finds a specific membership by trip and user.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - post: Returns Optional containing membership if found
     *   - calledBy: PermissionChecker, TripService#addMember
     *
     * @param tripId The trip ID
     * @param userId The user ID
     * @return Optional containing the membership if found
     */
    Optional<TripMember> findByTripIdAndUserId(UUID tripId, UUID userId);

    /**
     * Finds all members of a trip.
     *
     * @contract
     *   - pre: tripId != null
     *   - post: Returns list of members, may be empty
     *   - calledBy: TripService#getTripMembers
     *
     * @param tripId The trip ID
     * @return List of trip members
     */
    List<TripMember> findByTripId(UUID tripId);

    /**
     * Finds all memberships for a user across all trips.
     *
     * @contract
     *   - pre: userId != null
     *   - post: Returns list of memberships, may be empty
     *   - calledBy: UserService#getUserMemberships
     *
     * @param userId The user ID
     * @return List of memberships
     */
    List<TripMember> findByUserId(UUID userId);

    /**
     * Checks if a user is a member of a trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - post: Returns true if membership exists
     *   - calledBy: PermissionChecker#isMember
     *
     * @param tripId The trip ID
     * @param userId The user ID
     * @return true if user is a member
     */
    boolean existsByTripIdAndUserId(UUID tripId, UUID userId);

    /**
     * Counts members of a trip.
     *
     * @contract
     *   - pre: tripId != null
     *   - post: Returns count >= 0
     *   - calledBy: TripService#getMemberCount, InviteLinkService (for limit check)
     *
     * @param tripId The trip ID
     * @return Number of members
     */
    long countByTripId(UUID tripId);

    /**
     * Finds the owner of a trip.
     *
     * @contract
     *   - pre: tripId != null
     *   - post: Returns Optional containing owner if found (should always exist)
     *   - calledBy: TripService#getOwner
     *
     * @param tripId The trip ID
     * @param role The role (OWNER)
     * @return Optional containing the owner membership
     */
    Optional<TripMember> findByTripIdAndRole(UUID tripId, Role role);

    /**
     * Deletes all memberships for a trip.
     *
     * @contract
     *   - pre: tripId != null
     *   - post: All memberships for trip are deleted
     *   - calledBy: TripService#deleteTrip
     *
     * @param tripId The trip ID
     */
    void deleteByTripId(UUID tripId);

    /**
     * Deletes a specific membership.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - calledBy: TripService#removeMember
     *
     * @param tripId The trip ID
     * @param userId The user ID
     */
    void deleteByTripIdAndUserId(UUID tripId, UUID userId);

    // ========== Global Profile Methods ==========

    /**
     * Counts the number of trips a user is a member of.
     *
     * @contract
     *   - pre: userId != null
     *   - post: Returns count >= 0
     *   - calledBy: ProfileController#showProfile
     *
     * @param userId The user ID
     * @return Number of trips user is member of
     */
    long countByUserId(UUID userId);
}
