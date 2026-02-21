package com.wego.controller.web;

import com.wego.dto.response.PersonalExpenseItemResponse;
import com.wego.entity.User;
import com.wego.exception.ResourceNotFoundException;
import com.wego.exception.ValidationException;
import com.wego.security.UserPrincipal;
import com.wego.service.PersonalExpenseService;
import com.wego.service.PersonalExpenseService.TripDateRange;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PersonalExpenseWebController.class)
@ActiveProfiles("test")
class PersonalExpenseWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PersonalExpenseService personalExpenseService;

    private UUID userId;
    private UUID tripId;
    private UserPrincipal testPrincipal;
    private static final LocalDate TRIP_START = LocalDate.of(2024, 3, 1);
    private static final LocalDate TRIP_END = LocalDate.of(2024, 3, 31);

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tripId = UUID.randomUUID();

        User testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .provider("google")
                .providerId("google-id")
                .build();
        testPrincipal = new UserPrincipal(testUser);

        when(personalExpenseService.getBaseCurrency(eq(tripId), any())).thenReturn("TWD");
        when(personalExpenseService.getTripDateRange(eq(tripId), any()))
                .thenReturn(new TripDateRange(TRIP_START, TRIP_END));
    }

    @Nested
    @DisplayName("GET /create")
    class ShowCreateForm {

        @Test
        @DisplayName("should include baseCurrency and tripDateRange in model")
        void includesBaseCurrencyAndDateRange() throws Exception {
            mockMvc.perform(get("/trips/{tripId}/personal-expenses/create", tripId)
                    .with(SecurityMockMvcRequestPostProcessors.oauth2Login()
                            .oauth2User(testPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("baseCurrency", "TWD"))
                    .andExpect(model().attribute("tripStartDate", TRIP_START))
                    .andExpect(model().attribute("tripEndDate", TRIP_END))
                    .andExpect(model().attributeExists("tripId", "categories"))
                    .andExpect(view().name("expense/personal-create"));
        }

        @Test
        @DisplayName("with JPY baseCurrency passes JPY to model")
        void jpyBaseCurrency() throws Exception {
            when(personalExpenseService.getBaseCurrency(eq(tripId), any())).thenReturn("JPY");

            mockMvc.perform(get("/trips/{tripId}/personal-expenses/create", tripId)
                    .with(SecurityMockMvcRequestPostProcessors.oauth2Login()
                            .oauth2User(testPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("baseCurrency", "JPY"));
        }
    }

    @Nested
    @DisplayName("POST /create")
    class CreatePersonalExpense {

        @Test
        @DisplayName("date outside trip range returns to form with dateError")
        void dateOutsideRange_returnsFormWithError() throws Exception {
            when(personalExpenseService.createPersonalExpense(any(), eq(tripId), any()))
                    .thenThrow(new ValidationException("INVALID_DATE", "日期超出行程期間"));

            mockMvc.perform(post("/trips/{tripId}/personal-expenses", tripId)
                    .with(SecurityMockMvcRequestPostProcessors.oauth2Login()
                            .oauth2User(testPrincipal))
                    .with(csrf())
                    .param("description", "Test expense")
                    .param("amount", "100")
                    .param("expenseDate", "2024-04-01"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("expense/personal-create"))
                    .andExpect(model().attribute("dateError", "日期超出行程期間"))
                    .andExpect(model().attribute("tripStartDate", TRIP_START))
                    .andExpect(model().attribute("tripEndDate", TRIP_END))
                    .andExpect(model().attributeExists("baseCurrency", "categories"));
        }

        @Test
        @DisplayName("binding errors return to form without calling service")
        void bindingErrors_returnsForm() throws Exception {
            mockMvc.perform(post("/trips/{tripId}/personal-expenses", tripId)
                    .with(SecurityMockMvcRequestPostProcessors.oauth2Login()
                            .oauth2User(testPrincipal))
                    .with(csrf())
                    .param("description", "")
                    .param("amount", "0"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("expense/personal-create"))
                    .andExpect(model().attribute("tripStartDate", TRIP_START))
                    .andExpect(model().attribute("tripEndDate", TRIP_END));

            verify(personalExpenseService, never()).createPersonalExpense(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("POST /create success")
    class CreatePersonalExpenseSuccess {

        @Test
        @DisplayName("valid data redirects to expenses tab=personal")
        void validData_redirectsToExpensesTab() throws Exception {
            when(personalExpenseService.createPersonalExpense(any(), eq(tripId), any()))
                    .thenReturn(PersonalExpenseItemResponse.builder()
                            .source(PersonalExpenseItemResponse.Source.MANUAL)
                            .id(UUID.randomUUID())
                            .description("Lunch")
                            .amount(new BigDecimal("500"))
                            .build());

            mockMvc.perform(post("/trips/{tripId}/personal-expenses", tripId)
                    .with(SecurityMockMvcRequestPostProcessors.oauth2Login()
                            .oauth2User(testPrincipal))
                    .with(csrf())
                    .param("description", "Lunch")
                    .param("amount", "500")
                    .param("expenseDate", "2024-03-15"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "/expenses?tab=personal"));

            verify(personalExpenseService).createPersonalExpense(any(), eq(tripId), any());
        }
    }

    @Nested
    @DisplayName("GET /{id}/edit")
    class ShowEditForm {

        @Test
        @DisplayName("should return edit form with pre-filled data")
        void showEditForm_shouldReturnView() throws Exception {
            UUID expenseId = UUID.randomUUID();

            PersonalExpenseItemResponse item = PersonalExpenseItemResponse.builder()
                    .source(PersonalExpenseItemResponse.Source.MANUAL)
                    .id(expenseId)
                    .description("Taxi")
                    .amount(new BigDecimal("300"))
                    .originalAmount(new BigDecimal("300"))
                    .originalCurrency("TWD")
                    .category("Transport")
                    .expenseDate(LocalDate.of(2024, 3, 10))
                    .build();

            when(personalExpenseService.getPersonalExpenses(any(), eq(tripId)))
                    .thenReturn(List.of(item));

            mockMvc.perform(get("/trips/{tripId}/personal-expenses/{id}/edit", tripId, expenseId)
                    .with(SecurityMockMvcRequestPostProcessors.oauth2Login()
                            .oauth2User(testPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("expense/personal-edit"))
                    .andExpect(model().attribute("expenseId", expenseId))
                    .andExpect(model().attribute("baseCurrency", "TWD"))
                    .andExpect(model().attribute("tripStartDate", TRIP_START))
                    .andExpect(model().attribute("tripEndDate", TRIP_END))
                    .andExpect(model().attributeExists("request", "categories"));
        }

        @Test
        @DisplayName("should throw when expense not found")
        void showEditForm_notFound_shouldThrow() throws Exception {
            UUID expenseId = UUID.randomUUID();

            when(personalExpenseService.getPersonalExpenses(any(), eq(tripId)))
                    .thenReturn(List.of());

            mockMvc.perform(get("/trips/{tripId}/personal-expenses/{id}/edit", tripId, expenseId)
                    .with(SecurityMockMvcRequestPostProcessors.oauth2Login()
                            .oauth2User(testPrincipal)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /{id} update")
    class UpdatePersonalExpense {

        @Test
        @DisplayName("valid data redirects to expenses tab=personal")
        void validData_redirectsToExpensesTab() throws Exception {
            UUID expenseId = UUID.randomUUID();

            when(personalExpenseService.updatePersonalExpense(eq(expenseId), any(), any()))
                    .thenReturn(PersonalExpenseItemResponse.builder()
                            .source(PersonalExpenseItemResponse.Source.MANUAL)
                            .id(expenseId)
                            .description("Updated expense")
                            .amount(new BigDecimal("200"))
                            .build());

            mockMvc.perform(post("/trips/{tripId}/personal-expenses/{id}", tripId, expenseId)
                    .with(SecurityMockMvcRequestPostProcessors.oauth2Login()
                            .oauth2User(testPrincipal))
                    .with(csrf())
                    .param("description", "Updated expense")
                    .param("amount", "200")
                    .param("expenseDate", "2024-03-15"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trips/" + tripId + "/expenses?tab=personal"));

            verify(personalExpenseService).updatePersonalExpense(eq(expenseId), any(), any());
        }

        @Test
        @DisplayName("date outside trip range returns to edit form with dateError")
        void dateOutsideRange_returnsEditFormWithError() throws Exception {
            UUID expenseId = UUID.randomUUID();

            when(personalExpenseService.updatePersonalExpense(eq(expenseId), any(), any()))
                    .thenThrow(new ValidationException("INVALID_DATE", "日期超出行程期間"));

            mockMvc.perform(post("/trips/{tripId}/personal-expenses/{id}", tripId, expenseId)
                    .with(SecurityMockMvcRequestPostProcessors.oauth2Login()
                            .oauth2User(testPrincipal))
                    .with(csrf())
                    .param("description", "Updated expense")
                    .param("amount", "200")
                    .param("expenseDate", "2024-02-28"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("expense/personal-edit"))
                    .andExpect(model().attribute("dateError", "日期超出行程期間"))
                    .andExpect(model().attribute("tripStartDate", TRIP_START))
                    .andExpect(model().attribute("tripEndDate", TRIP_END))
                    .andExpect(model().attribute("expenseId", expenseId));
        }

        @Test
        @DisplayName("binding errors return to edit form without calling service")
        void bindingErrors_returnsEditForm() throws Exception {
            UUID expenseId = UUID.randomUUID();

            mockMvc.perform(post("/trips/{tripId}/personal-expenses/{id}", tripId, expenseId)
                    .with(SecurityMockMvcRequestPostProcessors.oauth2Login()
                            .oauth2User(testPrincipal))
                    .with(csrf())
                    .param("amount", "-1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("expense/personal-edit"))
                    .andExpect(model().attribute("expenseId", expenseId))
                    .andExpect(model().attribute("tripStartDate", TRIP_START))
                    .andExpect(model().attribute("tripEndDate", TRIP_END));

            verify(personalExpenseService, never()).updatePersonalExpense(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Unauthenticated access")
    class UnauthenticatedAccess {

        @Test
        @DisplayName("GET /create should redirect when not authenticated")
        void createForm_notAuthenticated_shouldRedirect() throws Exception {
            mockMvc.perform(get("/trips/{tripId}/personal-expenses/create", tripId))
                    .andExpect(status().is3xxRedirection());
        }

        @Test
        @DisplayName("POST should redirect when not authenticated")
        void createSubmit_notAuthenticated_shouldRedirect() throws Exception {
            mockMvc.perform(post("/trips/{tripId}/personal-expenses", tripId)
                    .with(csrf())
                    .param("description", "Test")
                    .param("amount", "100"))
                    .andExpect(status().is3xxRedirection());
        }
    }
}
