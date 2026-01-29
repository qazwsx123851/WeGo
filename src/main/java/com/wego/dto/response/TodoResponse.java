package com.wego.dto.response;

import com.wego.entity.Todo;
import com.wego.entity.TodoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for todo information.
 *
 * @contract
 *   - id: the todo ID
 *   - tripId: the trip this todo belongs to
 *   - title: todo title
 *   - status: current status (PENDING, IN_PROGRESS, COMPLETED)
 *   - isOverdue: calculated field indicating if todo is past due
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoResponse {

    private UUID id;
    private UUID tripId;
    private String title;
    private String description;
    private UUID assigneeId;
    private String assigneeName;
    private String assigneeAvatarUrl;
    private LocalDate dueDate;
    private TodoStatus status;
    private UUID createdBy;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    private boolean isOverdue;

    /**
     * Creates a TodoResponse from a Todo entity.
     * Note: assigneeName, createdByName, and avatars must be set separately.
     *
     * @contract
     *   - pre: todo != null
     *   - post: returns TodoResponse with all entity fields mapped
     *   - calledBy: TodoService#buildTodoResponse
     *
     * @param todo The todo entity
     * @return TodoResponse DTO
     */
    public static TodoResponse fromEntity(Todo todo) {
        return TodoResponse.builder()
                .id(todo.getId())
                .tripId(todo.getTripId())
                .title(todo.getTitle())
                .description(todo.getDescription())
                .assigneeId(todo.getAssigneeId())
                .dueDate(todo.getDueDate())
                .status(todo.getStatus())
                .createdBy(todo.getCreatedBy())
                .createdAt(todo.getCreatedAt())
                .updatedAt(todo.getUpdatedAt())
                .completedAt(todo.getCompletedAt())
                .isOverdue(todo.isOverdue())
                .build();
    }
}
