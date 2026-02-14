package com.wego.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.dto.request.ChatRequest;
import com.wego.dto.response.ChatResponse;
import com.wego.entity.User;
import com.wego.exception.BusinessException;
import com.wego.exception.ForbiddenException;
import com.wego.security.UserPrincipal;
import com.wego.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatApiController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("ChatApiController")
class ChatApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    private UUID tripId;
    private UUID userId;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        userId = UUID.randomUUID();

        User testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .provider("test")
                .providerId("test-id")
                .build();
        userPrincipal = new UserPrincipal(testUser);
    }

    @Nested
    @DisplayName("POST /api/trips/{tripId}/chat")
    class PostChat {

        @Test
        @DisplayName("should return 200 with AI reply on valid request")
        void shouldReturn200OnValidRequest() throws Exception {
            ChatResponse response = ChatResponse.builder().reply("推薦你去鼎泰豐！").build();
            when(chatService.chat(eq(tripId), eq(userId), eq("推薦餐廳"), any())).thenReturn(response);

            mockMvc.perform(post("/api/trips/{tripId}/chat", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    ChatRequest.builder().message("推薦餐廳").build())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.reply").value("推薦你去鼎泰豐！"));
        }

        @Test
        @DisplayName("should return 400 when message is blank")
        void shouldReturn400WhenMessageBlank() throws Exception {
            mockMvc.perform(post("/api/trips/{tripId}/chat", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    ChatRequest.builder().message("").build())))
                    .andExpect(status().isBadRequest());

            verify(chatService, never()).chat(any(), any(), any(), any());
        }

        @Test
        @DisplayName("should return 400 when message exceeds 500 characters")
        void shouldReturn400WhenMessageTooLong() throws Exception {
            String longMessage = "a".repeat(501);

            mockMvc.perform(post("/api/trips/{tripId}/chat", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    ChatRequest.builder().message(longMessage).build())))
                    .andExpect(status().isBadRequest());

            verify(chatService, never()).chat(any(), any(), any(), any());
        }

        @Test
        @DisplayName("should pass timezone from request to service")
        void shouldPassTimezoneToService() throws Exception {
            ChatResponse response = ChatResponse.builder().reply("ok").build();
            when(chatService.chat(eq(tripId), eq(userId), eq("test"), eq("Asia/Tokyo")))
                    .thenReturn(response);

            mockMvc.perform(post("/api/trips/{tripId}/chat", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    ChatRequest.builder().message("test").timezone("Asia/Tokyo").build())))
                    .andExpect(status().isOk());

            verify(chatService).chat(eq(tripId), eq(userId), eq("test"), eq("Asia/Tokyo"));
        }

        @Test
        @DisplayName("should return 403 when user is not a trip member")
        void shouldReturn403WhenNotMember() throws Exception {
            when(chatService.chat(eq(tripId), eq(userId), any(), any()))
                    .thenThrow(new ForbiddenException("你沒有權限存取此行程的聊天功能"));

            mockMvc.perform(post("/api/trips/{tripId}/chat", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    ChatRequest.builder().message("test").build())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 when rate limited")
        void shouldReturn400WhenRateLimited() throws Exception {
            when(chatService.chat(eq(tripId), eq(userId), any(), any()))
                    .thenThrow(new BusinessException("RATE_LIMITED", "請求太頻繁，請稍後再試"));

            mockMvc.perform(post("/api/trips/{tripId}/chat", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    ChatRequest.builder().message("test").build())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("RATE_LIMITED"));
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void shouldReturn403WhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/trips/{tripId}/chat", tripId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    ChatRequest.builder().message("test").build())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 403 when no CSRF token")
        void shouldReturn403WhenNoCsrf() throws Exception {
            mockMvc.perform(post("/api/trips/{tripId}/chat", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    ChatRequest.builder().message("test").build())))
                    .andExpect(status().isForbidden());
        }
    }
}
