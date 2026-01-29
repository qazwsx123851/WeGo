package com.wego.entity;

/**
 * Role enum representing user permissions within a trip.
 *
 * @contract
 *   - OWNER: Full access, can delete trip and manage members
 *   - EDITOR: Can add/edit activities, expenses, documents, todos
 *   - VIEWER: Read-only access to trip content
 *
 * @see TripMember
 * @see PermissionChecker
 */
public enum Role {

    /**
     * Trip owner with full permissions.
     * - Can delete the trip
     * - Can manage members (add, remove, change roles)
     * - Can edit all trip content
     * - Can generate invite links
     */
    OWNER,

    /**
     * Editor with write access.
     * - Can add/edit/delete activities
     * - Can add/edit own expenses
     * - Can upload documents
     * - Can manage todos
     * - Can generate invite links
     * - Cannot delete trip
     * - Cannot remove members
     */
    EDITOR,

    /**
     * Viewer with read-only access.
     * - Can view trip content
     * - Cannot modify any content
     * - Cannot generate invite links
     */
    VIEWER;

    /**
     * Checks if this role has at least editor-level permissions.
     *
     * @contract
     *   - post: returns true for OWNER and EDITOR, false for VIEWER
     *
     * @return true if role can edit content
     */
    public boolean canEdit() {
        return this == OWNER || this == EDITOR;
    }

    /**
     * Checks if this role can manage members (add/remove/change roles).
     *
     * @contract
     *   - post: returns true only for OWNER
     *
     * @return true if role can manage members
     */
    public boolean canManageMembers() {
        return this == OWNER;
    }

    /**
     * Checks if this role can delete the trip.
     *
     * @contract
     *   - post: returns true only for OWNER
     *
     * @return true if role can delete trip
     */
    public boolean canDelete() {
        return this == OWNER;
    }

    /**
     * Checks if this role can generate invite links.
     *
     * @contract
     *   - post: returns true for OWNER and EDITOR
     *
     * @return true if role can invite others
     */
    public boolean canInvite() {
        return this == OWNER || this == EDITOR;
    }
}
