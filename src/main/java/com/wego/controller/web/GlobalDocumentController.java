package com.wego.controller.web;

import com.wego.dto.DocumentFilter;
import com.wego.dto.response.GlobalDocumentOverviewResponse;
import com.wego.dto.response.TripSummary;
import com.wego.entity.User;
import com.wego.service.GlobalDocumentService;
import com.wego.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

/**
 * Controller for global document overview page.
 *
 * @contract
 *   - Handles /documents route for global document view
 *   - Requires authentication
 *   - Shows all documents across all trips with filtering
 *   - calledBy: Web browser requests, bottom navigation
 *   - calls: GlobalDocumentService, UserService
 */
@Controller
@RequestMapping("/documents")
@RequiredArgsConstructor
@Slf4j
public class GlobalDocumentController extends BaseWebController {

    private static final int PAGE_SIZE = 20;

    private final GlobalDocumentService globalDocumentService;

    /**
     * Shows the global document overview page.
     *
     * @contract
     *   - pre: user is authenticated
     *   - post: Returns document overview with all accessible documents
     *   - calls: GlobalDocumentService#getOverview
     *   - calledBy: Web browser request, bottom navigation
     *
     * @param principal The authenticated OAuth2 user
     * @param search Search term for filename/description
     * @param tripId Filter by specific trip
     * @param type File type filter (image, pdf, all)
     * @param page Page number (0-based)
     * @param model The model for view rendering
     * @return Template name or redirect
     */
    @GetMapping
    public String showDocumentOverview(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID tripId,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        // Validate page parameter
        int validPage = Math.max(0, page);

        DocumentFilter filter = DocumentFilter.builder()
                .search(search)
                .tripId(tripId)
                .fileType(type)
                .build();

        GlobalDocumentOverviewResponse overview = globalDocumentService.getOverview(
                user.getId(), filter, PageRequest.of(validPage, PAGE_SIZE));

        List<TripSummary> userTrips = globalDocumentService
                .getUserTripsWithDocuments(user.getId());

        model.addAttribute("overview", overview);
        model.addAttribute("userTrips", userTrips);
        model.addAttribute("filter", filter);
        model.addAttribute("currentPage", validPage);
        model.addAttribute("search", search);
        model.addAttribute("selectedTripId", tripId);
        model.addAttribute("selectedType", type);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        log.debug("Showing document overview for user {}: {} documents, page {}",
                user.getId(), overview.getTotalDocuments(), page);

        return "document/global-overview";
    }

}
