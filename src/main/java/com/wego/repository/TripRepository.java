package com.wego.repository;

import com.wego.entity.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Trip entity operations.
 *
 * @contract
 *   - pre: All parameters must be non-null unless marked Optional
 *   - post: Returns Optional.empty() when no match found
 *   - calledBy: TripService
 */
@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    /**
     * Finds all trips owned by a specific user.
     *
     * @contract
     *   - pre: ownerId != null
     *   - post: Returns list of trips owned by user, may be empty
     *   - calledBy: TripService#getOwnedTrips
     *
     * @param ownerId The owner's user ID
     * @return List of trips owned by the user
     */
    List<Trip> findByOwnerId(UUID ownerId);

    /**
     * Finds all trips owned by a specific user with pagination.
     *
     * @contract
     *   - pre: ownerId != null, pageable != null
     *   - post: Returns paginated list of trips
     *   - calledBy: TripService#getOwnedTrips
     *
     * @param ownerId The owner's user ID
     * @param pageable Pagination parameters
     * @return Page of trips
     */
    Page<Trip> findByOwnerId(UUID ownerId, Pageable pageable);

    /**
     * Finds all trips where a user is a member (via TripMember).
     *
     * @contract
     *   - pre: userId != null
     *   - post: Returns list of trips the user is a member of
     *   - calledBy: TripService#getUserTrips
     *
     * @param userId The user ID
     * @return List of trips the user is a member of
     */
    @Query("SELECT t FROM Trip t JOIN TripMember tm ON t.id = tm.tripId " +
           "WHERE tm.userId = :userId ORDER BY t.startDate DESC")
    List<Trip> findTripsByMemberId(@Param("userId") UUID userId);

    /**
     * Finds all trips where a user is a member with pagination.
     *
     * @contract
     *   - pre: userId != null, pageable != null
     *   - post: Returns paginated list of trips
     *   - calledBy: TripService#getUserTrips
     *
     * @param userId The user ID
     * @param pageable Pagination parameters
     * @return Page of trips
     */
    @Query("SELECT t FROM Trip t JOIN TripMember tm ON t.id = tm.tripId " +
           "WHERE tm.userId = :userId")
    Page<Trip> findTripsByMemberId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Checks if a trip exists with the given ID and owner.
     *
     * @contract
     *   - pre: tripId != null, ownerId != null
     *   - post: Returns true if trip exists with given owner
     *
     * @param tripId The trip ID
     * @param ownerId The owner's user ID
     * @return true if trip exists with this owner
     */
    boolean existsByIdAndOwnerId(UUID tripId, UUID ownerId);

    /**
     * Counts trips owned by a specific user.
     *
     * @contract
     *   - pre: ownerId != null
     *   - post: Returns count >= 0
     *
     * @param ownerId The owner's user ID
     * @return Number of trips owned by user
     */
    long countByOwnerId(UUID ownerId);
}
