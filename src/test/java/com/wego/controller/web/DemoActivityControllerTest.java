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
 * Web controller tests for {@link DemoActivityController}.
 *
 * <p>Demo routes are public; uses real {@link DemoDataProvider} bean.
 */
@WebMvcTest(DemoActivityController.class)
@Import({DemoDataProvider.class, SecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("DemoActivityController")
class DemoActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemoDataProvider demoDataProvider;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @Nested
    @DisplayName("GET /demo/trip/activities (list)")
    class ListTests {

        @Test
        @DisplayName("returns 200 with view demo/activity/list and timeline attributes")
        void listDemoActivities_returns200WithTimelineAttributes() throws Exception {
            mockMvc.perform(get("/demo/trip/activities"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("demo/activity/list"))
                    .andExpect(model().attributeExists("trip"))
                    .andExpect(model().attributeExists("dates"))
                    .andExpect(model().attributeExists("activitiesByDate"))
                    .andExpect(model().attributeExists("allActivities"))
                    .andExpect(model().attributeExists("totalActivityCount"))
                    .andExpect(model().attribute("isDemo", true))
                    .andExpect(model().attribute("canEdit", true));
        }
    }

    @Nested
    @DisplayName("GET /demo/trip/activities/{activityId} (detail)")
    class DetailTests {

        @Test
        @DisplayName("returns 200 with view demo/activity/detail when activityId exists")
        void showDemoActivityDetail_validId_returns200() throws Exception {
            UUID validId = demoDataProvider.getAllDemoActivities().get(0).getId();

            mockMvc.perform(get("/demo/trip/activities/{activityId}", validId))
                    .andExpect(status().isOk())
                    .andExpect(view().name("demo/activity/detail"))
                    .andExpect(model().attributeExists("trip"))
                    .andExpect(model().attributeExists("activity"))
                    .andExpect(model().attribute("isDemo", true));
        }

        @Test
        @DisplayName("returns 404 when activityId does not exist")
        void showDemoActivityDetail_invalidId_returns404() throws Exception {
            UUID nonExistentId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

            mockMvc.perform(get("/demo/trip/activities/{activityId}", nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }
}
