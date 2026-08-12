package com.resumepilot.dto.response;

/**
 * Optimization history row (also used for activity feed and CSV export).
 */
public record HistoryItemResponse(
        Long id,
        String resumeName,
        Long resumeId,
        String jobTitle,
        Integer atsScore,
        Integer keywordMatchPercent,
        String aiProvider,
        String status,
        Long resultVersionId,
        String createdAt
) {
}
