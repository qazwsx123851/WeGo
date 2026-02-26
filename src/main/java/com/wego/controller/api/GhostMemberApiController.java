package com.wego.controller.api;

import com.wego.dto.ApiResponse;
import com.wego.dto.request.CreateGhostMemberRequest;
import com.wego.dto.request.MergeGhostMemberRequest;
import com.wego.dto.response.GhostMemberResponse;
import com.wego.entity.GhostMember;
import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
import com.wego.service.GhostMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST API controller for ghost member operations.
 *
 * Ghost members are non-registered participants in a trip's expense splitting.
 * Only trip owners can create, remove, and merge ghost members.
 *
 * @contract
 *   - All endpoints require authentication
 *   - Create/Delete/Merge require OWNER permission
 *   - GET requires VIEWER+ permission (validated in service)
 *   - Returns ApiResponse wrapper for all responses
 *
 * @see GhostMemberService
 */
@Slf4j
@RestController
@RequestMapping("/api/trips/{tripId}/ghost-members")
@RequiredArgsConstructor
public class GhostMemberApiController {

    private final GhostMemberService ghostMemberService;

    /**
     * Creates a new ghost member in a trip.
     *
     * @param tripId The trip ID
     * @param request The ghost member creation request
     * @param principal The authenticated user
     * @return The created ghost member (201 Created)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<GhostMemberResponse>> createGhostMember(
            @PathVariable UUID tripId,
            @Valid @RequestBody CreateGhostMemberRequest request,
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getUser().getId();
        GhostMember ghost = ghostMemberService.createGhostMember(
                tripId, request.displayName(), request.note(), userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(GhostMemberResponse.fromEntity(ghost)));
    }

    /**
     * Lists all active ghost members for a trip.
     *
     * @param tripId The trip ID
     * @return List of active ghost members
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<GhostMemberResponse>>> getGhostMembers(
            @PathVariable UUID tripId) {

        List<GhostMemberResponse> ghosts = ghostMemberService.getActiveGhosts(tripId).stream()
                .map(GhostMemberResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(ghosts));
    }

    /**
     * Removes a ghost member from a trip.
     * Blocked if the ghost has existing expenses or splits.
     *
     * @param tripId The trip ID
     * @param ghostId The ghost member ID
     * @param principal The authenticated user
     * @return 204 No Content on success
     */
    @DeleteMapping("/{ghostId}")
    public ResponseEntity<Void> removeGhostMember(
            @PathVariable UUID tripId,
            @PathVariable UUID ghostId,
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getUser().getId();
        ghostMemberService.removeGhostMember(tripId, ghostId, userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Merges a ghost member into a real user, transferring all expense data.
     * This operation is irreversible.
     *
     * @param tripId The trip ID
     * @param ghostId The ghost member ID
     * @param request The merge request with target user ID
     * @param principal The authenticated user
     * @return 200 OK with success message
     */
    @PostMapping("/{ghostId}/merge")
    public ResponseEntity<ApiResponse<Void>> mergeGhostMember(
            @PathVariable UUID tripId,
            @PathVariable UUID ghostId,
            @Valid @RequestBody MergeGhostMemberRequest request,
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getUser().getId();
        ghostMemberService.mergeGhostToUser(tripId, ghostId, request.targetUserId(), userId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
