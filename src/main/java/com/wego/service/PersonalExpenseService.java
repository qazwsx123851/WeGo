package com.wego.service;

import com.wego.domain.permission.PermissionChecker;
import com.wego.dto.request.CreatePersonalExpenseRequest;
import com.wego.dto.request.SetPersonalBudgetRequest;
import com.wego.dto.request.UpdatePersonalExpenseRequest;
import com.wego.dto.response.PersonalExpenseItemResponse;
import com.wego.dto.response.PersonalExpenseItemResponse.Source;
import com.wego.dto.response.PersonalExpenseSummaryResponse;
import com.wego.dto.response.PersonalExpenseSummaryResponse.BudgetStatus;
import com.wego.entity.PersonalExpense;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.exception.ValidationException;
import com.wego.repository.ExpenseSplitRepository;
import com.wego.repository.ExpenseSplitRepository.AutoSplitProjection;
import com.wego.repository.PersonalExpenseRepository;
import com.wego.repository.TripMemberRepository;
import com.wego.repository.TripRepository;
import com.wego.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for managing personal expenses within a trip.
 *
 * Personal expenses aggregate two sources:
 * - AUTO: expense splits from shared trip expenses (read-only)
 * - MANUAL: user-entered private expenses
 *
 * All amounts in responses are converted to the trip's baseCurrency.
 *
 * @contract
 *   - invariant: all public methods check isMember before processing
 *   - invariant: toBase(amount, null) returns amount unchanged
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalExpenseService {

    private final ExpenseSplitRepository expenseSplitRepository;
    private final PersonalExpenseRepository personalExpenseRepository;
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserRepository userRepository;
    private final PermissionChecker permissionChecker;

    /**
     * Returns all personal expenses for a user in a trip, merging AUTO and MANUAL sources.
     *
     * @contract
     *   - pre: userId != null, tripId != null
     *   - pre: user must be a trip member (isMember check)
     *   - post: result sorted by expenseDate ASC, nulls last
     *   - post: AUTO entries have paidByName set, id=null, tripExpenseId set
     *   - post: MANUAL entries have id set, paidByName=null, tripExpenseId=null
     *   - calls: ExpenseSplitRepository#findPersonalSplitsByUserIdAndTripId,
     *            UserRepository#findAllById, PersonalExpenseRepository#findByUserIdAndTripId
     *
     * @param userId The user ID
     * @param tripId The trip ID
     * @return Merged and sorted list of personal expense items
     * @throws ForbiddenException if user is not a trip member
     */
    public List<PersonalExpenseItemResponse> getPersonalExpenses(UUID userId, UUID tripId) {
        if (!permissionChecker.isMember(tripId, userId)) {
            throw new ForbiddenException("Not a member of this trip");
        }
        return getPersonalExpensesInternal(userId, tripId);
    }

    /**
     * Returns a summary of personal expenses including totals, daily breakdown, and budget status.
     *
     * @contract
     *   - pre: userId != null, tripId != null
     *   - pre: user must be a trip member (isMember check)
     *   - post: totalAmount is sum of all items in baseCurrency
     *   - post: dailyAmounts zero-fills all trip dates
     *   - post: budgetStatus = NONE when no budget set
     *   - post: budgetOverage is non-null only when budgetStatus = RED
     *   - calls: TripRepository#findById, PersonalExpenseRepository, ExpenseSplitRepository,
     *            TripMemberRepository#findByTripIdAndUserId
     *
     * @param userId The user ID
     * @param tripId The trip ID
     * @return Summary response with totals, daily breakdown, category breakdown, and budget info
     * @throws ForbiddenException if user is not a trip member
     * @throws ResourceNotFoundException if trip not found
     */
    public PersonalExpenseSummaryResponse getPersonalSummary(UUID userId, UUID tripId) {
        if (!permissionChecker.isMember(tripId, userId)) {
            throw new ForbiddenException("Not a member of this trip");
        }

        var trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId.toString()));

        List<PersonalExpenseItemResponse> items = getPersonalExpensesInternal(userId, tripId);

        BigDecimal totalAmount = items.stream()
                .map(PersonalExpenseItemResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dailyAverage = null;
        if (trip.getEndDate() != null && trip.getStartDate() != null) {
            long tripDays = trip.getStartDate().until(trip.getEndDate()).getDays() + 1;
            if (tripDays > 0) {
                dailyAverage = totalAmount.divide(BigDecimal.valueOf(tripDays), 2, RoundingMode.HALF_UP);
            }
        }

        Map<String, BigDecimal> categoryBreakdown = items.stream()
                .filter(i -> i.getCategory() != null)
                .collect(Collectors.groupingBy(
                        PersonalExpenseItemResponse::getCategory,
                        Collectors.reducing(BigDecimal.ZERO,
                                PersonalExpenseItemResponse::getAmount,
                                BigDecimal::add)));

        Map<LocalDate, BigDecimal> dailyAmounts = new TreeMap<>();
        LocalDate start = trip.getStartDate();
        LocalDate end = trip.getEndDate() != null ? trip.getEndDate() : start;
        start.datesUntil(end.plusDays(1)).forEach(d -> dailyAmounts.put(d, BigDecimal.ZERO));
        items.stream()
                .filter(i -> i.getExpenseDate() != null)
                .forEach(i -> dailyAmounts.merge(i.getExpenseDate(), i.getAmount(), BigDecimal::add));

        var memberOpt = tripMemberRepository.findByTripIdAndUserId(tripId, userId);
        BigDecimal budget = memberOpt.map(m -> m.getPersonalBudget()).orElse(null);

        BudgetStatus budgetStatus = calculateBudgetStatus(totalAmount, budget);
        BigDecimal budgetOverage = (budgetStatus == BudgetStatus.RED && budget != null)
                ? totalAmount.subtract(budget).setScale(2, RoundingMode.HALF_UP)
                : null;

        return PersonalExpenseSummaryResponse.builder()
                .totalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP))
                .dailyAverage(dailyAverage)
                .categoryBreakdown(categoryBreakdown)
                .dailyAmounts(dailyAmounts)
                .budget(budget)
                .budgetStatus(budgetStatus)
                .budgetOverage(budgetOverage)
                .build();
    }

    /**
     * Creates a new manual personal expense for the user in the given trip.
     *
     * @contract
     *   - pre: userId != null, tripId != null, request != null
     *   - pre: user must be a trip member (isMember check)
     *   - pre: request.expenseDate (if non-null) must be within trip date range
     *   - post: PersonalExpense persisted with source=MANUAL
     *   - post: currency defaults to trip.baseCurrency when request.currency is null/blank
     *   - calls: TripRepository#findById, PersonalExpenseRepository#save
     *
     * @param userId  The user ID
     * @param tripId  The trip ID
     * @param request The create request
     * @return The created personal expense item response
     * @throws ForbiddenException        if user is not a trip member
     * @throws ResourceNotFoundException if trip not found
     * @throws ValidationException       if expenseDate is outside trip date range
     */
    @Transactional
    public PersonalExpenseItemResponse createPersonalExpense(UUID userId, UUID tripId,
            CreatePersonalExpenseRequest request) {
        if (!permissionChecker.isMember(tripId, userId)) {
            throw new ForbiddenException("Not a member of this trip");
        }

        var trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId.toString()));

        if (request.getExpenseDate() != null) {
            validateExpenseDate(request.getExpenseDate(), trip.getStartDate(), trip.getEndDate());
        }

        String currency = (request.getCurrency() != null && !request.getCurrency().isBlank())
                ? request.getCurrency()
                : trip.getBaseCurrency();

        var expense = PersonalExpense.builder()
                .userId(userId)
                .tripId(tripId)
                .description(request.getDescription())
                .amount(request.getAmount())
                .currency(currency)
                .exchangeRate(request.getExchangeRate())
                .category(request.getCategory())
                .expenseDate(request.getExpenseDate())
                .note(request.getNote())
                .build();

        var saved = personalExpenseRepository.save(expense);
        log.debug("Created personal expense id={} userId={} tripId={}", saved.getId(), userId, tripId);

        BigDecimal baseAmount = toBase(saved.getAmount(), saved.getExchangeRate());
        return PersonalExpenseItemResponse.builder()
                .source(Source.MANUAL)
                .id(saved.getId())
                .description(saved.getDescription())
                .amount(baseAmount)
                .originalAmount(saved.getAmount())
                .originalCurrency(saved.getCurrency())
                .category(saved.getCategory())
                .expenseDate(saved.getExpenseDate())
                .build();
    }

    /**
     * Updates an existing manual personal expense (PATCH semantics; only non-null fields applied).
     *
     * @contract
     *   - pre: id != null, userId != null, request != null
     *   - pre: caller must be the owner of the expense (userId check)
     *   - pre: request.expenseDate (if non-null) must be within the associated trip's date range
     *   - post: only non-null fields from request are applied
     *   - post: updatedAt is refreshed to Instant.now()
     *   - calls: PersonalExpenseRepository#findById, TripRepository#findById,
     *            PersonalExpenseRepository#save
     *
     * @param id      The personal expense ID
     * @param userId  The requesting user ID
     * @param request The update request
     * @return The updated personal expense item response
     * @throws ResourceNotFoundException if expense not found
     * @throws ForbiddenException        if expense does not belong to userId
     * @throws ValidationException       if expenseDate is outside trip date range
     */
    @Transactional
    public PersonalExpenseItemResponse updatePersonalExpense(UUID id, UUID userId,
            UpdatePersonalExpenseRequest request) {
        var expense = personalExpenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PersonalExpense", id.toString()));

        if (!expense.getUserId().equals(userId)) {
            throw new ForbiddenException("Not your expense");
        }

        if (request.getExpenseDate() != null) {
            var trip = tripRepository.findById(expense.getTripId())
                    .orElseThrow(() -> new ResourceNotFoundException("Trip", expense.getTripId().toString()));
            validateExpenseDate(request.getExpenseDate(), trip.getStartDate(), trip.getEndDate());
        }

        if (request.getDescription() != null) expense.setDescription(request.getDescription());
        if (request.getAmount() != null) expense.setAmount(request.getAmount());
        if (request.getCurrency() != null) expense.setCurrency(request.getCurrency());
        if (request.getExchangeRate() != null) expense.setExchangeRate(request.getExchangeRate());
        if (request.getCategory() != null) expense.setCategory(request.getCategory());
        if (request.getExpenseDate() != null) expense.setExpenseDate(request.getExpenseDate());
        if (request.getNote() != null) expense.setNote(request.getNote());
        expense.setUpdatedAt(Instant.now());

        var saved = personalExpenseRepository.save(expense);
        log.debug("Updated personal expense id={} userId={}", saved.getId(), userId);

        BigDecimal baseAmount = toBase(saved.getAmount(), saved.getExchangeRate());
        return PersonalExpenseItemResponse.builder()
                .source(Source.MANUAL)
                .id(saved.getId())
                .description(saved.getDescription())
                .amount(baseAmount)
                .originalAmount(saved.getAmount())
                .originalCurrency(saved.getCurrency())
                .category(saved.getCategory())
                .expenseDate(saved.getExpenseDate())
                .build();
    }

    /**
     * Deletes a manual personal expense by ID.
     *
     * @contract
     *   - pre: id != null, userId != null
     *   - pre: caller must be the owner of the expense (userId check)
     *   - post: PersonalExpense removed from persistence
     *   - calls: PersonalExpenseRepository#findById, PersonalExpenseRepository#delete
     *
     * @param id     The personal expense ID
     * @param userId The requesting user ID
     * @throws ResourceNotFoundException if expense not found
     * @throws ForbiddenException        if expense does not belong to userId
     */
    @Transactional
    public void deletePersonalExpense(UUID id, UUID userId) {
        var expense = personalExpenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PersonalExpense", id.toString()));

        if (!expense.getUserId().equals(userId)) {
            throw new ForbiddenException("Not your expense");
        }

        personalExpenseRepository.delete(expense);
        log.debug("Deleted personal expense id={} userId={}", id, userId);
    }

    /**
     * Sets or updates the personal budget for a user in a trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null, request != null
     *   - pre: user must be a trip member (isMember check)
     *   - pre: request.budget > 0 (else ValidationException)
     *   - post: TripMember.personalBudget updated and persisted
     *   - calls: TripMemberRepository#findByTripIdAndUserId, TripMemberRepository#save
     *
     * @param tripId  The trip ID
     * @param userId  The user ID
     * @param request The budget request
     * @throws ForbiddenException        if user is not a trip member
     * @throws ValidationException       if budget <= 0
     * @throws ResourceNotFoundException if TripMember record not found
     */
    @Transactional
    public void setPersonalBudget(UUID tripId, UUID userId, SetPersonalBudgetRequest request) {
        if (!permissionChecker.isMember(tripId, userId)) {
            throw new ForbiddenException("Not a member of this trip");
        }

        if (request.getBudget().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("INVALID_BUDGET", "預算必須大於 0");
        }

        var member = tripMemberRepository.findByTripIdAndUserId(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("TripMember",
                        tripId + "/" + userId));

        member.setPersonalBudget(request.getBudget());
        tripMemberRepository.save(member);
        log.debug("Set personal budget tripId={} userId={} budget={}", tripId, userId, request.getBudget());
    }

    // ========== Private Helpers ==========

    /**
     * Internal merge logic shared by getPersonalExpenses and getPersonalSummary.
     * Avoids exposing a second public method purely for DRY purposes.
     */
    private List<PersonalExpenseItemResponse> getPersonalExpensesInternal(UUID userId, UUID tripId) {
        List<AutoSplitProjection> autoSplits = expenseSplitRepository
                .findPersonalSplitsByUserIdAndTripId(userId, tripId);

        Set<UUID> payerIds = autoSplits.stream()
                .map(AutoSplitProjection::getPaidBy)
                .collect(Collectors.toSet());
        Map<UUID, User> payerMap = userRepository.findAllById(payerIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<PersonalExpense> manualItems = personalExpenseRepository
                .findByUserIdAndTripId(userId, tripId);

        List<PersonalExpenseItemResponse> result = new ArrayList<>();

        for (AutoSplitProjection split : autoSplits) {
            String payerName = payerMap.containsKey(split.getPaidBy())
                    ? payerMap.get(split.getPaidBy()).getNickname()
                    : "Unknown";
            BigDecimal baseAmount = toBase(split.getAmount(), split.getExchangeRate());
            result.add(PersonalExpenseItemResponse.builder()
                    .source(Source.AUTO)
                    .id(null)
                    .description(split.getDescription())
                    .amount(baseAmount)
                    .originalAmount(split.getAmount())
                    .originalCurrency(split.getCurrency())
                    .category(split.getCategory())
                    .expenseDate(split.getExpenseDate())
                    .paidByName(payerName)
                    .tripExpenseId(split.getTripExpenseId())
                    .build());
        }

        for (PersonalExpense manual : manualItems) {
            BigDecimal baseAmount = toBase(manual.getAmount(), manual.getExchangeRate());
            result.add(PersonalExpenseItemResponse.builder()
                    .source(Source.MANUAL)
                    .id(manual.getId())
                    .description(manual.getDescription())
                    .amount(baseAmount)
                    .originalAmount(manual.getAmount())
                    .originalCurrency(manual.getCurrency())
                    .category(manual.getCategory())
                    .expenseDate(manual.getExpenseDate())
                    .paidByName(null)
                    .tripExpenseId(null)
                    .build());
        }

        result.sort(Comparator.comparing(
                PersonalExpenseItemResponse::getExpenseDate,
                Comparator.nullsLast(Comparator.naturalOrder())));

        return result;
    }

    /**
     * Converts an amount to baseCurrency using the given exchange rate.
     * A null or zero exchangeRate means the amount is already in baseCurrency.
     *
     * @param amount       The original amount (never null)
     * @param exchangeRate The exchange rate to baseCurrency (nullable)
     * @return Amount in baseCurrency, rounded HALF_UP to 2 decimal places
     */
    private BigDecimal toBase(BigDecimal amount, BigDecimal exchangeRate) {
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) == 0) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Determines budget status based on spend ratio.
     *
     * @param total  Total amount spent
     * @param budget Personal budget (nullable)
     * @return GREEN (<80%), YELLOW (80–99%), RED (>=100%), NONE (no budget)
     */
    private BudgetStatus calculateBudgetStatus(BigDecimal total, BigDecimal budget) {
        if (budget == null || budget.compareTo(BigDecimal.ZERO) == 0) {
            return BudgetStatus.NONE;
        }
        BigDecimal ratio = total.divide(budget, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("1.00")) >= 0) return BudgetStatus.RED;
        if (ratio.compareTo(new BigDecimal("0.80")) >= 0) return BudgetStatus.YELLOW;
        return BudgetStatus.GREEN;
    }

    /**
     * Validates that an expense date falls within the trip's date range.
     *
     * @param expenseDate The expense date to validate
     * @param startDate   Trip start date (nullable — skip check if null)
     * @param endDate     Trip end date (nullable — skip check if null)
     * @throws ValidationException if expenseDate is before startDate or after endDate
     */
    private void validateExpenseDate(LocalDate expenseDate, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && expenseDate.isBefore(startDate)) {
            throw new ValidationException("INVALID_DATE", "日期超出行程期間");
        }
        if (endDate != null && expenseDate.isAfter(endDate)) {
            throw new ValidationException("INVALID_DATE", "日期超出行程期間");
        }
    }
}
