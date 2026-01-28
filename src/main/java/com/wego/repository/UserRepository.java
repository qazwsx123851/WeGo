package com.wego.repository;

import com.wego.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity operations.
 *
 * @contract
 *   - pre: All parameters must be non-null unless marked Optional
 *   - post: Returns Optional.empty() when no match found
 *   - calledBy: UserService, CustomOAuth2UserService
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their OAuth provider and provider-specific ID.
     *
     * @contract
     *   - pre: provider != null, providerId != null
     *   - post: Returns Optional containing user if found, empty otherwise
     *   - calledBy: CustomOAuth2UserService#loadUser
     *
     * @param provider The OAuth provider (e.g., "google")
     * @param providerId The provider-specific user ID
     * @return Optional containing the user if found
     */
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    /**
     * Finds a user by their email address.
     *
     * @contract
     *   - pre: email != null
     *   - post: Returns Optional containing user if found, empty otherwise
     *   - calledBy: UserService
     *
     * @param email The email address to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user exists with the given email.
     *
     * @contract
     *   - pre: email != null
     *   - post: Returns true if user exists, false otherwise
     *
     * @param email The email address to check
     * @return true if a user with this email exists
     */
    boolean existsByEmail(String email);
}
