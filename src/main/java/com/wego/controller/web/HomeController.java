package com.wego.controller.web;

import com.wego.dto.response.TripResponse;
import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
import com.wego.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for public pages and dashboard.
 *
 * @contract
 *   - calledBy: Browser requests
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final TripService tripService;

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
     *   - post: Returns dashboard view with user info, trips, and upcomingTrips
     *   - calls: UserPrincipal methods, TripService#getUserTrips
     */
    @GetMapping("/dashboard")
    public String dashboard(@CurrentUser UserPrincipal principal, Model model) {
        // User info
        model.addAttribute("userId", principal.getId());
        model.addAttribute("name", principal.getNickname());
        model.addAttribute("email", principal.getEmail());
        model.addAttribute("picture", principal.getAvatarUrl());

        // Dynamic greeting based on time of day
        model.addAttribute("greeting", getGreeting());

        // Fetch user's trips
        Page<TripResponse> tripPage = tripService.getUserTrips(
                principal.getId(),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "startDate"))
        );
        List<TripResponse> allTrips = tripPage.getContent();

        // Calculate daysUntil for each trip
        LocalDate today = LocalDate.now();
        allTrips.forEach(trip -> {
            if (trip.getStartDate() != null && !trip.getStartDate().isBefore(today)) {
                trip.setDaysUntil(ChronoUnit.DAYS.between(today, trip.getStartDate()));
            }
        });
        model.addAttribute("trips", allTrips);

        // Filter upcoming trips (today to next 30 days)
        List<TripResponse> upcomingTrips = allTrips.stream()
                .filter(t -> t.getStartDate() != null
                        && !t.getStartDate().isBefore(today)
                        && t.getStartDate().isBefore(today.plusDays(30)))
                .collect(Collectors.toList());
        model.addAttribute("upcomingTrips", upcomingTrips);

        return "dashboard";
    }

    /**
     * Returns a greeting based on the current time of day.
     *
     * @return "早安" (morning), "午安" (afternoon), or "晚安" (evening)
     */
    private String getGreeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) {
            return "早安";
        } else if (hour < 18) {
            return "午安";
        }
        return "晚安";
    }
}
