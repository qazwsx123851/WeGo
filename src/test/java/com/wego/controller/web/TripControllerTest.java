package com.wego.controller.web;

import com.wego.dto.response.ActivityResponse;
import com.wego.dto.response.TodoResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.Role;
import com.wego.entity.TodoStatus;
import com.wego.entity.User;
import com.wego.security.UserPrincipal;
import com.wego.service.ActivityService;
import com.wego.service.DocumentService;
import com.wego.service.ExpenseService;
import com.wego.service.TodoService;
import com.wego.service.TripService;
import com.wego.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests for TripController (Web).
 *
 * @contract
 *   - Tests list, detail, create form, edit form endpoints
 *   - Verifies model attributes and view names
 *   - Tests authentication and permission checks
 */
@WebMvcTest(TripController.class)
@ActiveProfiles("test")
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private TripService tripService;

    @MockBean
    private ActivityService activityService;

    @MockBean
    private TodoService todoService;

    @MockBean
    private ExpenseService expenseService;

    @MockBean
    private DocumentService documentService;

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
                .description("Summer vacation")
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(15))
                .baseCurrency("TWD")
                .members(List.of(ownerMember))
                .build();

    }

    private SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor oidcLogin() {
        return SecurityMockMvcRequestPostProcessors.oauth2Login().oauth2User(testPrincipal);
    }

    @Nested
    @DisplayName("GET /trips")
    class ListTripsTests {

        @Test
        @DisplayName("should return trip list view with trips")
        void listTrips_authenticated_shouldReturnListView() throws Exception {
            Page<TripResponse> tripPage = new PageImpl<>(List.of(testTrip));
            when(tripService.getUserTrips(eq(userId), any(Pageable.class))).thenReturn(tripPage);

            mockMvc.perform(get("/trips").with(oidcLogin()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("trip/list"))
                    .andExpect(model().attributeExists("trips", "name", "picture"))
                    .andExpect(model().attribute("name", "Test User"));
        }

        @Test
        @DisplayName("should return empty list when user has no trips")
        void listTrips_noTrips_shouldReturnEmptyList() throws Exception {
            Page<TripResponse> emptyPage = new PageImpl<>(Collections.emptyList());
            when(tripService.getUserTrips(eq(userId), any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/trips").with(oidcLogin()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("trip/list"))
                    .andExpect(model().attribute("trips", Collections.emptyList()));
        }

        @Test
        @DisplayName("should redirect to login when not authenticated")
        void listTrips_notAuthenticated_shouldRedirect() throws Exception {
            mockMvc.perform(get("/trips"))
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Nested
    @DisplayName("GET /trips/{id}")
    class ShowTripDetailTests {

        @BeforeEach
        void setUpDetail() {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
            when(activityService.getActivitiesByTrip(tripId, userId)).thenReturn(Collections.emptyList());
            when(todoService.getTodoStats(tripId, userId)).thenReturn(Map.of(
                    TodoStatus.PENDING, 2L,
                    TodoStatus.COMPLETED, 1L
            ));
            when(todoService.getTodosByTrip(tripId, userId)).thenReturn(Collections.emptyList());
            when(expenseService.getExpenseCount(tripId, userId)).thenReturn(3L);
            when(documentService.getDocumentCount(tripId, userId)).thenReturn(1L);
            when(expenseService.getTotalExpense(eq(tripId), anyString(), eq(userId)))
                    .thenReturn(new BigDecimal("15000"));
        }

        @Test
        @DisplayName("should return trip detail view with all model attributes")
        void showTripDetail_shouldReturnViewWithAttributes() throws Exception {
            mockMvc.perform(get("/trips/{id}", tripId).with(oidcLogin()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("trip/view"))
                    .andExpect(model().attributeExists(
                            "trip", "currentMember", "canEdit", "isOwner",
                            "name", "picture", "tripDays", "tripNights",
                            "members", "memberCount", "activityCount",
                            "expenseCount", "documentCount", "totalExpense",
                            "averageExpense", "todoCount", "todoCompletedCount",
                            "upcomingTodos", "weatherFallbackLat", "weatherFallbackLng"))
                    .andExpect(model().attribute("canEdit", true))
                    .andExpect(model().attribute("isOwner", true))
                    .andExpect(model().attribute("expenseCount", 3L))
                    .andExpect(model().attribute("documentCount", 1L));
        }

        @Test
        @DisplayName("should redirect when trip not found")
        void showTripDetail_tripNotFound_shouldRedirect() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(null);

            mockMvc.perform(get("/trips/{id}", tripId).with(oidcLogin()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard?error=trip_not_found"));
        }

        @Test
        @DisplayName("should set canEdit false for viewer")
        void showTripDetail_viewer_shouldNotCanEdit() throws Exception {
            TripResponse.MemberSummary viewerMember = TripResponse.MemberSummary.builder()
                    .userId(userId)
                    .nickname("Test User")
                    .role(Role.VIEWER)
                    .build();

            TripResponse viewerTrip = TripResponse.builder()
                    .id(tripId)
                    .title("Tokyo Trip")
                    .startDate(LocalDate.now().plusDays(10))
                    .endDate(LocalDate.now().plusDays(15))
                    .baseCurrency("TWD")
                    .members(List.of(viewerMember))
                    .build();

            when(tripService.getTrip(tripId, userId)).thenReturn(viewerTrip);

            mockMvc.perform(get("/trips/{id}", tripId).with(oidcLogin()))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("canEdit", false))
                    .andExpect(model().attribute("isOwner", false));
        }

        @Test
        @DisplayName("should calculate days until trip")
        void showTripDetail_futureTrip_shouldCalculateDaysUntil() throws Exception {
            mockMvc.perform(get("/trips/{id}", tripId).with(oidcLogin()))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("daysUntil"));
        }
    }

    @Nested
    @DisplayName("GET /trips/create")
    class ShowCreateFormTests {

        @Test
        @DisplayName("should return create form view")
        void showCreateForm_shouldReturnView() throws Exception {
            mockMvc.perform(get("/trips/create").with(oidcLogin()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("trip/create"))
                    .andExpect(model().attributeExists("name", "picture", "minDate", "isEdit", "trip"))
                    .andExpect(model().attribute("isEdit", false));
        }
    }

    @Nested
    @DisplayName("POST /trips/create")
    class CreateTripTests {

        @Test
        @DisplayName("should create trip and redirect")
        void createTrip_validData_shouldRedirect() throws Exception {
            TripResponse createdTrip = TripResponse.builder()
                    .id(tripId)
                    .title("New Trip")
                    .build();

            when(tripService.createTrip(any(), eq(testUser))).thenReturn(createdTrip);

            mockMvc.perform(post("/trips/create")
                            .param("title", "New Trip")
                            .param("description", "A description")
                            .param("startDate", LocalDate.now().plusDays(5).toString())
                            .param("endDate", LocalDate.now().plusDays(10).toString())
                            .with(oidcLogin())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId));
        }

        @Test
        @DisplayName("should return form with error when end date before start date")
        void createTrip_invalidDates_shouldReturnForm() throws Exception {
            mockMvc.perform(post("/trips/create")
                            .param("title", "New Trip")
                            .param("startDate", LocalDate.now().plusDays(10).toString())
                            .param("endDate", LocalDate.now().plusDays(5).toString())
                            .with(oidcLogin())
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("trip/create"))
                    .andExpect(model().attributeExists("dateError"));
        }
    }

    @Nested
    @DisplayName("GET /trips/{id}/edit")
    class ShowEditFormTests {

        @Test
        @DisplayName("should return edit form for owner")
        void showEditForm_owner_shouldReturnForm() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);

            mockMvc.perform(get("/trips/{id}/edit", tripId).with(oidcLogin()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("trip/create"))
                    .andExpect(model().attribute("isEdit", true))
                    .andExpect(model().attributeExists("trip"));
        }

        @Test
        @DisplayName("should redirect when viewer tries to edit")
        void showEditForm_viewer_shouldRedirect() throws Exception {
            TripResponse.MemberSummary viewerMember = TripResponse.MemberSummary.builder()
                    .userId(userId)
                    .nickname("Test User")
                    .role(Role.VIEWER)
                    .build();

            TripResponse viewerTrip = TripResponse.builder()
                    .id(tripId)
                    .title("Tokyo Trip")
                    .startDate(LocalDate.now().plusDays(10))
                    .endDate(LocalDate.now().plusDays(15))
                    .members(List.of(viewerMember))
                    .build();

            when(tripService.getTrip(tripId, userId)).thenReturn(viewerTrip);

            mockMvc.perform(get("/trips/{id}/edit", tripId).with(oidcLogin()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "?error=access_denied"));
        }

        @Test
        @DisplayName("should redirect when trip not found")
        void showEditForm_notFound_shouldRedirect() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(null);

            mockMvc.perform(get("/trips/{id}/edit", tripId).with(oidcLogin()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard?error=trip_not_found"));
        }
    }

    @Nested
    @DisplayName("POST /trips/{id}/edit")
    class UpdateTripTests {

        @Test
        @DisplayName("should update trip and redirect")
        void updateTrip_validData_shouldRedirect() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
            when(tripService.updateTrip(eq(tripId), any(), eq(userId))).thenReturn(testTrip);

            mockMvc.perform(post("/trips/{id}/edit", tripId)
                            .param("title", "Updated Trip")
                            .param("startDate", LocalDate.now().plusDays(10).toString())
                            .param("endDate", LocalDate.now().plusDays(15).toString())
                            .with(oidcLogin())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId));
        }

        @Test
        @DisplayName("should return form with error when dates invalid")
        void updateTrip_invalidDates_shouldReturnForm() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);

            mockMvc.perform(post("/trips/{id}/edit", tripId)
                            .param("title", "Updated Trip")
                            .param("startDate", LocalDate.now().plusDays(15).toString())
                            .param("endDate", LocalDate.now().plusDays(10).toString())
                            .with(oidcLogin())
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("trip/create"))
                    .andExpect(model().attributeExists("dateError"));
        }

        @Test
        @DisplayName("should redirect when viewer tries to update")
        void updateTrip_viewer_shouldRedirect() throws Exception {
            TripResponse.MemberSummary viewerMember = TripResponse.MemberSummary.builder()
                    .userId(userId)
                    .nickname("Test User")
                    .role(Role.VIEWER)
                    .build();

            TripResponse viewerTrip = TripResponse.builder()
                    .id(tripId)
                    .title("Tokyo Trip")
                    .startDate(LocalDate.now().plusDays(10))
                    .endDate(LocalDate.now().plusDays(15))
                    .members(List.of(viewerMember))
                    .build();

            when(tripService.getTrip(tripId, userId)).thenReturn(viewerTrip);

            mockMvc.perform(post("/trips/{id}/edit", tripId)
                            .param("title", "Updated")
                            .param("startDate", LocalDate.now().plusDays(10).toString())
                            .param("endDate", LocalDate.now().plusDays(15).toString())
                            .with(oidcLogin())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "?error=access_denied"));
        }
    }
}
