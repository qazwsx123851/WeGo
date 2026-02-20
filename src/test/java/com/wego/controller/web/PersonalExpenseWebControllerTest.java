package com.wego.controller.web;

import com.wego.entity.User;
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

import java.time.LocalDate;
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
    @DisplayName("POST /{id} update")
    class UpdatePersonalExpense {

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
    }
}
