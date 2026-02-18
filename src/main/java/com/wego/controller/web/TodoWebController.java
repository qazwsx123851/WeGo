package com.wego.controller.web;

import com.wego.dto.response.TodoResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.TodoStatus;
import com.wego.entity.User;
import com.wego.service.TodoService;
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
public class TodoWebController extends BaseWebController {

    private final TodoService todoService;

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

        // Get todos
        List<TodoResponse> todos = todoService.getTodosByTrip(tripId, user.getId());

        // Get stats
        Map<TodoStatus, Long> stats = todoService.getTodoStats(tripId, user.getId());
        long totalTodos = stats.values().stream().mapToLong(Long::longValue).sum();
        long completedTodos = stats.getOrDefault(TodoStatus.COMPLETED, 0L);
        long pendingTodos = stats.getOrDefault(TodoStatus.PENDING, 0L);
        long inProgressTodos = stats.getOrDefault(TodoStatus.IN_PROGRESS, 0L);

        // Find current member's role
        TripResponse.MemberSummary currentMember = findCurrentMember(trip, user.getId());
        boolean canEdit = canEdit(currentMember);

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

}
