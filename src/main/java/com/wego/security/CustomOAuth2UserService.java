package com.wego.security;

import com.wego.entity.User;
import com.wego.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Custom OAuth2 user service that handles user registration and updates.
 *
 * This service is invoked by Spring Security during OAuth2 login flow.
 * It either creates a new user or updates an existing one based on the
 * OAuth provider's user information.
 *
 * @contract
 *   - pre: OAuth2UserRequest contains valid provider info
 *   - post: Returns UserPrincipal wrapping persisted User entity
 *   - calls: UserRepository#findByProviderAndProviderId, UserRepository#save
 *   - calledBy: Spring Security OAuth2 login flow
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /**
     * Loads user from OAuth2 provider and syncs with database.
     *
     * @contract
     *   - pre: userRequest != null, contains valid OAuth2 token
     *   - post: User is persisted/updated in database
     *   - post: Returns UserPrincipal with User entity and OAuth attributes
     *   - calls: DefaultOAuth2UserService#loadUser, processOAuth2User
     *
     * @param userRequest The OAuth2 user request from Spring Security
     * @return UserPrincipal wrapping the persisted User
     * @throws OAuth2AuthenticationException if authentication fails
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        return processOAuth2User(registrationId, attributes);
    }

    /**
     * Processes OAuth2 user data and creates/updates database user.
     *
     * @contract
     *   - pre: provider != null, attributes != null
     *   - pre: attributes contains required fields (sub, email, name)
     *   - post: User is created if new, or updated if existing
     *   - calls: UserRepository#findByProviderAndProviderId, UserRepository#save
     *
     * @param provider The OAuth2 provider (e.g., "google")
     * @param attributes The user attributes from the OAuth2 provider
     * @return UserPrincipal wrapping the User entity
     */
    private UserPrincipal processOAuth2User(String provider, Map<String, Object> attributes) {
        String providerId = extractProviderId(provider, attributes);
        String email = (String) attributes.get("email");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("invalid_user_info"),
                "Email is required for authentication"
            );
        }

        String name = (String) attributes.get("name");
        if (name == null || name.isBlank()) {
            name = email.split("@")[0];
        }
        String picture = (String) attributes.get("picture");

        log.debug("Processing OAuth2 user: provider={}, providerId={}, email={}",
                provider, providerId, maskEmail(email));

        Optional<User> existingUser = userRepository.findByProviderAndProviderId(provider, providerId);

        User user;
        if (existingUser.isPresent()) {
            user = updateExistingUser(existingUser.get(), name, picture);
            log.info("Updated existing user: id={}, email={}", user.getId(), user.getEmail());
        } else {
            user = createNewUser(provider, providerId, email, name, picture);
            log.info("Created new user: id={}, email={}", user.getId(), user.getEmail());
        }

        return new UserPrincipal(user, attributes);
    }

    /**
     * Extracts provider-specific user ID from attributes.
     *
     * @contract
     *   - pre: provider != null, attributes != null
     *   - post: Returns non-null provider ID
     *
     * @param provider The OAuth2 provider
     * @param attributes The user attributes
     * @return The provider-specific user ID
     */
    private String extractProviderId(String provider, Map<String, Object> attributes) {
        return switch (provider.toLowerCase()) {
            case "google" -> (String) attributes.get("sub");
            default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        };
    }

    /**
     * Updates an existing user's mutable fields.
     *
     * @contract
     *   - pre: user != null
     *   - post: User nickname and avatar are updated
     *   - post: Returns the same user instance (persisted)
     *
     * @param user The existing user to update
     * @param name The new nickname
     * @param picture The new avatar URL
     * @return The updated user
     */
    private User updateExistingUser(User user, String name, String picture) {
        user.setNickname(name);
        user.setAvatarUrl(picture);
        return userRepository.save(user);
    }

    /**
     * Creates a new user from OAuth2 data.
     *
     * @contract
     *   - pre: All parameters except picture are non-null
     *   - post: New user is persisted to database
     *   - post: Returns the persisted user with generated ID
     *
     * @param provider The OAuth2 provider
     * @param providerId The provider-specific user ID
     * @param email The user's email
     * @param name The user's display name
     * @param picture The user's avatar URL (may be null)
     * @return The newly created user
     */
    private User createNewUser(String provider, String providerId,
                                String email, String name, String picture) {
        User newUser = User.builder()
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .nickname(name)
                .avatarUrl(picture)
                .build();

        return userRepository.save(newUser);
    }

    /**
     * Masks email for privacy-safe logging.
     *
     * @param email The email to mask
     * @return Masked email (e.g., "t***@example.com")
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        if (parts[0].isEmpty()) {
            return "***@" + parts[1];
        }
        return parts[0].charAt(0) + "***@" + parts[1];
    }
}
