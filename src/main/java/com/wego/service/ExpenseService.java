package com.wego.service;

import com.wego.domain.permission.PermissionChecker;
import com.wego.dto.request.CreateExpenseRequest;
import com.wego.dto.request.UpdateExpenseRequest;
import com.wego.dto.response.ExpenseResponse;
import com.wego.dto.response.ExpenseSplitResponse;
import com.wego.entity.Expense;
import com.wego.entity.ExpenseSplit;
import com.wego.entity.SplitType;
import com.wego.entity.TripMember;
import com.wego.entity.User;
import com.wego.exception.BusinessException;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.repository.ExpenseRepository;
import com.wego.repository.ExpenseSplitRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for managing expenses.
 *
 * @contract
 *   - All methods validate permissions before executing
 *   - Creates expense splits based on split type
 *   - Maintains referential integrity
 *
 * @see Expense
 * @see ExpenseSplit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserRepository userRepository;
    private final PermissionChecker permissionChecker;

    /**
     * Creates a new expense for a trip.
     *
     * @contract
     *   - pre: tripId != null, request != null, userId != null
     *   - pre: user has edit permission on trip
     *   - pre: for CUSTOM split type, split amounts must equal total
     *   - post: Expense and ExpenseSplits are persisted
     *   - calledBy: ExpenseApiController#createExpense
     *
     * @param tripId The trip ID
     * @param request The expense creation request
     * @param userId The ID of the user creating the expense
     * @return The created expense response
     * @throws ForbiddenException if user has no edit permission
     * @throws ResourceNotFoundException if trip not found
     * @throws BusinessException if split amounts are invalid
     */
    @Transactional
    public ExpenseResponse createExpense(UUID tripId, CreateExpenseRequest request, UUID userId) {
        log.debug("Creating expense for trip {} by user {}", tripId, userId);

        // Check permission
        if (!permissionChecker.canEdit(tripId, userId)) {
            throw new ForbiddenException("No permission to edit this trip");
        }

        // Verify trip exists
        var trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId.toString()));

        // Validate custom splits if applicable
        if (request.getSplitType() == SplitType.CUSTOM) {
            validateCustomSplits(request);
        }

        // Create expense entity
        Expense expense = Expense.builder()
                .tripId(tripId)
                .description(request.getDescription())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : trip.getBaseCurrency())
                .paidBy(request.getPaidBy())
                .splitType(request.getSplitType())
                .category(request.getCategory())
                .expenseDate(request.getExpenseDate())
                .activityId(request.getActivityId())
                .note(request.getNote())
                .createdBy(userId)
                .build();

        expense = expenseRepository.save(expense);

        // Create expense splits
        List<ExpenseSplit> splits = createExpenseSplits(expense, request, tripId);
        expenseSplitRepository.saveAll(splits);

        log.info("Created expense {} for trip {}", expense.getId(), tripId);

        return buildExpenseResponse(expense);
    }

    /**
     * Gets all expenses for a trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - pre: user has view permission on trip
     *   - post: returns list of expenses with splits
     *   - calledBy: ExpenseApiController#getExpensesByTrip
     *
     * @param tripId The trip ID
     * @param userId The ID of the user requesting
     * @return List of expense responses
     * @throws ForbiddenException if user has no view permission
     */
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByTrip(UUID tripId, UUID userId) {
        log.debug("Getting expenses for trip {} by user {}", tripId, userId);

        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("No permission to view this trip");
        }

        List<Expense> expenses = expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId);

        return expenses.stream()
                .map(this::buildExpenseResponse)
                .collect(Collectors.toList());
    }

    /**
     * Updates an expense.
     *
     * @contract
     *   - pre: expenseId != null, request != null, userId != null
     *   - pre: user has edit permission on the expense's trip
     *   - post: Expense is updated with non-null fields from request
     *   - calledBy: ExpenseApiController#updateExpense
     *
     * @param expenseId The expense ID
     * @param request The update request
     * @param userId The ID of the user updating
     * @return The updated expense response
     * @throws ResourceNotFoundException if expense not found
     * @throws ForbiddenException if user has no edit permission
     */
    @Transactional
    public ExpenseResponse updateExpense(UUID expenseId, UpdateExpenseRequest request, UUID userId) {
        log.debug("Updating expense {} by user {}", expenseId, userId);

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId.toString()));

        if (!permissionChecker.canEdit(expense.getTripId(), userId)) {
            throw new ForbiddenException("No permission to edit this expense");
        }

        // Update non-null fields
        if (request.getDescription() != null) {
            expense.setDescription(request.getDescription());
        }
        if (request.getAmount() != null) {
            expense.setAmount(request.getAmount());
        }
        if (request.getCurrency() != null) {
            expense.setCurrency(request.getCurrency());
        }
        if (request.getPaidBy() != null) {
            expense.setPaidBy(request.getPaidBy());
        }
        if (request.getCategory() != null) {
            expense.setCategory(request.getCategory());
        }
        if (request.getExpenseDate() != null) {
            expense.setExpenseDate(request.getExpenseDate());
        }
        if (request.getActivityId() != null) {
            expense.setActivityId(request.getActivityId());
        }
        if (request.getNote() != null) {
            expense.setNote(request.getNote());
        }

        expense.setUpdatedAt(Instant.now());

        // Handle split type change
        if (request.getSplitType() != null && request.getSplitType() != expense.getSplitType()) {
            expense.setSplitType(request.getSplitType());

            // Delete old splits and create new ones
            expenseSplitRepository.deleteByExpenseId(expenseId);

            CreateExpenseRequest createRequest = CreateExpenseRequest.builder()
                    .amount(expense.getAmount())
                    .splitType(request.getSplitType())
                    .splits(request.getSplits())
                    .build();

            List<ExpenseSplit> newSplits = createExpenseSplits(expense, createRequest, expense.getTripId());
            expenseSplitRepository.saveAll(newSplits);
        } else if (request.getAmount() != null && expense.getSplitType() == SplitType.EQUAL) {
            // Recalculate equal splits if amount changed
            expenseSplitRepository.deleteByExpenseId(expenseId);

            CreateExpenseRequest createRequest = CreateExpenseRequest.builder()
                    .amount(expense.getAmount())
                    .splitType(SplitType.EQUAL)
                    .build();

            List<ExpenseSplit> newSplits = createExpenseSplits(expense, createRequest, expense.getTripId());
            expenseSplitRepository.saveAll(newSplits);
        }

        expense = expenseRepository.save(expense);

        log.info("Updated expense {}", expenseId);

        return buildExpenseResponse(expense);
    }

    /**
     * Deletes an expense.
     *
     * @contract
     *   - pre: expenseId != null, userId != null
     *   - pre: user is the creator of the expense OR trip owner
     *   - post: Expense and all splits are deleted
     *   - calledBy: ExpenseApiController#deleteExpense
     *
     * @param expenseId The expense ID
     * @param userId The ID of the user deleting
     * @throws ResourceNotFoundException if expense not found
     * @throws ForbiddenException if user cannot delete this expense
     */
    @Transactional
    public void deleteExpense(UUID expenseId, UUID userId) {
        log.debug("Deleting expense {} by user {}", expenseId, userId);

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId.toString()));

        // Check if user can delete: must be creator or trip owner
        boolean isCreator = expense.getCreatedBy().equals(userId);
        boolean canDeleteAsOwner = permissionChecker.canDelete(expense.getTripId(), userId);

        if (!isCreator && !canDeleteAsOwner) {
            if (!permissionChecker.canEdit(expense.getTripId(), userId)) {
                throw new ForbiddenException("No permission to delete this expense");
            }
            throw new ForbiddenException("Only the creator or trip owner can delete this expense");
        }

        // Delete splits first (referential integrity)
        expenseSplitRepository.deleteByExpenseId(expenseId);
        expenseRepository.delete(expense);

        log.info("Deleted expense {}", expenseId);
    }

    /**
     * Validates custom split amounts match the total expense amount.
     */
    private void validateCustomSplits(CreateExpenseRequest request) {
        if (request.getSplits() == null || request.getSplits().isEmpty()) {
            throw new BusinessException("INVALID_SPLITS", "Splits are required for CUSTOM split type");
        }

        BigDecimal totalSplit = request.getSplits().stream()
                .map(s -> s.getAmount() != null ? s.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSplit.compareTo(request.getAmount()) != 0) {
            throw new BusinessException("INVALID_SPLITS",
                    String.format("Split amounts (%s) do not match expense amount (%s)",
                            totalSplit, request.getAmount()));
        }
    }

    /**
     * Creates expense splits based on the split type.
     */
    private List<ExpenseSplit> createExpenseSplits(Expense expense, CreateExpenseRequest request, UUID tripId) {
        List<ExpenseSplit> splits = new ArrayList<>();

        switch (expense.getSplitType()) {
            case EQUAL -> {
                List<TripMember> members = tripMemberRepository.findByTripId(tripId);
                if (members.isEmpty()) {
                    // If no members yet, create a single split for the payer
                    splits.add(ExpenseSplit.builder()
                            .expenseId(expense.getId())
                            .userId(expense.getPaidBy())
                            .amount(expense.getAmount())
                            .build());
                } else {
                    BigDecimal splitAmount = expense.getAmount()
                            .divide(BigDecimal.valueOf(members.size()), 2, RoundingMode.HALF_UP);

                    // Handle rounding remainder
                    BigDecimal remainder = expense.getAmount()
                            .subtract(splitAmount.multiply(BigDecimal.valueOf(members.size())));

                    for (int i = 0; i < members.size(); i++) {
                        TripMember member = members.get(i);
                        BigDecimal amount = splitAmount;
                        // Add remainder to first split
                        if (i == 0) {
                            amount = amount.add(remainder);
                        }
                        splits.add(ExpenseSplit.builder()
                                .expenseId(expense.getId())
                                .userId(member.getUserId())
                                .amount(amount)
                                .build());
                    }
                }
            }
            case CUSTOM -> {
                if (request.getSplits() != null) {
                    for (CreateExpenseRequest.SplitRequest splitReq : request.getSplits()) {
                        splits.add(ExpenseSplit.builder()
                                .expenseId(expense.getId())
                                .userId(splitReq.getUserId())
                                .amount(splitReq.getAmount())
                                .build());
                    }
                }
            }
            case PERCENTAGE -> {
                if (request.getSplits() != null) {
                    for (CreateExpenseRequest.SplitRequest splitReq : request.getSplits()) {
                        BigDecimal percentage = splitReq.getPercentage() != null
                                ? splitReq.getPercentage() : BigDecimal.ZERO;
                        BigDecimal amount = expense.getAmount()
                                .multiply(percentage)
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        splits.add(ExpenseSplit.builder()
                                .expenseId(expense.getId())
                                .userId(splitReq.getUserId())
                                .amount(amount)
                                .build());
                    }
                }
            }
            case SHARES -> {
                if (request.getSplits() != null) {
                    int totalShares = request.getSplits().stream()
                            .mapToInt(s -> s.getShares() != null ? s.getShares() : 1)
                            .sum();

                    for (CreateExpenseRequest.SplitRequest splitReq : request.getSplits()) {
                        int shares = splitReq.getShares() != null ? splitReq.getShares() : 1;
                        BigDecimal amount = expense.getAmount()
                                .multiply(BigDecimal.valueOf(shares))
                                .divide(BigDecimal.valueOf(totalShares), 2, RoundingMode.HALF_UP);
                        splits.add(ExpenseSplit.builder()
                                .expenseId(expense.getId())
                                .userId(splitReq.getUserId())
                                .amount(amount)
                                .build());
                    }
                }
            }
        }

        return splits;
    }

    /**
     * Builds an ExpenseResponse from an Expense entity.
     */
    private ExpenseResponse buildExpenseResponse(Expense expense) {
        ExpenseResponse response = ExpenseResponse.fromEntity(expense);

        // Get payer info
        userRepository.findById(expense.getPaidBy()).ifPresent(user -> {
            response.setPaidByName(user.getNickname());
            response.setPaidByAvatarUrl(user.getAvatarUrl());
        });

        // Get splits with user info
        List<ExpenseSplit> splits = expenseSplitRepository.findByExpenseId(expense.getId());
        Map<UUID, User> userMap = getUserMap(splits.stream()
                .map(ExpenseSplit::getUserId)
                .distinct()
                .collect(Collectors.toList()));

        List<ExpenseSplitResponse> splitResponses = splits.stream()
                .map(split -> {
                    ExpenseSplitResponse splitResponse = ExpenseSplitResponse.fromEntity(split);
                    User user = userMap.get(split.getUserId());
                    if (user != null) {
                        splitResponse.setUserNickname(user.getNickname());
                        splitResponse.setUserAvatarUrl(user.getAvatarUrl());
                    }
                    return splitResponse;
                })
                .collect(Collectors.toList());

        response.setSplits(splitResponses);

        return response;
    }

    /**
     * Gets a map of user IDs to User entities.
     */
    private Map<UUID, User> getUserMap(List<UUID> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
