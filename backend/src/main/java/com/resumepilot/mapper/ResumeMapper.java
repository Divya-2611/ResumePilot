package com.resumepilot.mapper;

import com.resumepilot.dto.response.ResumeResponse;
import com.resumepilot.dto.response.ResumeVersionResponse;
import com.resumepilot.entity.Resume;
import com.resumepilot.entity.ResumeVersion;
import com.resumepilot.util.DateTimeUtil;

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
                DateTimeUtil.toIsoString(r.getCreatedAt()),
                DateTimeUtil.toIsoString(r.getUpdatedAt()));
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
                DateTimeUtil.toIsoString(v.getCreatedAt()));
    }
}
