package com.wego.domain.permission;

import com.wego.entity.Role;
import com.wego.entity.TripMember;
import com.wego.repository.TripMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PermissionChecker.
 *
 * Covers test cases: P-001 to P-008
 */
@Tag("fast")
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionChecker Unit Tests")
class PermissionCheckerTest {

    @Mock
    private TripMemberRepository tripMemberRepository;

    @InjectMocks
    private PermissionChecker permissionChecker;

    private UUID tripId;
    private UUID ownerId;
    private UUID editorId;
    private UUID viewerId;
    private UUID nonMemberId;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        editorId = UUID.randomUUID();
        viewerId = UUID.randomUUID();
        nonMemberId = UUID.randomUUID();
    }

    private TripMember createMember(UUID userId, Role role) {
        return TripMember.builder()
                .id(UUID.randomUUID())
                .tripId(tripId)
                .userId(userId)
                .role(role)
                .build();
    }

    @Nested
    @DisplayName("canEdit Permission")
    class CanEditPermission {

        @Test
        @DisplayName("P-001: OWNER can edit")
        void canEdit_owner_shouldReturnTrue() {
            when(tripMemberRepository.findByTripIdAndUserId(tripId, ownerId))
                    .thenReturn(Optional.of(createMember(ownerId, Role.OWNER)));

            assertTrue(permissionChecker.canEdit(tripId, ownerId));
        }

        @Test
        @DisplayName("P-002: EDITOR can edit")
        void canEdit_editor_shouldReturnTrue() {
            when(tripMemberRepository.findByTripIdAndUserId(tripId, editorId))
                    .thenReturn(Optional.of(createMember(editorId, Role.EDITOR)));

            assertTrue(permissionChecker.canEdit(tripId, editorId));
        }

        @Test
        @DisplayName("P-003: VIEWER cannot edit")
        void canEdit_viewer_shouldReturnFalse() {
            when(tripMemberRepository.findByTripIdAndUserId(tripId, viewerId))
                    .thenReturn(Optional.of(createMember(viewerId, Role.VIEWER)));

            assertFalse(permissionChecker.canEdit(tripId, viewerId));
        }

        @Test
        @DisplayName("Non-member cannot edit")
        void canEdit_nonMember_shouldReturnFalse() {
            when(tripMemberRepository.findByTripIdAndUserId(tripId, nonMemberId))
                    .thenReturn(Optional.empty());

            assertFalse(permissionChecker.canEdit(tripId, nonMemberId));
        }
    }

    @Nested
    @DisplayName("canDelete Permission")
    class CanDeletePermission {

        @Test
        @DisplayName("P-004: OWNER can delete")
        void canDelete_owner_shouldReturnTrue() {
            when(tripMemberRepository.findByTripIdAndUserId(tripId, ownerId))
                    .thenReturn(Optional.of(createMember(ownerId, Role.OWNER)));

            assertTrue(permissionChecker.canDelete(tripId, ownerId));
        }

        @Test
        @DisplayName("P-005: EDITOR cannot delete")
        void canDelete_editor_shouldReturnFalse() {
            when(tripMemberRepository.findByTripIdAndUserId(tripId, editorId))
                    .thenReturn(Optional.of(createMember(editorId, Role.EDITOR)));

            assertFalse(permissionChecker.canDelete(tripId, editorId));
        }

        @Test
        @DisplayName("VIEWER cannot delete")
        void canDelete_viewer_shouldReturnFalse() {
            when(tripMemberRepository.findByTripIdAndUserId(tripId, viewerId))
                    .thenReturn(Optional.of(createMember(viewerId, Role.VIEWER)));

            assertFalse(permissionChecker.canDelete(tripId, viewerId));
        }

        @Test
        @DisplayName("Non-member cannot delete")
        void canDelete_nonMember_shouldReturnFalse() {
            when(tripMemberRepository.findByTripIdAndUserId(tripId, nonMemberId))
                    .thenReturn(Optional.empty());

            assertFalse(permissionChecker.canDelete(tripId, nonMemberId));
        }
    }

    @Nested
    @DisplayName("canManageMembers Permission")
    class CanManageMembersPermission {

        @Test
        @DisplayName("P-006: OWNER can manage members")
        void canManageMembers_owner_shouldReturnTrue() {
            when(tripMemberRepository.findByTripIdAndUserId(tripId, ownerId))
                    .thenReturn(Optional.of(createMember(ownerId, Role.OWNER)));

            assertTrue(permissionChecker.canManageMembers(tripId, ownerId));
        }

        @Test
        @DisplayName("P-007: EDITOR cannot manage members")
        void canManageMembers_editor_shouldReturnFalse() {
            when(tripMemberRepository.findByTripIdAndUserId(tripId, editorId))
                    .thenReturn(Optional.of(createMember(editorId, Role.EDITOR)));

            assertFalse(permissionChecker.canManageMembers(tripId, editorId));
        }

        @Test
        @DisplayName("VIEWER cannot manage members")
        void canManageMembers_viewer_shouldReturnFalse() {
            when(tripMemberRepository.findByTripIdAndUserId(tripId, viewerId))
                    .thenReturn(Optional.of(createMember(viewerId, Role.VIEWER)));

            assertFalse(permissionChecker.canManageMembers(tripId, viewerId));
        }
    }

    @Nested
    @DisplayName("canInvite Permission")
    class CanInvitePermission {

        @Test
        @DisplayName("OWNER can invite")
        void canInvite_owner_shouldReturnTrue() {
            when(tripMemberRepository.findByTripIdAndUserId(tripId, ownerId))
                    .thenReturn(Optional.of(createMember(ownerId, Role.OWNER)));

            assertTrue(permissionChecker.canInvite(tripId, ownerId));
        }

        @Test
        @DisplayName("EDITOR can invite")
        void canInvite_editor_shouldReturnTrue() {
            when(tripMemberRepository.findByTripIdAndUserId(tripId, editorId))
                    .thenReturn(Optional.of(createMember(editorId, Role.EDITOR)));

            assertTrue(permissionChecker.canInvite(tripId, editorId));
        }

        @Test
        @DisplayName("VIEWER cannot invite")
        void canInvite_viewer_shouldReturnFalse() {
            when(tripMemberRepository.findByTripIdAndUserId(tripId, viewerId))
                    .thenReturn(Optional.of(createMember(viewerId, Role.VIEWER)));

            assertFalse(permissionChecker.canInvite(tripId, viewerId));
        }
    }

    @Nested
    @DisplayName("canView Permission")
    class CanViewPermission {

        @Test
        @DisplayName("OWNER can view")
        void canView_owner_shouldReturnTrue() {
            when(tripMemberRepository.existsByTripIdAndUserId(tripId, ownerId))
                    .thenReturn(true);

            assertTrue(permissionChecker.canView(tripId, ownerId));
        }

        @Test
        @DisplayName("EDITOR can view")
        void canView_editor_shouldReturnTrue() {
            when(tripMemberRepository.existsByTripIdAndUserId(tripId, editorId))
                    .thenReturn(true);

            assertTrue(permissionChecker.canView(tripId, editorId));
        }

        @Test
        @DisplayName("VIEWER can view")
        void canView_viewer_shouldReturnTrue() {
            when(tripMemberRepository.existsByTripIdAndUserId(tripId, viewerId))
                    .thenReturn(true);

            assertTrue(permissionChecker.canView(tripId, viewerId));
        }

        @Test
        @DisplayName("P-008: Non-member cannot view")
        void canView_nonMember_shouldReturnFalse() {
            when(tripMemberRepository.existsByTripIdAndUserId(tripId, nonMemberId))
                    .thenReturn(false);

            assertFalse(permissionChecker.canView(tripId, nonMemberId));
        }
    }

    @Nested
    @DisplayName("getRole")
    class GetRole {

        @Test
        @DisplayName("Should return role for member")
        void getRole_member_shouldReturnRole() {
            when(tripMemberRepository.findByTripIdAndUserId(tripId, editorId))
                    .thenReturn(Optional.of(createMember(editorId, Role.EDITOR)));

            Optional<Role> role = permissionChecker.getRole(tripId, editorId);

            assertTrue(role.isPresent());
            assertEquals(Role.EDITOR, role.get());
        }

        @Test
        @DisplayName("Should return empty for non-member")
        void getRole_nonMember_shouldReturnEmpty() {
            when(tripMemberRepository.findByTripIdAndUserId(tripId, nonMemberId))
                    .thenReturn(Optional.empty());

            Optional<Role> role = permissionChecker.getRole(tripId, nonMemberId);

            assertTrue(role.isEmpty());
        }
    }

    @Nested
    @DisplayName("isMember")
    class IsMember {

        @Test
        @DisplayName("Should return true for member")
        void isMember_member_shouldReturnTrue() {
            when(tripMemberRepository.existsByTripIdAndUserId(tripId, ownerId))
                    .thenReturn(true);

            assertTrue(permissionChecker.isMember(tripId, ownerId));
        }

        @Test
        @DisplayName("Should return false for non-member")
        void isMember_nonMember_shouldReturnFalse() {
            when(tripMemberRepository.existsByTripIdAndUserId(tripId, nonMemberId))
                    .thenReturn(false);

            assertFalse(permissionChecker.isMember(tripId, nonMemberId));
        }
    }
}
