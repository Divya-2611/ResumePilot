package com.resumepilot.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Complete result of one optimization run: ATS analysis plus AI-generated content.
 */
public record OptimizationResultResponse(
        String resumeName,
        Long resumeId,
        AtsAnalysisResponse analysis,
        /** Structured AI-optimized sections keyed by section name. */
        Map<String, Object> optimizedSections,
        /** Optimized full-text content (renderable/editable). */
        String optimizedContent,
        /** Whether the AI provider produced the result (false = local fallback). */
        boolean aiGenerated,
        String aiProvider,
        Long historyId,
        /** Version id of the auto-saved optimized content (for download/restore). */
        Long resultVersionId
) {
}
