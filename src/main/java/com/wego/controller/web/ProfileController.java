package com.wego.controller.web;

import com.wego.dto.response.UserProfileResponse;
import com.wego.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for user profile pages.
 *
 * @contract
 *   - Handles /profile routes for profile view and edit
 *   - Requires authentication
 *   - calledBy: Web browser requests, header avatar link, bottom navigation
 *   - calls: UserService
 */
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController extends BaseWebController {

    /**
     * Shows the user profile page.
     *
     * @contract
     *   - pre: user is authenticated
     *   - post: Returns profile page with user info and statistics
     *   - calls: UserService#getUserProfile
     *   - calledBy: Web browser request, header avatar link, bottom navigation
     *
     * @param principal The authenticated OAuth2 user
     * @param model The model for view rendering
     * @return Template name or redirect
     */
    @GetMapping
    public String showProfile(@CurrentUser UserPrincipal principal, Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        UserProfileResponse profile = userService.getUserProfile(user);

        model.addAttribute("profile", profile);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        return "profile/index";
    }

    /**
     * Shows the profile edit form.
     *
     * @contract
     *   - pre: user is authenticated
     *   - post: Returns profile edit form
     *   - calledBy: Profile page edit button
     *
     * @param principal The authenticated OAuth2 user
     * @param model The model for view rendering
     * @return Template name or redirect
     */
    @GetMapping("/edit")
    public String showEditForm(@CurrentUser UserPrincipal principal, Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        return "profile/edit";
    }

    /**
     * Handles profile update form submission.
     *
     * @contract
     *   - pre: user is authenticated
     *   - pre: nickname is not blank and <= 50 chars
     *   - post: User profile is updated
     *   - calls: UserService#updateNickname
     *   - calledBy: Profile edit form submission
     *
     * @param nickname The new nickname
     * @param principal The authenticated OAuth2 user
     * @param model The model for view rendering
     * @param redirectAttributes For flash messages
     * @return Redirect to profile or edit form with errors
     */
    @PostMapping("/edit")
    public String updateProfile(@RequestParam String nickname,
                                 @CurrentUser UserPrincipal principal,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        // Validate nickname
        if (nickname == null || nickname.trim().isEmpty()) {
            model.addAttribute("error", "暱稱不可為空");
            model.addAttribute("user", user);
            model.addAttribute("name", user.getNickname());
            model.addAttribute("picture", user.getAvatarUrl());
            return "profile/edit";
        }

        String trimmedNickname = nickname.trim();
        if (trimmedNickname.length() > 50) {
            model.addAttribute("error", "暱稱不可超過 50 字");
            model.addAttribute("user", user);
            model.addAttribute("name", user.getNickname());
            model.addAttribute("picture", user.getAvatarUrl());
            return "profile/edit";
        }

        userService.updateNickname(user.getId(), trimmedNickname);
        log.info("User {} updated nickname to: {}", user.getId(), trimmedNickname);

        return "redirect:/profile?success=profile_updated";
    }

}
