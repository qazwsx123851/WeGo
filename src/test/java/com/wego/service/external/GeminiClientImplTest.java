package com.wego.service.external;

import com.wego.config.GeminiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeminiClientImpl")
class GeminiClientImplTest {

    @Mock
    private RestTemplate restTemplate;

    private GeminiClientImpl client;
    private GeminiProperties properties;

    private static final String SUCCESS_RESPONSE = """
            {
                "candidates": [{
                    "content": {
                        "parts": [{"text": "推薦你去鼎泰豐！"}],
                        "role": "model"
                    }
                }]
            }
            """;

    private static final String ERROR_RESPONSE = """
            {
                "error": {
                    "code": 400,
                    "message": "API key not valid",
                    "status": "INVALID_ARGUMENT"
                }
            }
            """;

    private static final String EMPTY_CANDIDATES_RESPONSE = """
            {
                "candidates": []
            }
            """;

    @BeforeEach
    void setUp() {
        properties = new GeminiProperties();
        properties.setApiKey("test-api-key");
        properties.setModel("gemini-2.5-flash");
        properties.setEnabled(true);
        client = new GeminiClientImpl(properties, restTemplate);
    }

    @Nested
    @DisplayName("chat")
    class Chat {

        @Test
        @DisplayName("should return reply on successful API call")
        void shouldReturnReplyOnSuccess() {
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(SUCCESS_RESPONSE));

            String reply = client.chat("You are a travel assistant", "推薦餐廳");

            assertThat(reply).isEqualTo("推薦你去鼎泰豐！");
        }

        @Test
        @DisplayName("should include system prompt and user message in request body")
        void shouldIncludePromptsInRequest() {
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(SUCCESS_RESPONSE));

            client.chat("System instruction", "User question");

            ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(String.class));

            String body = captor.getValue().getBody();
            assertThat(body).contains("System instruction");
            assertThat(body).contains("User question");
            assertThat(body).contains("system_instruction");
        }

        @Test
        @DisplayName("should include API key in header and model in URL")
        void shouldIncludeApiKeyInHeader() {
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(SUCCESS_RESPONSE));

            client.chat("prompt", "message");

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).postForEntity(urlCaptor.capture(), entityCaptor.capture(), eq(String.class));

            // API key should be in header, not URL
            assertThat(urlCaptor.getValue()).doesNotContain("test-api-key");
            assertThat(urlCaptor.getValue()).contains("gemini-2.5-flash");
            assertThat(entityCaptor.getValue().getHeaders().getFirst("x-goog-api-key")).isEqualTo("test-api-key");
        }

        @Test
        @DisplayName("should throw GeminiException on API error response")
        void shouldThrowOnApiError() {
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(ERROR_RESPONSE));

            assertThatThrownBy(() -> client.chat("prompt", "message"))
                    .isInstanceOf(GeminiException.class)
                    .hasMessageContaining("API key not valid");
        }

        @Test
        @DisplayName("should throw invalidApiKey on 403 error response")
        void shouldThrowInvalidApiKeyOn403() {
            String error403Response = """
                    {
                        "error": {
                            "code": 403,
                            "message": "Forbidden",
                            "status": "PERMISSION_DENIED"
                        }
                    }
                    """;
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(error403Response));

            assertThatThrownBy(() -> client.chat("prompt", "message"))
                    .isInstanceOf(GeminiException.class)
                    .satisfies(e -> assertThat(((GeminiException) e).getErrorCode()).isEqualTo("INVALID_API_KEY"));
        }

        @Test
        @DisplayName("should throw GeminiException on empty candidates")
        void shouldThrowOnEmptyCandidates() {
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(EMPTY_CANDIDATES_RESPONSE));

            assertThatThrownBy(() -> client.chat("prompt", "message"))
                    .isInstanceOf(GeminiException.class)
                    .hasMessageContaining("No candidates");
        }

        @Test
        @DisplayName("should throw GeminiException on network error")
        void shouldThrowOnNetworkError() {
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new ResourceAccessException("Connection refused"));

            assertThatThrownBy(() -> client.chat("prompt", "message"))
                    .isInstanceOf(GeminiException.class);
        }

        @Test
        @DisplayName("should throw timeout GeminiException on timeout")
        void shouldThrowTimeoutOnTimeout() {
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new ResourceAccessException("Read timed out",
                            new SocketTimeoutException("Read timed out")));

            assertThatThrownBy(() -> client.chat("prompt", "message"))
                    .isInstanceOf(GeminiException.class)
                    .satisfies(e -> assertThat(((GeminiException) e).getErrorCode()).isEqualTo("TIMEOUT"));
        }
    }
}
