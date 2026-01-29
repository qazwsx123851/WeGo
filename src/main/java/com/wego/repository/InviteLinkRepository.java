package com.wego.repository;

import com.wego.entity.InviteLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for InviteLink entity operations.
 *
 * @contract
 *   - pre: All parameters must be non-null unless marked Optional
 *   - post: Returns Optional.empty() when no match found
 *   - calledBy: InviteLinkService
 */
@Repository
public interface InviteLinkRepository extends JpaRepository<InviteLink, UUID> {

    /**
     * Finds an invite link by its token.
     *
     * @contract
     *   - pre: token != null
     *   - post: Returns Optional containing link if found
     *   - calledBy: InviteLinkService#acceptInvite
     *
     * @param token The invite token
     * @return Optional containing the invite link if found
     */
    Optional<InviteLink> findByToken(String token);

    /**
     * Finds all invite links for a trip.
     *
     * @contract
     *   - pre: tripId != null
     *   - post: Returns list of invite links, may be empty
     *   - calledBy: TripService#getInviteLinks
     *
     * @param tripId The trip ID
     * @return List of invite links
     */
    List<InviteLink> findByTripId(UUID tripId);

    /**
     * Finds all non-expired invite links for a trip.
     *
     * @contract
     *   - pre: tripId != null
     *   - post: Returns list of valid (non-expired) invite links
     *   - calledBy: TripService#getActiveInviteLinks
     *
     * @param tripId The trip ID
     * @param now The current timestamp
     * @return List of non-expired invite links
     */
    @Query("SELECT i FROM InviteLink i WHERE i.tripId = :tripId AND i.expiresAt > :now")
    List<InviteLink> findActiveByTripId(@Param("tripId") UUID tripId, @Param("now") Instant now);

    /**
     * Checks if a token exists.
     *
     * @contract
     *   - pre: token != null
     *   - post: Returns true if token exists
     *   - calledBy: InviteLinkService (for uniqueness check)
     *
     * @param token The invite token
     * @return true if token exists
     */
    boolean existsByToken(String token);

    /**
     * Deletes all expired invite links.
     *
     * @contract
     *   - post: All links with expiresAt <= now are deleted
     *   - calledBy: Scheduled cleanup task
     *
     * @param now The current timestamp
     * @return Number of deleted links
     */
    @Modifying
    @Query("DELETE FROM InviteLink i WHERE i.expiresAt <= :now")
    int deleteExpiredLinks(@Param("now") Instant now);

    /**
     * Deletes all invite links for a trip.
     *
     * @contract
     *   - pre: tripId != null
     *   - post: All invite links for trip are deleted
     *   - calledBy: TripService#deleteTrip
     *
     * @param tripId The trip ID
     */
    void deleteByTripId(UUID tripId);

    /**
     * Counts active (non-expired) invite links for a trip.
     *
     * @contract
     *   - pre: tripId != null
     *   - post: Returns count >= 0
     *
     * @param tripId The trip ID
     * @param now The current timestamp
     * @return Number of active invite links
     */
    @Query("SELECT COUNT(i) FROM InviteLink i WHERE i.tripId = :tripId AND i.expiresAt > :now")
    long countActiveByTripId(@Param("tripId") UUID tripId, @Param("now") Instant now);
}
