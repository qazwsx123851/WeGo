package com.wego.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InviteLink entity.
 *
 * Covers test cases: T-040 to T-050 (invite link management)
 */
@Tag("fast")
@DisplayName("InviteLink Entity Tests")
class InviteLinkTest {

    @Nested
    @DisplayName("InviteLink Creation")
    class InviteLinkCreation {

        @Test
        @DisplayName("T-040: Should create invite link with valid input")
        void createInviteLink_withValidInput_shouldCreateLink() {
            UUID tripId = UUID.randomUUID();
            UUID createdBy = UUID.randomUUID();
            Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

            InviteLink link = InviteLink.builder()
                    .tripId(tripId)
                    .role(Role.EDITOR)
                    .expiresAt(expiresAt)
                    .createdBy(createdBy)
                    .build();

            assertNotNull(link);
            assertNotNull(link.getToken());
            assertEquals(tripId, link.getTripId());
            assertEquals(Role.EDITOR, link.getRole());
            assertEquals(expiresAt, link.getExpiresAt());
            assertEquals(createdBy, link.getCreatedBy());
            assertNotNull(link.getCreatedAt());
        }

        @Test
        @DisplayName("Should generate secure random token")
        void createInviteLink_shouldGenerateSecureToken() {
            InviteLink link1 = InviteLink.builder()
                    .tripId(UUID.randomUUID())
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdBy(UUID.randomUUID())
                    .build();

            InviteLink link2 = InviteLink.builder()
                    .tripId(UUID.randomUUID())
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdBy(UUID.randomUUID())
                    .build();

            // Tokens should be different
            assertNotEquals(link1.getToken(), link2.getToken());

            // Token should be URL-safe and sufficiently long
            assertTrue(link1.getToken().length() >= 32);
            assertTrue(link1.getToken().matches("^[A-Za-z0-9_-]+$"));
        }

        @Test
        @DisplayName("Should set createdAt timestamp on creation")
        void createInviteLink_shouldHaveCreatedAtTimestamp() {
            Instant before = Instant.now();

            InviteLink link = InviteLink.builder()
                    .tripId(UUID.randomUUID())
                    .role(Role.VIEWER)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                    .createdBy(UUID.randomUUID())
                    .build();

            Instant after = Instant.now();

            assertNotNull(link.getCreatedAt());
            assertTrue(link.getCreatedAt().compareTo(before) >= 0);
            assertTrue(link.getCreatedAt().compareTo(after) <= 0);
        }

        @Test
        @DisplayName("Should use provided token if set explicitly")
        void createInviteLink_withExplicitToken_shouldUseProvided() {
            String customToken = "custom-test-token-12345678901234567890";

            InviteLink link = InviteLink.builder()
                    .tripId(UUID.randomUUID())
                    .role(Role.EDITOR)
                    .token(customToken)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdBy(UUID.randomUUID())
                    .build();

            assertEquals(customToken, link.getToken());
        }
    }

    @Nested
    @DisplayName("InviteLink Expiration")
    class InviteLinkExpiration {

        @Test
        @DisplayName("T-043: Valid link should not be expired")
        void isExpired_validLink_shouldReturnFalse() {
            InviteLink link = InviteLink.builder()
                    .tripId(UUID.randomUUID())
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdBy(UUID.randomUUID())
                    .build();

            assertFalse(link.isExpired());
        }

        @Test
        @DisplayName("T-044: Expired link should return true")
        void isExpired_expiredLink_shouldReturnTrue() {
            InviteLink link = InviteLink.builder()
                    .tripId(UUID.randomUUID())
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                    .createdBy(UUID.randomUUID())
                    .build();

            assertTrue(link.isExpired());
        }

        @Test
        @DisplayName("Link expiring at exactly now should be considered expired")
        void isExpired_expiringNow_shouldReturnTrue() {
            InviteLink link = InviteLink.builder()
                    .tripId(UUID.randomUUID())
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().minusMillis(1))
                    .createdBy(UUID.randomUUID())
                    .build();

            assertTrue(link.isExpired());
        }
    }

    @Nested
    @DisplayName("InviteLink Use Count")
    class InviteLinkUseCount {

        @Test
        @DisplayName("New link should have zero use count")
        void newLink_shouldHaveZeroUseCount() {
            InviteLink link = InviteLink.builder()
                    .tripId(UUID.randomUUID())
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdBy(UUID.randomUUID())
                    .build();

            assertEquals(0, link.getUseCount());
        }

        @Test
        @DisplayName("Should increment use count")
        void incrementUseCount_shouldIncreaseByOne() {
            InviteLink link = InviteLink.builder()
                    .tripId(UUID.randomUUID())
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdBy(UUID.randomUUID())
                    .build();

            link.incrementUseCount();
            assertEquals(1, link.getUseCount());

            link.incrementUseCount();
            assertEquals(2, link.getUseCount());
        }
    }

    @Nested
    @DisplayName("InviteLink Equality")
    class InviteLinkEquality {

        @Test
        @DisplayName("Same ID should be equal")
        void equals_sameId_shouldBeEqual() {
            UUID linkId = UUID.randomUUID();

            InviteLink link1 = new InviteLink();
            link1.setId(linkId);

            InviteLink link2 = new InviteLink();
            link2.setId(linkId);

            assertEquals(link1, link2);
            assertEquals(link1.hashCode(), link2.hashCode());
        }

        @Test
        @DisplayName("Different IDs should not be equal")
        void equals_differentId_shouldNotBeEqual() {
            InviteLink link1 = new InviteLink();
            link1.setId(UUID.randomUUID());

            InviteLink link2 = new InviteLink();
            link2.setId(UUID.randomUUID());

            assertNotEquals(link1, link2);
        }
    }

    @Nested
    @DisplayName("Token Generation")
    class TokenGeneration {

        @Test
        @DisplayName("Should generate URL-safe Base64 token")
        void generateToken_shouldBeUrlSafe() {
            String token = InviteLink.generateSecureToken();

            assertNotNull(token);
            assertTrue(token.length() >= 32);
            // URL-safe Base64 characters only
            assertTrue(token.matches("^[A-Za-z0-9_-]+$"));
        }

        @Test
        @DisplayName("Should generate unique tokens")
        void generateToken_shouldBeUnique() {
            String token1 = InviteLink.generateSecureToken();
            String token2 = InviteLink.generateSecureToken();
            String token3 = InviteLink.generateSecureToken();

            assertNotEquals(token1, token2);
            assertNotEquals(token2, token3);
            assertNotEquals(token1, token3);
        }
    }
}
