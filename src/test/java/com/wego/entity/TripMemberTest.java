package com.wego.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TripMember entity.
 *
 * Covers test cases: T-040 to T-050 (member management)
 */
@Tag("fast")
@DisplayName("TripMember Entity Tests")
class TripMemberTest {

    @Nested
    @DisplayName("TripMember Creation")
    class TripMemberCreation {

        @Test
        @DisplayName("Should create trip member with required fields")
        void createTripMember_withValidInput_shouldCreateMember() {
            UUID tripId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            TripMember member = TripMember.builder()
                    .tripId(tripId)
                    .userId(userId)
                    .role(Role.EDITOR)
                    .build();

            assertNotNull(member);
            assertEquals(tripId, member.getTripId());
            assertEquals(userId, member.getUserId());
            assertEquals(Role.EDITOR, member.getRole());
            assertNotNull(member.getJoinedAt());
        }

        @Test
        @DisplayName("Should set joinedAt timestamp on creation")
        void createTripMember_shouldHaveJoinedAtTimestamp() {
            Instant before = Instant.now();

            TripMember member = TripMember.builder()
                    .tripId(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .role(Role.VIEWER)
                    .build();

            Instant after = Instant.now();

            assertNotNull(member.getJoinedAt());
            assertTrue(member.getJoinedAt().compareTo(before) >= 0);
            assertTrue(member.getJoinedAt().compareTo(after) <= 0);
        }

        @Test
        @DisplayName("Should create owner member")
        void createTripMember_asOwner_shouldSetOwnerRole() {
            TripMember member = TripMember.builder()
                    .tripId(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .role(Role.OWNER)
                    .build();

            assertEquals(Role.OWNER, member.getRole());
            assertTrue(member.getRole().canEdit());
            assertTrue(member.getRole().canDelete());
            assertTrue(member.getRole().canManageMembers());
        }
    }

    @Nested
    @DisplayName("Permission Delegation to Role")
    class PermissionDelegation {

        @Test
        @DisplayName("Owner member can edit")
        void canEdit_ownerMember_shouldReturnTrue() {
            TripMember owner = TripMember.builder()
                    .tripId(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .role(Role.OWNER)
                    .build();

            assertTrue(owner.canEdit());
        }

        @Test
        @DisplayName("Editor member can edit")
        void canEdit_editorMember_shouldReturnTrue() {
            TripMember editor = TripMember.builder()
                    .tripId(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .role(Role.EDITOR)
                    .build();

            assertTrue(editor.canEdit());
        }

        @Test
        @DisplayName("Viewer member cannot edit")
        void canEdit_viewerMember_shouldReturnFalse() {
            TripMember viewer = TripMember.builder()
                    .tripId(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .role(Role.VIEWER)
                    .build();

            assertFalse(viewer.canEdit());
        }

        @Test
        @DisplayName("Only owner can delete")
        void canDelete_shouldOnlyReturnTrueForOwner() {
            TripMember owner = TripMember.builder()
                    .tripId(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .role(Role.OWNER)
                    .build();

            TripMember editor = TripMember.builder()
                    .tripId(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .role(Role.EDITOR)
                    .build();

            assertTrue(owner.canDelete());
            assertFalse(editor.canDelete());
        }
    }

    @Nested
    @DisplayName("Role Changes")
    class RoleChanges {

        @Test
        @DisplayName("T-049: Should change member role")
        void setRole_shouldChangeRole() {
            TripMember member = TripMember.builder()
                    .tripId(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .role(Role.EDITOR)
                    .build();

            member.setRole(Role.VIEWER);

            assertEquals(Role.VIEWER, member.getRole());
        }
    }

    @Nested
    @DisplayName("TripMember Equality")
    class TripMemberEquality {

        @Test
        @DisplayName("Same ID should be equal")
        void equals_sameId_shouldBeEqual() {
            UUID memberId = UUID.randomUUID();

            TripMember member1 = new TripMember();
            member1.setId(memberId);

            TripMember member2 = new TripMember();
            member2.setId(memberId);

            assertEquals(member1, member2);
            assertEquals(member1.hashCode(), member2.hashCode());
        }

        @Test
        @DisplayName("Different IDs should not be equal")
        void equals_differentId_shouldNotBeEqual() {
            TripMember member1 = new TripMember();
            member1.setId(UUID.randomUUID());

            TripMember member2 = new TripMember();
            member2.setId(UUID.randomUUID());

            assertNotEquals(member1, member2);
        }
    }
}
