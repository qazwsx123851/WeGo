package com.wego.controller.api;

import com.wego.dto.ApiResponse;
import com.wego.dto.request.CreateTodoRequest;
import com.wego.dto.request.UpdateTodoRequest;
import com.wego.dto.response.TodoResponse;
import com.wego.entity.TodoStatus;
import com.wego.service.TodoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wego.exception.UnauthorizedException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for todo operations.
 *
 * @contract
 *   - All endpoints require authentication
 *   - Returns ApiResponse wrapper for all responses
 *   - Validates request bodies
 *
 * @see TodoService
 */
@Slf4j
@RestController
@RequestMapping("/api/trips/{tripId}/todos")
@RequiredArgsConstructor
public class TodoApiController {

    private final TodoService todoService;

    /**
     * Creates a new todo for a trip.
     *
     * @contract
     *   - pre: user is authenticated
     *   - pre: request is valid
     *   - post: returns 201 with created todo
     *   - calls: TodoService#createTodo
     *
     * POST /api/trips/{tripId}/todos
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TodoResponse>> createTodo(
            @PathVariable UUID tripId,
            @Valid @RequestBody CreateTodoRequest request,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("POST /api/trips/{}/todos by user {}", tripId, userId);

        TodoResponse response = todoService.createTodo(tripId, request, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Todo created successfully"));
    }

    /**
     * Gets all todos for a trip.
     *
     * Returns todos sorted by:
     * 1. Incomplete todos first, ordered by due_date ASC (nulls last)
     * 2. Completed todos at bottom, ordered by completed_at DESC
     *
     * @contract
     *   - pre: user is authenticated
     *   - post: returns 200 with list of todos
     *   - calls: TodoService#getTodosByTrip
     *
     * GET /api/trips/{tripId}/todos
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TodoResponse>>> getTodosByTrip(
            @PathVariable UUID tripId,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("GET /api/trips/{}/todos by user {}", tripId, userId);

        List<TodoResponse> todos = todoService.getTodosByTrip(tripId, userId);

        return ResponseEntity.ok(ApiResponse.success(todos));
    }

    /**
     * Gets a single todo by ID.
     *
     * @contract
     *   - pre: user is authenticated
     *   - post: returns 200 with todo
     *   - calls: TodoService#getTodo
     *
     * GET /api/trips/{tripId}/todos/{todoId}
     */
    @GetMapping("/{todoId}")
    public ResponseEntity<ApiResponse<TodoResponse>> getTodo(
            @PathVariable UUID tripId,
            @PathVariable UUID todoId,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("GET /api/trips/{}/todos/{} by user {}", tripId, todoId, userId);

        TodoResponse todo = todoService.getTodo(tripId, todoId, userId);

        return ResponseEntity.ok(ApiResponse.success(todo));
    }

    /**
     * Updates a todo.
     *
     * @contract
     *   - pre: user is authenticated
     *   - pre: request is valid
     *   - post: returns 200 with updated todo
     *   - calls: TodoService#updateTodo
     *
     * PUT /api/trips/{tripId}/todos/{todoId}
     */
    @PutMapping("/{todoId}")
    public ResponseEntity<ApiResponse<TodoResponse>> updateTodo(
            @PathVariable UUID tripId,
            @PathVariable UUID todoId,
            @Valid @RequestBody UpdateTodoRequest request,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("PUT /api/trips/{}/todos/{} by user {}", tripId, todoId, userId);

        TodoResponse response = todoService.updateTodo(tripId, todoId, request, userId);

        return ResponseEntity.ok(ApiResponse.success(response, "Todo updated successfully"));
    }

    /**
     * Deletes a todo.
     *
     * @contract
     *   - pre: user is authenticated
     *   - post: returns 200 on success
     *   - calls: TodoService#deleteTodo
     *
     * DELETE /api/trips/{tripId}/todos/{todoId}
     */
    @DeleteMapping("/{todoId}")
    public ResponseEntity<ApiResponse<Void>> deleteTodo(
            @PathVariable UUID tripId,
            @PathVariable UUID todoId,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("DELETE /api/trips/{}/todos/{} by user {}", tripId, todoId, userId);

        todoService.deleteTodo(tripId, todoId, userId);

        return ResponseEntity.ok(ApiResponse.success(null, "Todo deleted successfully"));
    }

    /**
     * Gets todo statistics for a trip.
     *
     * @contract
     *   - pre: user is authenticated
     *   - post: returns 200 with status counts
     *   - calls: TodoService#getTodoStats
     *
     * GET /api/trips/{tripId}/todos/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<TodoStatus, Long>>> getTodoStats(
            @PathVariable UUID tripId,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("GET /api/trips/{}/todos/stats by user {}", tripId, userId);

        Map<TodoStatus, Long> stats = todoService.getTodoStats(tripId, userId);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Extracts user ID from the OAuth2 principal or throws UnauthorizedException if not authenticated.
     *
     * @contract
     *   - pre: principal != null
     *   - post: returns valid user UUID
     *   - throws: UnauthorizedException if principal is null
     */
    private UUID getCurrentUserId(OAuth2User principal) {
        if (principal == null) {
            throw new UnauthorizedException("認證已過期，請重新登入");
        }
        // Extract the user ID from the principal
        String sub = principal.getAttribute("sub");
        if (sub != null) {
            try {
                return UUID.fromString(sub);
            } catch (IllegalArgumentException e) {
                // If sub is not a valid UUID, generate one based on the sub hash
                return UUID.nameUUIDFromBytes(sub.getBytes());
            }
        }
        throw new UnauthorizedException("無法取得用戶身份");
    }
}
