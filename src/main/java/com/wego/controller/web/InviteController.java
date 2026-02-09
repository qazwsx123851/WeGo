package com.wego.controller.web;

import com.wego.entity.InviteLink;
import com.wego.entity.Trip;
import com.wego.entity.User;
import com.wego.exception.ValidationException;
import com.wego.repository.InviteLinkRepository;
import com.wego.repository.TripMemberRepository;
import com.wego.repository.TripRepository;
import com.wego.service.InviteLinkService;
import com.wego.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Web controller for invite link acceptance flow.
 *
 * @contract
 *   - pre: All endpoints require authentication (Spring Security handles redirect)
 *   - post: User is added to trip or shown appropriate error
 *   - calls: InviteLinkService, InviteLinkRepository, TripRepository, TripMemberRepository
 */
@Controller
@RequestMapping("/invite")
@RequiredArgsConstructor
@Slf4j
public class InviteController {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");
    private static final int MAX_TOKEN_LENGTH = 64;

    private final InviteLinkRepository inviteLinkRepository;
    private final InviteLinkService inviteLinkService;
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserService userService;

    /**
     * Displays the invite acceptance page.
     *
     * @contract
     *   - pre: user is authenticated (enforced by Spring Security)
     *   - post: renders invite page with trip info, or error/redirect
     */
    @GetMapping("/{token}")
    public String showInvitePage(@PathVariable String token,
                                  @AuthenticationPrincipal OAuth2User principal,
                                  Model model) {
        // Validate token format
        if (!isValidTokenFormat(token)) {
            model.addAttribute("error", "邀請連結無效");
            return "trip/invite";
        }

        // Look up invite link
        Optional<InviteLink> optLink = inviteLinkRepository.findByToken(token);
        if (optLink.isEmpty()) {
            model.addAttribute("error", "邀請連結無效或已過期");
            return "trip/invite";
        }

        InviteLink link = optLink.get();

        // Check expiry
        if (link.isExpired()) {
            model.addAttribute("error", "邀請連結已過期");
            return "trip/invite";
        }

        // Get trip info
        Optional<Trip> optTrip = tripRepository.findById(link.getTripId());
        if (optTrip.isEmpty()) {
            model.addAttribute("error", "行程不存在");
            return "trip/invite";
        }

        Trip trip = optTrip.get();
        User user = getCurrentUser(principal);

        if (user == null) {
            model.addAttribute("error", "使用者帳號尚未建立，請重新登入");
            return "trip/invite";
        }

        // Check if already a member
        if (user != null && tripMemberRepository.existsByTripIdAndUserId(trip.getId(), user.getId())) {
            return "redirect:/trips/" + trip.getId();
        }

        // Populate model for invite page
        long memberCount = tripMemberRepository.countByTripId(trip.getId());
        boolean expiresWithin24h = link.getExpiresAt().isBefore(Instant.now().plus(24, ChronoUnit.HOURS));

        model.addAttribute("token", token);
        model.addAttribute("trip", trip);
        model.addAttribute("inviteRole", link.getRole().name());
        model.addAttribute("expiresAt", link.getExpiresAt().atZone(ZoneId.of("Asia/Taipei")));
        model.addAttribute("memberCount", memberCount);
        model.addAttribute("expiresWithin24h", expiresWithin24h);

        return "trip/invite";
    }

    /**
     * Accepts the invite and adds the user to the trip.
     *
     * @contract
     *   - pre: user is authenticated, token is valid
     *   - post: user added to trip, redirected to trip page
     */
    @PostMapping("/{token}/accept")
    public String acceptInvite(@PathVariable String token,
                                @AuthenticationPrincipal OAuth2User principal,
                                RedirectAttributes redirectAttributes) {
        if (!isValidTokenFormat(token)) {
            redirectAttributes.addFlashAttribute("error", "邀請連結無效");
            return "redirect:/dashboard";
        }

        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            UUID tripId = inviteLinkService.acceptInvite(token, user.getId());
            String tokenPreview = token.substring(0, Math.min(8, token.length())) + "...";
            log.info("User {} accepted invite {}  and joined trip {}", user.getId(), tokenPreview, tripId);
            redirectAttributes.addFlashAttribute("success", "已成功加入行程！");
            return "redirect:/trips/" + tripId;
        } catch (ValidationException e) {
            String errorCode = e.getErrorCode();
            String message = switch (errorCode) {
                case "INVALID_INVITE_LINK" -> "邀請連結無效或已過期";
                case "DUPLICATE_MEMBER" -> "您已經是此行程的成員";
                case "MEMBER_LIMIT_EXCEEDED" -> "行程成員已達上限";
                default -> "加入行程失敗：" + e.getMessage();
            };

            if ("DUPLICATE_MEMBER".equals(errorCode)) {
                // Try to find the trip to redirect
                Optional<InviteLink> optLink = inviteLinkRepository.findByToken(token);
                if (optLink.isPresent()) {
                    return "redirect:/trips/" + optLink.get().getTripId();
                }
            }

            redirectAttributes.addFlashAttribute("error", message);
            return "redirect:/invite/" + token;
        }
    }

    private boolean isValidTokenFormat(String token) {
        return token != null
                && token.length() <= MAX_TOKEN_LENGTH
                && TOKEN_PATTERN.matcher(token).matches();
    }

    private User getCurrentUser(OAuth2User principal) {
        if (principal == null) {
            return null;
        }
        String email = principal.getAttribute("email");
        return userService.getUserByEmail(email);
    }
}
