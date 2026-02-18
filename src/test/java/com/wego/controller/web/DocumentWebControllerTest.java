package com.wego.controller.web;

import com.wego.dto.response.ActivityResponse;
import com.wego.dto.response.DocumentResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.Role;
import com.wego.entity.User;
import com.wego.security.UserPrincipal;
import com.wego.service.ActivityService;
import com.wego.service.DocumentService;
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

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for DocumentWebController.
 *
 * @contract
 *   - Tests document list page and upload form rendering
 *   - Verifies model attributes and authentication
 *   - Tests trip access and edge cases
 */
@WebMvcTest(DocumentWebController.class)
@ActiveProfiles("test")
class DocumentWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private TripService tripService;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private ActivityService activityService;

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

    // --- showDocuments tests ---

    @Test
    @DisplayName("GET /trips/{id}/documents - should return document list view")
    void showDocuments_authenticated_shouldReturnView() throws Exception {
        when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
        when(documentService.getDocumentsByTrip(tripId, userId, true))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/trips/{tripId}/documents", tripId).with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(view().name("document/list"))
                .andExpect(model().attributeExists("trip", "tripId", "documents", "name", "picture"))
                .andExpect(model().attribute("tripId", tripId));
    }

    @Test
    @DisplayName("GET /trips/{id}/documents - should redirect when trip not found")
    void showDocuments_tripNotFound_shouldRedirect() throws Exception {
        when(tripService.getTrip(tripId, userId)).thenReturn(null);

        mockMvc.perform(get("/trips/{tripId}/documents", tripId).with(oidcLogin()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?error=trip_not_found"));
    }

    @Test
    @DisplayName("GET /trips/{id}/documents - should redirect when not authenticated")
    void showDocuments_notAuthenticated_shouldRedirect() throws Exception {
        mockMvc.perform(get("/trips/{tripId}/documents", tripId))
                .andExpect(status().is3xxRedirection());
    }

    // --- showDocumentUploadForm tests ---

    @Test
    @DisplayName("GET /trips/{id}/documents/new - should return upload form view")
    void showUploadForm_authenticated_shouldReturnView() throws Exception {
        when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
        when(activityService.getActivitiesByTrip(tripId, userId))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/trips/{tripId}/documents/new", tripId).with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(view().name("document/upload"))
                .andExpect(model().attributeExists("trip", "tripId", "activities", "name", "picture"))
                .andExpect(model().attribute("activityId", (Object) null));
    }

    @Test
    @DisplayName("GET /trips/{id}/documents/new?activityId=xxx - should pass activityId")
    void showUploadForm_withActivityId_shouldPassActivityId() throws Exception {
        UUID activityId = UUID.randomUUID();
        when(tripService.getTrip(tripId, userId)).thenReturn(testTrip);
        when(activityService.getActivitiesByTrip(tripId, userId))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/trips/{tripId}/documents/new", tripId)
                        .param("activityId", activityId.toString())
                        .with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(view().name("document/upload"))
                .andExpect(model().attribute("activityId", activityId));
    }

    @Test
    @DisplayName("GET /trips/{id}/documents/new - should redirect when trip not found")
    void showUploadForm_tripNotFound_shouldRedirect() throws Exception {
        when(tripService.getTrip(tripId, userId)).thenReturn(null);

        mockMvc.perform(get("/trips/{tripId}/documents/new", tripId).with(oidcLogin()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?error=trip_not_found"));
    }

    @Test
    @DisplayName("GET /trips/{id}/documents/new - should redirect when not authenticated")
    void showUploadForm_notAuthenticated_shouldRedirect() throws Exception {
        mockMvc.perform(get("/trips/{tripId}/documents/new", tripId))
                .andExpect(status().is3xxRedirection());
    }
}
