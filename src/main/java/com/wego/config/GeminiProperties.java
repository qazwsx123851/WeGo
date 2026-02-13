package com.wego.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Gemini API integration.
 *
 * @contract
 *   - apiKey: Gemini API Key for authentication
 *   - enabled: Whether the Gemini integration is enabled
 *   - model: Gemini model to use (default: gemini-2.5-flash)
 *   - connectTimeoutMs: Connection timeout in milliseconds (default: 5000)
 *   - readTimeoutMs: Read timeout in milliseconds (default: 30000)
 *
 * @see com.wego.service.external.GeminiClient
 */
@Configuration
@ConfigurationProperties(prefix = "wego.external-api.gemini")
@Getter
@Setter
public class GeminiProperties {

    private String apiKey;

    private boolean enabled = false;

    private String model = "gemini-2.5-flash";

    private int connectTimeoutMs = 5000;

    private int readTimeoutMs = 30000;
}
