package com.wego.security;

import com.wego.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserPrincipal.
 */
class UserPrincipalTest {

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .provider("google")
                .providerId("google-123456")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should create UserPrincipal with user and attributes")
    void constructor_withUserAndAttributes_shouldSetBoth() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google-123456");
        attributes.put("email", "test@example.com");

        UserPrincipal principal = new UserPrincipal(testUser, attributes);

        assertThat(principal.getUser()).isEqualTo(testUser);
        assertThat(principal.getAttributes()).isEqualTo(attributes);
        assertThat(principal.getAttributes().get("sub")).isEqualTo("google-123456");
    }

    @Test
    @DisplayName("Should create UserPrincipal with user only")
    void constructor_withUserOnly_shouldHaveEmptyAttributes() {
        UserPrincipal principal = new UserPrincipal(testUser);

        assertThat(principal.getUser()).isEqualTo(testUser);
        assertThat(principal.getAttributes()).isEmpty();
    }

    @Test
    @DisplayName("Should return correct user ID")
    void getId_shouldReturnUserId() {
        UserPrincipal principal = new UserPrincipal(testUser);

        assertThat(principal.getId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should return correct email")
    void getEmail_shouldReturnUserEmail() {
        UserPrincipal principal = new UserPrincipal(testUser);

        assertThat(principal.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should return correct nickname")
    void getNickname_shouldReturnUserNickname() {
        UserPrincipal principal = new UserPrincipal(testUser);

        assertThat(principal.getNickname()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Should return correct avatar URL")
    void getAvatarUrl_shouldReturnUserAvatarUrl() {
        UserPrincipal principal = new UserPrincipal(testUser);

        assertThat(principal.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
    }

    @Test
    @DisplayName("Should return null avatar URL when not set")
    void getAvatarUrl_whenNotSet_shouldReturnNull() {
        User userWithoutAvatar = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .nickname("Test")
                .provider("google")
                .providerId("123")
                .build();

        UserPrincipal principal = new UserPrincipal(userWithoutAvatar);

        assertThat(principal.getAvatarUrl()).isNull();
    }

    @Test
    @DisplayName("Should return ROLE_USER authority")
    void getAuthorities_shouldReturnRoleUser() {
        UserPrincipal principal = new UserPrincipal(testUser);

        assertThat(principal.getAuthorities()).hasSize(1);
        assertThat(principal.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Should return user ID as name")
    void getName_shouldReturnUserIdAsString() {
        UserPrincipal principal = new UserPrincipal(testUser);

        assertThat(principal.getName()).isEqualTo(userId.toString());
    }
}
