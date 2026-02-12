package com.wego.service;

import com.wego.dto.response.UserProfileResponse;
import com.wego.entity.User;
import com.wego.exception.ResourceNotFoundException;
import com.wego.exception.UnauthorizedException;
import com.wego.repository.DocumentRepository;
import com.wego.repository.ExpenseRepository;
import com.wego.repository.TripMemberRepository;
import com.wego.repository.UserRepository;
import com.wego.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for user-related operations.
 *
 * @contract
 *   - calledBy: Controllers, other services
 *   - calls: UserRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final TripMemberRepository tripMemberRepository;
    private final DocumentRepository documentRepository;
    private final ExpenseRepository expenseRepository;

    /**
     * Retrieves a user by their UUID.
     *
     * @contract
     *   - pre: id != null
     *   - post: Returns User if found
     *   - post: Throws ResourceNotFoundException if not found
     *   - calls: UserRepository#findById
     *   - calledBy: Controllers, other services
     *
     * @param id The user's UUID
     * @return The user entity
     * @throws ResourceNotFoundException if user not found
     */
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));
    }

    /**
     * Retrieves a user by their email address.
     *
     * @contract
     *   - pre: email != null
     *   - post: Returns User if found
     *   - post: Throws ResourceNotFoundException if not found
     *   - calls: UserRepository#findByEmail
     *
     * @param email The user's email address
     * @return The user entity
     * @throws ResourceNotFoundException if user not found
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }

    /**
     * Gets the currently authenticated user from SecurityContext.
     *
     * @contract
     *   - pre: User is authenticated (SecurityContext contains valid principal)
     *   - post: Returns the current User entity
     *   - post: Throws UnauthorizedException if not authenticated
     *   - calls: SecurityContextHolder#getContext
     *   - calledBy: Controllers, other services
     *
     * @return The currently authenticated user
     * @throws UnauthorizedException if no user is authenticated
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUser();
        }

        throw new UnauthorizedException("Invalid authentication principal");
    }

    /**
     * Gets the UUID of the currently authenticated user.
     *
     * @contract
     *   - pre: User is authenticated
     *   - post: Returns the current user's UUID
     *   - post: Throws UnauthorizedException if not authenticated
     *   - calls: getCurrentUser
     *   - calledBy: Controllers, other services
     *
     * @return The current user's UUID
     * @throws UnauthorizedException if no user is authenticated
     */
    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * Checks if a user exists with the given email.
     *
     * @contract
     *   - pre: email != null
     *   - post: Returns true if user exists, false otherwise
     *   - calls: UserRepository#existsByEmail
     *
     * @param email The email to check
     * @return true if a user with this email exists
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Gets the user profile with statistics.
     *
     * @contract
     *   - pre: user != null
     *   - post: Returns UserProfileResponse with trip, document, and expense counts
     *   - calls: TripMemberRepository#countByUserId, DocumentRepository#countByUploadedBy,
     *            ExpenseRepository#countByCreatedBy
     *   - calledBy: ProfileController#showProfile
     *
     * @param user The user entity
     * @return UserProfileResponse with statistics
     */
    public UserProfileResponse getUserProfile(User user) {
        long tripCount = tripMemberRepository.countByUserId(user.getId());
        long documentsUploaded = documentRepository.countByUploadedBy(user.getId());
        long expensesCreated = expenseRepository.countByCreatedBy(user.getId());

        return UserProfileResponse.from(user, tripCount, documentsUploaded, expensesCreated);
    }

    /**
     * Updates a user's nickname.
     *
     * @contract
     *   - pre: userId != null, nickname != null and not blank
     *   - post: User's nickname is updated
     *   - calls: UserRepository#findById, UserRepository#save
     *   - calledBy: ProfileController#updateProfile
     *
     * @param userId The user ID
     * @param nickname The new nickname
     * @throws ResourceNotFoundException if user not found
     */
    private static final int MAX_NICKNAME_LENGTH = 50;

    @Transactional
    public void updateNickname(UUID userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        String sanitized = nickname != null
                ? nickname.replaceAll("<[^>]*>", "").trim()
                : "";
        if (sanitized.length() > MAX_NICKNAME_LENGTH) {
            sanitized = sanitized.substring(0, MAX_NICKNAME_LENGTH);
        }

        user.setNickname(sanitized);
        userRepository.save(user);
        log.info("Updated nickname for user {}", userId);
    }
}
