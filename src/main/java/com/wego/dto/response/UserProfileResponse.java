package com.wego.dto.response;

import com.wego.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for user profile page.
 *
 * @contract
 *   - Contains user info from OAuth and account statistics
 *   - Used in profile page display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID id;
    private String email;
    private String nickname;
    private String avatarUrl;
    private String provider;
    private Instant createdAt;

    // Statistics
    private long tripCount;
    private long documentsUploaded;
    private long expensesCreated;

    /**
     * Gets display name for OAuth provider.
     *
     * @return Formatted provider name
     */
    public String getProviderDisplayName() {
        if ("google".equalsIgnoreCase(provider)) {
            return "Google";
        }
        return provider != null ? provider : "Unknown";
    }

    /**
     * Gets account age in days.
     *
     * @return Number of days since account creation
     */
    public long getAccountAgeDays() {
        if (createdAt == null) {
            return 0;
        }
        return Duration.between(createdAt, Instant.now()).toDays();
    }

    /**
     * Creates a UserProfileResponse from a User entity.
     *
     * @param user The user entity
     * @param tripCount Number of trips user is member of
     * @param documentsUploaded Number of documents uploaded by user
     * @param expensesCreated Number of expenses created by user
     * @return UserProfileResponse DTO
     */
    public static UserProfileResponse from(User user, long tripCount,
                                            long documentsUploaded, long expensesCreated) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .provider(user.getProvider())
                .createdAt(user.getCreatedAt())
                .tripCount(tripCount)
                .documentsUploaded(documentsUploaded)
                .expensesCreated(expensesCreated)
                .build();
    }
}
