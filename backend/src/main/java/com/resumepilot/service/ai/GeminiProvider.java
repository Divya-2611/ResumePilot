package com.resumepilot.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.resumepilot.exception.AiServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Google Gemini provider (generateContent endpoint with the key in the query string).
 * Transient 429/5xx overloads are retried with a short backoff before giving up.
 */
@Slf4j
@Service
public class GeminiProvider implements AiProvider {

    private final RestClient restClient;
    private final String model;
    private final AiResponseParser parser;
    private final String apiKey;

    public GeminiProvider(@Value("${app.ai.gemini.base-url}") String baseUrl,
                          @Value("${app.ai.gemini.api-key}") String apiKey,
                          @Value("${app.ai.gemini.model}") String model,
                          @Value("${app.ai.gemini.timeout-ms:120000}") long timeoutMs,
                          AiResponseParser parser) {
        this.apiKey = apiKey;
        this.model = model;
        this.parser = parser;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                    setConnectTimeout((int) timeoutMs);
                    setReadTimeout((int) timeoutMs);
                }})
                .build();
    }

    @Override
    public String name() {
        return "GEMINI";
    }

    @Override
    public AiOptimizationResult optimize(String resumeText, String jdText) {
        if (apiKey.isBlank()) {
            throw new AiServiceException("Gemini API key is not configured");
        }
        String userPrompt = AiPromptBuilder.buildUserPrompt(resumeText, jdText);
        Exception lastError = null;

        // Retry on transient overloads (429 quota/rate-limit, 5xx spikes).
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                if (attempt > 1) {
                    Thread.sleep(2000L * (attempt - 1));
                }
                String raw = restClient.post()
                        .uri("/models/{model}:generateContent?key={key}", model, apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "contents", java.util.List.of(Map.of(
                                        "role", "user",
                                        "parts", java.util.List.of(Map.of(
                                                "text", AiPromptBuilder.SYSTEM_PROMPT
                                                        + "\n\n" + userPrompt))))))
                        .retrieve()
                        .body(String.class);

                JsonNode root = parser.extractJsonObject(raw);
                JsonNode text = root.path("candidates").path(0).path("content").path("parts")
                        .path(0).path("text");
                return parser.toResult(parser.extractJsonObject(text.asText()));
            } catch (HttpServerErrorException e) {
                lastError = e;
                log.warn("Gemini overloaded (attempt {}/3): {}", attempt, e.getMessage());
            } catch (Exception e) {
                lastError = e;
                break; // non-transient: no point retrying
            }
        }
        log.error("Gemini optimization failed: {}", lastError == null ? "unknown" : lastError.getMessage());
        throw new AiServiceException("Gemini request failed: "
                + (lastError == null ? "unknown error" : lastError.getMessage()));
    }
}
