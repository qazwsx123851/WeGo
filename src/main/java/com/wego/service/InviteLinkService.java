package com.wego.service;

import com.wego.domain.permission.PermissionChecker;
import com.wego.dto.request.CreateInviteLinkRequest;
import com.wego.dto.response.InviteLinkResponse;
import com.wego.dto.response.InvitePageData;
import com.wego.entity.InviteLink;
import com.wego.entity.Role;
import com.wego.entity.Trip;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.exception.ValidationException;
import com.wego.repository.GhostMemberRepository;
import com.wego.repository.InviteLinkRepository;
import com.wego.repository.TripMemberRepository;
import com.wego.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for invite link-related business logic.
 *
 * @contract
 *   - All methods that modify data are transactional
 *   - Permission checks are performed before modifications
 *   - Invite links use SecureRandom for token generation
 *
 * @see InviteLink
 * @see TripService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InviteLinkService {

    private static final int MAX_MEMBERS_PER_TRIP = com.wego.domain.TripConstants.MAX_MEMBERS_PER_TRIP;

    private final InviteLinkRepository inviteLinkRepository;
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final GhostMemberRepository ghostMemberRepository;
    private final TripService tripService;
    private final PermissionChecker permissionChecker;

    @Value("${wego.base-url}")
    private String baseUrl;

    /**
     * Creates a new invite link for a trip.
     *
     * @contract
     *   - pre: tripId != null, request != null, userId != null
     *   - pre: user must be able to invite (OWNER or EDITOR)
     *   - pre: request.role != OWNER
     *   - post: InviteLink is created with secure random token
     *   - calledBy: TripApiController#createInviteLink
     *
     * @param tripId The trip ID
     * @param request The invite link creation request
     * @param userId The requesting user ID
     * @return The created invite link response
     * @throws ForbiddenException if user cannot invite
     * @throws ValidationException if role is OWNER
     */
    @Transactional
    public InviteLinkResponse createInviteLink(UUID tripId, CreateInviteLinkRequest request, UUID userId) {
        // Verify trip exists
        if (!tripRepository.existsById(tripId)) {
            throw ResourceNotFoundException.withCode("TRIP_NOT_FOUND", "行程不存在");
        }

        // Check permission
        if (!permissionChecker.canInvite(tripId, userId)) {
            throw new ForbiddenException("您沒有權限建立邀請連結");
        }

        // Cannot invite as OWNER
        if (request.getRole() == Role.OWNER) {
            throw new ValidationException("INVALID_ROLE", "無法邀請成為 OWNER");
        }

        InviteLink link = InviteLink.builder()
                .tripId(tripId)
                .role(request.getRole())
                .expiresAt(Instant.now().plus(request.getExpiryDays(), ChronoUnit.DAYS))
                .createdBy(userId)
                .build();

        link = inviteLinkRepository.save(link);
        log.info("Created invite link: {} for trip: {} by user: {}", link.getId(), tripId, userId);

        return InviteLinkResponse.fromEntity(link, baseUrl);
    }

    /**
     * Accepts an invite link and adds the user to the trip.
     *
     * @contract
     *   - pre: token != null, userId != null
     *   - pre: link must not be expired
     *   - pre: user must not already be a member
     *   - pre: trip must not be at member limit
     *   - post: User is added to trip with link's role
     *   - post: Link's useCount is incremented
     *   - calledBy: AuthController#acceptInvite
     *
     * @param token The invite token
     * @param userId The accepting user ID
     * @return The trip ID that the user joined
     * @throws ValidationException if link is invalid or user already a member
     */
    @Transactional
    public UUID acceptInvite(String token, UUID userId) {
        InviteLink link = inviteLinkRepository.findByToken(token)
                .orElseThrow(() -> new ValidationException("INVALID_INVITE_LINK", "邀請連結無效或已過期"));

        // Check if expired
        if (link.isExpired()) {
            throw new ValidationException("INVALID_INVITE_LINK", "邀請連結已過期");
        }

        UUID tripId = link.getTripId();

        // Check if already a member
        if (tripMemberRepository.existsByTripIdAndUserId(tripId, userId)) {
            throw new ValidationException("DUPLICATE_MEMBER", "已是行程成員");
        }

        // Check member limit (real + ghost members)
        long realCount = tripMemberRepository.countByTripId(tripId);
        long ghostCount = ghostMemberRepository.countByTripIdAndMergedToUserIdIsNull(tripId);
        if (realCount + ghostCount >= MAX_MEMBERS_PER_TRIP) {
            throw new ValidationException("MEMBER_LIMIT_EXCEEDED", "行程成員已達上限");
        }

        // Add member
        tripService.addMember(tripId, userId, link.getRole());

        // Increment use count
        link.incrementUseCount();
        inviteLinkRepository.save(link);

        log.info("User: {} joined trip: {} via invite link: {}", userId, tripId, link.getId());

        return tripId;
    }

    /**
     * Gets all active invite links for a trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - pre: user must be able to view trip
     *   - post: Returns list of non-expired invite links
     *   - calledBy: TripApiController#getInviteLinks
     *
     * @param tripId The trip ID
     * @param userId The requesting user ID
     * @return List of active invite links
     * @throws ForbiddenException if user cannot view
     */
    @Transactional(readOnly = true)
    public List<InviteLinkResponse> getActiveInviteLinks(UUID tripId, UUID userId) {
        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("您沒有權限查看此行程");
        }

        return inviteLinkRepository.findActiveByTripId(tripId, Instant.now())
                .stream()
                .map(link -> InviteLinkResponse.fromEntity(link, baseUrl))
                .collect(Collectors.toList());
    }

    /**
     * Deletes an invite link.
     *
     * @contract
     *   - pre: linkId != null, userId != null
     *   - pre: user must be able to manage members (OWNER only)
     *   - post: Invite link is deleted
     *   - calledBy: TripApiController#deleteInviteLink
     *
     * @param linkId The invite link ID
     * @param userId The requesting user ID
     * @throws ForbiddenException if user cannot manage members
     */
    @Transactional
    public void deleteInviteLink(UUID linkId, UUID userId) {
        InviteLink link = inviteLinkRepository.findById(linkId)
                .orElseThrow(() -> ResourceNotFoundException.withCode("INVITE_LINK_NOT_FOUND", "找不到邀請連結"));

        if (!permissionChecker.canManageMembers(link.getTripId(), userId)) {
            throw new ForbiddenException("只有行程建立者可以刪除邀請連結");
        }

        inviteLinkRepository.delete(link);
        log.info("Deleted invite link: {} by user: {}", linkId, userId);
    }

    /**
     * Resolves invite page data for rendering the invite acceptance page.
     *
     * @contract
     *   - pre: token != null
     *   - post: returns InvitePageData with error message if invalid, or full page data if valid
     *   - calledBy: InviteController#showInvitePage
     *
     * @param token The invite token
     * @param userId The authenticated user's ID (nullable if user not found)
     * @return InvitePageData containing all data needed for the invite page
     */
    @Transactional(readOnly = true)
    public InvitePageData getInvitePageData(String token, UUID userId) {
        Optional<InviteLink> optLink = inviteLinkRepository.findByToken(token);
        if (optLink.isEmpty()) {
            return InvitePageData.builder()
                    .token(token)
                    .error("邀請連結無效或已過期")
                    .build();
        }

        InviteLink link = optLink.get();

        if (link.isExpired()) {
            return InvitePageData.builder()
                    .token(token)
                    .error("邀請連結已過期")
                    .build();
        }

        Optional<Trip> optTrip = tripRepository.findById(link.getTripId());
        if (optTrip.isEmpty()) {
            return InvitePageData.builder()
                    .token(token)
                    .error("行程不存在")
                    .build();
        }

        Trip trip = optTrip.get();

        if (userId != null && tripMemberRepository.existsByTripIdAndUserId(trip.getId(), userId)) {
            return InvitePageData.builder()
                    .token(token)
                    .tripId(trip.getId())
                    .alreadyMember(true)
                    .build();
        }

        long memberCount = tripMemberRepository.countByTripId(trip.getId())
                + ghostMemberRepository.countByTripIdAndMergedToUserIdIsNull(trip.getId());
        boolean expiresWithin24h = link.getExpiresAt().isBefore(Instant.now().plus(24, ChronoUnit.HOURS));

        return InvitePageData.builder()
                .token(token)
                .tripTitle(trip.getTitle())
                .tripId(trip.getId())
                .tripStartDate(trip.getStartDate())
                .tripEndDate(trip.getEndDate())
                .inviteRole(link.getRole().name())
                .expiresAt(link.getExpiresAt().atZone(ZoneId.of("Asia/Taipei")))
                .memberCount(memberCount)
                .expiresWithin24h(expiresWithin24h)
                .build();
    }

    /**
     * Finds the trip ID associated with an invite token.
     *
     * @contract
     *   - pre: token != null
     *   - post: returns Optional containing trip ID if token exists
     *   - calledBy: InviteController#acceptInvite (for redirect on duplicate member)
     *
     * @param token The invite token
     * @return Optional containing the trip ID
     */
    @Transactional(readOnly = true)
    public Optional<UUID> findTripIdByToken(String token) {
        return inviteLinkRepository.findByToken(token)
                .map(InviteLink::getTripId);
    }

    /**
     * Cleans up expired invite links.
     * Should be called by a scheduled job.
     *
     * @return Number of deleted links
     */
    @Transactional
    public int cleanupExpiredLinks() {
        int deleted = inviteLinkRepository.deleteExpiredLinks(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired invite links", deleted);
        }
        return deleted;
    }
}
