package com.resumepilot.dto.response;

/**
 * Resume payload used across listing, detail and dashboard views.
 */
public record ResumeResponse(
        Long id,
        String name,
        String content,
        String structure,
        String originalFileName,
        String fileType,
        Long fileSizeBytes,
        String template,
        boolean favorite,
        boolean optimized,
        Integer atsScore,
        long versionCount,
        String createdAt,
        String updatedAt
) {
}
