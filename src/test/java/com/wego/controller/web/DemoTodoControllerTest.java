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

@WebMvcTest(DemoTodoController.class)
@Import({DemoDataProvider.class, SecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("DemoTodoController")
class DemoTodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    @DisplayName("GET /demo/trip/todos returns 200 with todo stats")
    void listDemoTodos_returns200WithTodoStats() throws Exception {
        mockMvc.perform(get("/demo/trip/todos"))
                .andExpect(status().isOk())
                .andExpect(view().name("demo/todo/list"))
                .andExpect(model().attributeExists("trip"))
                .andExpect(model().attributeExists("todos"))
                .andExpect(model().attributeExists("totalTodos"))
                .andExpect(model().attributeExists("completedTodos"))
                .andExpect(model().attributeExists("pendingTodos"))
                .andExpect(model().attributeExists("inProgressTodos"))
                .andExpect(model().attribute("isDemo", true))
                .andExpect(model().attribute("canEdit", true));
    }
}
