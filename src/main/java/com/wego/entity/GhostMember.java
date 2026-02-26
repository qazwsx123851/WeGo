package com.wego.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * GhostMember entity representing a non-registered participant in a trip's expense splitting.
 *
 * Ghost members are created by trip owners for people who haven't registered.
 * Their UUID is used directly in Expense.paidBy and ExpenseSplit.userId,
 * allowing them to participate in expense splitting identically to real users.
 *
 * Lifecycle is per-trip. Ghost members can be merged into real users when they register.
 *
 * @contract
 *   - invariant: tripId is never null
 *   - invariant: displayName is never null or empty
 *   - invariant: createdBy is never null (always the trip owner)
 *   - invariant: id is used as participant UUID in Expense.paidBy and ExpenseSplit.userId
 *   - invariant: ghost can never be Expense.createdBy (only paidBy and split participant)
 *   - invariant: mergedToUserId is null for active ghosts, non-null for merged (soft-deleted)
 *
 * @see Expense
 * @see ExpenseSplit
 */
@Entity
@Table(name = "ghost_members", indexes = {
    @Index(name = "idx_ghost_member_trip_id", columnList = "trip_id"),
    @Index(name = "idx_ghost_member_trip_active", columnList = "trip_id, merged_to_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GhostMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "note", length = 200)
    private String note;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "merged_to_user_id")
    private UUID mergedToUserId;

    @Column(name = "merged_at")
    private Instant mergedAt;

    /**
     * Checks if this ghost member has been merged into a real user.
     *
     * @contract
     *   - post: returns true if mergedToUserId is non-null
     *
     * @return true if merged
     */
    public boolean isMerged() {
        return mergedToUserId != null;
    }

    /**
     * Equality based on ID for JPA entity identity.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GhostMember that = (GhostMember) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "GhostMember{" +
                "id=" + id +
                ", tripId=" + tripId +
                ", displayName='" + displayName + '\'' +
                ", merged=" + isMerged() +
                '}';
    }
}
