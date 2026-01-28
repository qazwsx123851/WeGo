package com.wego.service;

import com.wego.entity.User;
import com.wego.exception.ResourceNotFoundException;
import com.wego.exception.UnauthorizedException;
import com.wego.repository.UserRepository;
import com.wego.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserService.
 *
 * Test cases:
 * - U-010: Get current user info
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .email("test@example.com")
                .nickname("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .provider("google")
                .providerId("google-123456")
                .build();
    }

    @Test
    @DisplayName("Should get user by ID")
    void getUserById_withExistingId_shouldReturnUser() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        User result = userService.getUserById(testUserId);

        assertThat(result).isEqualTo(testUser);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should throw exception when user not found by ID")
    void getUserById_withNonExistingId_shouldThrowException() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should get user by email")
    void getUserByEmail_withExistingEmail_shouldReturnUser() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        User result = userService.getUserByEmail("test@example.com");

        assertThat(result).isEqualTo(testUser);
    }

    @Test
    @DisplayName("Should throw exception when user not found by email")
    void getUserByEmail_withNonExistingEmail_shouldThrowException() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail("nonexistent@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("U-010: Should get current user from SecurityContext")
    void getCurrentUser_withAuthenticatedUser_shouldReturnUser() {
        UserPrincipal userPrincipal = new UserPrincipal(testUser);
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        SecurityContextHolder.setContext(securityContext);

        try {
            User result = userService.getCurrentUser();

            assertThat(result).isEqualTo(testUser);
            assertThat(result.getId()).isEqualTo(testUserId);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("Should throw exception when no authentication")
    void getCurrentUser_withNoAuthentication_shouldThrowException() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);

        SecurityContextHolder.setContext(securityContext);

        try {
            assertThatThrownBy(() -> userService.getCurrentUser())
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("User is not authenticated");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("Should throw exception when not authenticated")
    void getCurrentUser_withUnauthenticatedUser_shouldThrowException() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        SecurityContextHolder.setContext(securityContext);

        try {
            assertThatThrownBy(() -> userService.getCurrentUser())
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("User is not authenticated");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("Should throw exception when principal is not UserPrincipal")
    void getCurrentUser_withInvalidPrincipal_shouldThrowException() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymous");

        SecurityContextHolder.setContext(securityContext);

        try {
            assertThatThrownBy(() -> userService.getCurrentUser())
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid authentication principal");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("Should get current user ID")
    void getCurrentUserId_withAuthenticatedUser_shouldReturnUserId() {
        UserPrincipal userPrincipal = new UserPrincipal(testUser);
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        SecurityContextHolder.setContext(securityContext);

        try {
            UUID result = userService.getCurrentUserId();

            assertThat(result).isEqualTo(testUserId);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("Should check if user exists by email")
    void existsByEmail_withExistingEmail_shouldReturnTrue() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        boolean result = userService.existsByEmail("test@example.com");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void existsByEmail_withNonExistingEmail_shouldReturnFalse() {
        when(userRepository.existsByEmail("nonexistent@example.com")).thenReturn(false);

        boolean result = userService.existsByEmail("nonexistent@example.com");

        assertThat(result).isFalse();
    }
}
