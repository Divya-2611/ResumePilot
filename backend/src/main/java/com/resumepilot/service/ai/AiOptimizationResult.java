package com.resumepilot.service.ai;

import java.util.List;
import java.util.Map;

/**
 * Normalized result contract returned by every AI provider, so providers can be
 * swapped (OpenAI / Gemini / Claude) without touching the rest of the system.
 *
 * @param atsScore           0-100
 * @param keywordMatchPercent 0-100
 * @param matchedKeywords    keywords present in the resume
 * @param missingKeywords    required keywords missing from the resume
 * @param weakBulletPoints   weak/weakly-phrased bullet points
 * @param grammarSuggestions grammar improvement hints
 * @param formattingSuggestions layout/format hints
 * @param readabilityScore   0-100
 * @param optimizedSections  section name -> optimized content (String or List&lt;String&gt;)
 * @param optimizedContent   full plain-text optimized resume
 */
public record AiOptimizationResult(
        int atsScore,
        int keywordMatchPercent,
        List<String> matchedKeywords,
        List<String> missingKeywords,
        List<String> weakBulletPoints,
        List<String> grammarSuggestions,
        List<String> formattingSuggestions,
        int readabilityScore,
        Map<String, Object> optimizedSections,
        String optimizedContent
) {
}
