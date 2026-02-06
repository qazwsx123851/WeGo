package com.wego.repository;

import com.wego.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Expense entity operations.
 *
 * @contract
 *   - pre: All parameters must be non-null unless marked Optional
 *   - post: Returns Optional.empty() when no match found
 *   - calledBy: ExpenseService, SettlementService
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    /**
     * Finds all expenses for a trip ordered by date descending.
     *
     * @param tripId The trip ID
     * @return List of expenses
     */
    List<Expense> findByTripIdOrderByCreatedAtDesc(UUID tripId);

    /**
     * Finds all expenses for a trip with pagination.
     *
     * @param tripId The trip ID
     * @param pageable Pagination parameters
     * @return Page of expenses
     */
    Page<Expense> findByTripId(UUID tripId, Pageable pageable);

    /**
     * Finds all expenses paid by a specific user in a trip.
     *
     * @param tripId The trip ID
     * @param paidBy The user ID who paid
     * @return List of expenses
     */
    List<Expense> findByTripIdAndPaidBy(UUID tripId, UUID paidBy);

    /**
     * Finds all expenses in a specific category.
     *
     * @param tripId The trip ID
     * @param category The expense category
     * @return List of expenses
     */
    List<Expense> findByTripIdAndCategory(UUID tripId, String category);

    /**
     * Calculates total expense amount for a trip in a specific currency.
     *
     * @param tripId The trip ID
     * @param currency The currency code
     * @return Total amount
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.tripId = :tripId AND e.currency = :currency")
    BigDecimal sumAmountByTripIdAndCurrency(@Param("tripId") UUID tripId,
                                             @Param("currency") String currency);

    /**
     * Counts expenses in a trip.
     *
     * @param tripId The trip ID
     * @return Number of expenses
     */
    long countByTripId(UUID tripId);

    /**
     * Deletes all expenses for a trip.
     *
     * @param tripId The trip ID
     */
    void deleteByTripId(UUID tripId);

    /**
     * Finds distinct categories used in a trip.
     *
     * @param tripId The trip ID
     * @return List of category names
     */
    @Query("SELECT DISTINCT e.category FROM Expense e " +
           "WHERE e.tripId = :tripId AND e.category IS NOT NULL")
    List<String> findDistinctCategoriesByTripId(@Param("tripId") UUID tripId);

    /**
     * Finds all expenses linked to a specific activity in a trip.
     *
     * @contract
     *   - pre: tripId != null, activityId != null
     *   - post: Returns expenses ordered by creation date descending
     *   - calledBy: ExpenseService#getExpensesByActivity
     *
     * @param tripId The trip ID
     * @param activityId The activity ID
     * @return List of expenses
     */
    List<Expense> findByTripIdAndActivityIdOrderByCreatedAtDesc(UUID tripId, UUID activityId);

    // ========== Global Expense Methods ==========

    /**
     * Sums total amount paid by user across multiple trips.
     *
     * @contract
     *   - pre: userId != null, tripIds not empty
     *   - post: Returns sum of amounts, 0 if none found
     *   - calledBy: GlobalExpenseService#getOverview
     *
     * @param userId The user ID
     * @param tripIds List of trip IDs
     * @return Total amount paid
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.paidBy = :userId AND e.tripId IN :tripIds")
    BigDecimal sumAmountPaidByUser(@Param("userId") UUID userId,
                                    @Param("tripIds") List<UUID> tripIds);

    /**
     * Counts expenses created by a user.
     *
     * @contract
     *   - pre: createdBy != null
     *   - post: Returns count >= 0
     *   - calledBy: ProfileController#showProfile
     *
     * @param createdBy The user ID
     * @return Number of expenses created
     */
    long countByCreatedBy(UUID createdBy);
}
