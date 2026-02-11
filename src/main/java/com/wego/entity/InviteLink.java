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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * InviteLink entity representing an invitation to join a trip.
 *
 * Invite links contain a secure random token that can be shared with
 * others to allow them to join a trip with a specific role. Links
 * have an expiration time and track usage count.
 *
 * @contract
 *   - invariant: token is unique and cryptographically secure
 *   - invariant: tripId is never null
 *   - invariant: role is never null (defaults to VIEWER if not specified)
 *   - invariant: expiresAt is never null
 *   - invariant: createdBy is never null
 *
 * @see Trip
 * @see Role
 */
@Entity
@Table(name = "invite_links", indexes = {
    @Index(name = "idx_invite_link_trip_id", columnList = "trip_id"),
    @Index(name = "idx_invite_link_created_by", columnList = "created_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteLink {

    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "token", nullable = false, unique = true, length = 64)
    @Builder.Default
    private String token = generateSecureToken();

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private Role role;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "use_count", nullable = false)
    @Builder.Default
    private int useCount = 0;

    /**
     * Generates a cryptographically secure random token.
     * Uses SecureRandom and URL-safe Base64 encoding.
     *
     * @contract
     *   - post: returns non-null, unique, URL-safe string of at least 32 characters
     *   - security: Uses SecureRandom to prevent token prediction
     *
     * @return URL-safe Base64 encoded random token
     */
    public static String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Checks if this invite link has expired.
     *
     * @contract
     *   - pre: expiresAt != null
     *   - post: returns true if current time is after or equal to expiresAt
     *   - calledBy: InviteLinkService#acceptInvite
     *
     * @return true if the link has expired
     */
    public boolean isExpired() {
        return !Instant.now().isBefore(expiresAt);
    }

    /**
     * Increments the use count of this invite link.
     *
     * @contract
     *   - post: useCount is incremented by 1
     *   - calledBy: InviteLinkService#acceptInvite
     */
    public void incrementUseCount() {
        this.useCount++;
    }

    /**
     * Equality based on ID for JPA entity identity.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InviteLink that = (InviteLink) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "InviteLink{" +
                "id=" + id +
                ", tripId=" + tripId +
                ", role=" + role +
                ", expiresAt=" + expiresAt +
                ", useCount=" + useCount +
                '}';
    }
}
