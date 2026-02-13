package com.wego.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for chat feature.
 *
 * @contract
 *   - maxMessageLength: Maximum allowed message length (default: 500)
 *   - rateLimitPerMinute: Maximum chat requests per user per minute (default: 5)
 *
 * @see com.wego.service.ChatService
 */
@Configuration
@ConfigurationProperties(prefix = "wego.chat")
@Getter
@Setter
public class ChatProperties {

    private int maxMessageLength = 500;

    private int rateLimitPerMinute = 5;
}
