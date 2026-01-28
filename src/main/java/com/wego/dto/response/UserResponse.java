package com.wego.dto.response;

import com.wego.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for user information responses.
 *
 * @contract
 *   - invariant: id, email, nickname are never null
 */
@Getter
@Builder
public class UserResponse {

    private final UUID id;
    private final String email;
    private final String nickname;
    private final String avatarUrl;
    private final String provider;
    private final Instant createdAt;

    /**
     * Creates a UserResponse from a User entity.
     *
     * @contract
     *   - pre: user != null
     *   - post: Returns UserResponse with all fields mapped
     *
     * @param user The user entity to convert
     * @return UserResponse DTO
     */
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .provider(user.getProvider())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
