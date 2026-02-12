package com.wego.controller.web;

import com.wego.dto.response.GlobalExpenseOverviewResponse;
import com.wego.entity.User;
import com.wego.security.UserPrincipal;
import com.wego.service.GlobalExpenseService;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for GlobalExpenseController.
 *
 * @contract
 *   - Tests global expense overview page
 *   - Verifies model attributes
 *   - Tests authentication requirement
 */
@WebMvcTest(GlobalExpenseController.class)
@ActiveProfiles("test")
class GlobalExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private TripService tripService;

    @MockBean
    private GlobalExpenseService globalExpenseService;

    private UUID userId;
    private User testUser;
    private UserPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .provider("google")
                .providerId("google-id")
                .build();

        testPrincipal = new UserPrincipal(testUser);
    }

    private RequestPostProcessor oauth2Login() {
        return SecurityMockMvcRequestPostProcessors.oauth2Login().oauth2User(testPrincipal);
    }

    @Test
    @DisplayName("GET /expenses - should return expense overview with data")
    void showExpenseOverview_authenticated_shouldReturnView() throws Exception {
        GlobalExpenseOverviewResponse overview = GlobalExpenseOverviewResponse.builder()
                .totalPaid(new BigDecimal("50000"))
                .totalOwed(new BigDecimal("5000"))
                .totalOwedToUser(new BigDecimal("3000"))
                .netBalance(new BigDecimal("-2000"))
                .tripCount(3)
                .build();

        when(globalExpenseService.getOverview(userId)).thenReturn(overview);
        when(globalExpenseService.getUnsettledTrips(userId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/expenses").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(view().name("expense/global-overview"))
                .andExpect(model().attributeExists("overview", "unsettledTrips", "name", "picture"))
                .andExpect(model().attribute("name", "Test User"));
    }

    @Test
    @DisplayName("GET /expenses - should redirect when not authenticated")
    void showExpenseOverview_notAuthenticated_shouldRedirect() throws Exception {
        mockMvc.perform(get("/expenses"))
                .andExpect(status().is3xxRedirection());
    }
}
