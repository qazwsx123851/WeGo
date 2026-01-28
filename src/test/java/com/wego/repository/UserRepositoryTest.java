package com.wego.repository;

import com.wego.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for UserRepository.
 *
 * Test cases:
 * - U-001: Create user on first OAuth login
 * - U-002: Find existing user by provider credentials
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .nickname("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .provider("google")
                .providerId("google-123456")
                .build();
    }

    @Test
    @DisplayName("U-001: Should save new user with generated UUID")
    void save_withValidUser_shouldGenerateUUID() {
        User savedUser = userRepository.save(testUser);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getProvider()).isEqualTo("google");
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("U-002: Should find existing user by provider and providerId")
    void findByProviderAndProviderId_withExistingUser_shouldReturnUser() {
        entityManager.persistAndFlush(testUser);

        Optional<User> found = userRepository.findByProviderAndProviderId("google", "google-123456");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getNickname()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Should return empty when user not found by provider credentials")
    void findByProviderAndProviderId_withNonExistingUser_shouldReturnEmpty() {
        Optional<User> found = userRepository.findByProviderAndProviderId("google", "non-existent-id");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find user by email")
    void findByEmail_withExistingEmail_shouldReturnUser() {
        entityManager.persistAndFlush(testUser);

        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getProviderId()).isEqualTo("google-123456");
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void findByEmail_withNonExistingEmail_shouldReturnEmpty() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should check if user exists by email")
    void existsByEmail_withExistingEmail_shouldReturnTrue() {
        entityManager.persistAndFlush(testUser);

        boolean exists = userRepository.existsByEmail("test@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void existsByEmail_withNonExistingEmail_shouldReturnFalse() {
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should update existing user")
    void save_withExistingUser_shouldUpdateFields() {
        User savedUser = entityManager.persistAndFlush(testUser);
        UUID savedId = savedUser.getId();

        entityManager.clear();

        User userToUpdate = userRepository.findById(savedId).orElseThrow();
        userToUpdate.setNickname("Updated Name");
        userToUpdate.setAvatarUrl("https://example.com/new-avatar.jpg");
        userRepository.saveAndFlush(userToUpdate);

        entityManager.clear();

        Optional<User> found = userRepository.findById(savedId);
        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("Updated Name");
        assertThat(found.get().getAvatarUrl()).isEqualTo("https://example.com/new-avatar.jpg");
    }
}
