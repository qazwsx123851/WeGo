package com.wego.controller.web;

import com.wego.dto.response.TripResponse;
import com.wego.entity.User;
import com.wego.service.ActivityService;
import com.wego.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Controller for trip document-related web pages.
 *
 * @contract
 *   - Handles document list and upload form for a specific trip
 *   - Requires authentication for all endpoints
 *   - calledBy: Web browser requests
 *   - calls: TripService, DocumentService, ActivityService
 */
@Controller
@RequestMapping("/trips/{tripId}/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentWebController extends BaseWebController {

    private final DocumentService documentService;
    private final ActivityService activityService;

    /**
     * Show trip documents page.
     *
     * @contract
     *   - pre: tripId != null, principal != null
     *   - post: Returns document list view
     *   - calls: TripService#getTrip, DocumentService#getDocumentsByTrip
     *   - calledBy: Web browser GET /trips/{tripId}/documents
     */
    @GetMapping
    public String showDocuments(@PathVariable UUID tripId,
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

        // Get documents for the trip (with signed URLs for direct CDN thumbnail loading)
        var documents = documentService.getDocumentsByTrip(tripId, user.getId(), true);

        model.addAttribute("trip", trip);
        model.addAttribute("tripId", tripId);
        model.addAttribute("documents", documents);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        return "document/list";
    }

    /**
     * Show document upload form.
     *
     * @contract
     *   - pre: tripId != null, principal != null
     *   - post: Returns document upload form view
     *   - calls: TripService#getTrip, ActivityService#getActivitiesByTrip
     *   - calledBy: Web browser GET /trips/{tripId}/documents/new
     */
    @GetMapping("/new")
    public String showDocumentUploadForm(@PathVariable UUID tripId,
                                          @RequestParam(required = false) UUID activityId,
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

        var activities = activityService.getActivitiesByTrip(tripId, user.getId());

        model.addAttribute("trip", trip);
        model.addAttribute("tripId", tripId);
        model.addAttribute("activities", activities);
        model.addAttribute("activityId", activityId);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        return "document/upload";
    }
}
