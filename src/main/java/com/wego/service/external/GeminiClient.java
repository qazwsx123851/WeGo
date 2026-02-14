package com.wego.service.external;

import java.util.List;

/**
 * Interface for Gemini AI API operations.
 * Allows swapping between real and mock implementations.
 *
 * @contract
 *   - All methods throw GeminiException on API failures
 *   - Mock implementation returns static travel advice
 *   - Real implementation calls Gemini REST API
 *
 * @see MockGeminiClient
 * @see GeminiClientImpl
 */
public interface GeminiClient {

    /**
     * Result of a Gemini chat call, including reply text and optional search sources.
     */
    record GeminiChatResult(String reply, List<SearchSource> sources) {
        public record SearchSource(String title, String uri) {}
    }

    /**
     * Sends a chat request to Gemini and returns reply with metadata.
     *
     * @contract
     *   - pre: systemPrompt != null and not blank
     *   - pre: userMessage != null and not blank
     *   - post: Returns reply text and optional search sources
     *   - throws: GeminiException if API call fails
     *
     * @param systemPrompt The system instruction for the AI
     * @param userMessage The user's question
     * @return The AI-generated reply with metadata
     */
    GeminiChatResult chatWithMetadata(String systemPrompt, String userMessage);

    /**
     * Sends a chat request to Gemini with a system prompt and user message.
     *
     * @contract
     *   - pre: systemPrompt != null and not blank
     *   - pre: userMessage != null and not blank
     *   - post: Returns AI-generated reply text
     *   - throws: GeminiException if API call fails
     *
     * @param systemPrompt The system instruction for the AI
     * @param userMessage The user's question
     * @return The AI-generated reply
     */
    default String chat(String systemPrompt, String userMessage) {
        return chatWithMetadata(systemPrompt, userMessage).reply();
    }
}
