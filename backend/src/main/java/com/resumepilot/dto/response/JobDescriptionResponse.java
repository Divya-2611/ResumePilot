package com.resumepilot.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Structured insights extracted from a job description.
 */
public record JobDescriptionResponse(
        Long id,
        String title,
        String content,
        String source,
        List<String> skills,
        List<String> responsibilities,
        List<String> keywords,
        List<String> experience,
        List<String> education,
        List<String> tools,
        String createdAt
) {
}
