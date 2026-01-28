package com.wego.dto.response;

import com.wego.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserResponse DTO.
 */
class UserResponseTest {

    @Test
    @DisplayName("Should create UserResponse from User entity")
    void from_withValidUser_shouldMapAllFields() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .provider("google")
                .providerId("google-123456")
                .createdAt(createdAt)
                .build();

        UserResponse response = UserResponse.from(user);

        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getNickname()).isEqualTo("Test User");
        assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
        assertThat(response.getProvider()).isEqualTo("google");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("Should handle null avatar URL")
    void from_withNullAvatarUrl_shouldMapOtherFields() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .provider("google")
                .providerId("google-123456")
                .build();

        UserResponse response = UserResponse.from(user);

        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getAvatarUrl()).isNull();
    }

    @Test
    @DisplayName("Should use builder to create UserResponse")
    void builder_shouldCreateValidResponse() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        UserResponse response = UserResponse.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .provider("google")
                .createdAt(createdAt)
                .build();

        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getNickname()).isEqualTo("Test User");
        assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
        assertThat(response.getProvider()).isEqualTo("google");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    }
}
