package com.wego.controller.web;

import com.wego.dto.response.TodoResponse;
import com.wego.dto.response.TripResponse;
import com.wego.service.DemoDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Web controller for demo todos page (unauthenticated).
 *
 * <p>Mirrors the read-only portion of {@link TodoWebController#listTodos}.
 *
 * @contract
 *   - pre: No authentication required
 *   - post: Renders demo todos list with synthetic data
 *   - calls: DemoDataProvider
 */
@Controller
@RequestMapping("/demo/trip/todos")
@RequiredArgsConstructor
public class DemoTodoController {

    private final DemoDataProvider demoDataProvider;

    @GetMapping
    public String listDemoTodos(Model model) {
        TripResponse trip = demoDataProvider.getDemoTrip();
        List<TodoResponse> todos = demoDataProvider.getDemoTodos();

        long totalTodos = todos.size();
        long completedTodos = todos.stream().filter(t -> "COMPLETED".equals(t.getStatus().name())).count();
        long pendingTodos = todos.stream().filter(t -> "PENDING".equals(t.getStatus().name())).count();
        long inProgressTodos = todos.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus().name())).count();

        model.addAttribute("trip", trip);
        model.addAttribute("todos", todos);
        model.addAttribute("members", trip.getMembers());
        model.addAttribute("totalTodos", totalTodos);
        model.addAttribute("completedTodos", completedTodos);
        model.addAttribute("pendingTodos", pendingTodos);
        model.addAttribute("inProgressTodos", inProgressTodos);
        model.addAttribute("canEdit", true);
        model.addAttribute("isOwner", true);
        model.addAttribute("isDemo", true);
        model.addAttribute("name", "Demo User");
        model.addAttribute("picture", null);
        model.addAttribute("currentPath", "/demo/trip/todos");

        return "demo/todo/list";
    }
}
