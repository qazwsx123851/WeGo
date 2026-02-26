package com.wego.repository;

import com.wego.entity.Todo;
import com.wego.entity.TodoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Todo entity operations.
 *
 * Provides custom sorting: by due_date ASC (nulls last), with COMPLETED status at bottom.
 *
 * @contract
 *   - pre: All parameters must be non-null unless marked Optional
 *   - post: Returns Optional.empty() when no match found
 *   - calledBy: TodoService
 *
 * @see Todo
 * @see TodoStatus
 */
@Repository
public interface TodoRepository extends JpaRepository<Todo, UUID> {

    /**
     * Finds all todos for a trip with custom sorting:
     * - Non-completed todos first, ordered by due_date ASC (nulls last)
     * - Completed todos at the bottom, ordered by completed_at DESC
     *
     * @contract
     *   - pre: tripId != null
     *   - post: returns list sorted with incomplete todos first by due date,
     *           completed todos last by completion date
     *   - calledBy: TodoService#getTodosByTrip
     *
     * @param tripId The trip ID
     * @return Sorted list of todos
     */
    @Query("SELECT t FROM Todo t WHERE t.tripId = :tripId " +
           "ORDER BY CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END, " +
           "CASE WHEN t.dueDate IS NULL THEN 1 ELSE 0 END, " +
           "t.dueDate ASC, " +
           "t.createdAt DESC")
    List<Todo> findByTripIdOrderedByDueDateAndStatus(@Param("tripId") UUID tripId);

    /**
     * Finds all todos for a trip ordered by creation date (newest first).
     *
     * @param tripId The trip ID
     * @return List of todos
     */
    List<Todo> findByTripIdOrderByCreatedAtDesc(UUID tripId);

    /**
     * Finds all todos for a trip with pagination.
     *
     * @param tripId The trip ID
     * @param pageable Pagination parameters
     * @return Page of todos
     */
    Page<Todo> findByTripId(UUID tripId, Pageable pageable);

    /**
     * Finds all todos assigned to a specific user in a trip.
     *
     * @param tripId The trip ID
     * @param assigneeId The assignee's user ID
     * @return List of todos
     */
    List<Todo> findByTripIdAndAssigneeId(UUID tripId, UUID assigneeId);

    /**
     * Finds all todos with a specific status in a trip.
     *
     * @param tripId The trip ID
     * @param status The todo status
     * @return List of todos
     */
    List<Todo> findByTripIdAndStatus(UUID tripId, TodoStatus status);

    /**
     * Finds all overdue incomplete todos in a trip.
     *
     * @param tripId The trip ID
     * @param today Today's date
     * @return List of overdue todos
     */
    @Query("SELECT t FROM Todo t WHERE t.tripId = :tripId " +
           "AND t.status != 'COMPLETED' " +
           "AND t.dueDate < :today " +
           "ORDER BY t.dueDate ASC")
    List<Todo> findOverdueTodos(@Param("tripId") UUID tripId, @Param("today") LocalDate today);

    /**
     * Counts todos by status in a trip.
     *
     * @param tripId The trip ID
     * @param status The todo status
     * @return Count of todos
     */
    long countByTripIdAndStatus(UUID tripId, TodoStatus status);

    /**
     * Counts todos grouped by status in a single query.
     *
     * @param tripId The trip ID
     * @return List of [TodoStatus, count] pairs
     */
    @Query("SELECT t.status, COUNT(t) FROM Todo t WHERE t.tripId = :tripId GROUP BY t.status")
    List<Object[]> countByTripIdGroupedByStatus(@Param("tripId") UUID tripId);

    /**
     * Counts all todos in a trip.
     *
     * @param tripId The trip ID
     * @return Count of todos
     */
    long countByTripId(UUID tripId);

    /**
     * Deletes all todos for a trip.
     *
     * @param tripId The trip ID
     */
    void deleteByTripId(UUID tripId);

    /**
     * Checks if a todo exists for a trip.
     *
     * @param id The todo ID
     * @param tripId The trip ID
     * @return true if exists
     */
    boolean existsByIdAndTripId(UUID id, UUID tripId);
}
