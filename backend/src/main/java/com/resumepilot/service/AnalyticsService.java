package com.resumepilot.service;

import com.resumepilot.dto.response.DashboardStatsResponse;
import com.resumepilot.dto.response.HistoryItemResponse;
import com.resumepilot.dto.response.ResumeResponse;
import com.resumepilot.mapper.OptimizationMapper;
import com.resumepilot.mapper.ResumeMapper;
import com.resumepilot.mapper.UserMapper;
import com.resumepilot.repository.DownloadRecordRepository;
import com.resumepilot.repository.OptimizationHistoryRepository;
import com.resumepilot.repository.ResumeRepository;
import com.resumepilot.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dashboard aggregates, history queries and CSV export.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ResumeRepository resumeRepository;
    private final OptimizationHistoryRepository historyRepository;
    private final DownloadRecordRepository downloadRepository;
    private final OptimizationMapper optimizationMapper;

    @Transactional(readOnly = true)
    public DashboardStatsResponse dashboard(Long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<ResumeResponse> recent = resumeRepository.findRecent(userId, 5)
                .stream().map(ResumeMapper::toResponse).toList();

        List<HistoryItemResponse> activity = historyRepository
                .findTop10ByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(optimizationMapper::toHistoryResponse).toList();

        return new DashboardStatsResponse(
                resumeRepository.countByUserId(userId),
                historyRepository.countByUserId(userId),
                downloadRepository.countByUserId(userId),
                resumeRepository.countByUserIdAndFavoriteTrue(userId),
                historyRepository.countByUserIdAndCreatedAtAfter(userId, thirtyDaysAgo),
                recent,
                activity,
                UserMapper.toResponse(SecurityUtils.currentUser()));
    }

    @Transactional(readOnly = true)
    public Page<HistoryItemResponse> history(Long userId, String query, Pageable pageable) {
        String q = (query == null || query.isBlank()) ? null : query.trim();
        // Pattern built in Java (not in SQL) to avoid PostgreSQL bytea/|| overload ambiguity.
        String pattern = q == null ? "%" : "%" + q.toLowerCase() + "%";
        return historyRepository.search(userId, q, pattern, pageable)
                .map(optimizationMapper::toHistoryResponse);
    }

    /** All history rows for the authenticated user (CSV export). */
    @Transactional(readOnly = true)
    public List<String[]> historyRowsForExport(Long userId) {
        return historyRepository.findByUserId(userId).stream()
                .map(h -> new String[]{
                        h.getCreatedAt() != null ? h.getCreatedAt().toString() : "",
                        h.getResume() != null ? h.getResume().getName() : "",
                        h.getJobDescription() != null ? h.getJobDescription().getTitle() : "",
                        h.getAtsScore() != null ? String.valueOf(h.getAtsScore()) : "",
                        h.getKeywordMatchPercent() != null ? String.valueOf(h.getKeywordMatchPercent()) : "",
                        h.getAiProvider() != null ? h.getAiProvider() : "",
                        h.getStatus() != null ? h.getStatus().name() : ""})
                .toList();
    }
}
