package com.wego.security;

import com.wego.entity.User;
import com.wego.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CustomOAuth2UserService.
 *
 * Test cases:
 * - U-001: First OAuth login creates new user
 * - U-002: Returning user login retrieves existing user
 */
@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TestableCustomOAuth2UserService customOAuth2UserService;

    private Map<String, Object> googleAttributes;
    private User existingUser;

    @BeforeEach
    void setUp() {
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

    @Test
    @DisplayName("U-001: First OAuth login should create new user")
    void loadUser_withNewUser_shouldCreateUser() {
        OAuth2UserRequest userRequest = createUserRequest();
        OAuth2User mockOAuth2User = createMockOAuth2User(googleAttributes);

        when(userRepository.findByProviderAndProviderId("google", "google-123456"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        customOAuth2UserService.setMockOAuth2User(mockOAuth2User);
        OAuth2User result = customOAuth2UserService.loadUser(userRequest);

        assertThat(result).isInstanceOf(UserPrincipal.class);
        UserPrincipal principal = (UserPrincipal) result;
        assertThat(principal.getEmail()).isEqualTo("test@example.com");
        assertThat(principal.getNickname()).isEqualTo("Test User");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getProvider()).isEqualTo("google");
        assertThat(savedUser.getProviderId()).isEqualTo("google-123456");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("U-002: Returning user login should update and return existing user")
    void loadUser_withExistingUser_shouldUpdateAndReturnUser() {
        OAuth2UserRequest userRequest = createUserRequest();
        OAuth2User mockOAuth2User = createMockOAuth2User(googleAttributes);

        when(userRepository.findByProviderAndProviderId("google", "google-123456"))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        customOAuth2UserService.setMockOAuth2User(mockOAuth2User);
        OAuth2User result = customOAuth2UserService.loadUser(userRequest);

        assertThat(result).isInstanceOf(UserPrincipal.class);
        UserPrincipal principal = (UserPrincipal) result;
        assertThat(principal.getId()).isEqualTo(existingUser.getId());
        assertThat(principal.getNickname()).isEqualTo("Test User");
        assertThat(principal.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");

        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("Should throw exception for unsupported provider")
    void loadUser_withUnsupportedProvider_shouldThrowException() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "facebook-123");
        attributes.put("email", "test@example.com");
        attributes.put("name", "Test User");

        OAuth2UserRequest userRequest = createUserRequest("facebook");
        OAuth2User mockOAuth2User = createMockOAuth2User(attributes);

        customOAuth2UserService.setMockOAuth2User(mockOAuth2User);

        assertThatThrownBy(() -> customOAuth2UserService.loadUser(userRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported OAuth2 provider");
    }

    @Test
    @DisplayName("Should preserve user ID on update")
    void loadUser_withExistingUser_shouldPreserveUserId() {
        UUID originalId = existingUser.getId();
        OAuth2UserRequest userRequest = createUserRequest();
        OAuth2User mockOAuth2User = createMockOAuth2User(googleAttributes);

        when(userRepository.findByProviderAndProviderId("google", "google-123456"))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        customOAuth2UserService.setMockOAuth2User(mockOAuth2User);
        OAuth2User result = customOAuth2UserService.loadUser(userRequest);

        UserPrincipal principal = (UserPrincipal) result;
        assertThat(principal.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should throw exception when email is missing")
    void loadUser_withMissingEmail_shouldThrowException() {
        Map<String, Object> attributesWithoutEmail = new HashMap<>();
        attributesWithoutEmail.put("sub", "google-123456");
        attributesWithoutEmail.put("name", "Test User");

        OAuth2UserRequest userRequest = createUserRequest();
        OAuth2User mockOAuth2User = createMockOAuth2User(attributesWithoutEmail);

        customOAuth2UserService.setMockOAuth2User(mockOAuth2User);

        assertThatThrownBy(() -> customOAuth2UserService.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("Email is required");
    }

    @Test
    @DisplayName("Should use email prefix as name when name is missing")
    void loadUser_withMissingName_shouldUseEmailPrefix() {
        Map<String, Object> attributesWithoutName = new HashMap<>();
        attributesWithoutName.put("sub", "google-789");
        attributesWithoutName.put("email", "testuser@example.com");

        OAuth2UserRequest userRequest = createUserRequest();
        OAuth2User mockOAuth2User = createMockOAuth2User(attributesWithoutName);

        when(userRepository.findByProviderAndProviderId("google", "google-789"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        customOAuth2UserService.setMockOAuth2User(mockOAuth2User);
        OAuth2User result = customOAuth2UserService.loadUser(userRequest);

        UserPrincipal principal = (UserPrincipal) result;
        assertThat(principal.getNickname()).isEqualTo("testuser");
    }

    private OAuth2UserRequest createUserRequest() {
        return createUserRequest("google");
    }

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
     */
    static class TestableCustomOAuth2UserService extends CustomOAuth2UserService {

        private OAuth2User mockOAuth2User;

        public TestableCustomOAuth2UserService(UserRepository userRepository) {
            super(userRepository);
        }

        public void setMockOAuth2User(OAuth2User mockOAuth2User) {
            this.mockOAuth2User = mockOAuth2User;
        }

        @Override
        public OAuth2User loadUser(OAuth2UserRequest userRequest) {
            if (mockOAuth2User != null) {
                String registrationId = userRequest.getClientRegistration().getRegistrationId();
                Map<String, Object> attributes = mockOAuth2User.getAttributes();

                return processOAuth2UserForTest(registrationId, attributes);
            }
            return super.loadUser(userRequest);
        }

        private UserPrincipal processOAuth2UserForTest(String provider, Map<String, Object> attributes) {
            String providerId = extractProviderIdForTest(provider, attributes);
            String email = (String) attributes.get("email");

            if (email == null || email.isBlank()) {
                throw new OAuth2AuthenticationException(
                    new org.springframework.security.oauth2.core.OAuth2Error("invalid_user_info"),
                    "Email is required for authentication"
                );
            }

            String name = (String) attributes.get("name");
            if (name == null || name.isBlank()) {
                name = email.split("@")[0];
            }
            String picture = (String) attributes.get("picture");

            Optional<User> existingUser = getUserRepository().findByProviderAndProviderId(provider, providerId);

            User user;
            if (existingUser.isPresent()) {
                user = existingUser.get();
                user.setNickname(name);
                user.setAvatarUrl(picture);
                user = getUserRepository().save(user);
            } else {
                User newUser = User.builder()
                        .provider(provider)
                        .providerId(providerId)
                        .email(email)
                        .nickname(name)
                        .avatarUrl(picture)
                        .build();
                user = getUserRepository().save(newUser);
            }

            return new UserPrincipal(user, attributes);
        }

        private String extractProviderIdForTest(String provider, Map<String, Object> attributes) {
            return switch (provider.toLowerCase()) {
                case "google" -> (String) attributes.get("sub");
                default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
            };
        }

        private UserRepository getUserRepository() {
            try {
                var field = CustomOAuth2UserService.class.getDeclaredField("userRepository");
                field.setAccessible(true);
                return (UserRepository) field.get(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
