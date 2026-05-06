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

@WebMvcTest(DemoMemberController.class)
@Import({DemoDataProvider.class, SecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("DemoMemberController")
class DemoMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    @DisplayName("GET /demo/trip/members returns 200 with members list")
    void listDemoMembers_returns200WithMembersList() throws Exception {
        mockMvc.perform(get("/demo/trip/members"))
                .andExpect(status().isOk())
                .andExpect(view().name("demo/trip/members"))
                .andExpect(model().attributeExists("trip"))
                .andExpect(model().attributeExists("members"))
                .andExpect(model().attributeExists("memberCount"))
                .andExpect(model().attributeExists("currentUserId"))
                .andExpect(model().attribute("isDemo", true))
                .andExpect(model().attribute("canInvite", true));
    }
}
