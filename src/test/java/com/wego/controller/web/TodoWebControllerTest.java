package com.wego.controller.web;

import com.wego.dto.response.TodoResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.Role;
import com.wego.entity.TodoStatus;
import com.wego.entity.User;
import com.wego.security.UserPrincipal;
import com.wego.service.TodoService;
import com.wego.service.TripService;
import com.wego.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for TodoWebController.
 *
 * @contract
 *   - Tests todo list page rendering
 *   - Verifies model attributes and permission flags
 *   - Tests authentication and trip access
 */
@WebMvcTest(TodoWebController.class)
@ActiveProfiles("test")
class TodoWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private TripService tripService;

    @MockBean
    private TodoService todoService;

    private UUID userId;
    private UUID tripId;
    private User testUser;
    private UserPrincipal testPrincipal;
    private TripResponse testTrip;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tripId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .provider("google")
                .providerId("google-id")
                .build();
        testPrincipal = new UserPrincipal(testUser);

        TripResponse.MemberSummary ownerMember = TripResponse.MemberSummary.builder()
                .userId(userId)
                .nickname("Test User")
                .role(Role.OWNER)
                .build();

        testTrip = TripResponse.builder()
                .id(tripId)
                .title("Tokyo Trip")
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(15))
                .baseCurrency("TWD")
                .members(List.of(ownerMember))
                .build();

    }

    private SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor oidcLogin() {
        return SecurityMockMvcRequestPostProcessors.oauth2Login().oauth2User(testPrincipal);
    }

    @Test
    @DisplayName("GET /trips/{id}/todos - should return todo list view with stats")
    void listTodos_authenticated_shouldReturnListView() throws Exception {
        when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
        when(todoService.getTodosByTrip(tripId, userId)).thenReturn(Collections.emptyList());
        when(todoService.getTodoStats(tripId, userId)).thenReturn(Map.of(
                TodoStatus.PENDING, 2L,
                TodoStatus.COMPLETED, 1L,
                TodoStatus.IN_PROGRESS, 1L
        ));

        mockMvc.perform(get("/trips/{tripId}/todos", tripId).with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(view().name("todo/list"))
                .andExpect(model().attributeExists("trip", "todos", "members",
                        "totalTodos", "completedTodos", "pendingTodos",
                        "inProgressTodos", "canEdit", "name", "picture"))
                .andExpect(model().attribute("canEdit", true))
                .andExpect(model().attribute("totalTodos", 4L))
                .andExpect(model().attribute("completedTodos", 1L));
    }

    @Test
    @DisplayName("GET /trips/{id}/todos - should redirect when trip not found")
    void listTodos_tripNotFound_shouldRedirect() throws Exception {
        when(tripService.getTrip(tripId, userId)).thenReturn(null);

        mockMvc.perform(get("/trips/{tripId}/todos", tripId).with(oidcLogin()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?error=trip_not_found"));
    }

    @Test
    @DisplayName("GET /trips/{id}/todos - should redirect when not authenticated")
    void listTodos_notAuthenticated_shouldRedirect() throws Exception {
        mockMvc.perform(get("/trips/{tripId}/todos", tripId))
                .andExpect(status().is3xxRedirection());
    }
}
