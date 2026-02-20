package com.wego.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for User entity.
 *
 * Covers: builder defaults, field setters, immutable ID.
 */
@Tag("fast")
@DisplayName("User Entity Tests")
class UserTest {

    @Nested
    @DisplayName("Builder Defaults")
    class BuilderDefaults {

        @Test
        @DisplayName("Should default provider to google")
        void build_shouldDefaultProviderToGoogle() {
            User user = User.builder()
                    .email("test@example.com")
                    .nickname("Test")
                    .providerId("google-123")
                    .build();

            assertEquals("google", user.getProvider());
        }

        @Test
        @DisplayName("Should set createdAt on build")
        void build_shouldSetCreatedAt() {
            Instant before = Instant.now();

            User user = User.builder()
                    .email("test@example.com")
                    .nickname("Test")
                    .providerId("google-123")
                    .build();

            Instant after = Instant.now();

            assertNotNull(user.getCreatedAt());
            assertTrue(user.getCreatedAt().compareTo(before) >= 0);
            assertTrue(user.getCreatedAt().compareTo(after) <= 0);
        }

        @Test
        @DisplayName("Should allow overriding provider")
        void build_withCustomProvider_shouldOverride() {
            User user = User.builder()
                    .email("test@example.com")
                    .nickname("Test")
                    .provider("github")
                    .providerId("gh-123")
                    .build();

            assertEquals("github", user.getProvider());
        }
    }

    @Nested
    @DisplayName("All Fields")
    class AllFields {

        @Test
        @DisplayName("Should set all fields via builder")
        void build_allFields_shouldSetCorrectly() {
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();

            User user = User.builder()
                    .id(id)
                    .email("alice@example.com")
                    .nickname("Alice")
                    .avatarUrl("https://example.com/avatar.png")
                    .provider("google")
                    .providerId("google-456")
                    .createdAt(now)
                    .build();

            assertEquals(id, user.getId());
            assertEquals("alice@example.com", user.getEmail());
            assertEquals("Alice", user.getNickname());
            assertEquals("https://example.com/avatar.png", user.getAvatarUrl());
            assertEquals("google", user.getProvider());
            assertEquals("google-456", user.getProviderId());
            assertEquals(now, user.getCreatedAt());
        }

        @Test
        @DisplayName("Should allow null avatarUrl")
        void build_nullAvatar_shouldBeNull() {
            User user = User.builder()
                    .email("bob@example.com")
                    .nickname("Bob")
                    .providerId("google-789")
                    .build();

            assertNull(user.getAvatarUrl());
        }
    }

    @Nested
    @DisplayName("Setter Tests")
    class SetterTests {

        @Test
        @DisplayName("Should update nickname via setter")
        void setNickname_shouldUpdateValue() {
            User user = User.builder()
                    .email("test@example.com")
                    .nickname("Original")
                    .providerId("google-123")
                    .build();

            user.setNickname("Updated");
            assertEquals("Updated", user.getNickname());
        }

        @Test
        @DisplayName("Should update avatarUrl via setter")
        void setAvatarUrl_shouldUpdateValue() {
            User user = User.builder()
                    .email("test@example.com")
                    .nickname("Test")
                    .providerId("google-123")
                    .build();

            user.setAvatarUrl("https://new-avatar.com/pic.jpg");
            assertEquals("https://new-avatar.com/pic.jpg", user.getAvatarUrl());
        }
    }
}
