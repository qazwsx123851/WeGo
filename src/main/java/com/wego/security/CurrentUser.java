package com.wego.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to inject the current authenticated user's UserPrincipal.
 *
 * Usage:
 * <pre>
 * {@code
 * @GetMapping("/profile")
 * public String profile(@CurrentUser UserPrincipal principal) {
 *     UUID userId = principal.getId();
 *     // ...
 * }
 * }
 * </pre>
 *
 * @contract
 *   - pre: User must be authenticated
 *   - post: Injects UserPrincipal from SecurityContext
 */
@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}
