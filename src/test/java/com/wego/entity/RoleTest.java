package com.wego.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Role enum.
 *
 * Covers test cases: P-001 to P-008 (permissions)
 */
@Tag("fast")
@DisplayName("Role Enum Tests")
class RoleTest {

    @Nested
    @DisplayName("canEdit Permission")
    class CanEditPermission {

        @Test
        @DisplayName("P-001: OWNER can edit")
        void canEdit_owner_shouldReturnTrue() {
            assertTrue(Role.OWNER.canEdit());
        }

        @Test
        @DisplayName("P-002: EDITOR can edit")
        void canEdit_editor_shouldReturnTrue() {
            assertTrue(Role.EDITOR.canEdit());
        }

        @Test
        @DisplayName("P-003: VIEWER cannot edit")
        void canEdit_viewer_shouldReturnFalse() {
            assertFalse(Role.VIEWER.canEdit());
        }
    }

    @Nested
    @DisplayName("canDelete Permission")
    class CanDeletePermission {

        @Test
        @DisplayName("P-004: OWNER can delete")
        void canDelete_owner_shouldReturnTrue() {
            assertTrue(Role.OWNER.canDelete());
        }

        @Test
        @DisplayName("P-005: EDITOR cannot delete")
        void canDelete_editor_shouldReturnFalse() {
            assertFalse(Role.EDITOR.canDelete());
        }

        @Test
        @DisplayName("VIEWER cannot delete")
        void canDelete_viewer_shouldReturnFalse() {
            assertFalse(Role.VIEWER.canDelete());
        }
    }

    @Nested
    @DisplayName("canManageMembers Permission")
    class CanManageMembersPermission {

        @Test
        @DisplayName("P-006: OWNER can manage members")
        void canManageMembers_owner_shouldReturnTrue() {
            assertTrue(Role.OWNER.canManageMembers());
        }

        @Test
        @DisplayName("P-007: EDITOR cannot manage members")
        void canManageMembers_editor_shouldReturnFalse() {
            assertFalse(Role.EDITOR.canManageMembers());
        }

        @Test
        @DisplayName("VIEWER cannot manage members")
        void canManageMembers_viewer_shouldReturnFalse() {
            assertFalse(Role.VIEWER.canManageMembers());
        }
    }

    @Nested
    @DisplayName("canInvite Permission")
    class CanInvitePermission {

        @Test
        @DisplayName("OWNER can invite")
        void canInvite_owner_shouldReturnTrue() {
            assertTrue(Role.OWNER.canInvite());
        }

        @Test
        @DisplayName("EDITOR can invite")
        void canInvite_editor_shouldReturnTrue() {
            assertTrue(Role.EDITOR.canInvite());
        }

        @Test
        @DisplayName("VIEWER cannot invite")
        void canInvite_viewer_shouldReturnFalse() {
            assertFalse(Role.VIEWER.canInvite());
        }
    }

    @Nested
    @DisplayName("Role Values")
    class RoleValues {

        @Test
        @DisplayName("Should have exactly 3 roles")
        void values_shouldHaveThreeRoles() {
            assertEquals(3, Role.values().length);
        }

        @Test
        @DisplayName("Should convert from string correctly")
        void valueOf_shouldConvertFromString() {
            assertEquals(Role.OWNER, Role.valueOf("OWNER"));
            assertEquals(Role.EDITOR, Role.valueOf("EDITOR"));
            assertEquals(Role.VIEWER, Role.valueOf("VIEWER"));
        }
    }
}
