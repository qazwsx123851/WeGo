package com.wego.repository;

import com.wego.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Activity entity operations.
 *
 * @contract
 *   - pre: All parameters must be non-null unless marked Optional
 *   - post: Returns Optional.empty() when no match found
 *   - calledBy: ActivityService
 */
@Repository
public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    /**
     * Finds all activities for a specific trip, ordered by day and sortOrder.
     *
     * @param tripId The trip ID
     * @return List of activities ordered by day and sortOrder
     */
    List<Activity> findByTripIdOrderByDayAscSortOrderAsc(UUID tripId);

    /**
     * Finds all activities for a specific day of a trip.
     *
     * @param tripId The trip ID
     * @param day The day number
     * @return List of activities for that day, ordered by sortOrder
     */
    List<Activity> findByTripIdAndDayOrderBySortOrderAsc(UUID tripId, int day);

    /**
     * Finds the maximum sortOrder for a day in a trip.
     *
     * @param tripId The trip ID
     * @param day The day number
     * @return Maximum sortOrder, or null if no activities exist
     */
    @Query("SELECT MAX(a.sortOrder) FROM Activity a WHERE a.tripId = :tripId AND a.day = :day")
    Optional<Integer> findMaxSortOrderByTripIdAndDay(@Param("tripId") UUID tripId, @Param("day") int day);

    /**
     * Counts activities for a specific day.
     *
     * @param tripId The trip ID
     * @param day The day number
     * @return Number of activities for that day
     */
    long countByTripIdAndDay(UUID tripId, int day);

    /**
     * Counts all activities in a trip.
     *
     * @param tripId The trip ID
     * @return Total number of activities
     */
    long countByTripId(UUID tripId);

    /**
     * Deletes all activities for a trip.
     *
     * @param tripId The trip ID
     */
    void deleteByTripId(UUID tripId);

    /**
     * Updates sortOrder for activities after a certain position.
     *
     * @param tripId The trip ID
     * @param day The day number
     * @param afterSortOrder The sortOrder threshold
     * @param increment The amount to increment
     */
    @Modifying
    @Query("UPDATE Activity a SET a.sortOrder = a.sortOrder + :increment " +
           "WHERE a.tripId = :tripId AND a.day = :day AND a.sortOrder > :afterSortOrder")
    void incrementSortOrderAfter(@Param("tripId") UUID tripId,
                                  @Param("day") int day,
                                  @Param("afterSortOrder") int afterSortOrder,
                                  @Param("increment") int increment);
}
