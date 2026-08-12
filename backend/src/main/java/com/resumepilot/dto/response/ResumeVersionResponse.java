package com.resumepilot.dto.response;

/**
 * Version history item of a resume.
 */
public record ResumeVersionResponse(
        Long id,
        Long resumeId,
        String name,
        String label,
        String content,
        String structure,
        Integer atsScore,
        Integer keywordMatchPercent,
        boolean aiGenerated,
        boolean favorite,
        String createdAt
) {
}
