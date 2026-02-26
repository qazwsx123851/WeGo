package com.wego.repository;

import com.wego.entity.GhostMember;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for GhostMember entity operations.
 *
 * All active ghost queries filter by mergedToUserIdIsNull to exclude merged (soft-deleted) ghosts.
 *
 * @contract
 *   - pre: All parameters must be non-null unless marked Optional
 *   - post: Returns Optional.empty() when no match found
 *   - calledBy: GhostMemberService, ParticipantResolver, ExpenseService, TripService
 */
@Repository
public interface GhostMemberRepository extends JpaRepository<GhostMember, UUID> {

    /**
     * Finds all active (non-merged) ghost members for a trip.
     *
     * @param tripId The trip ID
     * @return List of active ghost members
     */
    List<GhostMember> findByTripIdAndMergedToUserIdIsNull(UUID tripId);

    /**
     * Counts active ghost members for a trip.
     *
     * @param tripId The trip ID
     * @return Number of active ghost members
     */
    long countByTripIdAndMergedToUserIdIsNull(UUID tripId);

    /**
     * Finds a ghost member by ID and trip ID.
     *
     * @param id The ghost member ID
     * @param tripId The trip ID
     * @return Optional containing the ghost member if found
     */
    Optional<GhostMember> findByIdAndTripId(UUID id, UUID tripId);

    /**
     * Finds a ghost member by ID and trip ID with pessimistic write lock.
     * Used during merge to prevent concurrent operations on the same ghost.
     *
     * @contract
     *   - pre: Must be called within a @Transactional context
     *   - post: Acquires row-level lock on the ghost member
     *   - calledBy: GhostMemberService#mergeGhostToUser
     *
     * @param id The ghost member ID
     * @param tripId The trip ID
     * @return Optional containing the locked ghost member if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GhostMember g WHERE g.id = :id AND g.tripId = :tripId")
    Optional<GhostMember> findByIdAndTripIdForUpdate(@Param("id") UUID id,
                                                      @Param("tripId") UUID tripId);

    /**
     * Checks if an active ghost member with the given name exists in a trip.
     *
     * @param tripId The trip ID
     * @param displayName The display name to check
     * @return true if a ghost with this name exists in the trip
     */
    boolean existsByTripIdAndDisplayNameAndMergedToUserIdIsNull(UUID tripId, String displayName);

    /**
     * Deletes all ghost members for a trip. Used during trip deletion.
     *
     * @param tripId The trip ID
     */
    void deleteByTripId(UUID tripId);
}
