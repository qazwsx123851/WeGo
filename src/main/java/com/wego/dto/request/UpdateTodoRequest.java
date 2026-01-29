package com.wego.dto.request;

import com.wego.entity.TodoStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for updating an existing todo item.
 *
 * All fields are optional. Only non-null fields will be updated.
 *
 * @contract
 *   - title: optional, 1-200 characters if provided
 *   - description: optional, max 1000 characters if provided
 *   - assigneeId: optional, if provided must be a valid trip member
 *   - dueDate: optional
 *   - status: optional, valid TodoStatus value
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTodoRequest {

    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    /**
     * Optional assignee for this todo.
     * If provided, must be a member of the trip.
     * Use "clearAssignee" flag to explicitly remove assignee.
     */
    private UUID assigneeId;

    /**
     * Set to true to explicitly remove the assignee.
     */
    @Builder.Default
    private Boolean clearAssignee = false;

    /**
     * Optional due date for this todo.
     * Use "clearDueDate" flag to explicitly remove due date.
     */
    private LocalDate dueDate;

    /**
     * Set to true to explicitly remove the due date.
     */
    @Builder.Default
    private Boolean clearDueDate = false;

    /**
     * New status for the todo.
     * When changing to COMPLETED, completedAt will be set automatically.
     * When changing from COMPLETED to another status, completedAt will be cleared.
     */
    private TodoStatus status;
}
