package com.wego.controller.web;

import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for public pages and dashboard.
 *
 * @contract
 *   - calledBy: Browser requests
 */
@Controller
public class HomeController {

    /**
     * Landing page for unauthenticated users.
     *
     * @contract
     *   - post: Returns landing page view
     */
    @GetMapping("/")
    public String home() {
        return "index";
    }

    /**
     * Dashboard page for authenticated users.
     *
     * @contract
     *   - pre: User is authenticated via OAuth2 (enforced by SecurityConfig)
     *   - post: Returns dashboard view with user info from database
     *   - calls: UserPrincipal methods
     */
    @GetMapping("/dashboard")
    public String dashboard(@CurrentUser UserPrincipal principal, Model model) {
        // Principal is guaranteed non-null by SecurityConfig
        // which requires authentication for this endpoint
        model.addAttribute("userId", principal.getId());
        model.addAttribute("name", principal.getNickname());
        model.addAttribute("email", principal.getEmail());
        model.addAttribute("picture", principal.getAvatarUrl());
        return "dashboard";
    }
}
