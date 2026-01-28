package com.wego.security;

import com.wego.entity.User;
import com.wego.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CustomOAuth2UserService.
 *
 * Uses Mockito spy to mock the parent class's loadUser() method
 * while testing the actual production code in processOAuth2User().
 *
 * Test cases:
 * - U-001: First OAuth login creates new user
 * - U-002: Returning user login retrieves existing user
 * - U-003: Missing email throws exception
 * - U-004: Missing name uses email prefix
 * - U-005: Unsupported provider throws exception
 *
 * @contract
 *   - tests: CustomOAuth2UserService
 *   - coverage: loadUser, processOAuth2User, extractProviderId
 */
@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private Map<String, Object> googleAttributes;
    private User existingUser;

    @BeforeEach
    void setUp() {
        // Standard Google OAuth attributes
        googleAttributes = new HashMap<>();
        googleAttributes.put("sub", "google-123456");
        googleAttributes.put("email", "test@example.com");
        googleAttributes.put("name", "Test User");
        googleAttributes.put("picture", "https://example.com/avatar.jpg");

        existingUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .nickname("Old Name")
                .avatarUrl("https://example.com/old-avatar.jpg")
                .provider("google")
                .providerId("google-123456")
                .build();
    }

    @Nested
    @DisplayName("loadUser_withNewUser")
    class NewUserTests {

        @Test
        @DisplayName("U-001: Should create new user on first OAuth login")
        void loadUser_withNewUser_shouldCreateUser() {
            // Arrange
            OAuth2UserRequest userRequest = createUserRequest("google");
            OAuth2User mockOAuth2User = createMockOAuth2User(googleAttributes);

            // Use testable subclass to inject mock OAuth2User
            // This allows testing the actual processOAuth2User logic
            TestableOAuth2UserService testableService = new TestableOAuth2UserService(userRepository);
            testableService.setMockOAuth2User(mockOAuth2User);

            when(userRepository.findByProviderAndProviderId("google", "google-123456"))
                    .thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });

            // Act
            OAuth2User result = testableService.loadUser(userRequest);

            // Assert
            assertThat(result).isInstanceOf(UserPrincipal.class);
            UserPrincipal principal = (UserPrincipal) result;
            assertThat(principal.getEmail()).isEqualTo("test@example.com");
            assertThat(principal.getNickname()).isEqualTo("Test User");
            assertThat(principal.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");

            // Verify user was saved
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getProvider()).isEqualTo("google");
            assertThat(savedUser.getProviderId()).isEqualTo("google-123456");
            assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("loadUser_withExistingUser")
    class ExistingUserTests {

        @Test
        @DisplayName("U-002: Should update and return existing user on returning login")
        void loadUser_withExistingUser_shouldUpdateAndReturnUser() {
            // Arrange
            OAuth2UserRequest userRequest = createUserRequest("google");
            OAuth2User mockOAuth2User = createMockOAuth2User(googleAttributes);

            TestableOAuth2UserService testableService = new TestableOAuth2UserService(userRepository);
            testableService.setMockOAuth2User(mockOAuth2User);

            when(userRepository.findByProviderAndProviderId("google", "google-123456"))
                    .thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            OAuth2User result = testableService.loadUser(userRequest);

            // Assert
            assertThat(result).isInstanceOf(UserPrincipal.class);
            UserPrincipal principal = (UserPrincipal) result;
            assertThat(principal.getId()).isEqualTo(existingUser.getId());
            // Name and avatar should be updated
            assertThat(principal.getNickname()).isEqualTo("Test User");
            assertThat(principal.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");

            verify(userRepository).save(existingUser);
        }

        @Test
        @DisplayName("Should preserve user ID on update")
        void loadUser_withExistingUser_shouldPreserveUserId() {
            // Arrange
            UUID originalId = existingUser.getId();
            OAuth2UserRequest userRequest = createUserRequest("google");
            OAuth2User mockOAuth2User = createMockOAuth2User(googleAttributes);

            TestableOAuth2UserService testableService = new TestableOAuth2UserService(userRepository);
            testableService.setMockOAuth2User(mockOAuth2User);

            when(userRepository.findByProviderAndProviderId("google", "google-123456"))
                    .thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            OAuth2User result = testableService.loadUser(userRequest);

            // Assert
            UserPrincipal principal = (UserPrincipal) result;
            assertThat(principal.getId()).isEqualTo(originalId);
        }
    }

    @Nested
    @DisplayName("loadUser_withInvalidData")
    class InvalidDataTests {

        @Test
        @DisplayName("U-003: Should throw exception when email is missing")
        void loadUser_withMissingEmail_shouldThrowException() {
            // Arrange
            Map<String, Object> attributesWithoutEmail = new HashMap<>();
            attributesWithoutEmail.put("sub", "google-123456");
            attributesWithoutEmail.put("name", "Test User");

            OAuth2UserRequest userRequest = createUserRequest("google");
            OAuth2User mockOAuth2User = createMockOAuth2User(attributesWithoutEmail);

            TestableOAuth2UserService testableService = new TestableOAuth2UserService(userRepository);
            testableService.setMockOAuth2User(mockOAuth2User);

            // Act & Assert
            assertThatThrownBy(() -> testableService.loadUser(userRequest))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .hasMessageContaining("Email is required");

            // Verify no user was saved
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("U-004: Should use email prefix when name is missing")
        void loadUser_withMissingName_shouldUseEmailPrefix() {
            // Arrange
            Map<String, Object> attributesWithoutName = new HashMap<>();
            attributesWithoutName.put("sub", "google-789");
            attributesWithoutName.put("email", "testuser@example.com");

            OAuth2UserRequest userRequest = createUserRequest("google");
            OAuth2User mockOAuth2User = createMockOAuth2User(attributesWithoutName);

            TestableOAuth2UserService testableService = new TestableOAuth2UserService(userRepository);
            testableService.setMockOAuth2User(mockOAuth2User);

            when(userRepository.findByProviderAndProviderId("google", "google-789"))
                    .thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });

            // Act
            OAuth2User result = testableService.loadUser(userRequest);

            // Assert
            UserPrincipal principal = (UserPrincipal) result;
            assertThat(principal.getNickname()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("U-005: Should throw exception for unsupported provider")
        void loadUser_withUnsupportedProvider_shouldThrowException() {
            // Arrange
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("sub", "facebook-123");
            attributes.put("email", "test@example.com");
            attributes.put("name", "Test User");

            OAuth2UserRequest userRequest = createUserRequest("facebook");
            OAuth2User mockOAuth2User = createMockOAuth2User(attributes);

            TestableOAuth2UserService testableService = new TestableOAuth2UserService(userRepository);
            testableService.setMockOAuth2User(mockOAuth2User);

            // Act & Assert
            assertThatThrownBy(() -> testableService.loadUser(userRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported OAuth2 provider");
        }

        @Test
        @DisplayName("Should throw exception when email is blank")
        void loadUser_withBlankEmail_shouldThrowException() {
            // Arrange
            Map<String, Object> attributesWithBlankEmail = new HashMap<>();
            attributesWithBlankEmail.put("sub", "google-123456");
            attributesWithBlankEmail.put("email", "   ");
            attributesWithBlankEmail.put("name", "Test User");

            OAuth2UserRequest userRequest = createUserRequest("google");
            OAuth2User mockOAuth2User = createMockOAuth2User(attributesWithBlankEmail);

            TestableOAuth2UserService testableService = new TestableOAuth2UserService(userRepository);
            testableService.setMockOAuth2User(mockOAuth2User);

            // Act & Assert
            assertThatThrownBy(() -> testableService.loadUser(userRequest))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .hasMessageContaining("Email is required");
        }
    }

    // Helper methods

    private OAuth2UserRequest createUserRequest(String registrationId) {
        ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId(registrationId)
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-access-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        return new OAuth2UserRequest(clientRegistration, accessToken);
    }

    private OAuth2User createMockOAuth2User(Map<String, Object> attributes) {
        return new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                "sub"
        );
    }

    /**
     * Testable subclass that allows injecting mock OAuth2User.
     *
     * This approach is used because:
     * 1. super.loadUser() makes HTTP calls to the OAuth provider
     * 2. We cannot easily mock static parent class methods
     * 3. The production code processOAuth2User() is package-private
     *
     * The subclass only overrides loadUser() to inject the mock OAuth2User,
     * then delegates to the actual processOAuth2User() logic via a protected accessor.
     */
    static class TestableOAuth2UserService extends CustomOAuth2UserService {

        private OAuth2User mockOAuth2User;

        public TestableOAuth2UserService(UserRepository userRepository) {
            super(userRepository);
        }

        public void setMockOAuth2User(OAuth2User mockOAuth2User) {
            this.mockOAuth2User = mockOAuth2User;
        }

        @Override
        public OAuth2User loadUser(OAuth2UserRequest userRequest) {
            // Instead of calling super.loadUser() which makes HTTP calls,
            // use the injected mock OAuth2User and process it using reflection
            // to call the actual production processOAuth2User method
            if (mockOAuth2User == null) {
                return super.loadUser(userRequest);
            }

            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            Map<String, Object> attributes = mockOAuth2User.getAttributes();

            // Use reflection to call the private processOAuth2User method
            // This ensures we're testing the actual production code
            try {
                java.lang.reflect.Method method = CustomOAuth2UserService.class.getDeclaredMethod(
                        "processOAuth2User", String.class, Map.class);
                method.setAccessible(true);
                return (OAuth2User) method.invoke(this, registrationId, attributes);
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof OAuth2AuthenticationException) {
                    throw (OAuth2AuthenticationException) cause;
                }
                if (cause instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) cause;
                }
                throw new RuntimeException("Failed to invoke processOAuth2User", e);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke processOAuth2User", e);
            }
        }
    }
}
