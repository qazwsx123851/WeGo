package com.wego.controller.web;

import com.wego.dto.response.InviteLinkResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.User;
import com.wego.service.InviteLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

/**
 * Controller for trip member-related web pages.
 *
 * @contract
 *   - Handles member list page for a specific trip
 *   - Requires authentication for all endpoints
 *   - calledBy: Web browser requests
 *   - calls: TripService, InviteLinkService
 */
@Controller
@RequestMapping("/trips/{tripId}/members")
@RequiredArgsConstructor
@Slf4j
public class MemberWebController extends BaseWebController {

    private final InviteLinkService inviteLinkService;

    /**
     * Show trip members page.
     *
     * @contract
     *   - pre: tripId != null, principal != null
     *   - post: Returns member list view with invite link info
     *   - calls: TripService#getTrip, TripService#getTripMembers, InviteLinkService#getActiveInviteLinks
     *   - calledBy: Web browser GET /trips/{tripId}/members
     */
    @GetMapping
    public String showMembersPage(@PathVariable UUID tripId,
                                   @CurrentUser UserPrincipal principal,
                                   Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip = loadTrip(tripId, user.getId());
        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        List<TripResponse.MemberSummary> members = tripService.getTripMembers(tripId, user.getId());
        TripResponse.MemberSummary currentMember = findCurrentMember(trip, user.getId());

        model.addAttribute("trip", trip);
        model.addAttribute("members", members);
        model.addAttribute("currentMember", currentMember);
        model.addAttribute("currentUserId", user.getId());
        model.addAttribute("isOwner", isOwner(currentMember));
        model.addAttribute("canEdit", canEdit(currentMember));
        model.addAttribute("memberCount", members.size());
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        // Invite link attributes
        model.addAttribute("canInvite", canEdit(currentMember));
        try {
            List<InviteLinkResponse> activeLinks = inviteLinkService.getActiveInviteLinks(tripId, user.getId());
            if (activeLinks != null && !activeLinks.isEmpty()) {
                InviteLinkResponse link = activeLinks.get(0);
                model.addAttribute("inviteLink", link.getInviteUrl());
                model.addAttribute("inviteLinkExpiry", link.getExpiresAt());
            } else {
                model.addAttribute("inviteLink", null);
                model.addAttribute("inviteLinkExpiry", null);
            }
        } catch (Exception e) {
            log.warn("Failed to load invite links for trip {}: {}", tripId, e.getMessage());
            model.addAttribute("inviteLink", null);
            model.addAttribute("inviteLinkExpiry", null);
        }

        return "trip/members";
    }
}
