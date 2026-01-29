package com.wego.service;

import com.wego.domain.permission.PermissionChecker;
import com.wego.dto.request.CreateTripRequest;
import com.wego.dto.request.UpdateTripRequest;
import com.wego.dto.response.TripResponse;
import com.wego.entity.Role;
import com.wego.entity.Trip;
import com.wego.entity.TripMember;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.exception.ValidationException;
import com.wego.repository.TripMemberRepository;
import com.wego.repository.TripRepository;
import com.wego.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for trip-related business logic.
 *
 * @contract
 *   - All methods that modify data are transactional
 *   - Permission checks are performed before modifications
 *   - Cascading deletes handled for trip-related entities
 *
 * @see Trip
 * @see TripMember
 * @see PermissionChecker
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private static final int MAX_MEMBERS_PER_TRIP = 10;

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserRepository userRepository;
    private final PermissionChecker permissionChecker;

    /**
     * Creates a new trip and sets the creator as owner.
     *
     * @contract
     *   - pre: request != null, user != null
     *   - pre: request.endDate >= request.startDate
     *   - post: Trip is persisted, TripMember with OWNER role is created
     *   - calledBy: TripApiController#createTrip
     *
     * @param request The trip creation request
     * @param user The creating user (will become owner)
     * @return The created trip response
     * @throws ValidationException if validation fails
     */
    @Transactional
    public TripResponse createTrip(CreateTripRequest request, User user) {
        validateTripDates(request.getStartDate(), request.getEndDate());

        Trip trip = Trip.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .baseCurrency(request.getBaseCurrency() != null ? request.getBaseCurrency() : "TWD")
                .coverImageUrl(request.getCoverImageUrl())
                .ownerId(user.getId())
                .build();

        trip = tripRepository.save(trip);
        log.info("Created trip: {} by user: {}", trip.getId(), user.getId());

        // Create owner membership
        TripMember ownerMember = TripMember.builder()
                .tripId(trip.getId())
                .userId(user.getId())
                .role(Role.OWNER)
                .build();

        tripMemberRepository.save(ownerMember);
        log.debug("Created owner membership for trip: {}", trip.getId());

        TripResponse response = TripResponse.fromEntity(trip);
        response.setMemberCount(1);
        response.setCurrentUserRole(Role.OWNER);

        return response;
    }

    /**
     * Gets a trip by ID with permission check.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - post: Returns trip if user is a member
     *   - calledBy: TripApiController#getTrip
     *
     * @param tripId The trip ID
     * @param userId The requesting user ID
     * @return The trip response
     * @throws ResourceNotFoundException if trip not found
     * @throws ForbiddenException if user is not a member
     */
    @Transactional(readOnly = true)
    public TripResponse getTrip(UUID tripId, UUID userId) {
        Trip trip = findTripById(tripId);

        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("您沒有權限查看此行程");
        }

        TripResponse response = TripResponse.fromEntity(trip);
        response.setMemberCount((int) tripMemberRepository.countByTripId(tripId));
        response.setCurrentUserRole(permissionChecker.getRole(tripId, userId).orElse(null));
        response.setMembers(getMemberSummaries(tripId));

        return response;
    }

    /**
     * Gets all trips for a user (as member).
     *
     * @contract
     *   - pre: userId != null, pageable != null
     *   - post: Returns paginated list of trips
     *   - calledBy: TripApiController#getUserTrips
     *
     * @param userId The user ID
     * @param pageable Pagination parameters
     * @return Page of trip responses
     */
    @Transactional(readOnly = true)
    public Page<TripResponse> getUserTrips(UUID userId, Pageable pageable) {
        return tripRepository.findTripsByMemberId(userId, pageable)
                .map(trip -> {
                    TripResponse response = TripResponse.fromEntity(trip);
                    response.setMemberCount((int) tripMemberRepository.countByTripId(trip.getId()));
                    response.setCurrentUserRole(permissionChecker.getRole(trip.getId(), userId).orElse(null));
                    return response;
                });
    }

    /**
     * Updates a trip (owner only for basic info).
     *
     * @contract
     *   - pre: tripId != null, request != null, userId != null
     *   - pre: user must be OWNER
     *   - post: Trip is updated
     *   - calledBy: TripApiController#updateTrip
     *
     * @param tripId The trip ID
     * @param request The update request
     * @param userId The requesting user ID
     * @return The updated trip response
     * @throws ResourceNotFoundException if trip not found
     * @throws ForbiddenException if user is not owner
     * @throws ValidationException if validation fails
     */
    @Transactional
    public TripResponse updateTrip(UUID tripId, UpdateTripRequest request, UUID userId) {
        Trip trip = findTripById(tripId);

        if (!permissionChecker.canDelete(tripId, userId)) { // Only owner can update basic info
            throw new ForbiddenException("只有行程建立者可以修改基本資訊");
        }

        // Validate dates if either is being updated
        LocalDate newStartDate = request.getStartDate() != null ? request.getStartDate() : trip.getStartDate();
        LocalDate newEndDate = request.getEndDate() != null ? request.getEndDate() : trip.getEndDate();
        validateTripDates(newStartDate, newEndDate);

        // Update fields
        if (request.getTitle() != null) {
            trip.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            trip.setDescription(request.getDescription());
        }
        if (request.getStartDate() != null) {
            trip.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            trip.setEndDate(request.getEndDate());
        }
        if (request.getBaseCurrency() != null) {
            trip.setBaseCurrency(request.getBaseCurrency());
        }
        if (request.getCoverImageUrl() != null) {
            trip.setCoverImageUrl(request.getCoverImageUrl());
        }

        trip = tripRepository.save(trip);
        log.info("Updated trip: {} by user: {}", tripId, userId);

        TripResponse response = TripResponse.fromEntity(trip);
        response.setMemberCount((int) tripMemberRepository.countByTripId(tripId));
        response.setCurrentUserRole(Role.OWNER);

        return response;
    }

    /**
     * Deletes a trip (owner only).
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - pre: user must be OWNER
     *   - post: Trip and all related entities are deleted
     *   - calledBy: TripApiController#deleteTrip
     *
     * @param tripId The trip ID
     * @param userId The requesting user ID
     * @throws ResourceNotFoundException if trip not found
     * @throws ForbiddenException if user is not owner
     */
    @Transactional
    public void deleteTrip(UUID tripId, UUID userId) {
        Trip trip = findTripById(tripId);

        if (!permissionChecker.canDelete(tripId, userId)) {
            throw new ForbiddenException("只有行程建立者可以刪除行程");
        }

        // Delete related entities (in proper order for FK constraints)
        tripMemberRepository.deleteByTripId(tripId);
        tripRepository.delete(trip);

        log.info("Deleted trip: {} by user: {}", tripId, userId);
    }

    /**
     * Gets members of a trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - post: Returns list of members if user can view
     *   - calledBy: TripApiController#getTripMembers
     *
     * @param tripId The trip ID
     * @param userId The requesting user ID
     * @return List of member summaries
     * @throws ForbiddenException if user cannot view
     */
    @Transactional(readOnly = true)
    public List<TripResponse.MemberSummary> getTripMembers(UUID tripId, UUID userId) {
        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("您沒有權限查看此行程");
        }

        return getMemberSummaries(tripId);
    }

    /**
     * Changes a member's role (owner only).
     *
     * @contract
     *   - pre: tripId != null, targetUserId != null, newRole != null, requesterId != null
     *   - pre: requester must be OWNER
     *   - pre: newRole != OWNER (use transferOwnership instead)
     *   - post: Member's role is updated
     *   - calledBy: TripApiController#changeMemberRole
     *
     * @param tripId The trip ID
     * @param targetUserId The target member's user ID
     * @param newRole The new role
     * @param requesterId The requesting user ID
     * @throws ForbiddenException if requester is not owner
     * @throws ValidationException if trying to assign OWNER role
     */
    @Transactional
    public void changeMemberRole(UUID tripId, UUID targetUserId, Role newRole, UUID requesterId) {
        if (!permissionChecker.canManageMembers(tripId, requesterId)) {
            throw new ForbiddenException("只有行程建立者可以變更成員角色");
        }

        if (newRole == Role.OWNER) {
            throw new ValidationException("INVALID_ROLE_CHANGE", "無法將角色變更為 OWNER，請使用轉移所有權功能");
        }

        TripMember member = tripMemberRepository.findByTripIdAndUserId(tripId, targetUserId)
                .orElseThrow(() -> ResourceNotFoundException.withCode("MEMBER_NOT_FOUND", "找不到此成員"));

        if (member.getRole() == Role.OWNER) {
            throw new ValidationException("INVALID_ROLE_CHANGE", "無法變更行程建立者的角色");
        }

        member.setRole(newRole);
        tripMemberRepository.save(member);

        log.info("Changed role of user: {} to: {} in trip: {} by: {}",
                targetUserId, newRole, tripId, requesterId);
    }

    /**
     * Removes a member from a trip (owner only).
     *
     * @contract
     *   - pre: tripId != null, targetUserId != null, requesterId != null
     *   - pre: requester must be OWNER
     *   - pre: target cannot be OWNER
     *   - post: Member is removed
     *   - calledBy: TripApiController#removeMember
     *
     * @param tripId The trip ID
     * @param targetUserId The target member's user ID
     * @param requesterId The requesting user ID
     * @throws ForbiddenException if requester is not owner or target is owner
     */
    @Transactional
    public void removeMember(UUID tripId, UUID targetUserId, UUID requesterId) {
        if (!permissionChecker.canManageMembers(tripId, requesterId)) {
            throw new ForbiddenException("只有行程建立者可以移除成員");
        }

        TripMember member = tripMemberRepository.findByTripIdAndUserId(tripId, targetUserId)
                .orElseThrow(() -> ResourceNotFoundException.withCode("MEMBER_NOT_FOUND", "找不到此成員"));

        if (member.getRole() == Role.OWNER) {
            throw new ForbiddenException("無法移除行程建立者");
        }

        tripMemberRepository.delete(member);

        log.info("Removed user: {} from trip: {} by: {}", targetUserId, tripId, requesterId);
    }

    /**
     * Adds a member to a trip with specified role.
     *
     * @contract
     *   - pre: tripId != null, userId != null, role != null
     *   - pre: role != OWNER
     *   - pre: trip must exist and not be at member limit
     *   - post: New member is added
     *
     * @param tripId The trip ID
     * @param userId The user ID to add
     * @param role The role to assign
     * @throws ValidationException if already a member or limit exceeded
     */
    @Transactional
    public void addMember(UUID tripId, UUID userId, Role role) {
        findTripById(tripId); // Verify trip exists

        if (tripMemberRepository.existsByTripIdAndUserId(tripId, userId)) {
            throw new ValidationException("DUPLICATE_MEMBER", "已是行程成員");
        }

        long currentCount = tripMemberRepository.countByTripId(tripId);
        if (currentCount >= MAX_MEMBERS_PER_TRIP) {
            throw new ValidationException("MEMBER_LIMIT_EXCEEDED", "行程成員已達上限（" + MAX_MEMBERS_PER_TRIP + "人）");
        }

        TripMember member = TripMember.builder()
                .tripId(tripId)
                .userId(userId)
                .role(role)
                .build();

        tripMemberRepository.save(member);

        log.info("Added user: {} to trip: {} with role: {}", userId, tripId, role);
    }

    /**
     * Gets the member count for a trip.
     *
     * @param tripId The trip ID
     * @return The number of members
     */
    public int getMemberCount(UUID tripId) {
        return (int) tripMemberRepository.countByTripId(tripId);
    }

    // Private helper methods

    private Trip findTripById(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> ResourceNotFoundException.withCode("TRIP_NOT_FOUND", "行程不存在"));
    }

    private void validateTripDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new ValidationException("INVALID_DATE_RANGE", "結束日期不可早於開始日期");
        }
    }

    private List<TripResponse.MemberSummary> getMemberSummaries(UUID tripId) {
        List<TripMember> members = tripMemberRepository.findByTripId(tripId);

        return members.stream()
                .map(member -> {
                    User user = userRepository.findById(member.getUserId()).orElse(null);
                    return TripResponse.MemberSummary.builder()
                            .userId(member.getUserId())
                            .nickname(user != null ? user.getNickname() : "Unknown")
                            .avatarUrl(user != null ? user.getAvatarUrl() : null)
                            .role(member.getRole())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
