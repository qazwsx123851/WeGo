package com.wego.service;

import com.wego.domain.permission.PermissionChecker;
import com.wego.dto.request.CreateExpenseRequest;
import com.wego.dto.request.UpdateExpenseRequest;
import com.wego.dto.response.ExpenseResponse;
import com.wego.dto.response.ExpenseSplitResponse;
import com.wego.entity.Expense;
import com.wego.entity.ExpenseSplit;
import com.wego.entity.GhostMember;
import com.wego.entity.SplitType;
import com.wego.entity.TripMember;
import com.wego.exception.BusinessException;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.exception.ValidationException;
import com.wego.repository.ExpenseRepository;
import com.wego.repository.ExpenseSplitRepository;
import com.wego.repository.GhostMemberRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
    private final GhostMemberRepository ghostMemberRepository;
    private final ParticipantResolver participantResolver;
    private final PermissionChecker permissionChecker;
    private final SettlementService settlementService;

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

        // Validate paidBy is a trip member or active ghost member
        Set<UUID> validParticipantIds = getAllValidParticipantIds(tripId);
        if (!validParticipantIds.contains(request.getPaidBy())) {
            throw new ValidationException("INVALID_PAYER", "付款人不是行程成員");
        }

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

        settlementService.evictExpenseCaches(tripId);

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

        return buildExpenseResponsesBatch(expenses, tripId);
    }

    /**
     * Gets a single expense by ID with permission check.
     *
     * @contract
     *   - pre: expenseId != null, userId != null
     *   - pre: user has view permission on the expense's trip
     *   - post: Returns expense response
     *   - calledBy: ExpenseWebController#showExpenseDetail, ExpenseWebController#showEditForm
     *
     * @param expenseId The expense ID
     * @param userId The ID of the requesting user
     * @return The expense response
     * @throws ResourceNotFoundException if expense not found
     * @throws ForbiddenException if user has no view permission
     */
    @Transactional(readOnly = true)
    public ExpenseResponse getExpense(UUID expenseId, UUID userId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId.toString()));

        if (!permissionChecker.canView(expense.getTripId(), userId)) {
            throw new ForbiddenException("No permission to view this expense");
        }

        return buildExpenseResponse(expense);
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
            // Validate paidBy is a trip member or active ghost member (CRITICAL: prevent cross-trip ghost injection)
            Set<UUID> validParticipantIds = getAllValidParticipantIds(expense.getTripId());
            if (!validParticipantIds.contains(request.getPaidBy())) {
                throw new ValidationException("INVALID_PAYER", "付款人不是行程成員");
            }
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
            // Recalculate equal splits if amount changed, preserving existing split members
            List<ExpenseSplit> existingSplits = expenseSplitRepository.findByExpenseId(expenseId);
            List<CreateExpenseRequest.SplitRequest> existingParticipants = existingSplits.stream()
                    .map(s -> CreateExpenseRequest.SplitRequest.builder()
                            .userId(s.getUserId())
                            .build())
                    .toList();

            expenseSplitRepository.deleteByExpenseId(expenseId);

            CreateExpenseRequest createRequest = CreateExpenseRequest.builder()
                    .amount(expense.getAmount())
                    .splitType(SplitType.EQUAL)
                    .splits(existingParticipants)
                    .build();

            List<ExpenseSplit> newSplits = createExpenseSplits(expense, createRequest, expense.getTripId());
            expenseSplitRepository.saveAll(newSplits);
        } else if (request.getAmount() != null && request.getSplits() != null
                   && (expense.getSplitType() == SplitType.CUSTOM
                       || expense.getSplitType() == SplitType.PERCENTAGE
                       || expense.getSplitType() == SplitType.SHARES)) {
            // Recalculate non-EQUAL splits when amount changes and new splits provided
            expenseSplitRepository.deleteByExpenseId(expenseId);

            CreateExpenseRequest createRequest = CreateExpenseRequest.builder()
                    .amount(expense.getAmount())
                    .splitType(expense.getSplitType())
                    .splits(request.getSplits())
                    .build();

            List<ExpenseSplit> newSplits = createExpenseSplits(expense, createRequest, expense.getTripId());
            expenseSplitRepository.saveAll(newSplits);
        }

        expense = expenseRepository.save(expense);

        log.info("Updated expense {}", expenseId);

        settlementService.evictExpenseCaches(expense.getTripId());

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

        UUID tripId = expense.getTripId();

        // Delete splits first (referential integrity)
        expenseSplitRepository.deleteByExpenseId(expenseId);
        expenseRepository.delete(expense);

        settlementService.evictExpenseCaches(tripId);

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
     *
     * @contract
     *   - pre: expense != null, tripId != null
     *   - pre: For CUSTOM: split amounts sum to expense total
     *   - pre: For PERCENTAGE: percentages sum to 100%
     *   - pre: All split userIds are valid trip members
     *   - post: Returns list of ExpenseSplit entities
     *   - throws: ValidationException if validation fails
     */
    private List<ExpenseSplit> createExpenseSplits(Expense expense, CreateExpenseRequest request, UUID tripId) {
        List<ExpenseSplit> splits = new ArrayList<>();

        // Validate splits for CUSTOM and PERCENTAGE types
        if (request.getSplits() != null && !request.getSplits().isEmpty()) {
            validateSplitUserIds(request.getSplits(), tripId);

            if (expense.getSplitType() == SplitType.CUSTOM) {
                validateCustomSplitAmounts(request.getSplits(), expense.getAmount());
            } else if (expense.getSplitType() == SplitType.PERCENTAGE) {
                validatePercentageSplits(request.getSplits());
            }
        }

        switch (expense.getSplitType()) {
            case EQUAL -> {
                // Use selected participants if provided, otherwise fall back to all trip members
                List<UUID> participantUserIds;
                if (request.getSplits() != null && !request.getSplits().isEmpty()) {
                    participantUserIds = request.getSplits().stream()
                            .map(CreateExpenseRequest.SplitRequest::getUserId)
                            .toList();
                } else {
                    // Include both real members and active ghost members
                    List<UUID> memberIds = tripMemberRepository.findByTripId(tripId).stream()
                            .map(TripMember::getUserId)
                            .toList();
                    List<UUID> ghostIds = ghostMemberRepository.findByTripIdAndMergedToUserIdIsNull(tripId).stream()
                            .map(GhostMember::getId)
                            .toList();
                    participantUserIds = new ArrayList<>(memberIds);
                    participantUserIds.addAll(ghostIds);
                }

                if (participantUserIds.isEmpty()) {
                    // If no members yet, create a single split for the payer
                    splits.add(ExpenseSplit.builder()
                            .expenseId(expense.getId())
                            .userId(expense.getPaidBy())
                            .amount(expense.getAmount())
                            .build());
                } else {
                    BigDecimal splitAmount = expense.getAmount()
                            .divide(BigDecimal.valueOf(participantUserIds.size()), 2, RoundingMode.HALF_UP);

                    // Handle rounding remainder
                    BigDecimal remainder = expense.getAmount()
                            .subtract(splitAmount.multiply(BigDecimal.valueOf(participantUserIds.size())));

                    for (int i = 0; i < participantUserIds.size(); i++) {
                        BigDecimal amount = splitAmount;
                        // Add remainder to first split
                        if (i == 0) {
                            amount = amount.add(remainder);
                        }
                        splits.add(ExpenseSplit.builder()
                                .expenseId(expense.getId())
                                .userId(participantUserIds.get(i))
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
     * Validates that all split userIds are valid trip members.
     *
     * @contract
     *   - pre: splitRequests != null, tripId != null
     *   - throws: ValidationException if any userId is not a trip member
     */
    private void validateSplitUserIds(List<CreateExpenseRequest.SplitRequest> splitRequests, UUID tripId) {
        Set<UUID> validIds = getAllValidParticipantIds(tripId);

        for (CreateExpenseRequest.SplitRequest split : splitRequests) {
            if (split.getUserId() != null && !validIds.contains(split.getUserId())) {
                throw new ValidationException("INVALID_SPLIT_USER",
                        "分帳用戶不是行程成員: " + split.getUserId());
            }
        }
    }

    /**
     * Gets all valid participant IDs for a trip (real members + active ghost members).
     * Used for payer and split member validation.
     *
     * @contract
     *   - post: Returns Set containing User.id from TripMembers AND GhostMember.id (active only)
     */
    private Set<UUID> getAllValidParticipantIds(UUID tripId) {
        Set<UUID> validIds = tripMemberRepository.findByTripId(tripId).stream()
                .map(TripMember::getUserId)
                .collect(Collectors.toSet());
        ghostMemberRepository.findByTripIdAndMergedToUserIdIsNull(tripId)
                .forEach(g -> validIds.add(g.getId()));
        return validIds;
    }

    /**
     * Validates that CUSTOM split amounts sum to the expense total.
     *
     * @contract
     *   - pre: splitRequests != null, expenseAmount != null
     *   - throws: ValidationException if amounts don't sum to total
     */
    private void validateCustomSplitAmounts(List<CreateExpenseRequest.SplitRequest> splitRequests, BigDecimal expenseAmount) {
        BigDecimal totalSplitAmount = splitRequests.stream()
                .map(s -> s.getAmount() != null ? s.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Allow small rounding difference (up to 0.01)
        if (totalSplitAmount.subtract(expenseAmount).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new ValidationException("SPLIT_AMOUNT_MISMATCH",
                    String.format("分帳金額總和 (%s) 與支出金額 (%s) 不符",
                            totalSplitAmount.setScale(2, RoundingMode.HALF_UP),
                            expenseAmount.setScale(2, RoundingMode.HALF_UP)));
        }
    }

    /**
     * Validates that PERCENTAGE splits sum to 100%.
     *
     * @contract
     *   - pre: splitRequests != null
     *   - throws: ValidationException if percentages don't sum to 100
     */
    private void validatePercentageSplits(List<CreateExpenseRequest.SplitRequest> splitRequests) {
        BigDecimal totalPercentage = splitRequests.stream()
                .map(s -> s.getPercentage() != null ? s.getPercentage() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Allow small rounding difference (up to 0.01)
        if (totalPercentage.subtract(new BigDecimal("100")).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new ValidationException("PERCENTAGE_SUM_INVALID",
                    String.format("分帳百分比總和 (%s%%) 不等於 100%%",
                            totalPercentage.setScale(2, RoundingMode.HALF_UP)));
        }
    }

    /**
     * Builds an ExpenseResponse from an Expense entity.
     * Uses batch user loading to minimize DB queries (2 queries instead of 3).
     */
    private ExpenseResponse buildExpenseResponse(Expense expense) {
        ExpenseResponse response = ExpenseResponse.fromEntity(expense);

        // 1 query: get splits
        List<ExpenseSplit> splits = expenseSplitRepository.findByExpenseId(expense.getId());

        // Collect ALL participant IDs (payer + split participants) for batch resolve
        Set<UUID> allParticipantIds = new HashSet<>();
        allParticipantIds.add(expense.getPaidBy());
        splits.forEach(s -> allParticipantIds.add(s.getUserId()));

        // Max 2 queries: batch resolve from User + GhostMember tables
        Map<UUID, ParticipantResolver.ParticipantInfo> participantMap =
                participantResolver.resolveAll(allParticipantIds);

        // Set payer info
        ParticipantResolver.ParticipantInfo payer = participantMap.get(expense.getPaidBy());
        if (payer != null) {
            response.setPaidByName(payer.nickname());
            response.setPaidByAvatarUrl(payer.avatarUrl());
            response.setPaidByIsGhost(payer.isGhost());
        }

        List<ExpenseSplitResponse> splitResponses = splits.stream()
                .map(split -> {
                    ExpenseSplitResponse splitResponse = ExpenseSplitResponse.fromEntity(split);
                    ParticipantResolver.ParticipantInfo info = participantMap.get(split.getUserId());
                    if (info != null) {
                        splitResponse.setUserNickname(info.nickname());
                        splitResponse.setUserAvatarUrl(info.avatarUrl());
                        splitResponse.setGhost(info.isGhost());
                    }
                    return splitResponse;
                })
                .collect(Collectors.toList());

        response.setSplits(splitResponses);

        return response;
    }

    /**
     * Builds ExpenseResponses in batch to avoid N+1 queries.
     * Uses a single query for all splits and a single query for all users.
     */
    private List<ExpenseResponse> buildExpenseResponsesBatch(List<Expense> expenses, UUID tripId) {
        if (expenses.isEmpty()) {
            return List.of();
        }

        // 1 query: batch load all splits for the trip
        List<ExpenseSplit> allSplits = expenseSplitRepository.findByTripId(tripId);
        Map<UUID, List<ExpenseSplit>> splitsByExpenseId = allSplits.stream()
                .collect(Collectors.groupingBy(ExpenseSplit::getExpenseId));

        // Collect all participant IDs (payers + split participants)
        Set<UUID> allParticipantIds = new HashSet<>();
        expenses.forEach(e -> allParticipantIds.add(e.getPaidBy()));
        allSplits.forEach(s -> allParticipantIds.add(s.getUserId()));

        // Max 2 queries: batch resolve from User + GhostMember tables
        Map<UUID, ParticipantResolver.ParticipantInfo> participantMap =
                participantResolver.resolveAll(allParticipantIds);

        // Map in memory — no additional DB queries
        return expenses.stream()
                .map(expense -> {
                    ExpenseResponse response = ExpenseResponse.fromEntity(expense);

                    ParticipantResolver.ParticipantInfo payer = participantMap.get(expense.getPaidBy());
                    if (payer != null) {
                        response.setPaidByName(payer.nickname());
                        response.setPaidByAvatarUrl(payer.avatarUrl());
                        response.setPaidByIsGhost(payer.isGhost());
                    }

                    List<ExpenseSplit> splits = splitsByExpenseId.getOrDefault(expense.getId(), List.of());
                    List<ExpenseSplitResponse> splitResponses = splits.stream()
                            .map(split -> {
                                ExpenseSplitResponse splitResponse = ExpenseSplitResponse.fromEntity(split);
                                ParticipantResolver.ParticipantInfo info = participantMap.get(split.getUserId());
                                if (info != null) {
                                    splitResponse.setUserNickname(info.nickname());
                                    splitResponse.setUserAvatarUrl(info.avatarUrl());
                                    splitResponse.setGhost(info.isGhost());
                                }
                                return splitResponse;
                            })
                            .collect(Collectors.toList());

                    response.setSplits(splitResponses);
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets expense count for a trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - pre: user has view permission on trip
     *   - post: returns count of expenses in trip
     *   - calledBy: TripController#showTripDetail
     *
     * @param tripId The trip ID
     * @param userId The ID of the user requesting
     * @return Number of expenses in trip
     * @throws ForbiddenException if user has no view permission
     */
    @Transactional(readOnly = true)
    public long getExpenseCount(UUID tripId, UUID userId) {
        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("您沒有權限查看此行程");
        }
        return expenseRepository.countByTripId(tripId);
    }

    /**
     * Gets total expense amount for a trip in a specific currency.
     *
     * @contract
     *   - pre: tripId != null, currency != null, userId != null
     *   - pre: user has view permission on trip
     *   - post: returns sum of expenses in specified currency
     *   - calledBy: TripController#showTripDetail
     *
     * @param tripId The trip ID
     * @param currency The currency code (e.g., "TWD")
     * @param userId The ID of the user requesting
     * @return Total expense amount (0 if none)
     * @throws ForbiddenException if user has no view permission
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalExpense(UUID tripId, String currency, UUID userId) {
        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("您沒有權限查看此行程");
        }
        return expenseRepository.sumAmountByTripIdAndCurrency(tripId, currency);
    }

    /**
     * Calculates user's balance in a specific trip.
     *
     * @contract
     *   - pre: userId != null, tripId != null
     *   - pre: user has view permission on trip
     *   - post: Returns positive if owed to user, negative if user owes
     *   - calls: ExpenseSplitRepository queries
     *   - calledBy: TripController#showExpenses
     *
     * @param userId The user to calculate balance for
     * @param tripId The trip to calculate balance in
     * @return Balance amount (positive = owed to user, negative = user owes)
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateUserBalanceInTrip(UUID userId, UUID tripId) {
        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("您沒有權限查看此行程");
        }
        BigDecimal owedToUser = java.util.Optional.ofNullable(
                expenseSplitRepository.sumUnsettledAmountOwedToUserInTrip(userId, tripId))
                .orElse(BigDecimal.ZERO);
        BigDecimal owedByUser = java.util.Optional.ofNullable(
                expenseSplitRepository.sumUnsettledAmountByUserIdAndTripId(userId, tripId))
                .orElse(BigDecimal.ZERO);
        return owedToUser.subtract(owedByUser);
    }

    /**
     * Gets expenses linked to a specific activity.
     *
     * @contract
     *   - pre: tripId != null, activityId != null, userId != null
     *   - pre: user has view permission on trip
     *   - post: returns list of expenses for the activity
     *   - calls: ExpenseRepository#findByTripIdAndActivityId
     *   - calledBy: TripController#showActivityDetail
     *
     * @param tripId The trip ID
     * @param activityId The activity ID
     * @param userId The user requesting
     * @return List of expense responses linked to the activity
     */
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByActivity(UUID tripId, UUID activityId, UUID userId) {
        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("您沒有權限查看此行程");
        }
        List<Expense> expenses = expenseRepository.findByTripIdAndActivityIdOrderByCreatedAtDesc(tripId, activityId);
        return buildExpenseResponsesBatch(expenses, tripId);
    }
}
