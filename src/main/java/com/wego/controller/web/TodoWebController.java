package com.wego.controller.web;

import com.wego.dto.response.TodoResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.Role;
import com.wego.entity.TodoStatus;
import com.wego.entity.User;
import com.wego.service.TodoService;
import com.wego.service.TripService;
import com.wego.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for Todo-related web pages.
 *
 * @contract
 *   - Handles todo list page for trips
 *   - Requires authentication for all endpoints
 *   - Validates user has permission to view trip todos
 *   - calledBy: Web browser requests
 *   - calls: TodoService, TripService, UserService
 */
@Controller
@RequestMapping("/trips/{tripId}/todos")
@RequiredArgsConstructor
@Slf4j
public class TodoWebController {

    private final TodoService todoService;
    private final TripService tripService;
    private final UserService userService;

    /**
     * List all todos for a trip.
     *
     * @contract
     *   - pre: user is authenticated
     *   - pre: user has view permission on the trip
     *   - post: returns todo/list template with todos, trip, members, and stats
     *   - calls: TodoService#getTodosByTrip, TodoService#getTodoStats, TripService#getTrip
     *   - calledBy: GET /trips/{tripId}/todos
     *
     * @param tripId The trip ID
     * @param principal The authenticated user
     * @param model The view model
     * @return The todo list template view name
     */
    @GetMapping
    public String listTodos(@PathVariable UUID tripId,
                           @AuthenticationPrincipal OAuth2User principal,
                           Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        TripResponse trip;
        try {
            trip = tripService.getTrip(tripId, user.getId());
        } catch (Exception e) {
            log.warn("Failed to get trip {}: {}", tripId, e.getMessage());
            return "redirect:/dashboard?error=trip_not_found";
        }

        if (trip == null) {
            return "redirect:/dashboard?error=trip_not_found";
        }

        // Get todos
        List<TodoResponse> todos = todoService.getTodosByTrip(tripId, user.getId());

        // Get stats
        Map<TodoStatus, Long> stats = todoService.getTodoStats(tripId, user.getId());
        long totalTodos = stats.values().stream().mapToLong(Long::longValue).sum();
        long completedTodos = stats.getOrDefault(TodoStatus.COMPLETED, 0L);
        long pendingTodos = stats.getOrDefault(TodoStatus.PENDING, 0L);
        long inProgressTodos = stats.getOrDefault(TodoStatus.IN_PROGRESS, 0L);

        // Find current member's role
        TripResponse.MemberSummary currentMember = trip.getMembers().stream()
                .filter(m -> m.getUserId().equals(user.getId()))
                .findFirst()
                .orElse(null);

        boolean canEdit = currentMember != null &&
                (currentMember.getRole() == Role.OWNER ||
                 currentMember.getRole() == Role.EDITOR);

        // Add data to model
        model.addAttribute("trip", trip);
        model.addAttribute("todos", todos);
        model.addAttribute("members", trip.getMembers());
        model.addAttribute("totalTodos", totalTodos);
        model.addAttribute("completedTodos", completedTodos);
        model.addAttribute("pendingTodos", pendingTodos);
        model.addAttribute("inProgressTodos", inProgressTodos);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("currentMember", currentMember);
        model.addAttribute("name", user.getNickname());
        model.addAttribute("picture", user.getAvatarUrl());

        return "todo/list";
    }

    /**
     * Gets the current user from OAuth2 principal.
     *
     * @param principal The OAuth2 user principal
     * @return The user entity or null if not found
     */
    private User getCurrentUser(OAuth2User principal) {
        if (principal == null) {
            return null;
        }
        String email = principal.getAttribute("email");
        try {
            return userService.getUserByEmail(email);
        } catch (Exception e) {
            log.warn("Failed to get user by email {}: {}", email, e.getMessage());
            return null;
        }
    }
}
