package com.wego.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.dto.request.CreateTodoRequest;
import com.wego.dto.request.UpdateTodoRequest;
import com.wego.dto.response.TodoResponse;
import com.wego.entity.TodoStatus;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.security.UserPrincipal;
import com.wego.service.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for TodoApiController.
 *
 * @contract
 *   - Tests all 6 todo API endpoints
 *   - Verifies request validation
 *   - Tests error handling (404, 403)
 *   - Verifies authentication requirements
 */
@WebMvcTest(TodoApiController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class TodoApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TodoService todoService;

    private UUID tripId;
    private UUID todoId;
    private UUID userId;
    private UserPrincipal userPrincipal;
    private TodoResponse testTodoResponse;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        todoId = UUID.randomUUID();
        userId = UUID.randomUUID();

        User testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .provider("test")
                .providerId("test-id")
                .build();
        userPrincipal = new UserPrincipal(testUser);

        testTodoResponse = TodoResponse.builder()
                .id(todoId)
                .tripId(tripId)
                .title("Buy plane tickets")
                .description("Round trip to Tokyo")
                .status(TodoStatus.PENDING)
                .createdBy(userId)
                .createdByName("Test User")
                .dueDate(LocalDate.now().plusDays(7))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isOverdue(false)
                .build();
    }

    @Nested
    @DisplayName("POST /api/trips/{tripId}/todos")
    class CreateTodoTests {

        @Test
        @DisplayName("should create todo and return 201")
        void createTodo_withValidRequest_shouldReturn201() throws Exception {
            CreateTodoRequest request = CreateTodoRequest.builder()
                    .title("Buy plane tickets")
                    .description("Round trip to Tokyo")
                    .dueDate(LocalDate.now().plusDays(7))
                    .build();

            when(todoService.createTodo(eq(tripId), any(CreateTodoRequest.class), eq(userId)))
                    .thenReturn(testTodoResponse);

            mockMvc.perform(post("/api/trips/{tripId}/todos", tripId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(todoId.toString()))
                    .andExpect(jsonPath("$.data.title").value("Buy plane tickets"))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        @DisplayName("should return 400 when title is blank")
        void createTodo_blankTitle_shouldReturn400() throws Exception {
            CreateTodoRequest request = CreateTodoRequest.builder()
                    .title("")
                    .build();

            mockMvc.perform(post("/api/trips/{tripId}/todos", tripId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void createTodo_notAuthenticated_shouldReturn403() throws Exception {
            CreateTodoRequest request = CreateTodoRequest.builder()
                    .title("Buy plane tickets")
                    .build();

            mockMvc.perform(post("/api/trips/{tripId}/todos", tripId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 403 when user has no permission")
        void createTodo_noPermission_shouldReturn403() throws Exception {
            CreateTodoRequest request = CreateTodoRequest.builder()
                    .title("Buy plane tickets")
                    .build();

            when(todoService.createTodo(eq(tripId), any(CreateTodoRequest.class), eq(userId)))
                    .thenThrow(new ForbiddenException("You do not have permission"));

            mockMvc.perform(post("/api/trips/{tripId}/todos", tripId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/trips/{tripId}/todos")
    class GetTodosByTripTests {

        @Test
        @DisplayName("should return todos list with 200")
        void getTodosByTrip_shouldReturn200() throws Exception {
            TodoResponse todo2 = TodoResponse.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .title("Book hotel")
                    .status(TodoStatus.COMPLETED)
                    .createdBy(userId)
                    .build();

            when(todoService.getTodosByTrip(tripId, userId))
                    .thenReturn(List.of(testTodoResponse, todo2));

            mockMvc.perform(get("/api/trips/{tripId}/todos", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].title").value("Buy plane tickets"))
                    .andExpect(jsonPath("$.data[1].title").value("Book hotel"));
        }

        @Test
        @DisplayName("should return empty list when no todos")
        void getTodosByTrip_noTodos_shouldReturnEmptyList() throws Exception {
            when(todoService.getTodosByTrip(tripId, userId))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/trips/{tripId}/todos", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void getTodosByTrip_notAuthenticated_shouldReturn403() throws Exception {
            mockMvc.perform(get("/api/trips/{tripId}/todos", tripId))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/trips/{tripId}/todos/{todoId}")
    class GetTodoTests {

        @Test
        @DisplayName("should return single todo with 200")
        void getTodo_shouldReturn200() throws Exception {
            when(todoService.getTodo(tripId, todoId, userId))
                    .thenReturn(testTodoResponse);

            mockMvc.perform(get("/api/trips/{tripId}/todos/{todoId}", tripId, todoId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(todoId.toString()))
                    .andExpect(jsonPath("$.data.title").value("Buy plane tickets"))
                    .andExpect(jsonPath("$.data.description").value("Round trip to Tokyo"));
        }

        @Test
        @DisplayName("should return 404 when todo not found")
        void getTodo_notFound_shouldReturn404() throws Exception {
            when(todoService.getTodo(tripId, todoId, userId))
                    .thenThrow(new ResourceNotFoundException("Todo", todoId.toString()));

            mockMvc.perform(get("/api/trips/{tripId}/todos/{todoId}", tripId, todoId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/trips/{tripId}/todos/{todoId}")
    class UpdateTodoTests {

        @Test
        @DisplayName("should update todo and return 200")
        void updateTodo_withValidRequest_shouldReturn200() throws Exception {
            UpdateTodoRequest request = UpdateTodoRequest.builder()
                    .title("Buy plane tickets - updated")
                    .status(TodoStatus.IN_PROGRESS)
                    .build();

            TodoResponse updatedResponse = TodoResponse.builder()
                    .id(todoId)
                    .tripId(tripId)
                    .title("Buy plane tickets - updated")
                    .status(TodoStatus.IN_PROGRESS)
                    .createdBy(userId)
                    .build();

            when(todoService.updateTodo(eq(tripId), eq(todoId), any(UpdateTodoRequest.class), eq(userId)))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/api/trips/{tripId}/todos/{todoId}", tripId, todoId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("Buy plane tickets - updated"))
                    .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("should return 404 when todo not found")
        void updateTodo_notFound_shouldReturn404() throws Exception {
            UpdateTodoRequest request = UpdateTodoRequest.builder()
                    .title("Updated")
                    .build();

            when(todoService.updateTodo(eq(tripId), eq(todoId), any(UpdateTodoRequest.class), eq(userId)))
                    .thenThrow(new ResourceNotFoundException("Todo", todoId.toString()));

            mockMvc.perform(put("/api/trips/{tripId}/todos/{todoId}", tripId, todoId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 403 when user has no permission")
        void updateTodo_noPermission_shouldReturn403() throws Exception {
            UpdateTodoRequest request = UpdateTodoRequest.builder()
                    .title("Updated")
                    .build();

            when(todoService.updateTodo(eq(tripId), eq(todoId), any(UpdateTodoRequest.class), eq(userId)))
                    .thenThrow(new ForbiddenException("You do not have permission"));

            mockMvc.perform(put("/api/trips/{tripId}/todos/{todoId}", tripId, todoId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/trips/{tripId}/todos/{todoId}")
    class DeleteTodoTests {

        @Test
        @DisplayName("should delete todo and return 200")
        void deleteTodo_shouldReturn200() throws Exception {
            doNothing().when(todoService).deleteTodo(tripId, todoId, userId);

            mockMvc.perform(delete("/api/trips/{tripId}/todos/{todoId}", tripId, todoId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should return 404 when todo not found")
        void deleteTodo_notFound_shouldReturn404() throws Exception {
            doThrow(new ResourceNotFoundException("Todo", todoId.toString()))
                    .when(todoService).deleteTodo(tripId, todoId, userId);

            mockMvc.perform(delete("/api/trips/{tripId}/todos/{todoId}", tripId, todoId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 403 when user has no permission")
        void deleteTodo_noPermission_shouldReturn403() throws Exception {
            doThrow(new ForbiddenException("You do not have permission"))
                    .when(todoService).deleteTodo(tripId, todoId, userId);

            mockMvc.perform(delete("/api/trips/{tripId}/todos/{todoId}", tripId, todoId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void deleteTodo_notAuthenticated_shouldReturn403() throws Exception {
            mockMvc.perform(delete("/api/trips/{tripId}/todos/{todoId}", tripId, todoId)
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/trips/{tripId}/todos/stats")
    class GetTodoStatsTests {

        @Test
        @DisplayName("should return stats with 200")
        void getTodoStats_shouldReturn200() throws Exception {
            Map<TodoStatus, Long> stats = Map.of(
                    TodoStatus.PENDING, 3L,
                    TodoStatus.IN_PROGRESS, 2L,
                    TodoStatus.COMPLETED, 5L
            );

            when(todoService.getTodoStats(tripId, userId)).thenReturn(stats);

            mockMvc.perform(get("/api/trips/{tripId}/todos/stats", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.PENDING").value(3))
                    .andExpect(jsonPath("$.data.IN_PROGRESS").value(2))
                    .andExpect(jsonPath("$.data.COMPLETED").value(5));
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void getTodoStats_notAuthenticated_shouldReturn403() throws Exception {
            mockMvc.perform(get("/api/trips/{tripId}/todos/stats", tripId))
                    .andExpect(status().isForbidden());
        }
    }
}
