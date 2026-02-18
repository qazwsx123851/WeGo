package com.wego.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.dto.request.CreatePersonalExpenseRequest;
import com.wego.dto.request.SetPersonalBudgetRequest;
import com.wego.dto.request.UpdatePersonalExpenseRequest;
import com.wego.dto.response.PersonalExpenseItemResponse;
import com.wego.dto.response.PersonalExpenseSummaryResponse;
import com.wego.entity.User;
import com.wego.security.UserPrincipal;
import com.wego.service.PersonalExpenseService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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
 * Tests for PersonalExpenseApiController.
 *
 * @contract
 *   - Tests all API endpoints for personal expenses
 *   - Verifies request validation
 *   - Tests authentication enforcement
 */
@WebMvcTest(PersonalExpenseApiController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class PersonalExpenseApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PersonalExpenseService personalExpenseService;

    private UUID tripId;
    private UUID expenseId;
    private UUID userId;
    private UserPrincipal userPrincipal;
    private PersonalExpenseItemResponse testItemResponse;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        expenseId = UUID.randomUUID();
        userId = UUID.randomUUID();

        User testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .provider("google")
                .providerId("google-123")
                .build();

        // Create UserPrincipal with attributes containing "sub" for authentication
        Map<String, Object> attributes = Map.of("sub", userId.toString());
        userPrincipal = new UserPrincipal(testUser, attributes);

        testItemResponse = PersonalExpenseItemResponse.builder()
                .source(PersonalExpenseItemResponse.Source.MANUAL)
                .id(expenseId)
                .description("Test Personal Expense")
                .amount(new BigDecimal("500"))
                .originalAmount(new BigDecimal("500"))
                .originalCurrency("TWD")
                .category("Food")
                .expenseDate(LocalDate.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/trips/{tripId}/personal-expenses")
    class ListPersonalExpensesTests {

        @Test
        @DisplayName("should return 200 with list of personal expenses")
        void listPersonalExpenses_withValidTrip_shouldReturn200() throws Exception {
            // Given
            List<PersonalExpenseItemResponse> items = Collections.singletonList(testItemResponse);
            when(personalExpenseService.getPersonalExpenses(any(UUID.class), eq(tripId)))
                    .thenReturn(items);

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}/personal-expenses", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].description").value("Test Personal Expense"))
                    .andExpect(jsonPath("$.data[0].source").value("MANUAL"));
        }
    }

    @Nested
    @DisplayName("POST /api/trips/{tripId}/personal-expenses")
    class CreatePersonalExpenseTests {

        @Test
        @DisplayName("should create personal expense and return 201")
        void createPersonalExpense_withValidInput_shouldReturn201() throws Exception {
            // Given
            CreatePersonalExpenseRequest request = CreatePersonalExpenseRequest.builder()
                    .description("Taxi fare")
                    .amount(new BigDecimal("300"))
                    .currency("TWD")
                    .category("Transport")
                    .expenseDate(LocalDate.now())
                    .build();

            when(personalExpenseService.createPersonalExpense(
                    any(UUID.class), eq(tripId), any(CreatePersonalExpenseRequest.class)))
                    .thenReturn(testItemResponse);

            // When & Then
            mockMvc.perform(post("/api/trips/{tripId}/personal-expenses", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(expenseId.toString()))
                    .andExpect(jsonPath("$.data.description").value("Test Personal Expense"));
        }

        @Test
        @DisplayName("should return 400 when description is missing")
        void createPersonalExpense_withMissingDescription_shouldReturn400() throws Exception {
            // Given - no description (required field omitted)
            CreatePersonalExpenseRequest request = CreatePersonalExpenseRequest.builder()
                    .amount(new BigDecimal("300"))
                    .currency("TWD")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/trips/{tripId}/personal-expenses", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("PUT /api/trips/{tripId}/personal-expenses/{id}")
    class UpdatePersonalExpenseTests {

        @Test
        @DisplayName("should update personal expense and return 200")
        void updatePersonalExpense_withValidInput_shouldReturn200() throws Exception {
            // Given
            UpdatePersonalExpenseRequest request = UpdatePersonalExpenseRequest.builder()
                    .description("Updated Taxi fare")
                    .amount(new BigDecimal("350"))
                    .build();

            PersonalExpenseItemResponse updatedResponse = PersonalExpenseItemResponse.builder()
                    .source(PersonalExpenseItemResponse.Source.MANUAL)
                    .id(expenseId)
                    .description("Updated Taxi fare")
                    .amount(new BigDecimal("350"))
                    .originalAmount(new BigDecimal("350"))
                    .originalCurrency("TWD")
                    .build();

            when(personalExpenseService.updatePersonalExpense(
                    eq(expenseId), any(UUID.class), any(UpdatePersonalExpenseRequest.class)))
                    .thenReturn(updatedResponse);

            // When & Then
            mockMvc.perform(put("/api/trips/{tripId}/personal-expenses/{id}", tripId, expenseId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.description").value("Updated Taxi fare"))
                    .andExpect(jsonPath("$.data.amount").value(350));
        }
    }

    @Nested
    @DisplayName("DELETE /api/trips/{tripId}/personal-expenses/{id}")
    class DeletePersonalExpenseTests {

        @Test
        @DisplayName("should delete personal expense and return 204")
        void deletePersonalExpense_withValidInput_shouldReturn204() throws Exception {
            // Given
            doNothing().when(personalExpenseService)
                    .deletePersonalExpense(eq(expenseId), any(UUID.class));

            // When & Then
            mockMvc.perform(delete("/api/trips/{tripId}/personal-expenses/{id}", tripId, expenseId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("GET /api/trips/{tripId}/personal-expenses/summary")
    class GetPersonalSummaryTests {

        @Test
        @DisplayName("should return 200 with personal expense summary")
        void getPersonalSummary_withValidTrip_shouldReturn200() throws Exception {
            // Given
            PersonalExpenseSummaryResponse summaryResponse = PersonalExpenseSummaryResponse.builder()
                    .totalAmount(new BigDecimal("1500"))
                    .dailyAverage(new BigDecimal("500"))
                    .categoryBreakdown(Map.of("Food", new BigDecimal("800"), "Transport", new BigDecimal("700")))
                    .dailyAmounts(Map.of(LocalDate.now(), new BigDecimal("1500")))
                    .budget(new BigDecimal("2000"))
                    .budgetStatus(PersonalExpenseSummaryResponse.BudgetStatus.GREEN)
                    .build();

            when(personalExpenseService.getPersonalSummary(any(UUID.class), eq(tripId)))
                    .thenReturn(summaryResponse);

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}/personal-expenses/summary", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalAmount").value(1500))
                    .andExpect(jsonPath("$.data.budgetStatus").value("GREEN"));
        }
    }

    @Nested
    @DisplayName("PUT /api/trips/{tripId}/personal-expenses/budget")
    class SetPersonalBudgetTests {

        @Test
        @DisplayName("should set personal budget and return 200")
        void setPersonalBudget_withValidInput_shouldReturn200() throws Exception {
            // Given
            SetPersonalBudgetRequest request = SetPersonalBudgetRequest.builder()
                    .budget(new BigDecimal("5000"))
                    .build();

            doNothing().when(personalExpenseService)
                    .setPersonalBudget(eq(tripId), any(UUID.class), any(SetPersonalBudgetRequest.class));

            // When & Then
            mockMvc.perform(put("/api/trips/{tripId}/personal-expenses/budget", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should return 400 when budget is zero")
        void setPersonalBudget_withZeroBudget_shouldReturn400() throws Exception {
            // Given - budget = 0 violates @DecimalMin("0.01")
            SetPersonalBudgetRequest request = SetPersonalBudgetRequest.builder()
                    .budget(BigDecimal.ZERO)
                    .build();

            // When & Then
            mockMvc.perform(put("/api/trips/{tripId}/personal-expenses/budget", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("Unauthenticated access")
    class UnauthenticatedAccessTests {

        @Test
        @DisplayName("should deny access when not authenticated")
        void listPersonalExpenses_withoutAuth_shouldDenyAccess() throws Exception {
            // When & Then - no oauth2Login(), so request is unauthenticated
            // Spring Security may respond with 401, 302 (redirect to login), or 403
            mockMvc.perform(get("/api/trips/{tripId}/personal-expenses", tripId))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status != 401 && status != 302 && status != 403) {
                            throw new AssertionError(
                                "Expected 401, 302, or 403 but got " + status);
                        }
                    });
        }
    }
}
