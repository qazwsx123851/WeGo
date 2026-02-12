package com.wego.controller.web;

import com.wego.dto.response.InvitePageData;
import com.wego.entity.User;
import com.wego.exception.ValidationException;
import com.wego.service.InviteLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Web controller for invite link acceptance flow.
 *
 * @contract
 *   - pre: All endpoints require authentication (Spring Security handles redirect)
 *   - post: User is added to trip or shown appropriate error
 *   - calls: InviteLinkService
 */
@Controller
@RequestMapping("/invite")
@RequiredArgsConstructor
@Slf4j
public class InviteController extends BaseWebController {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");
    private static final int MAX_TOKEN_LENGTH = 64;

    private final InviteLinkService inviteLinkService;

    /**
     * Displays the invite acceptance page.
     *
     * @contract
     *   - pre: user is authenticated (enforced by Spring Security)
     *   - post: renders invite page with trip info, or error/redirect
     */
    @GetMapping("/{token}")
    public String showInvitePage(@PathVariable String token,
                                  @CurrentUser UserPrincipal principal,
                                  Model model) {
        if (!isValidTokenFormat(token)) {
            model.addAttribute("error", "邀請連結無效");
            return "trip/invite";
        }

        User user = getCurrentUser(principal);
        if (user == null) {
            model.addAttribute("error", "使用者帳號尚未建立，請重新登入");
            return "trip/invite";
        }

        InvitePageData pageData = inviteLinkService.getInvitePageData(token, user.getId());

        if (pageData.getError() != null) {
            model.addAttribute("error", pageData.getError());
            return "trip/invite";
        }

        if (pageData.isAlreadyMember()) {
            return "redirect:/trips/" + pageData.getTripId();
        }

        model.addAttribute("token", pageData.getToken());
        model.addAttribute("tripTitle", pageData.getTripTitle());
        model.addAttribute("tripId", pageData.getTripId());
        model.addAttribute("tripStartDate", pageData.getTripStartDate());
        model.addAttribute("tripEndDate", pageData.getTripEndDate());
        model.addAttribute("inviteRole", pageData.getInviteRole());
        model.addAttribute("expiresAt", pageData.getExpiresAt());
        model.addAttribute("memberCount", pageData.getMemberCount());
        model.addAttribute("expiresWithin24h", pageData.isExpiresWithin24h());

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
                                @CurrentUser UserPrincipal principal,
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
                Optional<UUID> tripId = inviteLinkService.findTripIdByToken(token);
                if (tripId.isPresent()) {
                    return "redirect:/trips/" + tripId.get();
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

}
