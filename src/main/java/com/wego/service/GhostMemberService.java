package com.wego.service;

import com.wego.domain.TripConstants;
import com.wego.domain.permission.PermissionChecker;
import com.wego.entity.ExpenseSplit;
import com.wego.entity.GhostMember;
import com.wego.entity.Role;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing ghost members (non-registered trip participants).
 *
 * Ghost members participate in expense splitting identically to real users.
 * Their UUID is used directly in Expense.paidBy and ExpenseSplit.userId.
 *
 * @contract
 *   - invariant: Only OWNER can create, remove, or merge ghost members
 *   - invariant: Ghost members count toward MAX_MEMBERS_PER_TRIP
 *   - invariant: Ghost members with expenses cannot be removed (must merge or delete expenses first)
 *   - calledBy: GhostMemberApiController
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GhostMemberService {

    private final GhostMemberRepository ghostMemberRepository;
    private final TripMemberRepository tripMemberRepository;
    private final TripRepository tripRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final PermissionChecker permissionChecker;
    private final CacheManager cacheManager;

    /**
     * Creates a new ghost member in a trip.
     *
     * @contract
     *   - pre: ownerId must have OWNER role in the trip
     *   - pre: total members (real + ghost) < MAX_MEMBERS_PER_TRIP
     *   - pre: displayName must be unique among active ghosts in the trip
     *   - post: GhostMember persisted with sanitized fields
     *   - throws: ForbiddenException if not owner
     *   - throws: ValidationException if member limit exceeded or duplicate name
     *
     * @param tripId The trip ID
     * @param displayName The display name for the ghost
     * @param note Optional note
     * @param ownerId The owner's user ID
     * @return The created GhostMember
     */
    @Transactional
    public GhostMember createGhostMember(UUID tripId, String displayName, String note, UUID ownerId) {
        if (!permissionChecker.canManageMembers(tripId, ownerId)) {
            throw new ForbiddenException("只有行程擁有者可以新增虛擬成員");
        }

        tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId.toString()));

        String sanitizedName = sanitizeField(displayName, 50);
        String sanitizedNote = note != null ? sanitizeField(note, 200) : null;

        if (sanitizedName.isEmpty()) {
            throw new ValidationException("INVALID_NAME", "名稱不可為空");
        }

        long realCount = tripMemberRepository.countByTripId(tripId);
        long ghostCount = ghostMemberRepository.countByTripIdAndMergedToUserIdIsNull(tripId);
        if (realCount + ghostCount >= TripConstants.MAX_MEMBERS_PER_TRIP) {
            throw new ValidationException("MEMBER_LIMIT_EXCEEDED",
                    "行程成員已達上限（" + TripConstants.MAX_MEMBERS_PER_TRIP + " 人）");
        }

        if (ghostMemberRepository.existsByTripIdAndDisplayNameAndMergedToUserIdIsNull(tripId, sanitizedName)) {
            throw new ValidationException("DUPLICATE_GHOST_NAME", "此名稱已存在");
        }

        GhostMember ghost = GhostMember.builder()
                .tripId(tripId)
                .displayName(sanitizedName)
                .note(sanitizedNote)
                .createdBy(ownerId)
                .build();

        GhostMember saved = ghostMemberRepository.save(ghost);
        log.info("Created ghost member {} '{}' in trip {}", saved.getId(), sanitizedName, tripId);
        return saved;
    }

    /**
     * Lists all active (non-merged) ghost members for a trip.
     *
     * @param tripId The trip ID
     * @return List of active ghost members
     */
    @Transactional(readOnly = true)
    public List<GhostMember> getActiveGhosts(UUID tripId) {
        return ghostMemberRepository.findByTripIdAndMergedToUserIdIsNull(tripId);
    }

    /**
     * Removes a ghost member from a trip.
     * Blocked if the ghost has any expenses or splits.
     *
     * @contract
     *   - pre: ownerId must have OWNER role
     *   - pre: ghost must have no expenses (paidBy) or splits (userId) in the trip
     *   - post: GhostMember deleted from database
     *   - throws: BusinessException if ghost has existing expenses/splits
     *
     * @param tripId The trip ID
     * @param ghostId The ghost member ID
     * @param ownerId The owner's user ID
     */
    @Transactional
    public void removeGhostMember(UUID tripId, UUID ghostId, UUID ownerId) {
        if (!permissionChecker.canManageMembers(tripId, ownerId)) {
            throw new ForbiddenException("只有行程擁有者可以移除虛擬成員");
        }

        GhostMember ghost = ghostMemberRepository.findByIdAndTripId(ghostId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException("GhostMember", ghostId.toString()));

        if (ghost.isMerged()) {
            throw new BusinessException("GHOST_ALREADY_MERGED", "此虛擬成員已被合併");
        }

        boolean hasExpenses = expenseRepository.existsByTripIdAndPaidBy(tripId, ghostId);
        boolean hasSplits = expenseSplitRepository.existsByUserIdAndTripId(ghostId, tripId);

        if (hasExpenses || hasSplits) {
            throw new BusinessException("GHOST_HAS_EXPENSES",
                    "此虛擬成員已參與分帳，請先移除相關支出或將其合併到真實成員");
        }

        ghostMemberRepository.delete(ghost);
        log.info("Removed ghost member {} '{}' from trip {}", ghostId, ghost.getDisplayName(), tripId);
    }

    /**
     * Merges a ghost member into a real user, transferring all expense data.
     *
     * This operation:
     * 1. Updates all Expense.paidBy from ghostId to targetUserId
     * 2. Updates all ExpenseSplit.userId from ghostId to targetUserId
     * 3. Consolidates duplicate splits (same expense + same user) by summing amounts
     * 4. Marks the ghost as merged (soft-delete)
     *
     * @contract
     *   - pre: ownerId must have OWNER role
     *   - pre: ghost must be active (not already merged)
     *   - pre: targetUserId must be a real TripMember
     *   - post: All expense FKs transferred, duplicate splits consolidated
     *   - post: Ghost soft-deleted with mergedToUserId set
     *   - post: Consolidated splits marked as unsettled (conservative)
     *   - throws: BusinessException if ghost already merged
     *   - throws: ValidationException if target is not a trip member
     *
     * @param tripId The trip ID
     * @param ghostId The ghost member ID
     * @param targetUserId The real user ID to merge into
     * @param ownerId The owner's user ID
     */
    @Transactional
    public void mergeGhostToUser(UUID tripId, UUID ghostId, UUID targetUserId, UUID ownerId) {
        if (!permissionChecker.canManageMembers(tripId, ownerId)) {
            throw new ForbiddenException("只有行程擁有者可以合併成員");
        }

        // Pessimistic lock to prevent concurrent operations on this ghost
        GhostMember ghost = ghostMemberRepository.findByIdAndTripIdForUpdate(ghostId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException("GhostMember", ghostId.toString()));

        if (ghost.isMerged()) {
            throw new BusinessException("GHOST_ALREADY_MERGED", "此虛擬成員已被合併");
        }

        TripMember targetMember = tripMemberRepository.findByTripIdAndUserId(tripId, targetUserId)
                .orElseThrow(() -> new ValidationException("TARGET_NOT_MEMBER", "目標用戶不是行程成員"));
        if (targetMember.getRole() == Role.OWNER) {
            throw new ValidationException("CANNOT_MERGE_TO_OWNER", "不能將虛擬成員合併給行程擁有者");
        }

        // 1. Bulk update Expense.paidBy
        int updatedExpenses = expenseRepository.updatePaidByForTrip(tripId, ghostId, targetUserId);

        // 2. Bulk update ExpenseSplit.userId
        int updatedSplits = expenseSplitRepository.updateUserIdForTrip(tripId, ghostId, targetUserId);

        // 3. Consolidate duplicate splits (same expense + same user after merge)
        consolidateDuplicateSplits(targetUserId, tripId);

        // 4. Soft-delete ghost
        ghost.setMergedToUserId(targetUserId);
        ghost.setMergedAt(Instant.now());
        ghostMemberRepository.save(ghost);

        // 5. Evict settlement cache
        evictSettlementCache(tripId);

        log.info("Merged ghost {} '{}' to user {} in trip {}. Updated {} expenses, {} splits.",
                ghostId, ghost.getDisplayName(), targetUserId, tripId, updatedExpenses, updatedSplits);
    }

    /**
     * Consolidates duplicate splits that may result from a merge.
     * When a ghost and a real user both had splits on the same expense,
     * after the userId update there will be two splits with the same (expenseId, userId).
     * This method sums their amounts into one split and deletes the duplicate.
     * Consolidated splits are marked as unsettled (conservative strategy).
     */
    private void consolidateDuplicateSplits(UUID userId, UUID tripId) {
        List<ExpenseSplit> duplicates = expenseSplitRepository.findDuplicateSplitsByUserIdAndTripId(userId, tripId);

        if (duplicates.isEmpty()) {
            return;
        }

        // Group by expenseId
        Map<UUID, List<ExpenseSplit>> byExpense = new HashMap<>();
        for (ExpenseSplit split : duplicates) {
            byExpense.computeIfAbsent(split.getExpenseId(), k -> new ArrayList<>()).add(split);
        }

        List<ExpenseSplit> toDelete = new ArrayList<>();

        for (Map.Entry<UUID, List<ExpenseSplit>> entry : byExpense.entrySet()) {
            List<ExpenseSplit> splits = entry.getValue();
            if (splits.size() < 2) {
                continue;
            }

            // Keep the first split, sum amounts from others
            ExpenseSplit keeper = splits.get(0);
            BigDecimal totalAmount = keeper.getAmount();

            for (int i = 1; i < splits.size(); i++) {
                totalAmount = totalAmount.add(splits.get(i).getAmount());
                toDelete.add(splits.get(i));
            }

            keeper.setAmount(totalAmount);
            // Conservative: mark as unsettled after merge
            keeper.markAsUnsettled();
            expenseSplitRepository.save(keeper);
        }

        if (!toDelete.isEmpty()) {
            expenseSplitRepository.deleteAll(toDelete);
            log.info("Consolidated {} duplicate splits for user {} in trip {}",
                    toDelete.size(), userId, tripId);
        }
    }

    private void evictSettlementCache(UUID tripId) {
        var cache = cacheManager.getCache("settlement");
        if (cache != null) {
            cache.evict(tripId);
        }
    }

    /**
     * Sanitizes user input by removing control characters and collapsing whitespace.
     * Same pattern as ChatService.sanitizeField().
     */
    static String sanitizeField(String input, int maxLength) {
        if (input == null) {
            return "";
        }
        String sanitized = input.replaceAll("[\\r\\n\\t]", " ");
        sanitized = sanitized.replaceAll("[\\p{Cf}]", "");
        sanitized = sanitized.replaceAll(" {2,}", " ").trim();
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength);
        }
        return sanitized;
    }
}
