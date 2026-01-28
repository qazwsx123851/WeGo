package com.wego.security;

import com.wego.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Custom OAuth2User implementation that wraps our User entity.
 *
 * This allows us to access our domain User directly from the security context
 * while still implementing the OAuth2User interface required by Spring Security.
 *
 * @contract
 *   - invariant: user is never null
 *   - invariant: attributes is never null (may be empty)
 */
@Getter
public class UserPrincipal implements OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;

    /**
     * Creates a UserPrincipal with User entity and OAuth attributes.
     *
     * @contract
     *   - pre: user != null, attributes != null
     *   - post: UserPrincipal is created with given values
     */
    public UserPrincipal(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    /**
     * Creates a UserPrincipal with User entity only (no OAuth attributes).
     *
     * @contract
     *   - pre: user != null
     *   - post: UserPrincipal is created with empty attributes
     */
    public UserPrincipal(User user) {
        this(user, Collections.emptyMap());
    }

    /**
     * Returns the user's UUID.
     *
     * @contract
     *   - post: Returns non-null UUID
     */
    public UUID getId() {
        return user.getId();
    }

    /**
     * Returns the user's email.
     *
     * @contract
     *   - post: Returns non-null email
     */
    public String getEmail() {
        return user.getEmail();
    }

    /**
     * Returns the user's nickname.
     *
     * @contract
     *   - post: Returns non-null nickname
     */
    public String getNickname() {
        return user.getNickname();
    }

    /**
     * Returns the user's avatar URL.
     *
     * @contract
     *   - post: May return null if no avatar set
     */
    public String getAvatarUrl() {
        return user.getAvatarUrl();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getName() {
        return user.getId().toString();
    }
}
