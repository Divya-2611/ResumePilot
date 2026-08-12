package com.resumepilot.service.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves the active AI provider from configuration
 * (app.ai.provider: OPENAI | GEMINI | CLAUDE), so switching providers is a
 * config change, not a code change.
 */
@Component
@RequiredArgsConstructor
public class AiProviderFactory {

    private final OpenAiProvider openAiProvider;
    private final GeminiProvider geminiProvider;
    private final ClaudeProvider claudeProvider;

    @Value("${app.ai.provider:OPENAI}")
    private String configuredProvider;

    public AiProvider getProvider() {
        return switch (configuredProvider.toUpperCase()) {
            case "GEMINI" -> geminiProvider;
            case "CLAUDE" -> claudeProvider;
            case "OPENAI" -> openAiProvider;
            default -> throw new IllegalArgumentException(
                    "Unknown AI provider: " + configuredProvider);
        };
    }

    public String activeProviderName() {
        return getProvider().name();
    }
}
