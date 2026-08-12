package com.resumepilot.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.resumepilot.exception.AiServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Anthropic Claude provider (Messages API).
 */
@Slf4j
@Service
public class ClaudeProvider implements AiProvider {

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final String model;
    private final AiResponseParser parser;
    private final String apiKey;

    public ClaudeProvider(@Value("${app.ai.claude.base-url}") String baseUrl,
                          @Value("${app.ai.claude.api-key}") String apiKey,
                          @Value("${app.ai.claude.model}") String model,
                          @Value("${app.ai.claude.timeout-ms:120000}") long timeoutMs,
                          AiResponseParser parser) {
        this.apiKey = apiKey;
        this.model = model;
        this.parser = parser;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                    setConnectTimeout((int) timeoutMs);
                    setReadTimeout((int) timeoutMs);
                }})
                .build();
    }

    @Override
    public String name() {
        return "CLAUDE";
    }

    @Override
    public AiOptimizationResult optimize(String resumeText, String jdText) {
        if (apiKey.isBlank()) {
            throw new AiServiceException("Claude API key is not configured");
        }
        try {
            String raw = restClient.post()
                    .uri("/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", model,
                            "max_tokens", 4000,
                            "system", AiPromptBuilder.SYSTEM_PROMPT,
                            "messages", java.util.List.of(Map.of(
                                    "role", "user",
                                    "content", AiPromptBuilder.buildUserPrompt(resumeText, jdText)))))
                    .retrieve()
                    .body(String.class);

            JsonNode root = parser.extractJsonObject(raw);
            JsonNode text = root.path("content").path(0).path("text");
            return parser.toResult(parser.extractJsonObject(text.asText()));
        } catch (Exception e) {
            log.error("Claude optimization failed: {}", e.getMessage());
            throw new AiServiceException("Claude request failed: " + e.getMessage());
        }
    }
}
