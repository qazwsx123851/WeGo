package com.wego.service.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of GeminiClient for testing and development.
 * Returns a static travel recommendation response.
 *
 * @contract
 *   - Only active when gemini.enabled is false (default)
 *   - Returns structurally valid travel advice
 *
 * @see GeminiClient
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "wego.external-api.gemini.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class MockGeminiClient implements GeminiClient {

    private static final String MOCK_REPLY =
            "根據你的行程安排，我推薦以下選擇：\n\n" +
            "1. **在地特色餐廳** — 距離你目前的景點步行約 5 分鐘，主打當地料理，午間套餐約 NT$300-500，Google 評分 4.3\n\n" +
            "2. **人氣排隊名店** — 需搭乘一站捷運，以招牌料理聞名，建議避開尖峰時段，人均消費約 NT$400-600\n\n" +
            "3. **隱藏版美食** — 位於巷弄內的小店，在地人推薦，CP 值極高，人均消費約 NT$200-350\n\n" +
            "以上推薦都在你行程動線附近，不會影響下午的安排。需要更詳細的資訊嗎？";

    @Override
    public String chat(String systemPrompt, String userMessage) {
        log.info("[MOCK] Gemini chat request received: {}", truncate(userMessage, 100));
        return MOCK_REPLY;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
