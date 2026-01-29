package com.wego.service;

import com.wego.domain.permission.PermissionChecker;
import com.wego.dto.request.CreateTodoRequest;
import com.wego.dto.request.UpdateTodoRequest;
import com.wego.dto.response.TodoResponse;
import com.wego.entity.Todo;
import com.wego.entity.TodoStatus;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.repository.TodoRepository;
import com.wego.repository.TripMemberRepository;
import com.wego.repository.TripRepository;
import com.wego.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for managing todo items.
 *
 * @contract
 *   - All methods validate permissions before executing
 *   - OWNER and EDITOR can create, update, and delete todos
 *   - VIEWER can only read todos
 *   - Maintains referential integrity with trips and users
 *
 * @see Todo
 * @see TodoStatus
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserRepository userRepository;
    private final PermissionChecker permissionChecker;

    /**
     * Creates a new todo for a trip.
     *
     * @contract
     *   - pre: tripId != null, request != null, userId != null
     *   - pre: user has edit permission on trip
     *   - pre: if assigneeId provided, assignee must be a trip member
     *   - post: Todo is persisted with status=PENDING
     *   - calledBy: TodoApiController#createTodo
     *
     * @param tripId The trip ID
     * @param request The todo creation request
     * @param userId The ID of the user creating the todo
     * @return The created todo response
     * @throws ForbiddenException if user has no edit permission
     * @throws ResourceNotFoundException if trip not found
     */
    @Transactional
    public TodoResponse createTodo(UUID tripId, CreateTodoRequest request, UUID userId) {
        log.debug("Creating todo for trip {} by user {}", tripId, userId);

        // Check permission
        if (!permissionChecker.canEdit(tripId, userId)) {
            throw new ForbiddenException("No permission to edit this trip");
        }

        // Verify trip exists
        if (!tripRepository.existsById(tripId)) {
            throw new ResourceNotFoundException("Trip", tripId.toString());
        }

        // Validate assignee if provided
        if (request.getAssigneeId() != null) {
            validateAssigneeIsMember(tripId, request.getAssigneeId());
        }

        // Create todo entity
        Todo todo = Todo.builder()
                .tripId(tripId)
                .title(request.getTitle())
                .description(request.getDescription())
                .assigneeId(request.getAssigneeId())
                .dueDate(request.getDueDate())
                .status(TodoStatus.PENDING)
                .createdBy(userId)
                .build();

        todo = todoRepository.save(todo);

        log.info("Created todo {} for trip {}", todo.getId(), tripId);

        return buildTodoResponse(todo);
    }

    /**
     * Gets all todos for a trip with custom sorting.
     *
     * Sorting order:
     * 1. Incomplete todos first, ordered by due_date ASC (nulls last)
     * 2. Completed todos at bottom, ordered by completed_at DESC
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - pre: user has view permission on trip
     *   - post: returns list of todos with user details populated
     *   - calledBy: TodoApiController#getTodosByTrip
     *
     * @param tripId The trip ID
     * @param userId The ID of the user requesting
     * @return List of todo responses
     * @throws ForbiddenException if user has no view permission
     */
    @Transactional(readOnly = true)
    public List<TodoResponse> getTodosByTrip(UUID tripId, UUID userId) {
        log.debug("Getting todos for trip {} by user {}", tripId, userId);

        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("No permission to view this trip");
        }

        List<Todo> todos = todoRepository.findByTripIdOrderedByDueDateAndStatus(tripId);

        return todos.stream()
                .map(this::buildTodoResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gets a single todo by ID.
     *
     * @contract
     *   - pre: tripId != null, todoId != null, userId != null
     *   - pre: user has view permission on trip
     *   - post: returns todo with user details populated
     *   - calledBy: TodoApiController#getTodo
     *
     * @param tripId The trip ID
     * @param todoId The todo ID
     * @param userId The ID of the user requesting
     * @return The todo response
     * @throws ForbiddenException if user has no view permission
     * @throws ResourceNotFoundException if todo not found
     */
    @Transactional(readOnly = true)
    public TodoResponse getTodo(UUID tripId, UUID todoId, UUID userId) {
        log.debug("Getting todo {} for trip {} by user {}", todoId, tripId, userId);

        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("No permission to view this trip");
        }

        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new ResourceNotFoundException("Todo", todoId.toString()));

        if (!todo.getTripId().equals(tripId)) {
            throw new ResourceNotFoundException("Todo", todoId.toString());
        }

        return buildTodoResponse(todo);
    }

    /**
     * Updates a todo.
     *
     * @contract
     *   - pre: tripId != null, todoId != null, request != null, userId != null
     *   - pre: user has edit permission on the trip
     *   - pre: if assigneeId provided, assignee must be a trip member
     *   - post: Todo is updated with non-null fields from request
     *   - post: if status changes to COMPLETED, completedAt is set
     *   - post: if status changes from COMPLETED, completedAt is cleared
     *   - calledBy: TodoApiController#updateTodo
     *
     * @param tripId The trip ID
     * @param todoId The todo ID
     * @param request The update request
     * @param userId The ID of the user updating
     * @return The updated todo response
     * @throws ResourceNotFoundException if todo not found
     * @throws ForbiddenException if user has no edit permission
     */
    @Transactional
    public TodoResponse updateTodo(UUID tripId, UUID todoId, UpdateTodoRequest request, UUID userId) {
        log.debug("Updating todo {} by user {}", todoId, userId);

        if (!permissionChecker.canEdit(tripId, userId)) {
            throw new ForbiddenException("No permission to edit this trip");
        }

        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new ResourceNotFoundException("Todo", todoId.toString()));

        if (!todo.getTripId().equals(tripId)) {
            throw new ResourceNotFoundException("Todo", todoId.toString());
        }

        // Update non-null fields
        if (request.getTitle() != null) {
            todo.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            todo.setDescription(request.getDescription());
        }

        // Handle assignee update
        if (Boolean.TRUE.equals(request.getClearAssignee())) {
            todo.setAssigneeId(null);
        } else if (request.getAssigneeId() != null) {
            validateAssigneeIsMember(tripId, request.getAssigneeId());
            todo.setAssigneeId(request.getAssigneeId());
        }

        // Handle due date update
        if (Boolean.TRUE.equals(request.getClearDueDate())) {
            todo.setDueDate(null);
        } else if (request.getDueDate() != null) {
            todo.setDueDate(request.getDueDate());
        }

        // Handle status update
        if (request.getStatus() != null) {
            TodoStatus oldStatus = todo.getStatus();
            TodoStatus newStatus = request.getStatus();

            if (oldStatus != newStatus) {
                if (newStatus == TodoStatus.COMPLETED) {
                    todo.markAsCompleted();
                } else if (oldStatus == TodoStatus.COMPLETED) {
                    todo.markAsIncomplete();
                    todo.setStatus(newStatus);
                } else {
                    todo.setStatus(newStatus);
                }
            }
        }

        todo.setUpdatedAt(Instant.now());
        todo = todoRepository.save(todo);

        log.info("Updated todo {}", todoId);

        return buildTodoResponse(todo);
    }

    /**
     * Deletes a todo.
     *
     * @contract
     *   - pre: tripId != null, todoId != null, userId != null
     *   - pre: user has edit permission on the trip
     *   - post: Todo is deleted
     *   - calledBy: TodoApiController#deleteTodo
     *
     * @param tripId The trip ID
     * @param todoId The todo ID
     * @param userId The ID of the user deleting
     * @throws ResourceNotFoundException if todo not found
     * @throws ForbiddenException if user has no edit permission
     */
    @Transactional
    public void deleteTodo(UUID tripId, UUID todoId, UUID userId) {
        log.debug("Deleting todo {} by user {}", todoId, userId);

        if (!permissionChecker.canEdit(tripId, userId)) {
            throw new ForbiddenException("No permission to edit this trip");
        }

        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new ResourceNotFoundException("Todo", todoId.toString()));

        if (!todo.getTripId().equals(tripId)) {
            throw new ResourceNotFoundException("Todo", todoId.toString());
        }

        todoRepository.delete(todo);

        log.info("Deleted todo {}", todoId);
    }

    /**
     * Gets todo statistics for a trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - pre: user has view permission on trip
     *   - post: returns counts for each status
     *   - calledBy: TripApiController (summary endpoint)
     *
     * @param tripId The trip ID
     * @param userId The ID of the user requesting
     * @return Map of status to count
     * @throws ForbiddenException if user has no view permission
     */
    @Transactional(readOnly = true)
    public Map<TodoStatus, Long> getTodoStats(UUID tripId, UUID userId) {
        log.debug("Getting todo stats for trip {} by user {}", tripId, userId);

        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("No permission to view this trip");
        }

        long pending = todoRepository.countByTripIdAndStatus(tripId, TodoStatus.PENDING);
        long inProgress = todoRepository.countByTripIdAndStatus(tripId, TodoStatus.IN_PROGRESS);
        long completed = todoRepository.countByTripIdAndStatus(tripId, TodoStatus.COMPLETED);

        return Map.of(
                TodoStatus.PENDING, pending,
                TodoStatus.IN_PROGRESS, inProgress,
                TodoStatus.COMPLETED, completed
        );
    }

    /**
     * Validates that the assignee is a member of the trip.
     *
     * @throws ForbiddenException if assignee is not a trip member
     */
    private void validateAssigneeIsMember(UUID tripId, UUID assigneeId) {
        if (!tripMemberRepository.existsByTripIdAndUserId(tripId, assigneeId)) {
            throw new ForbiddenException("Assignee must be a member of the trip");
        }
    }

    /**
     * Builds a TodoResponse from a Todo entity with user details.
     */
    private TodoResponse buildTodoResponse(Todo todo) {
        TodoResponse response = TodoResponse.fromEntity(todo);

        // Get assignee info
        if (todo.getAssigneeId() != null) {
            userRepository.findById(todo.getAssigneeId()).ifPresent(user -> {
                response.setAssigneeName(user.getNickname());
                response.setAssigneeAvatarUrl(user.getAvatarUrl());
            });
        }

        // Get creator info
        userRepository.findById(todo.getCreatedBy()).ifPresent(user -> {
            response.setCreatedByName(user.getNickname());
        });

        return response;
    }

    /**
     * Builds TodoResponse list with optimized user queries.
     */
    private List<TodoResponse> buildTodoResponses(List<Todo> todos) {
        // Collect all user IDs
        List<UUID> userIds = todos.stream()
                .flatMap(t -> {
                    if (t.getAssigneeId() != null) {
                        return java.util.stream.Stream.of(t.getCreatedBy(), t.getAssigneeId());
                    }
                    return java.util.stream.Stream.of(t.getCreatedBy());
                })
                .distinct()
                .collect(Collectors.toList());

        // Batch fetch users
        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // Build responses
        return todos.stream()
                .map(todo -> {
                    TodoResponse response = TodoResponse.fromEntity(todo);

                    if (todo.getAssigneeId() != null) {
                        User assignee = userMap.get(todo.getAssigneeId());
                        if (assignee != null) {
                            response.setAssigneeName(assignee.getNickname());
                            response.setAssigneeAvatarUrl(assignee.getAvatarUrl());
                        }
                    }

                    User creator = userMap.get(todo.getCreatedBy());
                    if (creator != null) {
                        response.setCreatedByName(creator.getNickname());
                    }

                    return response;
                })
                .collect(Collectors.toList());
    }
}
