package com.resumepilot.dto.response;

import java.util.List;
import java.util.Map;

/**
 * ATS compatibility analysis produced for a resume against a job description.
 */
public record AtsAnalysisResponse(
        int atsScore,
        int keywordMatchPercent,
        List<String> matchedKeywords,
        List<String> missingKeywords,
        List<String> weakBulletPoints,
        List<String> grammarSuggestions,
        List<String> formattingSuggestions,
        int readabilityScore,
        Map<String, String> scoreBreakdown
) {
}
