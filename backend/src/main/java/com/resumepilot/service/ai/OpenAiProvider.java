package com.resumepilot.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.resumepilot.exception.AiServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * OpenAI provider (Chat Completions API, JSON mode).
 */
@Slf4j
@Service
public class OpenAiProvider implements AiProvider {

    private final RestClient restClient;
    private final String model;
    private final AiResponseParser parser;
    private final String apiKey;

    public OpenAiProvider(@Value("${app.ai.openai.base-url}") String baseUrl,
                          @Value("${app.ai.openai.api-key}") String apiKey,
                          @Value("${app.ai.openai.model}") String model,
                          @Value("${app.ai.openai.timeout-ms:120000}") long timeoutMs,
                          AiResponseParser parser) {
        this.apiKey = apiKey;
        this.model = model;
        this.parser = parser;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                    setConnectTimeout((int) timeoutMs);
                    setReadTimeout((int) timeoutMs);
                }})
                .build();
    }

    @Override
    public String name() {
        return "OPENAI";
    }

    @Override
    public AiOptimizationResult optimize(String resumeText, String jdText) {
        if (apiKey.isBlank()) {
            throw new AiServiceException("OpenAI API key is not configured");
        }
        try {
            String raw = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", model,
                            "temperature", 0.3,
                            "response_format", Map.of("type", "json_object"),
                            "messages", java.util.List.of(
                                    Map.of("role", "system", "content", AiPromptBuilder.SYSTEM_PROMPT),
                                    Map.of("role", "user", "content", AiPromptBuilder.buildUserPrompt(resumeText, jdText)))))
                    .retrieve()
                    .body(String.class);

            JsonNode root = parser.extractJsonObject(raw);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            return parser.toResult(parser.extractJsonObject(content.asText()));
        } catch (Exception e) {
            log.error("OpenAI optimization failed: {}", e.getMessage());
            throw new AiServiceException("OpenAI request failed: " + e.getMessage());
        }
    }
}
