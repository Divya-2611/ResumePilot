package com.resumepilot.controller;

import com.resumepilot.dto.response.ApiResponse;
import com.resumepilot.dto.response.DashboardStatsResponse;
import com.resumepilot.dto.response.HistoryItemResponse;
import com.resumepilot.service.AnalyticsService;
import com.resumepilot.service.DownloadService;
import com.resumepilot.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Dashboard, history and download endpoints.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final DownloadService downloadService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.dashboard(SecurityUtils.currentUserId())));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<HistoryItemResponse>>> history(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.history(SecurityUtils.currentUserId(), search,
                        PageRequest.of(page, size))));
    }

    @GetMapping("/history/export")
    public ResponseEntity<Resource> exportHistory() {
        byte[] csv = downloadService.exportHistoryAsCsv(
                analyticsService.historyRowsForExport(SecurityUtils.currentUserId()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"optimization-history.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new org.springframework.core.io.ByteArrayResource(csv));
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(
            @RequestParam("resumeId") Long resumeId,
            @RequestParam(value = "versionId", required = false) Long versionId,
            @RequestParam("format") String format,
            @RequestParam(value = "fileName", required = false) String fileName) {
        return downloadService.download(SecurityUtils.currentUser(), resumeId,
                versionId, format, fileName);
    }
}
