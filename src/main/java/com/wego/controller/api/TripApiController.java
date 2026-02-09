package com.wego.controller.api;

import com.wego.dto.ApiResponse;
import com.wego.dto.request.ChangeMemberRoleRequest;
import com.wego.dto.request.CreateInviteLinkRequest;
import com.wego.dto.request.CreateTripRequest;
import com.wego.dto.request.UpdateTripRequest;
import com.wego.dto.response.InviteLinkResponse;
import com.wego.dto.response.TripResponse;
import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
import com.wego.service.InviteLinkService;
import com.wego.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for trip-related operations.
 *
 * Provides endpoints for managing trips, members, and invitations.
 *
 * @contract
 *   - pre: User must be authenticated for all endpoints
 *   - post: Returns standardized ApiResponse format
 *   - calls: TripService, InviteLinkService
 *   - calledBy: Frontend API calls
 *
 * @see TripService
 * @see InviteLinkService
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class TripApiController {

    private final TripService tripService;
    private final InviteLinkService inviteLinkService;

    // ========== Trip CRUD Operations ==========

    /**
     * Creates a new trip.
     *
     * @contract
     *   - pre: request is valid, user is authenticated
     *   - post: Trip is created, user becomes OWNER
     *   - calls: TripService#createTrip
     *
     * @param request The trip creation request
     * @param principal The authenticated user
     * @return Created trip with 201 status
     */
    @PostMapping("/trips")
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(
            @Valid @RequestBody CreateTripRequest request,
            @CurrentUser UserPrincipal principal) {

        log.info("Creating trip: {} by user: {}", request.getTitle(), principal.getId());

        TripResponse response = tripService.createTrip(request, principal.getUser());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "行程建立成功"));
    }

    /**
     * Gets paginated list of trips for the current user.
     *
     * @contract
     *   - pre: user is authenticated
     *   - post: Returns paginated trips where user is a member
     *   - calls: TripService#getUserTrips
     *
     * @param page Page number (0-based)
     * @param size Page size (default 10, max 50)
     * @param principal The authenticated user
     * @return Paginated list of trips
     */
    @GetMapping("/trips")
    public ResponseEntity<ApiResponse<Page<TripResponse>>> getUserTrips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @CurrentUser UserPrincipal principal) {

        // Limit page size to prevent excessive data retrieval
        size = Math.min(size, 50);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<TripResponse> trips = tripService.getUserTrips(principal.getId(), pageable);

        return ResponseEntity.ok(ApiResponse.success(trips));
    }

    /**
     * Gets a single trip by ID.
     *
     * @contract
     *   - pre: tripId is valid, user is a member
     *   - post: Returns trip with member details
     *   - calls: TripService#getTrip
     *
     * @param tripId The trip ID
     * @param principal The authenticated user
     * @return Trip details
     */
    @GetMapping("/trips/{tripId}")
    public ResponseEntity<ApiResponse<TripResponse>> getTrip(
            @PathVariable UUID tripId,
            @CurrentUser UserPrincipal principal) {

        TripResponse response = tripService.getTrip(tripId, principal.getId());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Updates a trip (owner only).
     *
     * @contract
     *   - pre: tripId is valid, user is OWNER, request is valid
     *   - post: Trip is updated
     *   - calls: TripService#updateTrip
     *
     * @param tripId The trip ID
     * @param request The update request
     * @param principal The authenticated user
     * @return Updated trip
     */
    @PutMapping("/trips/{tripId}")
    public ResponseEntity<ApiResponse<TripResponse>> updateTrip(
            @PathVariable UUID tripId,
            @Valid @RequestBody UpdateTripRequest request,
            @CurrentUser UserPrincipal principal) {

        log.info("Updating trip: {} by user: {}", tripId, principal.getId());

        TripResponse response = tripService.updateTrip(tripId, request, principal.getId());

        return ResponseEntity.ok(ApiResponse.success(response, "行程更新成功"));
    }

    /**
     * Deletes a trip (owner only).
     *
     * @contract
     *   - pre: tripId is valid, user is OWNER
     *   - post: Trip and all related entities are deleted
     *   - calls: TripService#deleteTrip
     *
     * @param tripId The trip ID
     * @param principal The authenticated user
     * @return 204 No Content on success
     */
    @DeleteMapping("/trips/{tripId}")
    public ResponseEntity<Void> deleteTrip(
            @PathVariable UUID tripId,
            @CurrentUser UserPrincipal principal) {

        log.info("Deleting trip: {} by user: {}", tripId, principal.getId());

        tripService.deleteTrip(tripId, principal.getId());

        return ResponseEntity.noContent().build();
    }

    // ========== Member Management ==========

    /**
     * Gets all members of a trip.
     *
     * @contract
     *   - pre: tripId is valid, user can view trip
     *   - post: Returns list of member summaries
     *   - calls: TripService#getTripMembers
     *
     * @param tripId The trip ID
     * @param principal The authenticated user
     * @return List of members
     */
    @GetMapping("/trips/{tripId}/members")
    public ResponseEntity<ApiResponse<List<TripResponse.MemberSummary>>> getTripMembers(
            @PathVariable UUID tripId,
            @CurrentUser UserPrincipal principal) {

        List<TripResponse.MemberSummary> members = tripService.getTripMembers(tripId, principal.getId());

        return ResponseEntity.ok(ApiResponse.success(members));
    }

    /**
     * Current user leaves a trip (self-removal).
     *
     * @contract
     *   - pre: tripId is valid, user is a member but not OWNER
     *   - post: User is removed from trip
     *   - calls: TripService#removeMember
     *
     * @param tripId The trip ID
     * @param principal The authenticated user
     * @return 204 No Content on success
     */
    @DeleteMapping("/trips/{tripId}/members/me")
    public ResponseEntity<Void> leaveTrip(
            @PathVariable UUID tripId,
            @CurrentUser UserPrincipal principal) {

        log.info("User {} leaving trip {}", principal.getId(), tripId);

        tripService.removeMember(tripId, principal.getId(), principal.getId());

        return ResponseEntity.noContent().build();
    }

    /**
     * Removes a member from a trip (owner only).
     *
     * @contract
     *   - pre: tripId is valid, user is OWNER, target is not OWNER
     *   - post: Member is removed from trip
     *   - calls: TripService#removeMember
     *
     * @param tripId The trip ID
     * @param targetUserId The user ID to remove
     * @param principal The authenticated user (requester)
     * @return 204 No Content on success
     */
    @DeleteMapping("/trips/{tripId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID tripId,
            @PathVariable("userId") UUID targetUserId,
            @CurrentUser UserPrincipal principal) {

        log.info("Removing member: {} from trip: {} by user: {}",
                targetUserId, tripId, principal.getId());

        tripService.removeMember(tripId, targetUserId, principal.getId());

        return ResponseEntity.noContent().build();
    }

    /**
     * Changes a member's role (owner only).
     *
     * @contract
     *   - pre: tripId is valid, user is OWNER, newRole is not OWNER
     *   - post: Member's role is updated
     *   - calls: TripService#changeMemberRole
     *
     * @param tripId The trip ID
     * @param targetUserId The user ID to change role
     * @param request The role change request
     * @param principal The authenticated user (requester)
     * @return Success message
     */
    @PutMapping("/trips/{tripId}/members/{userId}/role")
    public ResponseEntity<ApiResponse<Void>> changeMemberRole(
            @PathVariable UUID tripId,
            @PathVariable("userId") UUID targetUserId,
            @Valid @RequestBody ChangeMemberRoleRequest request,
            @CurrentUser UserPrincipal principal) {

        log.info("Changing role of user: {} to: {} in trip: {} by user: {}",
                targetUserId, request.getRole(), tripId, principal.getId());

        tripService.changeMemberRole(tripId, targetUserId, request.getRole(), principal.getId());

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("成員角色已更新")
                .build());
    }

    // ========== Invite Link Management ==========

    /**
     * Creates a new invite link for a trip.
     *
     * @contract
     *   - pre: tripId is valid, user can invite (OWNER or EDITOR), role is not OWNER
     *   - post: Invite link is created with specified role and expiry
     *   - calls: InviteLinkService#createInviteLink
     *
     * @param tripId The trip ID
     * @param request The invite link creation request
     * @param principal The authenticated user
     * @return Created invite link with 201 status
     */
    @PostMapping("/trips/{tripId}/invites")
    public ResponseEntity<ApiResponse<InviteLinkResponse>> createInviteLink(
            @PathVariable UUID tripId,
            @Valid @RequestBody CreateInviteLinkRequest request,
            @CurrentUser UserPrincipal principal) {

        log.info("Creating invite link for trip: {} with role: {} by user: {}",
                tripId, request.getRole(), principal.getId());

        InviteLinkResponse response = inviteLinkService.createInviteLink(
                tripId, request, principal.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "邀請連結建立成功"));
    }

    /**
     * Gets all active invite links for a trip.
     *
     * @contract
     *   - pre: tripId is valid, user can view trip
     *   - post: Returns list of non-expired invite links
     *   - calls: InviteLinkService#getActiveInviteLinks
     *
     * @param tripId The trip ID
     * @param principal The authenticated user
     * @return List of active invite links
     */
    @GetMapping("/trips/{tripId}/invites")
    public ResponseEntity<ApiResponse<List<InviteLinkResponse>>> getActiveInviteLinks(
            @PathVariable UUID tripId,
            @CurrentUser UserPrincipal principal) {

        List<InviteLinkResponse> links = inviteLinkService.getActiveInviteLinks(
                tripId, principal.getId());

        return ResponseEntity.ok(ApiResponse.success(links));
    }

    /**
     * Accepts an invite link and joins the trip.
     *
     * @contract
     *   - pre: token is valid and not expired, user is not already a member
     *   - pre: trip has not reached member limit
     *   - post: User is added to trip with invite's role
     *   - calls: InviteLinkService#acceptInvite
     *
     * @param token The invite link token
     * @param principal The authenticated user
     * @return Trip ID that the user joined
     */
    @PostMapping("/invites/{token}/accept")
    public ResponseEntity<ApiResponse<Map<String, UUID>>> acceptInvite(
            @PathVariable String token,
            @CurrentUser UserPrincipal principal) {

        log.info("User: {} accepting invite: {}", principal.getId(), token);

        UUID joinedTripId = inviteLinkService.acceptInvite(token, principal.getId());

        return ResponseEntity.ok(ApiResponse.success(
                Map.of("tripId", joinedTripId),
                "已成功加入行程"));
    }
}
