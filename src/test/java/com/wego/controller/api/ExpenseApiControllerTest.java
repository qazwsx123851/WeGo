package com.wego.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.dto.request.CreateExpenseRequest;
import com.wego.dto.request.UpdateExpenseRequest;
import com.wego.dto.response.ExpenseResponse;
import com.wego.dto.response.ExpenseSplitResponse;
import com.wego.dto.response.SettlementResponse;
import com.wego.entity.SplitType;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.service.ExpenseService;
import com.wego.service.SettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for ExpenseApiController.
 *
 * @contract
 *   - Tests all API endpoints
 *   - Verifies request validation
 *   - Tests error handling
 */
@WebMvcTest(ExpenseApiController.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExpenseApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExpenseService expenseService;

    @MockBean
    private SettlementService settlementService;

    private UUID tripId;
    private UUID expenseId;
    private UUID splitId;
    private UUID userId;
    private ExpenseResponse testExpenseResponse;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        expenseId = UUID.randomUUID();
        splitId = UUID.randomUUID();
        userId = UUID.randomUUID();

        ExpenseSplitResponse splitResponse = ExpenseSplitResponse.builder()
                .id(splitId)
                .userId(userId)
                .userNickname("Test User")
                .amount(new BigDecimal("500"))
                .isSettled(false)
                .build();

        testExpenseResponse = ExpenseResponse.builder()
                .id(expenseId)
                .tripId(tripId)
                .description("Test Expense")
                .amount(new BigDecimal("1000"))
                .currency("TWD")
                .paidBy(userId)
                .paidByName("Test User")
                .splitType(SplitType.EQUAL)
                .splits(Collections.singletonList(splitResponse))
                .category("Food")
                .expenseDate(LocalDate.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/trips/{tripId}/expenses")
    class CreateExpenseTests {

        @Test
        @WithMockUser
        @DisplayName("should create expense and return 201")
        void createExpense_withValidInput_shouldReturn201() throws Exception {
            // Given
            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .description("Dinner")
                    .amount(new BigDecimal("1000"))
                    .currency("TWD")
                    .paidBy(userId)
                    .splitType(SplitType.EQUAL)
                    .category("Food")
                    .build();

            when(expenseService.createExpense(eq(tripId), any(CreateExpenseRequest.class), any(UUID.class)))
                    .thenReturn(testExpenseResponse);

            // When & Then
            mockMvc.perform(post("/api/trips/{tripId}/expenses", tripId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(expenseId.toString()))
                    .andExpect(jsonPath("$.data.description").value("Test Expense"));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 400 when description is missing")
        void createExpense_withMissingDescription_shouldReturn400() throws Exception {
            // Given
            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .amount(new BigDecimal("1000"))
                    .paidBy(userId)
                    .splitType(SplitType.EQUAL)
                    .build();

            // When & Then
            mockMvc.perform(post("/api/trips/{tripId}/expenses", tripId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 400 when amount is zero or negative")
        void createExpense_withInvalidAmount_shouldReturn400() throws Exception {
            // Given
            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .description("Dinner")
                    .amount(BigDecimal.ZERO)
                    .paidBy(userId)
                    .splitType(SplitType.EQUAL)
                    .build();

            // When & Then
            mockMvc.perform(post("/api/trips/{tripId}/expenses", tripId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 403 when user has no permission")
        void createExpense_withNoPermission_shouldReturn403() throws Exception {
            // Given
            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .description("Dinner")
                    .amount(new BigDecimal("1000"))
                    .paidBy(userId)
                    .splitType(SplitType.EQUAL)
                    .build();

            when(expenseService.createExpense(eq(tripId), any(CreateExpenseRequest.class), any(UUID.class)))
                    .thenThrow(new ForbiddenException("No permission to edit trip"));

            // When & Then
            mockMvc.perform(post("/api/trips/{tripId}/expenses", tripId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 404 when trip not found")
        void createExpense_withNonExistentTrip_shouldReturn404() throws Exception {
            // Given
            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .description("Dinner")
                    .amount(new BigDecimal("1000"))
                    .paidBy(userId)
                    .splitType(SplitType.EQUAL)
                    .build();

            when(expenseService.createExpense(eq(tripId), any(CreateExpenseRequest.class), any(UUID.class)))
                    .thenThrow(new ResourceNotFoundException("Trip", tripId.toString()));

            // When & Then
            mockMvc.perform(post("/api/trips/{tripId}/expenses", tripId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/trips/{tripId}/expenses")
    class GetExpensesTests {

        @Test
        @WithMockUser
        @DisplayName("should return expenses list")
        void getExpenses_withValidTrip_shouldReturn200() throws Exception {
            // Given
            when(expenseService.getExpensesByTrip(eq(tripId), any(UUID.class)))
                    .thenReturn(Collections.singletonList(testExpenseResponse));

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}/expenses", tripId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].description").value("Test Expense"));
        }

        @Test
        @WithMockUser
        @DisplayName("should return empty list when no expenses")
        void getExpenses_withNoExpenses_shouldReturnEmptyList() throws Exception {
            // Given
            when(expenseService.getExpensesByTrip(eq(tripId), any(UUID.class)))
                    .thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}/expenses", tripId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @WithMockUser
        @DisplayName("should return 403 when user has no view permission")
        void getExpenses_withNoPermission_shouldReturn403() throws Exception {
            // Given
            when(expenseService.getExpensesByTrip(eq(tripId), any(UUID.class)))
                    .thenThrow(new ForbiddenException("No permission to view trip"));

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}/expenses", tripId))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("PUT /api/expenses/{expenseId}")
    class UpdateExpenseTests {

        @Test
        @WithMockUser
        @DisplayName("should update expense and return 200")
        void updateExpense_withValidInput_shouldReturn200() throws Exception {
            // Given
            UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                    .description("Updated Dinner")
                    .amount(new BigDecimal("1500"))
                    .build();

            ExpenseResponse updatedResponse = ExpenseResponse.builder()
                    .id(expenseId)
                    .tripId(tripId)
                    .description("Updated Dinner")
                    .amount(new BigDecimal("1500"))
                    .currency("TWD")
                    .paidBy(userId)
                    .splitType(SplitType.EQUAL)
                    .build();

            when(expenseService.updateExpense(eq(expenseId), any(UpdateExpenseRequest.class), any(UUID.class)))
                    .thenReturn(updatedResponse);

            // When & Then
            mockMvc.perform(put("/api/expenses/{expenseId}", expenseId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.description").value("Updated Dinner"))
                    .andExpect(jsonPath("$.data.amount").value(1500));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 404 when expense not found")
        void updateExpense_withNonExistentExpense_shouldReturn404() throws Exception {
            // Given
            UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                    .description("Updated")
                    .build();

            when(expenseService.updateExpense(eq(expenseId), any(UpdateExpenseRequest.class), any(UUID.class)))
                    .thenThrow(new ResourceNotFoundException("Expense", expenseId.toString()));

            // When & Then
            mockMvc.perform(put("/api/expenses/{expenseId}", expenseId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("DELETE /api/expenses/{expenseId}")
    class DeleteExpenseTests {

        @Test
        @WithMockUser
        @DisplayName("should delete expense and return 200")
        void deleteExpense_withValidInput_shouldReturn200() throws Exception {
            // Given
            doNothing().when(expenseService).deleteExpense(eq(expenseId), any(UUID.class));

            // When & Then
            mockMvc.perform(delete("/api/expenses/{expenseId}", expenseId)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @WithMockUser
        @DisplayName("should return 403 when user cannot delete")
        void deleteExpense_withNoPermission_shouldReturn403() throws Exception {
            // Given
            doThrow(new ForbiddenException("Cannot delete this expense"))
                    .when(expenseService).deleteExpense(eq(expenseId), any(UUID.class));

            // When & Then
            mockMvc.perform(delete("/api/expenses/{expenseId}", expenseId)
                            .with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/trips/{tripId}/settlement")
    class GetSettlementTests {

        @Test
        @WithMockUser
        @DisplayName("should return settlement calculation")
        void getSettlement_withValidTrip_shouldReturn200() throws Exception {
            // Given
            SettlementResponse.SettlementItemResponse item = SettlementResponse.SettlementItemResponse.builder()
                    .fromUserId(UUID.randomUUID())
                    .fromUserName("User 2")
                    .toUserId(userId)
                    .toUserName("User 1")
                    .amount(new BigDecimal("500"))
                    .build();

            SettlementResponse response = SettlementResponse.builder()
                    .settlements(Collections.singletonList(item))
                    .totalExpenses(new BigDecimal("1000"))
                    .baseCurrency("TWD")
                    .expenseCount(1)
                    .build();

            when(settlementService.calculateSettlement(eq(tripId), any(UUID.class)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}/settlement", tripId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalExpenses").value(1000))
                    .andExpect(jsonPath("$.data.baseCurrency").value("TWD"))
                    .andExpect(jsonPath("$.data.settlements").isArray())
                    .andExpect(jsonPath("$.data.settlements[0].amount").value(500));
        }

        @Test
        @WithMockUser
        @DisplayName("should return empty settlements when no expenses")
        void getSettlement_withNoExpenses_shouldReturnEmptySettlements() throws Exception {
            // Given
            SettlementResponse response = SettlementResponse.builder()
                    .settlements(Collections.emptyList())
                    .totalExpenses(BigDecimal.ZERO)
                    .baseCurrency("TWD")
                    .expenseCount(0)
                    .build();

            when(settlementService.calculateSettlement(eq(tripId), any(UUID.class)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}/settlement", tripId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.settlements").isEmpty())
                    .andExpect(jsonPath("$.data.totalExpenses").value(0));
        }
    }

    @Nested
    @DisplayName("PUT /api/expense-splits/{splitId}/settle")
    class MarkAsSettledTests {

        @Test
        @WithMockUser
        @DisplayName("should mark split as settled")
        void markAsSettled_withValidInput_shouldReturn200() throws Exception {
            // Given
            doNothing().when(settlementService).markAsSettled(eq(splitId), any(UUID.class));

            // When & Then
            mockMvc.perform(put("/api/expense-splits/{splitId}/settle", splitId)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 404 when split not found")
        void markAsSettled_withNonExistentSplit_shouldReturn404() throws Exception {
            // Given
            doThrow(new ResourceNotFoundException("ExpenseSplit", splitId.toString()))
                    .when(settlementService).markAsSettled(eq(splitId), any(UUID.class));

            // When & Then
            mockMvc.perform(put("/api/expense-splits/{splitId}/settle", splitId)
                            .with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("PUT /api/expense-splits/{splitId}/unsettle")
    class MarkAsUnsettledTests {

        @Test
        @WithMockUser
        @DisplayName("should mark split as unsettled")
        void markAsUnsettled_withValidInput_shouldReturn200() throws Exception {
            // Given
            doNothing().when(settlementService).markAsUnsettled(eq(splitId), any(UUID.class));

            // When & Then
            mockMvc.perform(put("/api/expense-splits/{splitId}/unsettle", splitId)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 403 when user has no permission")
        void markAsUnsettled_withNoPermission_shouldReturn403() throws Exception {
            // Given
            doThrow(new ForbiddenException("No permission"))
                    .when(settlementService).markAsUnsettled(eq(splitId), any(UUID.class));

            // When & Then
            mockMvc.perform(put("/api/expense-splits/{splitId}/unsettle", splitId)
                            .with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
