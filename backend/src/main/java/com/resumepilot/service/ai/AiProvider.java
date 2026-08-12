package com.resumepilot.service.ai;

/**
 * Pluggable AI backend. Implementations talk to OpenAI, Gemini or Claude.
 * Keep this interface stable: the provider is chosen by configuration.
 */
public interface AiProvider {

    /** Identifier stored in history rows, e.g. "OPENAI". */
    String name();

    /**
     * Analyzes the resume against the job description and returns structured
     * optimization guidance plus rewritten content.
     *
     * @throws com.resumepilot.exception.AiServiceException when the provider fails
     */
    AiOptimizationResult optimize(String resumeText, String jdText);
}
