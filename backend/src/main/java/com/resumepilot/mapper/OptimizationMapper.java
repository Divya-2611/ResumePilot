package com.resumepilot.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumepilot.dto.response.HistoryItemResponse;
import com.resumepilot.dto.response.JobDescriptionResponse;
import com.resumepilot.entity.JobDescription;
import com.resumepilot.entity.OptimizationHistory;
import com.resumepilot.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps optimization / job description entities to DTOs and JSON columns to lists.
 */
@Component
@RequiredArgsConstructor
public class OptimizationMapper {

    private final ObjectMapper objectMapper;

    /** Parses a JSON-array column safely; returns empty list on any parse issue. */
    public List<String> jsonToList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    public JobDescriptionResponse toJdResponse(JobDescription jd) {
        return new JobDescriptionResponse(
                jd.getId(),
                jd.getTitle(),
                jd.getContent(),
                jd.getSource(),
                jsonToList(jd.getSkillsJson()),
                jsonToList(jd.getResponsibilitiesJson()),
                jsonToList(jd.getKeywordsJson()),
                jsonToList(jd.getExperienceJson()),
                jsonToList(jd.getEducationJson()),
                jsonToList(jd.getToolsJson()),
                DateTimeUtil.toIsoString(jd.getCreatedAt()));
    }

    public HistoryItemResponse toHistoryResponse(OptimizationHistory h) {
        String resumeName = h.getResume() != null ? h.getResume().getName() : "Deleted resume";
        Long resumeId = h.getResume() != null ? h.getResume().getId() : null;
        String jobTitle = h.getJobDescription() != null ? h.getJobDescription().getTitle() : null;
        Long versionId = h.getResultVersion() != null ? h.getResultVersion().getId() : null;
        return new HistoryItemResponse(
                h.getId(),
                resumeName,
                resumeId,
                jobTitle,
                h.getAtsScore(),
                h.getKeywordMatchPercent(),
                h.getAiProvider(),
                h.getStatus().name(),
                versionId,
                DateTimeUtil.toIsoString(h.getCreatedAt()));
    }
}
