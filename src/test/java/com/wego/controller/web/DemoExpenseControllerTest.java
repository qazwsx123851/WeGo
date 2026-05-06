package com.wego.controller.web;

import com.wego.config.SecurityConfig;
import com.wego.security.CustomOAuth2UserService;
import com.wego.service.DemoDataProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Web controller tests for {@link DemoExpenseController}.
 *
 * <p>Demo routes are public; uses real {@link DemoDataProvider} bean.
 */
@WebMvcTest(DemoExpenseController.class)
@Import({DemoDataProvider.class, SecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("DemoExpenseController")
class DemoExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemoDataProvider demoDataProvider;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @Nested
    @DisplayName("GET /demo/trip/expenses (list)")
    class ListTests {

        @Test
        @DisplayName("returns 200 with view demo/expense/list and summary attributes")
        void listDemoExpenses_returns200WithSummaryAttributes() throws Exception {
            mockMvc.perform(get("/demo/trip/expenses"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("demo/expense/list"))
                    .andExpect(model().attributeExists("trip"))
                    .andExpect(model().attributeExists("expenses"))
                    .andExpect(model().attributeExists("expensesByDate"))
                    .andExpect(model().attributeExists("totalExpense"))
                    .andExpect(model().attributeExists("perPersonAverage"))
                    .andExpect(model().attributeExists("userBalance"))
                    .andExpect(model().attribute("isDemo", true))
                    .andExpect(model().attribute("defaultCurrency", "JPY"));
        }
    }

    @Nested
    @DisplayName("GET /demo/trip/expenses/{expenseId} (detail)")
    class DetailTests {

        @Test
        @DisplayName("returns 200 with view demo/expense/detail when expenseId exists")
        void showDemoExpenseDetail_validId_returns200() throws Exception {
            UUID validId = demoDataProvider.getDemoExpenses().get(0).getId();

            mockMvc.perform(get("/demo/trip/expenses/{expenseId}", validId))
                    .andExpect(status().isOk())
                    .andExpect(view().name("demo/expense/detail"))
                    .andExpect(model().attributeExists("trip"))
                    .andExpect(model().attributeExists("expense"))
                    .andExpect(model().attribute("isDemo", true));
        }

        @Test
        @DisplayName("returns 404 when expenseId does not exist")
        void showDemoExpenseDetail_invalidId_returns404() throws Exception {
            UUID nonExistentId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

            mockMvc.perform(get("/demo/trip/expenses/{expenseId}", nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }
}
