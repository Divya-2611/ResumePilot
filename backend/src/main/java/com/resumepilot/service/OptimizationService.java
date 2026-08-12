package com.resumepilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumepilot.dto.request.OptimizeRequest;
import com.resumepilot.dto.response.AtsAnalysisResponse;
import com.resumepilot.dto.response.OptimizationResultResponse;
import com.resumepilot.entity.JobDescription;
import com.resumepilot.entity.OptimizationHistory;
import com.resumepilot.entity.OptimizationHistory.Status;
import com.resumepilot.entity.Resume;
import com.resumepilot.entity.ResumeVersion;
import com.resumepilot.entity.User;
import com.resumepilot.exception.BadRequestException;
import com.resumepilot.repository.JobDescriptionRepository;
import com.resumepilot.repository.OptimizationHistoryRepository;
import com.resumepilot.service.ai.AiOptimizationResult;
import com.resumepilot.service.ai.AiProvider;
import com.resumepilot.service.ai.AiProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the optimize flow:
 *
 * <ol>
 *   <li>Loads the resume and job description (stored or inline).</li>
 *   <li>Runs the local ATS analysis (always available).</li>
 *   <li>Calls the configured AI provider; on failure falls back to the local
 *       analysis and rewrites (AI degradation - the endpoint still succeeds).</li>
 *   <li>Persists an optimization history row and a result version.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizationService {

    private final ResumeService resumeService;
    private final JobDescriptionService jdService;
    private final JobDescriptionRepository jdRepository;
    private final AtsAnalysisService atsAnalysisService;
    private final AiProviderFactory aiProviderFactory;
    private final OptimizationHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OptimizationResultResponse optimize(User user, OptimizeRequest request) {
        Resume resume = resumeService.getOwned(request.resumeId(), user.getId());
        JobDescription jd = resolveJobDescription(user, request);

        // 1. Local analysis - always available.
        AtsAnalysisResponse localAnalysis = atsAnalysisService.analyze(
                resume.getContent() != null ? resume.getContent() : "",
                jd.getContent(),
                List.of());

        boolean aiUsed = false;
        String providerName = null;
        AiOptimizationResult aiResult = null;
        long started = System.currentTimeMillis();

        try {
            AiProvider provider = aiProviderFactory.getProvider();
            providerName = provider.name();
            aiResult = provider.optimize(resume.getContent(), jd.getContent());
            aiUsed = true;
            log.info("AI optimization succeeded via {}", providerName);
        } catch (Exception e) {
            log.warn("AI provider unavailable ({}), using local analysis: {}",
                    aiProviderFactory.activeProviderName(), e.getMessage());
        }

        long duration = System.currentTimeMillis() - started;

        // 2. Merge AI result with local analysis (AI wins on content, local on robustness).
        OptimizationResultResponse result = buildResult(resume, jd, localAnalysis, aiResult, aiUsed, providerName);

        // 3. Persist history + auto-save the optimized content as a new version.
        OptimizationHistory history = persistHistory(user, resume, jd, localAnalysis, aiResult, providerName, duration);
        persistVersion(resume, jd, localAnalysis, aiResult, history);

        resumeService.updateAtsScore(resume.getId(), result.analysis().atsScore());
        Long resultVersionId = history.getResultVersion() != null
                ? history.getResultVersion().getId() : null;
        return new OptimizationResultResponse(
                resume.getName(), resume.getId(), result.analysis(),
                result.optimizedSections(), result.optimizedContent(), aiUsed, providerName,
                history.getId(), resultVersionId);
    }

    /** History row for the dashboard, activity feed and CSV export. */
    private OptimizationHistory persistHistory(User user, Resume resume, JobDescription jd,
                                               AtsAnalysisResponse local, AiOptimizationResult ai,
                                               String providerName, long duration) {
        OptimizationHistory history = new OptimizationHistory();
        history.setUser(user);
        history.setResume(resume);
        history.setJobDescription(jd);
        history.setAtsScore(ai != null ? ai.atsScore() : local.atsScore());
        history.setKeywordMatchPercent(ai != null ? ai.keywordMatchPercent() : local.keywordMatchPercent());
        history.setMissingKeywordsJson(toJson(ai != null ? ai.missingKeywords() : local.missingKeywords()));
        history.setStatus(Status.COMPLETED);
        history.setAiProvider(providerName);
        history.setDurationMs(duration);
        history.setCompletedAt(LocalDateTime.now());
        return historyRepository.save(history);
    }

    /** Auto-saves the result as a new version so "undo" always works. */
    private void persistVersion(Resume resume, JobDescription jd,
                                AtsAnalysisResponse local, AiOptimizationResult ai,
                                OptimizationHistory history) {
        String content = ai != null && ai.optimizedContent() != null
                ? ai.optimizedContent() : resume.getContent();
        String structure = ai != null
                ? toJson(ai.optimizedSections()) : null;
        int ats = ai != null ? ai.atsScore() : local.atsScore();
        int kwm = ai != null ? ai.keywordMatchPercent() : local.keywordMatchPercent();

        ResumeVersion version = resumeService.saveVersion(
                resume.getId(), resume.getUser().getId(),
                "Optimized - " + (jd.getTitle() != null ? jd.getTitle() : "JD"),
                jd.getTitle(),
                content, structure, ats, kwm, ai != null);
        history.setResultVersion(version);
        historyRepository.save(history);
    }

    private OptimizationResultResponse buildResult(Resume resume, JobDescription jd,
                                                   AtsAnalysisResponse local,
                                                   AiOptimizationResult ai,
                                                   boolean aiUsed, String providerName) {
        if (ai == null) {
            // Local fallback: score + hints, content unchanged.
            return new OptimizationResultResponse(
                    resume.getName(), resume.getId(), local,
                    Map.of(), resume.getContent(), false, null, null, null);
        }
        return new OptimizationResultResponse(
                resume.getName(), resume.getId(), aiAnalysis(ai, local),
                ai.optimizedSections(),
                ai.optimizedContent() != null ? ai.optimizedContent() : resume.getContent(),
                true, providerName, null, null);
    }

    /** AI analysis preferred; missing fields backfilled from local analysis. */
    private AtsAnalysisResponse aiAnalysis(AiOptimizationResult ai, AtsAnalysisResponse local) {
        return new AtsAnalysisResponse(
                clamp(ai.atsScore()),
                clamp(ai.keywordMatchPercent()),
                ai.matchedKeywords() != null && !ai.matchedKeywords().isEmpty()
                        ? ai.matchedKeywords() : local.matchedKeywords(),
                ai.missingKeywords() != null ? ai.missingKeywords() : local.missingKeywords(),
                ai.weakBulletPoints() != null ? ai.weakBulletPoints() : local.weakBulletPoints(),
                ai.grammarSuggestions() != null ? ai.grammarSuggestions() : local.grammarSuggestions(),
                ai.formattingSuggestions() != null ? ai.formattingSuggestions() : local.formattingSuggestions(),
                clamp(ai.readabilityScore()),
                local.scoreBreakdown());
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    /** Resolves the JD used for this optimization run. */
    private JobDescription resolveJobDescription(User user, OptimizeRequest request) {
        if (request.jobDescriptionId() != null) {
            return jdService.getOwned(request.jobDescriptionId(), user.getId());
        }
        if (request.jobDescriptionText() != null && !request.jobDescriptionText().isBlank()) {
            // Persist the inline JD so history keeps a reference.
            return jdService.paste(user, request.jobDescriptionText(), null);
        }
        throw new BadRequestException("Provide a job description (id or text)");
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }
}
