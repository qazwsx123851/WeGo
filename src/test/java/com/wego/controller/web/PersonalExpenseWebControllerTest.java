package com.wego.controller.web;

import com.wego.entity.User;
import com.wego.security.UserPrincipal;
import com.wego.service.PersonalExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    }

    @Test
    @DisplayName("GET /create should include baseCurrency in model")
    void showCreateForm_includesBaseCurrency() throws Exception {
        when(personalExpenseService.getBaseCurrency(eq(tripId), any())).thenReturn("TWD");

        mockMvc.perform(get("/trips/{tripId}/personal-expenses/create", tripId)
                .with(SecurityMockMvcRequestPostProcessors.oauth2Login()
                        .oauth2User(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("baseCurrency"))
                .andExpect(model().attribute("baseCurrency", "TWD"))
                .andExpect(model().attributeExists("tripId"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(view().name("expense/personal-create"));
    }

    @Test
    @DisplayName("GET /create with JPY baseCurrency passes JPY to model")
    void showCreateForm_jpyBaseCurrency() throws Exception {
        when(personalExpenseService.getBaseCurrency(eq(tripId), any())).thenReturn("JPY");

        mockMvc.perform(get("/trips/{tripId}/personal-expenses/create", tripId)
                .with(SecurityMockMvcRequestPostProcessors.oauth2Login()
                        .oauth2User(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("baseCurrency", "JPY"));
    }
}
