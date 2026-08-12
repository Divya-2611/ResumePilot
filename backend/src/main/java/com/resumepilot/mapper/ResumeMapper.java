package com.resumepilot.mapper;

import com.resumepilot.dto.response.ResumeResponse;
import com.resumepilot.dto.response.ResumeVersionResponse;
import com.resumepilot.entity.Resume;
import com.resumepilot.entity.ResumeVersion;

/**
 * Maps between Resume / ResumeVersion entities and DTOs.
 */
public final class ResumeMapper {

    private ResumeMapper() {
    }

    public static ResumeResponse toResponse(Resume r) {
        return new ResumeResponse(
                r.getId(),
                r.getName(),
                r.getContent(),
                r.getStructure(),
                r.getOriginalFileName(),
                r.getFileType(),
                r.getFileSizeBytes(),
                r.getTemplate(),
                r.isFavorite(),
                r.isOptimized(),
                r.getAtsScore(),
                r.getVersions().size(),
                r.getCreatedAt() != null ? r.getCreatedAt().toString() : null,
                r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : null);
    }

    public static ResumeVersionResponse toVersionResponse(ResumeVersion v) {
        return new ResumeVersionResponse(
                v.getId(),
                v.getResume().getId(),
                v.getName(),
                v.getLabel(),
                v.getContent(),
                v.getStructure(),
                v.getAtsScore(),
                v.getKeywordMatchPercent(),
                v.isAiGenerated(),
                v.isFavorite(),
                v.getCreatedAt() != null ? v.getCreatedAt().toString() : null);
    }
}
