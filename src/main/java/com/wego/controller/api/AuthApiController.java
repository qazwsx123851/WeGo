package com.wego.controller.api;

import com.wego.dto.ApiResponse;
import com.wego.dto.response.UserResponse;
import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API controller for authentication-related operations.
 *
 * @contract
 *   - pre: All endpoints except /api/auth/me require authentication
 *   - calledBy: Frontend API calls
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthApiController {

    /**
     * Returns the currently authenticated user's information.
     *
     * @contract
     *   - pre: User is authenticated
     *   - post: Returns UserResponse wrapped in ApiResponse
     *   - post: Returns 401 if not authenticated
     *
     * @param principal The current user principal injected by Spring Security
     * @return ApiResponse containing UserResponse
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(@CurrentUser UserPrincipal principal) {
        log.debug("Getting current user info for: {}", principal.getEmail());

        UserResponse userResponse = UserResponse.from(principal.getUser());
        return ApiResponse.success(userResponse);
    }

    /**
     * Logs out the current user via API.
     *
     * This endpoint provides an API-based logout mechanism that:
     * - Invalidates the HTTP session
     * - Clears the SecurityContext
     * - Deletes authentication cookies
     *
     * @contract
     *   - pre: User is authenticated
     *   - post: Session is invalidated
     *   - post: SecurityContext is cleared
     *   - post: Returns success message
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @return ApiResponse with logout confirmation
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            log.info("User logged out successfully");
        }

        return ApiResponse.success(null, "Logged out successfully");
    }
}
