package com.wego.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.config.GeminiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Real implementation of GeminiClient that calls Gemini REST API.
 *
 * @contract
 *   - pre: GeminiProperties must be configured with valid apiKey
 *   - post: All API calls include API key in URL
 *   - throws: GeminiException on API errors or invalid responses
 *
 * @see GeminiClient
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "wego.external-api.gemini.enabled",
        havingValue = "true"
)
public class GeminiClientImpl implements GeminiClient {

    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private final GeminiProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiClientImpl(GeminiProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiClientImpl(GeminiProperties properties) {
        this(properties, createRestTemplateWithTimeouts(properties));
    }

    private static RestTemplate createRestTemplateWithTimeouts(GeminiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        return new RestTemplate(factory);
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        log.debug("Sending chat request to Gemini model: {}", properties.getModel());

        String url = String.format(API_URL, properties.getModel());

        String requestBody = buildRequestBody(systemPrompt, userMessage);
        log.debug("Gemini request URL: {}", url);
        log.debug("Gemini request body: {}", requestBody);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", properties.getApiKey());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );

            log.debug("Gemini response body: {}", response.getBody());
            String reply = extractReply(response.getBody());
            log.info("Received Gemini reply ({} chars)", reply.length());
            return reply;

        } catch (GeminiException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("HTTP error calling Gemini API: {}", e.getMessage());
            Throwable cause = e.getMostSpecificCause();
            if (cause instanceof java.net.SocketTimeoutException) {
                throw GeminiException.timeout();
            }
            throw GeminiException.networkError(e);
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            throw GeminiException.apiError("Failed to get response: " + e.getMessage());
        }
    }

    private String buildRequestBody(String systemPrompt, String userMessage) {
        try {
            var root = objectMapper.createObjectNode();

            var systemInstruction = objectMapper.createObjectNode();
            var systemParts = objectMapper.createObjectNode();
            systemParts.put("text", systemPrompt);
            systemInstruction.set("parts", objectMapper.createArrayNode().add(systemParts));
            root.set("system_instruction", systemInstruction);

            var contents = objectMapper.createArrayNode();
            var userContent = objectMapper.createObjectNode();
            userContent.put("role", "user");
            var userParts = objectMapper.createArrayNode();
            var textPart = objectMapper.createObjectNode();
            textPart.put("text", userMessage);
            userParts.add(textPart);
            userContent.set("parts", userParts);
            contents.add(userContent);
            root.set("contents", contents);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw GeminiException.apiError("Failed to build request: " + e.getMessage());
        }
    }

    private String extractReply(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                int code = error.path("code").asInt();
                String message = error.path("message").asText("Unknown error");
                if (code == 403) {
                    throw GeminiException.invalidApiKey();
                }
                throw GeminiException.apiError(message);
            }

            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty()) {
                throw GeminiException.apiError("No candidates in response");
            }

            JsonNode firstCandidate = candidates.get(0);
            JsonNode parts = firstCandidate.path("content").path("parts");
            if (parts.isEmpty()) {
                throw GeminiException.apiError("No parts in response");
            }

            return parts.get(0).path("text").asText("");
        } catch (GeminiException e) {
            throw e;
        } catch (Exception e) {
            throw GeminiException.apiError("Failed to parse response: " + e.getMessage());
        }
    }
}
