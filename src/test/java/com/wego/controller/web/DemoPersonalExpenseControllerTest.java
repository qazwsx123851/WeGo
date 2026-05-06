package com.wego.controller.web;

import com.wego.config.SecurityConfig;
import com.wego.security.CustomOAuth2UserService;
import com.wego.service.DemoDataProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Web controller tests for {@link DemoPersonalExpenseController}.
 *
 * <p>Demo routes are public; uses real {@link DemoDataProvider} bean.
 */
@WebMvcTest(DemoPersonalExpenseController.class)
@Import({DemoDataProvider.class, SecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("DemoPersonalExpenseController")
class DemoPersonalExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    @DisplayName("GET /demo/trip/personal-expenses returns 200 with chart JSON attributes")
    void showDemoPersonalExpenses_returns200WithChartJsonAttributes() throws Exception {
        mockMvc.perform(get("/demo/trip/personal-expenses"))
                .andExpect(status().isOk())
                .andExpect(view().name("demo/expense/personal-tab"))
                .andExpect(model().attributeExists("trip"))
                .andExpect(model().attributeExists("personalExpenses"))
                .andExpect(model().attributeExists("personalSummary"))
                .andExpect(model().attributeExists("hasBudget"))
                .andExpect(model().attributeExists("personalCategoryBreakdownJson"))
                .andExpect(model().attributeExists("personalDailyAmountsJson"))
                .andExpect(model().attribute("isDemo", true))
                .andExpect(model().attribute("budgetPercentageCapped", 75));
    }
}
