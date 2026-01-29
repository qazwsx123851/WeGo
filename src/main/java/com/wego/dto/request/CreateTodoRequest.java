package com.wego.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a new todo item.
 *
 * @contract
 *   - title: 1-200 characters, required
 *   - description: optional, max 1000 characters
 *   - assigneeId: optional, if provided must be a valid trip member
 *   - dueDate: optional, should be today or in the future
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTodoRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    /**
     * Optional assignee for this todo.
     * If provided, must be a member of the trip.
     */
    private UUID assigneeId;

    /**
     * Optional due date for this todo.
     */
    private LocalDate dueDate;
}
