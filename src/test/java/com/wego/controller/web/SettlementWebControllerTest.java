package com.wego.controller.web;

import com.wego.domain.permission.PermissionChecker;
import com.wego.dto.response.SettlementResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.Role;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
import com.wego.security.UserPrincipal;
import com.wego.service.SettlementService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for SettlementWebController.
 *
 * @contract
 *   - Tests settlement page rendering
 *   - Verifies per-person average calculation
 *   - Tests permission checks and error handling
 */
@WebMvcTest(SettlementWebController.class)
@ActiveProfiles("test")
class SettlementWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private TripService tripService;

    @MockBean
    private SettlementService settlementService;

    @MockBean
    private PermissionChecker permissionChecker;

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
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(15))
                .baseCurrency("TWD")
                .members(List.of(ownerMember))
                .build();

    }

    private SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor oidcLogin() {
        return SecurityMockMvcRequestPostProcessors.oauth2Login().oauth2User(testPrincipal);
    }

    @Test
    @DisplayName("GET /trips/{id}/settlement - should return settlement view")
    void showSettlement_authenticated_shouldReturnView() throws Exception {
        SettlementResponse settlement = SettlementResponse.builder()
                .settlements(Collections.emptyList())
                .totalExpenses(new BigDecimal("30000"))
                .baseCurrency("TWD")
                .expenseCount(5)
                .currencyBreakdown(Map.of("TWD", new BigDecimal("30000")))
                .build();

        when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
        when(settlementService.calculateSettlement(tripId, userId)).thenReturn(settlement);
        when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);

        mockMvc.perform(get("/trips/{tripId}/settlement", tripId).with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(view().name("expense/settlement"))
                .andExpect(model().attributeExists("trip", "settlement",
                        "perPersonAverage", "canEdit", "name", "picture"))
                .andExpect(model().attribute("canEdit", true));
    }

    @Test
    @DisplayName("GET /trips/{id}/settlement - should redirect when forbidden")
    void showSettlement_forbidden_shouldRedirect() throws Exception {
        when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
        when(settlementService.calculateSettlement(tripId, userId))
                .thenThrow(new ForbiddenException("No permission"));

        mockMvc.perform(get("/trips/{tripId}/settlement", tripId).with(oidcLogin()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trips/" + tripId + "/expenses?error=access_denied"));
    }

    @Test
    @DisplayName("GET /trips/{id}/settlement - should redirect when trip not found")
    void showSettlement_tripNotFound_shouldRedirect() throws Exception {
        when(tripService.getTrip(tripId, userId)).thenReturn(null);

        mockMvc.perform(get("/trips/{tripId}/settlement", tripId).with(oidcLogin()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?error=trip_not_found"));
    }

    @Test
    @DisplayName("GET /trips/{id}/settlement - should redirect when not authenticated")
    void showSettlement_notAuthenticated_shouldRedirect() throws Exception {
        mockMvc.perform(get("/trips/{tripId}/settlement", tripId))
                .andExpect(status().is3xxRedirection());
    }
}
