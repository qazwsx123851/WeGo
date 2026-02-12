package com.wego.controller.web;

import com.wego.dto.response.ExpenseResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.Role;
import com.wego.entity.SplitType;
import com.wego.entity.User;
import com.wego.exception.ResourceNotFoundException;
import com.wego.security.UserPrincipal;
import com.wego.service.ActivityService;
import com.wego.service.ExpenseService;
import com.wego.service.ExpenseViewHelper;
import com.wego.service.TripService;
import com.wego.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests for ExpenseWebController.
 *
 * @contract
 *   - Tests list, create form, create submission, detail, edit form, update, statistics endpoints
 *   - Verifies model attributes and view names
 *   - Tests authentication and permission checks
 */
@WebMvcTest(ExpenseWebController.class)
@ActiveProfiles("test")
class ExpenseWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private TripService tripService;

    @MockBean
    private ExpenseService expenseService;

    @MockBean
    private ActivityService activityService;

    @MockBean
    private ExpenseViewHelper expenseViewHelper;

    private UUID userId;
    private UUID tripId;
    private UUID expenseId;
    private User testUser;
    private UserPrincipal testPrincipal;
    private TripResponse testTrip;
    private ExpenseResponse testExpense;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tripId = UUID.randomUUID();
        expenseId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .provider("google")
                .providerId("google-id")
                .build();

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

        testExpense = ExpenseResponse.builder()
                .id(expenseId)
                .tripId(tripId)
                .description("Lunch")
                .amount(new BigDecimal("500.00"))
                .currency("TWD")
                .paidBy(userId)
                .paidByName("Test User")
                .splitType(SplitType.EQUAL)
                .category("food")
                .expenseDate(LocalDate.now())
                .build();

        testPrincipal = new UserPrincipal(testUser);

        // Default stubs for ExpenseViewHelper
        when(expenseViewHelper.groupExpensesByDate(any())).thenReturn(new TreeMap<>());
        when(expenseViewHelper.calculatePerPersonAverage(any(), any(int.class))).thenReturn(BigDecimal.ZERO);
        when(expenseViewHelper.parseSplitType(any())).thenReturn(SplitType.EQUAL);
        when(expenseViewHelper.buildSplits(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
    }

    private RequestPostProcessor oauth2Login() {
        return SecurityMockMvcRequestPostProcessors.oauth2Login().oauth2User(testPrincipal);
    }

    @Nested
    @DisplayName("GET /trips/{tripId}/expenses")
    class ListExpensesTests {

        @Test
        @DisplayName("should return expense list view with model attributes")
        void listExpenses_authenticated_shouldReturnListView() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
            when(expenseService.getExpensesByTrip(tripId, userId)).thenReturn(List.of(testExpense));
            when(expenseService.getTotalExpense(eq(tripId), anyString(), eq(userId)))
                    .thenReturn(new BigDecimal("500.00"));
            when(expenseService.calculateUserBalanceInTrip(userId, tripId))
                    .thenReturn(new BigDecimal("250.00"));

            mockMvc.perform(get("/trips/{tripId}/expenses", tripId).with(oauth2Login()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("expense/list"))
                    .andExpect(model().attributeExists(
                            "trip", "expenses", "expensesByDate",
                            "totalExpense", "perPersonAverage", "userBalance",
                            "defaultCurrency", "name", "picture"));
        }

        @Test
        @DisplayName("should redirect to dashboard when trip not found")
        void listExpenses_tripNotFound_shouldRedirect() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(null);

            mockMvc.perform(get("/trips/{tripId}/expenses", tripId).with(oauth2Login()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard?error=trip_not_found"));
        }
    }

    @Nested
    @DisplayName("GET /trips/{tripId}/expenses/create")
    class ShowCreateFormTests {

        @Test
        @DisplayName("should return create form view for owner")
        void showCreateForm_owner_shouldReturnView() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);

            mockMvc.perform(get("/trips/{tripId}/expenses/create", tripId).with(oauth2Login()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("expense/create"))
                    .andExpect(model().attributeExists(
                            "trip", "tripId", "members", "currentUserId",
                            "name", "picture"));
        }

        @Test
        @DisplayName("should redirect when viewer tries to access create form")
        void showCreateForm_viewer_shouldRedirect() throws Exception {
            TripResponse.MemberSummary viewerMember = TripResponse.MemberSummary.builder()
                    .userId(userId)
                    .nickname("Test User")
                    .role(Role.VIEWER)
                    .build();

            TripResponse viewerTrip = TripResponse.builder()
                    .id(tripId)
                    .title("Tokyo Trip")
                    .baseCurrency("TWD")
                    .members(List.of(viewerMember))
                    .build();

            when(tripService.getTrip(tripId, userId)).thenReturn(viewerTrip);

            mockMvc.perform(get("/trips/{tripId}/expenses/create", tripId).with(oauth2Login()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "/expenses?error=access_denied"));
        }
    }

    @Nested
    @DisplayName("POST /trips/{tripId}/expenses")
    class CreateExpenseTests {

        @Test
        @DisplayName("should create expense and redirect to list")
        void createExpense_validData_shouldRedirect() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
            when(expenseService.createExpense(eq(tripId), any(), eq(userId)))
                    .thenReturn(testExpense);

            mockMvc.perform(post("/trips/{tripId}/expenses", tripId)
                            .param("amount", "500.00")
                            .param("currency", "TWD")
                            .param("description", "Lunch")
                            .param("category", "food")
                            .param("expenseDate", LocalDate.now().toString())
                            .param("payerId", userId.toString())
                            .param("splitMethod", "EQUAL")
                            .with(oauth2Login())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "/expenses"));
        }

        @Test
        @DisplayName("should redirect to dashboard when trip not found on create")
        void createExpense_tripNotFound_shouldRedirect() throws Exception {
            when(tripService.getTrip(tripId, userId))
                    .thenThrow(new ResourceNotFoundException("Trip", tripId.toString()));

            mockMvc.perform(post("/trips/{tripId}/expenses", tripId)
                            .param("amount", "500.00")
                            .param("description", "Lunch")
                            .param("payerId", userId.toString())
                            .with(oauth2Login())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard"));
        }
    }

    @Nested
    @DisplayName("GET /trips/{tripId}/expenses/{expenseId}")
    class ShowExpenseDetailTests {

        @Test
        @DisplayName("should return expense detail view with model attributes")
        void showDetail_shouldReturnView() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
            when(expenseService.getExpense(expenseId, userId)).thenReturn(testExpense);

            mockMvc.perform(get("/trips/{tripId}/expenses/{expenseId}", tripId, expenseId)
                            .with(oauth2Login()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("expense/detail"))
                    .andExpect(model().attributeExists(
                            "trip", "expense", "canEdit", "isOwner",
                            "name", "picture"))
                    .andExpect(model().attribute("canEdit", true))
                    .andExpect(model().attribute("isOwner", true));
        }

        @Test
        @DisplayName("should redirect when expense not found")
        void showDetail_expenseNotFound_shouldRedirect() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
            when(expenseService.getExpense(expenseId, userId))
                    .thenThrow(new ResourceNotFoundException("Expense", expenseId.toString()));

            mockMvc.perform(get("/trips/{tripId}/expenses/{expenseId}", tripId, expenseId)
                            .with(oauth2Login()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "/expenses?error=expense_not_found"));
        }
    }

    @Nested
    @DisplayName("GET /trips/{tripId}/expenses/{expenseId}/edit")
    class ShowEditFormTests {

        @Test
        @DisplayName("should return edit form for owner")
        void showEditForm_owner_shouldReturnView() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
            when(expenseService.getExpense(expenseId, userId)).thenReturn(testExpense);

            mockMvc.perform(get("/trips/{tripId}/expenses/{expenseId}/edit", tripId, expenseId)
                            .with(oauth2Login()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("expense/create"))
                    .andExpect(model().attributeExists(
                            "trip", "tripId", "members", "currentUserId",
                            "expense", "name", "picture"));
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
                    .baseCurrency("TWD")
                    .members(List.of(viewerMember))
                    .build();

            when(tripService.getTrip(tripId, userId)).thenReturn(viewerTrip);

            mockMvc.perform(get("/trips/{tripId}/expenses/{expenseId}/edit", tripId, expenseId)
                            .with(oauth2Login()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "/expenses?error=access_denied"));
        }
    }

    @Nested
    @DisplayName("POST /trips/{tripId}/expenses/{expenseId}")
    class UpdateExpenseTests {

        @Test
        @DisplayName("should update expense and redirect to list")
        void updateExpense_validData_shouldRedirect() throws Exception {
            when(expenseService.updateExpense(eq(expenseId), any(), eq(userId)))
                    .thenReturn(testExpense);

            mockMvc.perform(post("/trips/{tripId}/expenses/{expenseId}", tripId, expenseId)
                            .param("amount", "600.00")
                            .param("currency", "TWD")
                            .param("description", "Dinner")
                            .param("category", "food")
                            .param("expenseDate", LocalDate.now().toString())
                            .param("payerId", userId.toString())
                            .param("splitMethod", "EQUAL")
                            .with(oauth2Login())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "/expenses"));
        }

        @Test
        @DisplayName("should redirect when update throws ResourceNotFoundException")
        void updateExpense_notFound_shouldRedirect() throws Exception {
            when(expenseService.updateExpense(eq(expenseId), any(), eq(userId)))
                    .thenThrow(new ResourceNotFoundException("Expense", expenseId.toString()));

            mockMvc.perform(post("/trips/{tripId}/expenses/{expenseId}", tripId, expenseId)
                            .param("amount", "600.00")
                            .param("description", "Dinner")
                            .param("payerId", userId.toString())
                            .with(oauth2Login())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "/expenses"));
        }
    }

    @Nested
    @DisplayName("GET /trips/{tripId}/expenses/statistics")
    class ShowStatisticsTests {

        @Test
        @DisplayName("should return statistics view with model attributes")
        void showStatistics_shouldReturnView() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);

            mockMvc.perform(get("/trips/{tripId}/expenses/statistics", tripId)
                            .with(oauth2Login()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("expense/statistics"))
                    .andExpect(model().attributeExists("trip", "tripId", "name", "picture"));
        }

        @Test
        @DisplayName("should redirect when trip not found")
        void showStatistics_tripNotFound_shouldRedirect() throws Exception {
            when(tripService.getTrip(tripId, userId)).thenReturn(null);

            mockMvc.perform(get("/trips/{tripId}/expenses/statistics", tripId)
                            .with(oauth2Login()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard?error=trip_not_found"));
        }
    }
}
