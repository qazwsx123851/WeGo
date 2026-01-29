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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TodoService.
 *
 * @contract
 *   - Tests follow TDD methodology
 *   - Covers all public methods
 *   - Tests edge cases and error scenarios
 *   - Target coverage: 80%+
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripMemberRepository tripMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionChecker permissionChecker;

    @InjectMocks
    private TodoService todoService;

    private UUID tripId;
    private UUID userId;
    private UUID todoId;
    private UUID assigneeId;
    private User testUser;
    private User assigneeUser;
    private Todo testTodo;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        userId = UUID.randomUUID();
        todoId = UUID.randomUUID();
        assigneeId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .provider("google")
                .providerId("123456")
                .build();

        assigneeUser = User.builder()
                .id(assigneeId)
                .email("assignee@example.com")
                .nickname("Assignee User")
                .provider("google")
                .providerId("654321")
                .avatarUrl("https://example.com/avatar.jpg")
                .build();

        testTodo = Todo.builder()
                .id(todoId)
                .tripId(tripId)
                .title("Test Todo")
                .description("Test Description")
                .status(TodoStatus.PENDING)
                .createdBy(userId)
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("createTodo")
    class CreateTodoTests {

        @Test
        @DisplayName("should create todo with valid input")
        void createTodo_withValidInput_shouldReturnCreatedTodo() {
            // Given
            CreateTodoRequest request = CreateTodoRequest.builder()
                    .title("Buy tickets")
                    .description("Book flight tickets")
                    .dueDate(LocalDate.now().plusDays(7))
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(tripRepository.existsById(tripId)).thenReturn(true);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> {
                Todo todo = invocation.getArgument(0);
                todo.setId(todoId);
                return todo;
            });

            // When
            TodoResponse response = todoService.createTodo(tripId, request, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo("Buy tickets");
            assertThat(response.getDescription()).isEqualTo("Book flight tickets");
            assertThat(response.getStatus()).isEqualTo(TodoStatus.PENDING);
            assertThat(response.getCreatedByName()).isEqualTo("Test User");

            verify(todoRepository).save(any(Todo.class));
        }

        @Test
        @DisplayName("should create todo with assignee")
        void createTodo_withAssignee_shouldSetAssignee() {
            // Given
            CreateTodoRequest request = CreateTodoRequest.builder()
                    .title("Pack luggage")
                    .assigneeId(assigneeId)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(tripRepository.existsById(tripId)).thenReturn(true);
            when(tripMemberRepository.existsByTripIdAndUserId(tripId, assigneeId)).thenReturn(true);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(assigneeId)).thenReturn(Optional.of(assigneeUser));
            when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> {
                Todo todo = invocation.getArgument(0);
                todo.setId(todoId);
                return todo;
            });

            // When
            TodoResponse response = todoService.createTodo(tripId, request, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAssigneeId()).isEqualTo(assigneeId);
            assertThat(response.getAssigneeName()).isEqualTo("Assignee User");
            assertThat(response.getAssigneeAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
        }

        @Test
        @DisplayName("should throw ForbiddenException when user has no edit permission")
        void createTodo_withNoPermission_shouldThrowForbiddenException() {
            // Given
            CreateTodoRequest request = CreateTodoRequest.builder()
                    .title("Buy tickets")
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> todoService.createTodo(tripId, request, userId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("permission");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when trip not found")
        void createTodo_withNonExistentTrip_shouldThrowResourceNotFoundException() {
            // Given
            CreateTodoRequest request = CreateTodoRequest.builder()
                    .title("Buy tickets")
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(tripRepository.existsById(tripId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> todoService.createTodo(tripId, request, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Trip");
        }

        @Test
        @DisplayName("should throw ForbiddenException when assignee is not a trip member")
        void createTodo_withNonMemberAssignee_shouldThrowForbiddenException() {
            // Given
            CreateTodoRequest request = CreateTodoRequest.builder()
                    .title("Buy tickets")
                    .assigneeId(assigneeId)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(tripRepository.existsById(tripId)).thenReturn(true);
            when(tripMemberRepository.existsByTripIdAndUserId(tripId, assigneeId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> todoService.createTodo(tripId, request, userId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("member");
        }
    }

    @Nested
    @DisplayName("getTodosByTrip")
    class GetTodosByTripTests {

        @Test
        @DisplayName("should return todos for trip when user has view permission")
        void getTodosByTrip_withValidPermission_shouldReturnTodos() {
            // Given
            Todo todo1 = Todo.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .title("Todo 1")
                    .status(TodoStatus.PENDING)
                    .dueDate(LocalDate.now().plusDays(1))
                    .createdBy(userId)
                    .createdAt(Instant.now())
                    .build();

            Todo todo2 = Todo.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .title("Todo 2")
                    .status(TodoStatus.COMPLETED)
                    .createdBy(userId)
                    .createdAt(Instant.now())
                    .completedAt(Instant.now())
                    .build();

            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(todoRepository.findByTripIdOrderedByDueDateAndStatus(tripId))
                    .thenReturn(Arrays.asList(todo1, todo2));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            List<TodoResponse> responses = todoService.getTodosByTrip(tripId, userId);

            // Then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getTitle()).isEqualTo("Todo 1");
            assertThat(responses.get(1).getTitle()).isEqualTo("Todo 2");
        }

        @Test
        @DisplayName("should throw ForbiddenException when user has no view permission")
        void getTodosByTrip_withNoPermission_shouldThrowForbiddenException() {
            // Given
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> todoService.getTodosByTrip(tripId, userId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should return empty list when no todos exist")
        void getTodosByTrip_withNoTodos_shouldReturnEmptyList() {
            // Given
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(todoRepository.findByTripIdOrderedByDueDateAndStatus(tripId))
                    .thenReturn(Collections.emptyList());

            // When
            List<TodoResponse> responses = todoService.getTodosByTrip(tripId, userId);

            // Then
            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("getTodo")
    class GetTodoTests {

        @Test
        @DisplayName("should return todo when user has view permission")
        void getTodo_withValidPermission_shouldReturnTodo() {
            // Given
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(testTodo));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            TodoResponse response = todoService.getTodo(tripId, todoId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(todoId);
            assertThat(response.getTitle()).isEqualTo("Test Todo");
        }

        @Test
        @DisplayName("should throw ForbiddenException when user has no view permission")
        void getTodo_withNoPermission_shouldThrowForbiddenException() {
            // Given
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> todoService.getTodo(tripId, todoId, userId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when todo not found")
        void getTodo_withNonExistentTodo_shouldThrowResourceNotFoundException() {
            // Given
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(todoRepository.findById(todoId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> todoService.getTodo(tripId, todoId, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Todo");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when todo belongs to different trip")
        void getTodo_withDifferentTrip_shouldThrowResourceNotFoundException() {
            // Given
            UUID differentTripId = UUID.randomUUID();
            Todo todoFromDifferentTrip = Todo.builder()
                    .id(todoId)
                    .tripId(differentTripId)
                    .title("Other Todo")
                    .status(TodoStatus.PENDING)
                    .createdBy(userId)
                    .build();

            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todoFromDifferentTrip));

            // When & Then
            assertThatThrownBy(() -> todoService.getTodo(tripId, todoId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateTodo")
    class UpdateTodoTests {

        @Test
        @DisplayName("should update todo when user has edit permission")
        void updateTodo_withValidInput_shouldReturnUpdatedTodo() {
            // Given
            UpdateTodoRequest request = UpdateTodoRequest.builder()
                    .title("Updated Title")
                    .description("Updated Description")
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(testTodo));
            when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            TodoResponse response = todoService.updateTodo(tripId, todoId, request, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo("Updated Title");
            assertThat(response.getDescription()).isEqualTo("Updated Description");
        }

        @Test
        @DisplayName("should update status to COMPLETED and set completedAt")
        void updateTodo_toCompleted_shouldSetCompletedAt() {
            // Given
            UpdateTodoRequest request = UpdateTodoRequest.builder()
                    .status(TodoStatus.COMPLETED)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(testTodo));
            when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            TodoResponse response = todoService.updateTodo(tripId, todoId, request, userId);

            // Then
            assertThat(response.getStatus()).isEqualTo(TodoStatus.COMPLETED);
            assertThat(response.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("should clear completedAt when status changes from COMPLETED")
        void updateTodo_fromCompleted_shouldClearCompletedAt() {
            // Given
            testTodo.setStatus(TodoStatus.COMPLETED);
            testTodo.setCompletedAt(Instant.now());

            UpdateTodoRequest request = UpdateTodoRequest.builder()
                    .status(TodoStatus.IN_PROGRESS)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(testTodo));
            when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            TodoResponse response = todoService.updateTodo(tripId, todoId, request, userId);

            // Then
            assertThat(response.getStatus()).isEqualTo(TodoStatus.IN_PROGRESS);
            assertThat(response.getCompletedAt()).isNull();
        }

        @Test
        @DisplayName("should clear assignee when clearAssignee is true")
        void updateTodo_withClearAssignee_shouldClearAssignee() {
            // Given
            testTodo.setAssigneeId(assigneeId);

            UpdateTodoRequest request = UpdateTodoRequest.builder()
                    .clearAssignee(true)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(testTodo));
            when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            TodoResponse response = todoService.updateTodo(tripId, todoId, request, userId);

            // Then
            assertThat(response.getAssigneeId()).isNull();
        }

        @Test
        @DisplayName("should clear due date when clearDueDate is true")
        void updateTodo_withClearDueDate_shouldClearDueDate() {
            // Given
            testTodo.setDueDate(LocalDate.now().plusDays(7));

            UpdateTodoRequest request = UpdateTodoRequest.builder()
                    .clearDueDate(true)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(testTodo));
            when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            TodoResponse response = todoService.updateTodo(tripId, todoId, request, userId);

            // Then
            assertThat(response.getDueDate()).isNull();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when todo not found")
        void updateTodo_withNonExistentTodo_shouldThrowResourceNotFoundException() {
            // Given
            UpdateTodoRequest request = UpdateTodoRequest.builder()
                    .title("Updated")
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(todoRepository.findById(todoId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> todoService.updateTodo(tripId, todoId, request, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Todo");
        }

        @Test
        @DisplayName("should throw ForbiddenException when user has no edit permission")
        void updateTodo_withNoPermission_shouldThrowForbiddenException() {
            // Given
            UpdateTodoRequest request = UpdateTodoRequest.builder()
                    .title("Updated")
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> todoService.updateTodo(tripId, todoId, request, userId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should only update provided fields")
        void updateTodo_withPartialUpdate_shouldOnlyUpdateProvidedFields() {
            // Given
            testTodo.setDescription("Original Description");
            testTodo.setDueDate(LocalDate.now().plusDays(7));

            UpdateTodoRequest request = UpdateTodoRequest.builder()
                    .title("New Title")
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(testTodo));
            when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            TodoResponse response = todoService.updateTodo(tripId, todoId, request, userId);

            // Then
            assertThat(response.getTitle()).isEqualTo("New Title");
            assertThat(response.getDescription()).isEqualTo("Original Description"); // unchanged
            assertThat(response.getDueDate()).isEqualTo(LocalDate.now().plusDays(7)); // unchanged
        }

        @Test
        @DisplayName("should validate new assignee is a trip member")
        void updateTodo_withNewAssignee_shouldValidateMembership() {
            // Given
            UpdateTodoRequest request = UpdateTodoRequest.builder()
                    .assigneeId(assigneeId)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(testTodo));
            when(tripMemberRepository.existsByTripIdAndUserId(tripId, assigneeId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> todoService.updateTodo(tripId, todoId, request, userId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("member");
        }
    }

    @Nested
    @DisplayName("deleteTodo")
    class DeleteTodoTests {

        @Test
        @DisplayName("should delete todo when user has edit permission")
        void deleteTodo_withValidPermission_shouldDeleteSuccessfully() {
            // Given
            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(testTodo));

            // When
            todoService.deleteTodo(tripId, todoId, userId);

            // Then
            verify(todoRepository).delete(testTodo);
        }

        @Test
        @DisplayName("should throw ForbiddenException when user has no edit permission")
        void deleteTodo_withNoPermission_shouldThrowForbiddenException() {
            // Given
            when(permissionChecker.canEdit(tripId, userId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> todoService.deleteTodo(tripId, todoId, userId))
                    .isInstanceOf(ForbiddenException.class);

            verify(todoRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when todo not found")
        void deleteTodo_withNonExistentTodo_shouldThrowResourceNotFoundException() {
            // Given
            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(todoRepository.findById(todoId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> todoService.deleteTodo(tripId, todoId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when todo belongs to different trip")
        void deleteTodo_withDifferentTrip_shouldThrowResourceNotFoundException() {
            // Given
            UUID differentTripId = UUID.randomUUID();
            Todo todoFromDifferentTrip = Todo.builder()
                    .id(todoId)
                    .tripId(differentTripId)
                    .title("Other Todo")
                    .status(TodoStatus.PENDING)
                    .createdBy(userId)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todoFromDifferentTrip));

            // When & Then
            assertThatThrownBy(() -> todoService.deleteTodo(tripId, todoId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getTodoStats")
    class GetTodoStatsTests {

        @Test
        @DisplayName("should return correct stats for each status")
        void getTodoStats_withValidPermission_shouldReturnStats() {
            // Given
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(todoRepository.countByTripIdAndStatus(tripId, TodoStatus.PENDING)).thenReturn(5L);
            when(todoRepository.countByTripIdAndStatus(tripId, TodoStatus.IN_PROGRESS)).thenReturn(3L);
            when(todoRepository.countByTripIdAndStatus(tripId, TodoStatus.COMPLETED)).thenReturn(10L);

            // When
            Map<TodoStatus, Long> stats = todoService.getTodoStats(tripId, userId);

            // Then
            assertThat(stats).hasSize(3);
            assertThat(stats.get(TodoStatus.PENDING)).isEqualTo(5L);
            assertThat(stats.get(TodoStatus.IN_PROGRESS)).isEqualTo(3L);
            assertThat(stats.get(TodoStatus.COMPLETED)).isEqualTo(10L);
        }

        @Test
        @DisplayName("should throw ForbiddenException when user has no view permission")
        void getTodoStats_withNoPermission_shouldThrowForbiddenException() {
            // Given
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> todoService.getTodoStats(tripId, userId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should return zeros when no todos exist")
        void getTodoStats_withNoTodos_shouldReturnZeros() {
            // Given
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(todoRepository.countByTripIdAndStatus(tripId, TodoStatus.PENDING)).thenReturn(0L);
            when(todoRepository.countByTripIdAndStatus(tripId, TodoStatus.IN_PROGRESS)).thenReturn(0L);
            when(todoRepository.countByTripIdAndStatus(tripId, TodoStatus.COMPLETED)).thenReturn(0L);

            // When
            Map<TodoStatus, Long> stats = todoService.getTodoStats(tripId, userId);

            // Then
            assertThat(stats.get(TodoStatus.PENDING)).isEqualTo(0L);
            assertThat(stats.get(TodoStatus.IN_PROGRESS)).isEqualTo(0L);
            assertThat(stats.get(TodoStatus.COMPLETED)).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle todo with null description")
        void createTodo_withNullDescription_shouldSucceed() {
            // Given
            CreateTodoRequest request = CreateTodoRequest.builder()
                    .title("Simple Todo")
                    .description(null)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(tripRepository.existsById(tripId)).thenReturn(true);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> {
                Todo todo = invocation.getArgument(0);
                todo.setId(todoId);
                return todo;
            });

            // When
            TodoResponse response = todoService.createTodo(tripId, request, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getDescription()).isNull();
        }

        @Test
        @DisplayName("should handle todo with past due date (already overdue)")
        void createTodo_withPastDueDate_shouldShowAsOverdue() {
            // Given
            CreateTodoRequest request = CreateTodoRequest.builder()
                    .title("Overdue Todo")
                    .dueDate(LocalDate.now().minusDays(1))
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(tripRepository.existsById(tripId)).thenReturn(true);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> {
                Todo todo = invocation.getArgument(0);
                todo.setId(todoId);
                return todo;
            });

            // When
            TodoResponse response = todoService.createTodo(tripId, request, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isOverdue()).isTrue();
        }

        @Test
        @DisplayName("completed todo should not be overdue even with past due date")
        void getTodo_completedWithPastDueDate_shouldNotBeOverdue() {
            // Given
            testTodo.setDueDate(LocalDate.now().minusDays(1));
            testTodo.setStatus(TodoStatus.COMPLETED);
            testTodo.setCompletedAt(Instant.now());

            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(testTodo));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            TodoResponse response = todoService.getTodo(tripId, todoId, userId);

            // Then
            assertThat(response.isOverdue()).isFalse();
        }

        @Test
        @DisplayName("should handle status transition from PENDING to IN_PROGRESS")
        void updateTodo_fromPendingToInProgress_shouldUpdate() {
            // Given
            UpdateTodoRequest request = UpdateTodoRequest.builder()
                    .status(TodoStatus.IN_PROGRESS)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(testTodo));
            when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            TodoResponse response = todoService.updateTodo(tripId, todoId, request, userId);

            // Then
            assertThat(response.getStatus()).isEqualTo(TodoStatus.IN_PROGRESS);
            assertThat(response.getCompletedAt()).isNull();
        }
    }
}
