package com.wego.service.external;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MockGeminiClient")
class MockGeminiClientTest {

    private MockGeminiClient client;

    @BeforeEach
    void setUp() {
        client = new MockGeminiClient();
    }

    @Test
    @DisplayName("should return non-empty travel recommendation")
    void shouldReturnTravelRecommendation() {
        String reply = client.chat("system prompt", "推薦餐廳");

        assertThat(reply).isNotBlank();
        assertThat(reply).contains("推薦");
    }

    @Test
    @DisplayName("should return response with travel-related content")
    void shouldReturnTravelContent() {
        String reply = client.chat("system", "任何問題");

        assertThat(reply).contains("餐廳");
    }
}
