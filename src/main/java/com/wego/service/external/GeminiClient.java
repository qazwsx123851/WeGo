package com.wego.service.external;

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
    String chat(String systemPrompt, String userMessage);
}
