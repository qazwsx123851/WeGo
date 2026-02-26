package com.wego.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * TripMember entity representing the membership relationship between User and Trip.
 *
 * Each TripMember defines a user's role and permissions within a specific trip.
 * A user can only be a member of a trip once (unique constraint on tripId + userId).
 *
 * @contract
 *   - invariant: tripId is never null
 *   - invariant: userId is never null
 *   - invariant: role is never null
 *   - invariant: A trip has exactly one member with OWNER role
 *   - invariant: Unique constraint on (tripId, userId)
 *
 * @see Trip
 * @see User
 * @see Role
 */
@Entity
@Table(name = "trip_members",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_trip_member",
           columnNames = {"trip_id", "user_id"}
       ),
       indexes = {
           @Index(name = "idx_trip_member_user_id", columnList = "user_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private Role role;

    @Column(name = "joined_at", nullable = false)
    @Builder.Default
    private Instant joinedAt = Instant.now();

    @Column(name = "personal_budget", precision = 12, scale = 2)
    private BigDecimal personalBudget;

    /**
     * Checks if this member can edit trip content.
     * Delegates to Role.canEdit().
     *
     * @contract
     *   - pre: role != null
     *   - post: returns true for OWNER and EDITOR roles
     *   - calledBy: PermissionChecker
     *
     * @return true if member can edit
     */
    public boolean canEdit() {
        return role.canEdit();
    }

    /**
     * Checks if this member can delete the trip.
     * Delegates to Role.canDelete().
     *
     * @contract
     *   - pre: role != null
     *   - post: returns true only for OWNER role
     *   - calledBy: PermissionChecker
     *
     * @return true if member can delete
     */
    public boolean canDelete() {
        return role.canDelete();
    }

    /**
     * Checks if this member can manage other members.
     * Delegates to Role.canManageMembers().
     *
     * @contract
     *   - pre: role != null
     *   - post: returns true only for OWNER role
     *   - calledBy: PermissionChecker
     *
     * @return true if member can manage members
     */
    public boolean canManageMembers() {
        return role.canManageMembers();
    }

    /**
     * Checks if this member can generate invite links.
     * Delegates to Role.canInvite().
     *
     * @contract
     *   - pre: role != null
     *   - post: returns true for OWNER and EDITOR roles
     *   - calledBy: PermissionChecker
     *
     * @return true if member can invite
     */
    public boolean canInvite() {
        return role.canInvite();
    }

    /**
     * Equality based on ID for JPA entity identity.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TripMember that = (TripMember) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "TripMember{" +
                "id=" + id +
                ", tripId=" + tripId +
                ", userId=" + userId +
                ", role=" + role +
                '}';
    }
}
