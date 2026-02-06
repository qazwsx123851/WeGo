package com.wego.repository;

import com.wego.entity.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
     * Deletes all splits for expenses in a trip.
     * Used when deleting a trip to prevent orphaned data.
     *
     * @param tripId The trip ID
     */
    @Modifying
    @Query("DELETE FROM ExpenseSplit es WHERE es.expenseId IN " +
           "(SELECT e.id FROM Expense e WHERE e.tripId = :tripId)")
    void deleteByTripId(@Param("tripId") UUID tripId);

    /**
     * Counts splits for an expense.
     *
     * @param expenseId The expense ID
     * @return Number of splits
     */
    long countByExpenseId(UUID expenseId);

    // ========== Global Expense Methods ==========

    /**
     * Sums unsettled amount user owes to others across multiple trips.
     * Only counts splits where user is NOT the payer.
     *
     * @contract
     *   - pre: userId != null, tripIds not empty
     *   - post: Returns sum of amounts, 0 if none found
     *   - calledBy: GlobalExpenseService#getOverview
     *
     * @param userId The user ID
     * @param tripIds List of trip IDs
     * @return Total unsettled amount user owes
     */
    @Query("SELECT COALESCE(SUM(es.amount), 0) FROM ExpenseSplit es " +
           "JOIN Expense e ON es.expenseId = e.id " +
           "WHERE es.userId = :userId AND e.tripId IN :tripIds " +
           "AND es.isSettled = false AND es.userId != e.paidBy")
    BigDecimal sumUnsettledAmountByUserIdAndTripIds(@Param("userId") UUID userId,
                                                     @Param("tripIds") List<UUID> tripIds);

    /**
     * Sums unsettled amount owed TO user (from expenses user paid).
     * Only counts splits where other users have unsettled amounts.
     *
     * @contract
     *   - pre: userId != null, tripIds not empty
     *   - post: Returns sum of amounts, 0 if none found
     *   - calledBy: GlobalExpenseService#getOverview
     *
     * @param userId The user ID (the payer)
     * @param tripIds List of trip IDs
     * @return Total unsettled amount owed to user
     */
    @Query("SELECT COALESCE(SUM(es.amount), 0) FROM ExpenseSplit es " +
           "JOIN Expense e ON es.expenseId = e.id " +
           "WHERE e.paidBy = :userId AND e.tripId IN :tripIds " +
           "AND es.isSettled = false AND es.userId != :userId")
    BigDecimal sumUnsettledAmountOwedToUser(@Param("userId") UUID userId,
                                             @Param("tripIds") List<UUID> tripIds);

    /**
     * Finds trip IDs with unsettled splits involving user.
     *
     * @contract
     *   - pre: userId != null
     *   - post: Returns distinct trip IDs
     *   - calledBy: GlobalExpenseService#getUnsettledTrips
     *
     * @param userId The user ID
     * @return List of trip IDs with unsettled balances
     */
    @Query("SELECT DISTINCT e.tripId FROM Expense e " +
           "JOIN ExpenseSplit es ON es.expenseId = e.id " +
           "WHERE (es.userId = :userId OR e.paidBy = :userId) " +
           "AND es.isSettled = false AND es.userId != e.paidBy")
    List<UUID> findUnsettledTripIdsByUserId(@Param("userId") UUID userId);

    /**
     * Finds unsettled splits between two users in a trip (creditor paid, debtor owes).
     *
     * @param tripId The trip ID
     * @param creditorId The user who paid (expense.paidBy)
     * @param debtorId The user who owes (split.userId)
     * @return List of unsettled splits
     */
    @Query("SELECT es FROM ExpenseSplit es JOIN Expense e ON es.expenseId = e.id " +
           "WHERE e.tripId = :tripId AND e.paidBy = :creditorId " +
           "AND es.userId = :debtorId AND es.isSettled = false")
    List<ExpenseSplit> findUnsettledByTripIdAndUsers(@Param("tripId") UUID tripId,
                                                      @Param("creditorId") UUID creditorId,
                                                      @Param("debtorId") UUID debtorId);

    /**
     * Finds settled splits between two users in a trip (creditor paid, debtor owes).
     *
     * @param tripId The trip ID
     * @param creditorId The user who paid (expense.paidBy)
     * @param debtorId The user who owes (split.userId)
     * @return List of settled splits
     */
    @Query("SELECT es FROM ExpenseSplit es JOIN Expense e ON es.expenseId = e.id " +
           "WHERE e.tripId = :tripId AND e.paidBy = :creditorId " +
           "AND es.userId = :debtorId AND es.isSettled = true")
    List<ExpenseSplit> findSettledByTripIdAndUsers(@Param("tripId") UUID tripId,
                                                    @Param("creditorId") UUID creditorId,
                                                    @Param("debtorId") UUID debtorId);

    /**
     * Sums unsettled amount owed TO user in a specific trip.
     *
     * @contract
     *   - pre: userId != null, tripId != null
     *   - post: Returns sum of amounts, 0 if none found
     *   - calledBy: GlobalExpenseService#calculateUserBalanceInTrip
     *
     * @param userId The user ID (the payer)
     * @param tripId The trip ID
     * @return Total unsettled amount owed to user in trip
     */
    @Query("SELECT COALESCE(SUM(es.amount), 0) FROM ExpenseSplit es " +
           "JOIN Expense e ON es.expenseId = e.id " +
           "WHERE e.paidBy = :userId AND e.tripId = :tripId " +
           "AND es.isSettled = false AND es.userId != :userId")
    BigDecimal sumUnsettledAmountOwedToUserInTrip(@Param("userId") UUID userId,
                                                   @Param("tripId") UUID tripId);

    /**
     * Sums unsettled amount user owes in a specific trip.
     *
     * @contract
     *   - pre: userId != null, tripId != null
     *   - post: Returns sum of amounts, 0 if none found
     *   - calledBy: GlobalExpenseService#calculateUserBalanceInTrip
     *
     * @param userId The user ID
     * @param tripId The trip ID
     * @return Total unsettled amount user owes in trip
     */
    @Query("SELECT COALESCE(SUM(es.amount), 0) FROM ExpenseSplit es " +
           "JOIN Expense e ON es.expenseId = e.id " +
           "WHERE es.userId = :userId AND e.tripId = :tripId " +
           "AND es.isSettled = false AND es.userId != e.paidBy")
    BigDecimal sumUnsettledAmountByUserIdAndTripId(@Param("userId") UUID userId,
                                                    @Param("tripId") UUID tripId);
}
