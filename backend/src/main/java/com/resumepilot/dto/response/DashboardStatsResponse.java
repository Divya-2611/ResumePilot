package com.resumepilot.dto.response;

import java.util.List;

/**
 * Aggregate numbers shown on the dashboard.
 */
public record DashboardStatsResponse(
        long totalResumes,
        long totalOptimizations,
        long totalDownloads,
        long savedResumes,
        long optimizationsLast30Days,
        List<ResumeResponse> recentResumes,
        List<HistoryItemResponse> recentActivity,
        UserResponse profile
) {
}
