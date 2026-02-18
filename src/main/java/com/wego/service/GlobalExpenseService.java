package com.wego.service;

import com.wego.dto.response.GlobalExpenseOverviewResponse;
import com.wego.dto.response.TripExpenseSummaryResponse;
import com.wego.entity.Trip;
import com.wego.entity.TripMember;
import com.wego.repository.ExpenseRepository;
import com.wego.repository.ExpenseSplitRepository;
import com.wego.repository.TripMemberRepository;
import com.wego.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for global expense operations across all trips.
 *
 * @contract
 *   - All methods filter by trips user is member of
 *   - calledBy: GlobalExpenseController
 *   - calls: ExpenseRepository, ExpenseSplitRepository, TripMemberRepository, TripRepository
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlobalExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final TripMemberRepository tripMemberRepository;
    private final TripRepository tripRepository;

    /**
     * Gets the global expense overview for a user.
     *
     * @contract
     *   - pre: userId != null
     *   - post: Returns aggregated expense data across all trips user is member of
     *   - calls: TripMemberRepository#findByUserId, ExpenseRepository queries
     *   - calledBy: GlobalExpenseController#showExpenseOverview
     *
     * @param userId The user ID
     * @return Global expense overview
     */
    public GlobalExpenseOverviewResponse getOverview(UUID userId) {
        // Get all trips user is a member of
        List<UUID> tripIds = tripMemberRepository.findByUserId(userId).stream()
                .map(TripMember::getTripId)
                .collect(Collectors.toList());

        if (tripIds.isEmpty()) {
            log.debug("User {} has no trips, returning empty overview", userId);
            return GlobalExpenseOverviewResponse.empty();
        }

        // Calculate aggregates with null safety
        BigDecimal totalPaid = Optional.ofNullable(
                expenseRepository.sumAmountPaidByUser(userId, tripIds))
                .orElse(BigDecimal.ZERO);
        BigDecimal totalOwed = Optional.ofNullable(
                expenseSplitRepository.sumUnsettledAmountByUserIdAndTripIds(userId, tripIds))
                .orElse(BigDecimal.ZERO);
        BigDecimal totalOwedToUser = Optional.ofNullable(
                expenseSplitRepository.sumUnsettledAmountOwedToUser(userId, tripIds))
                .orElse(BigDecimal.ZERO);
        BigDecimal netBalance = totalOwedToUser.subtract(totalOwed);

        log.debug("User {} expense overview: paid={}, owes={}, owed={}, net={}",
                userId, totalPaid, totalOwed, totalOwedToUser, netBalance);

        return GlobalExpenseOverviewResponse.builder()
                .totalPaid(totalPaid)
                .totalOwed(totalOwed)
                .totalOwedToUser(totalOwedToUser)
                .netBalance(netBalance)
                .tripCount(tripIds.size())
                .build();
    }

    /**
     * Gets trips with unsettled balances for a user.
     *
     * @contract
     *   - pre: userId != null
     *   - post: Returns list of trips with unsettled balances, sorted by absolute balance
     *   - calls: ExpenseSplitRepository#findUnsettledTripIdsByUserId
     *   - calledBy: GlobalExpenseController#showExpenseOverview
     *
     * @param userId The user ID
     * @return List of trip expense summaries with unsettled balances
     */
    public List<TripExpenseSummaryResponse> getUnsettledTrips(UUID userId) {
        List<UUID> unsettledTripIds = expenseSplitRepository
                .findUnsettledTripIdsByUserId(userId);

        if (unsettledTripIds.isEmpty()) {
            log.debug("User {} has no unsettled trips", userId);
            return List.of();
        }

        // Batch query: fetch all balances in one query instead of 2N queries
        Map<UUID, BigDecimal> balanceMap = expenseSplitRepository
                .sumBalancesByUserAndTripIds(userId, unsettledTripIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> {
                            BigDecimal owedToUser = row[1] instanceof BigDecimal bd ? bd : new BigDecimal(row[1].toString());
                            BigDecimal owedByUser = row[2] instanceof BigDecimal bd ? bd : new BigDecimal(row[2].toString());
                            return owedToUser.subtract(owedByUser);
                        }
                ));

        return tripRepository.findAllById(unsettledTripIds).stream()
                .map(trip -> TripExpenseSummaryResponse.builder()
                        .tripId(trip.getId())
                        .tripTitle(trip.getTitle())
                        .coverImageUrl(trip.getCoverImageUrl())
                        .userBalance(balanceMap.getOrDefault(trip.getId(), BigDecimal.ZERO))
                        .build())
                .sorted(Comparator.comparing(t -> t.getUserBalance().abs(),
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /**
     * Calculates user's balance in a specific trip.
     *
     * @contract
     *   - pre: userId != null, tripId != null
     *   - post: Returns positive if owed to user, negative if user owes
     *   - calls: ExpenseSplitRepository queries
     *
     * @param userId The user ID
     * @param tripId The trip ID
     * @return User balance in trip (positive = owed to user)
     */
    private BigDecimal calculateUserBalanceInTrip(UUID userId, UUID tripId) {
        BigDecimal owedToUser = Optional.ofNullable(
                expenseSplitRepository.sumUnsettledAmountOwedToUserInTrip(userId, tripId))
                .orElse(BigDecimal.ZERO);
        BigDecimal owedByUser = Optional.ofNullable(
                expenseSplitRepository.sumUnsettledAmountByUserIdAndTripId(userId, tripId))
                .orElse(BigDecimal.ZERO);
        return owedToUser.subtract(owedByUser);
    }
}
