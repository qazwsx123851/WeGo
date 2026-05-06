package com.wego.controller.web;

import com.wego.dto.response.TripResponse;
import com.wego.service.DemoDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

/**
 * Web controller for demo trip members page (unauthenticated).
 *
 * <p>Mirrors the read-only portion of {@link MemberWebController#listMembers}.
 *
 * @contract
 *   - pre: No authentication required
 *   - post: Renders demo members list with synthetic data
 *   - calls: DemoDataProvider
 */
@Controller
@RequestMapping("/demo/trip/members")
@RequiredArgsConstructor
public class DemoMemberController {

    private final DemoDataProvider demoDataProvider;

    @GetMapping
    public String listDemoMembers(Model model) {
        TripResponse trip = demoDataProvider.getDemoTrip();
        List<TripResponse.MemberSummary> members = trip.getMembers();
        UUID currentUserId = trip.getOwnerId(); // demo: present visitor as the owner

        model.addAttribute("trip", trip);
        model.addAttribute("members", members);
        model.addAttribute("memberCount", members != null ? members.size() : 0);
        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("canEdit", true);
        model.addAttribute("isOwner", true);
        model.addAttribute("canInvite", true);
        model.addAttribute("isDemo", true);
        model.addAttribute("name", "Demo User");
        model.addAttribute("picture", null);
        model.addAttribute("currentPath", "/demo/trip/members");
        // Demo: invite link is intentionally null — invite button intercepted by data-demo-cta
        model.addAttribute("inviteLink", null);
        model.addAttribute("inviteLinkExpiry", null);

        return "demo/trip/members";
    }
}
