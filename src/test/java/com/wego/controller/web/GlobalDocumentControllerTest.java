package com.wego.controller.web;

import com.wego.dto.response.GlobalDocumentOverviewResponse;
import com.wego.entity.User;
import com.wego.security.UserPrincipal;
import com.wego.service.GlobalDocumentService;
import com.wego.service.TripService;
import com.wego.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for GlobalDocumentController.
 *
 * @contract
 *   - Tests global document overview page
 *   - Verifies model attributes and filter parameters
 *   - Tests authentication requirement
 */
@WebMvcTest(GlobalDocumentController.class)
@ActiveProfiles("test")
class GlobalDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private TripService tripService;

    @MockBean
    private GlobalDocumentService globalDocumentService;

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
    @DisplayName("GET /documents - should return document overview")
    void showDocumentOverview_authenticated_shouldReturnView() throws Exception {
        GlobalDocumentOverviewResponse overview = GlobalDocumentOverviewResponse.builder()
                .documents(List.of())
                .totalDocuments(5)
                .totalStorageBytes(1024 * 1024)
                .currentPage(0)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(globalDocumentService.getOverview(eq(userId), any(), any(PageRequest.class)))
                .thenReturn(overview);
        when(globalDocumentService.getUserTripsWithDocuments(userId))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/documents").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(view().name("document/global-overview"))
                .andExpect(model().attributeExists("overview", "userTrips", "filter",
                        "currentPage", "name", "picture"));
    }

    @Test
    @DisplayName("GET /documents - should redirect when not authenticated")
    void showDocumentOverview_notAuthenticated_shouldRedirect() throws Exception {
        mockMvc.perform(get("/documents"))
                .andExpect(status().is3xxRedirection());
    }
}
