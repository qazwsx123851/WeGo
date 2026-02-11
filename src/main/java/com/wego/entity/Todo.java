package com.wego.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Todo entity representing a task item within a trip.
 *
 * Todos can be assigned to specific trip members and have optional due dates.
 * They track status progression from PENDING through IN_PROGRESS to COMPLETED.
 *
 * @contract
 *   - invariant: tripId is never null
 *   - invariant: title is never null or empty
 *   - invariant: status is never null (defaults to PENDING)
 *   - invariant: createdBy is never null
 *   - invariant: completedAt is set when status becomes COMPLETED
 *
 * @see Trip
 * @see TodoStatus
 */
@Entity
@Table(name = "todos", indexes = {
    @Index(name = "idx_todo_trip_id", columnList = "trip_id"),
    @Index(name = "idx_todo_assignee_id", columnList = "assignee_id"),
    @Index(name = "idx_todo_created_by", columnList = "created_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TodoStatus status = TodoStatus.PENDING;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Marks the todo as completed and sets the completedAt timestamp.
     *
     * @contract
     *   - pre: status != COMPLETED (no-op if already completed)
     *   - post: status == COMPLETED, completedAt is set to now
     *   - calledBy: TodoService#updateTodo
     */
    public void markAsCompleted() {
        if (this.status != TodoStatus.COMPLETED) {
            this.status = TodoStatus.COMPLETED;
            this.completedAt = Instant.now();
            this.updatedAt = Instant.now();
        }
    }

    /**
     * Marks the todo as incomplete (reverts to PENDING) and clears completedAt.
     *
     * @contract
     *   - pre: status == COMPLETED (no-op if already incomplete)
     *   - post: status == PENDING, completedAt is null
     *   - calledBy: TodoService#updateTodo
     */
    public void markAsIncomplete() {
        if (this.status == TodoStatus.COMPLETED) {
            this.status = TodoStatus.PENDING;
            this.completedAt = null;
            this.updatedAt = Instant.now();
        }
    }

    /**
     * Checks if the todo is overdue.
     *
     * @contract
     *   - pre: none
     *   - post: returns true if dueDate is before today and status is not COMPLETED
     *
     * @return true if overdue, false otherwise
     */
    public boolean isOverdue() {
        if (dueDate == null || status == TodoStatus.COMPLETED) {
            return false;
        }
        return dueDate.isBefore(LocalDate.now());
    }

    /**
     * Equality based on ID for JPA entity identity.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Todo todo = (Todo) o;
        return id != null && Objects.equals(id, todo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Todo{" +
                "id=" + id +
                ", tripId=" + tripId +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", dueDate=" + dueDate +
                '}';
    }
}
