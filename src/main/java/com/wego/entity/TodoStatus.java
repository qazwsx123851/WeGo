package com.wego.entity;

/**
 * Enum representing the status of a Todo item.
 *
 * @contract
 *   - PENDING: Initial state, task not started
 *   - IN_PROGRESS: Task is being worked on
 *   - COMPLETED: Task is finished
 *
 * @see Todo
 */
public enum TodoStatus {

    /**
     * Task has not been started yet.
     * This is the default status for newly created todos.
     */
    PENDING,

    /**
     * Task is currently being worked on.
     */
    IN_PROGRESS,

    /**
     * Task has been completed.
     * CompletedAt timestamp should be set when transitioning to this status.
     */
    COMPLETED;

    /**
     * Checks if this status represents an active (incomplete) task.
     *
     * @contract
     *   - post: returns true for PENDING and IN_PROGRESS, false for COMPLETED
     *
     * @return true if the task is not yet completed
     */
    public boolean isActive() {
        return this == PENDING || this == IN_PROGRESS;
    }

    /**
     * Checks if this status represents a completed task.
     *
     * @contract
     *   - post: returns true only for COMPLETED
     *
     * @return true if the task is completed
     */
    public boolean isCompleted() {
        return this == COMPLETED;
    }
}
