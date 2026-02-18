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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real implementation of GeminiClient that calls Gemini REST API.
 *
 * @contract
 *   - pre: GeminiProperties must be configured with valid apiKey
 *   - post: All API calls include API key in header
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
    private static final int MAX_REPLY_LENGTH = 5000;

    private final GeminiProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Circuit breaker state
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong circuitOpenedAt = new AtomicLong(0);

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiClientImpl(GeminiProperties properties,
                            @org.springframework.beans.factory.annotation.Qualifier("geminiRestTemplate") RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public GeminiChatResult chatWithMetadata(String systemPrompt, String userMessage, boolean useSearchGrounding) {
        checkCircuitBreaker();

        boolean effectiveGrounding = properties.isSearchGroundingEnabled() && useSearchGrounding;
        log.debug("Sending chat request to Gemini model: {}", properties.getModel());

        String url = String.format(API_URL, properties.getModel());

        String requestBody = buildRequestBody(systemPrompt, userMessage, effectiveGrounding);
        log.info("Gemini request: model={}, searchGrounding={} (requested={}, global={}), bodyLength={}",
                properties.getModel(), effectiveGrounding, useSearchGrounding,
                properties.isSearchGroundingEnabled(), requestBody.length());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", properties.getApiKey());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );

            String responseBody = response.getBody();
            GeminiChatResult result = extractResult(responseBody);
            recordSuccess();
            boolean hasGrounding = responseBody != null && responseBody.contains("groundingMetadata");
            log.info("Gemini response: {} chars, {} sources, hasGroundingMetadata={}",
                    result.reply().length(), result.sources().size(), hasGrounding);
            return result;

        } catch (GeminiException e) {
            recordFailure();
            throw e;
        } catch (RestClientException e) {
            recordFailure();
            log.error("HTTP error calling Gemini API: {}", e.getMessage());
            Throwable cause = e.getMostSpecificCause();
            if (cause instanceof java.net.SocketTimeoutException) {
                throw GeminiException.timeout();
            }
            throw GeminiException.networkError(e);
        } catch (Exception e) {
            recordFailure();
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            throw GeminiException.apiError("Failed to get response: " + e.getMessage());
        }
    }

    private void checkCircuitBreaker() {
        long openedAt = circuitOpenedAt.get();
        if (openedAt > 0) {
            long cooldownMs = properties.getCircuitBreakerCooldownMinutes() * 60 * 1000L;
            if (System.currentTimeMillis() - openedAt < cooldownMs) {
                throw GeminiException.circuitBreakerOpen();
            }
            // Cooldown elapsed — allow retry (half-open)
            log.info("Gemini circuit breaker half-open, allowing retry");
        }
    }

    private void recordSuccess() {
        consecutiveFailures.set(0);
        circuitOpenedAt.set(0);
    }

    private void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= properties.getCircuitBreakerFailureThreshold()) {
            long now = System.currentTimeMillis();
            if (circuitOpenedAt.compareAndSet(0, now)) {
                log.warn("Gemini circuit breaker OPENED after {} consecutive failures", failures);
            }
        }
    }

    private String buildRequestBody(String systemPrompt, String userMessage, boolean useSearchGrounding) {
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

            var generationConfig = objectMapper.createObjectNode();
            generationConfig.put("maxOutputTokens", properties.getMaxOutputTokens());
            root.set("generationConfig", generationConfig);

            // Add Google Search grounding tool if enabled (global kill-switch AND per-request flag)
            if (useSearchGrounding) {
                var tools = objectMapper.createArrayNode();
                var googleSearchTool = objectMapper.createObjectNode();
                googleSearchTool.set("google_search", objectMapper.createObjectNode());
                tools.add(googleSearchTool);
                root.set("tools", tools);
            }

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw GeminiException.apiError("Failed to build request: " + e.getMessage());
        }
    }

    /**
     * Extracts reply text and grounding sources from Gemini API response.
     */
    private GeminiChatResult extractResult(String responseBody) {
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
            String finishReason = firstCandidate.path("finishReason").asText("");
            if ("MAX_TOKENS".equals(finishReason)) {
                log.warn("Gemini response truncated by MAX_TOKENS limit");
            }
            JsonNode parts = firstCandidate.path("content").path("parts");
            if (parts.isEmpty()) {
                throw GeminiException.apiError("No parts in response");
            }

            String reply = parts.get(0).path("text").asText("");
            if (reply.length() > MAX_REPLY_LENGTH) {
                log.warn("Gemini response too long ({} chars), truncating to {}", reply.length(), MAX_REPLY_LENGTH);
                reply = reply.substring(0, MAX_REPLY_LENGTH) + "…(回覆已截斷)";
            }

            // Extract grounding sources from groundingMetadata
            List<GeminiChatResult.SearchSource> sources = extractGroundingSources(firstCandidate);

            return new GeminiChatResult(reply, sources);
        } catch (GeminiException e) {
            throw e;
        } catch (Exception e) {
            throw GeminiException.apiError("Failed to parse response: " + e.getMessage());
        }
    }

    /**
     * Extracts search sources from groundingMetadata.groundingChunks in the response.
     * Returns empty list if no grounding metadata is present (model did not search).
     */
    private List<GeminiChatResult.SearchSource> extractGroundingSources(JsonNode candidate) {
        JsonNode groundingMetadata = candidate.path("groundingMetadata");
        if (groundingMetadata.isMissingNode()) {
            return List.of();
        }

        JsonNode chunks = groundingMetadata.path("groundingChunks");
        if (chunks.isMissingNode() || !chunks.isArray() || chunks.isEmpty()) {
            return List.of();
        }

        List<GeminiChatResult.SearchSource> sources = new ArrayList<>();
        for (JsonNode chunk : chunks) {
            JsonNode web = chunk.path("web");
            if (!web.isMissingNode()) {
                String title = web.path("title").asText("");
                String uri = web.path("uri").asText("");
                if (!uri.isEmpty()) {
                    sources.add(new GeminiChatResult.SearchSource(
                            title.isEmpty() ? uri : title, uri));
                }
            }
        }

        if (!sources.isEmpty()) {
            JsonNode queries = groundingMetadata.path("webSearchQueries");
            if (queries.isArray() && !queries.isEmpty()) {
                log.debug("Gemini used Google Search with queries: {}", queries);
            }
        }

        return List.copyOf(sources);
    }
}
