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
 * Web controller tests for {@link DemoTripController}.
 *
 * <p>Demo routes are public (no auth) so we don't mock OAuth principals.
 * Real {@link DemoDataProvider} bean is imported (deterministic data, no
 * external dependencies) so we don't mock it.
 */
@WebMvcTest(DemoTripController.class)
@Import({DemoDataProvider.class, SecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("DemoTripController")
class DemoTripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    @DisplayName("GET /demo/trip returns 200 with view demo/trip/view and expected attributes")
    void showDemoTrip_returnsViewWithExpectedAttributes() throws Exception {
        mockMvc.perform(get("/demo/trip"))
                .andExpect(status().isOk())
                .andExpect(view().name("demo/trip/view"))
                .andExpect(model().attributeExists("trip"))
                .andExpect(model().attributeExists("members"))
                .andExpect(model().attributeExists("memberCount"))
                .andExpect(model().attributeExists("upcomingActivities"))
                .andExpect(model().attributeExists("upcomingTodos"))
                .andExpect(model().attributeExists("totalExpense"))
                .andExpect(model().attributeExists("averageExpense"))
                .andExpect(model().attribute("isDemo", true))
                .andExpect(model().attribute("canEdit", true))
                .andExpect(model().attribute("isOwner", true))
                .andExpect(model().attribute("tripDays", 4))
                .andExpect(model().attribute("tripNights", 3));
    }

    @Test
    @DisplayName("GET /demo/trip is publicly accessible (no auth)")
    void showDemoTrip_unauthenticatedRequest_returns200() throws Exception {
        mockMvc.perform(get("/demo/trip"))
                .andExpect(status().isOk());
    }
}
