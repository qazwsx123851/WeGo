package com.wego.controller.web;

import com.wego.dto.response.ActivityResponse;
import com.wego.dto.response.TripResponse;
import com.wego.exception.ResourceNotFoundException;
import com.wego.service.DemoDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

/**
 * Web controller for demo activity list and detail pages (unauthenticated).
 *
 * <p>Mirrors {@link ActivityWebController#listActivities} and
 * {@link ActivityWebController#showActivityDetail} structure for demo visitors.
 * All write actions are intercepted by {@code data-demo-cta} on the client side.
 *
 * @contract
 *   - pre: No authentication required
 *   - post: Renders demo activity list/detail with synthetic data
 *   - calls: DemoDataProvider
 */
@Controller
@RequestMapping("/demo/trip/activities")
@RequiredArgsConstructor
public class DemoActivityController {

    private final DemoDataProvider demoDataProvider;

    /**
     * Renders the demo activity list (timeline view grouped by date).
     *
     * @contract
     *   - pre: None (public endpoint)
     *   - post: Model populated with attributes matching real activity/list.html
     */
    @GetMapping
    public String listDemoActivities(Model model) {
        TripResponse trip = demoDataProvider.getDemoTrip();
        List<ActivityResponse> allActivities = demoDataProvider.getAllDemoActivities();

        model.addAttribute("trip", trip);
        model.addAttribute("dates", demoDataProvider.getDemoDates());
        model.addAttribute("activitiesByDate", demoDataProvider.getDemoActivitiesByDate());
        model.addAttribute("allActivities", allActivities);
        model.addAttribute("totalActivityCount", allActivities.size());
        model.addAttribute("currentDay", 1);
        model.addAttribute("canEdit", true);
        model.addAttribute("isOwner", true);
        model.addAttribute("isDemo", true);
        model.addAttribute("name", "Demo User");
        model.addAttribute("picture", null);
        model.addAttribute("currentPath", "/demo/trip/activities");

        return "demo/activity/list";
    }

    /**
     * Renders the demo activity detail page.
     *
     * @contract
     *   - pre: activityId is a valid UUID provided by DemoDataProvider
     *   - post: Model populated with activity, trip, nextActivity, related expenses
     *   - throws: ResourceNotFoundException → 404 if id not found
     */
    @GetMapping("/{activityId}")
    public String showDemoActivityDetail(@PathVariable UUID activityId, Model model) {
        TripResponse trip = demoDataProvider.getDemoTrip();
        ActivityResponse activity = demoDataProvider.getActivity(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", activityId.toString()));

        // Find next activity (same day, next ordering) for transit info display
        List<ActivityResponse> sameDay = demoDataProvider.getAllDemoActivities().stream()
                .filter(a -> a.getDay() == activity.getDay())
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .toList();
        ActivityResponse nextActivity = null;
        for (int i = 0; i < sameDay.size() - 1; i++) {
            if (activityId.equals(sameDay.get(i).getId())) {
                nextActivity = sameDay.get(i + 1);
                break;
            }
        }

        model.addAttribute("trip", trip);
        model.addAttribute("activity", activity);
        model.addAttribute("nextActivity", nextActivity);
        model.addAttribute("canEdit", true);
        model.addAttribute("isOwner", true);
        model.addAttribute("isDemo", true);
        model.addAttribute("name", "Demo User");
        model.addAttribute("picture", null);
        model.addAttribute("currentPath", "/demo/trip/activities");
        model.addAttribute("relatedExpenses", List.of());

        return "demo/activity/detail";
    }
}
