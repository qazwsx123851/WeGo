package com.wego.repository;

import com.wego.entity.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Repository for ExpenseSplit entity operations.
 *
 * @contract
 *   - pre: All parameters must be non-null unless marked Optional
 *   - calledBy: ExpenseService, SettlementService
 */
@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, UUID> {

    /**
     * Finds all splits for an expense.
     *
     * @param expenseId The expense ID
     * @return List of splits
     */
    List<ExpenseSplit> findByExpenseId(UUID expenseId);

    /**
     * Finds all splits for a user across all their expenses.
     *
     * @param userId The user ID
     * @return List of splits
     */
    List<ExpenseSplit> findByUserId(UUID userId);

    /**
     * Finds unsettled splits for a user.
     *
     * @param userId The user ID
     * @return List of unsettled splits
     */
    List<ExpenseSplit> findByUserIdAndIsSettledFalse(UUID userId);

    /**
     * Calculates total amount owed by a user across all expenses.
     *
     * @param userId The user ID
     * @return Total amount owed
     */
    @Query("SELECT COALESCE(SUM(es.amount), 0) FROM ExpenseSplit es " +
           "WHERE es.userId = :userId AND es.isSettled = false")
    BigDecimal sumUnsettledAmountByUserId(@Param("userId") UUID userId);

    /**
     * Finds all splits for expenses in a trip.
     *
     * @param tripId The trip ID
     * @return List of all splits for the trip
     */
    @Query("SELECT es FROM ExpenseSplit es JOIN Expense e ON es.expenseId = e.id " +
           "WHERE e.tripId = :tripId")
    List<ExpenseSplit> findByTripId(@Param("tripId") UUID tripId);

    /**
     * Finds all unsettled splits for a trip.
     *
     * @param tripId The trip ID
     * @return List of unsettled splits
     */
    @Query("SELECT es FROM ExpenseSplit es JOIN Expense e ON es.expenseId = e.id " +
           "WHERE e.tripId = :tripId AND es.isSettled = false")
    List<ExpenseSplit> findUnsettledByTripId(@Param("tripId") UUID tripId);

    /**
     * Deletes all splits for an expense.
     *
     * @param expenseId The expense ID
     */
    void deleteByExpenseId(UUID expenseId);

    /**
     * Counts splits for an expense.
     *
     * @param expenseId The expense ID
     * @return Number of splits
     */
    long countByExpenseId(UUID expenseId);
}
