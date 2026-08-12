package com.resumepilot.controller;

import com.resumepilot.dto.request.OptimizeRequest;
import com.resumepilot.dto.response.ApiResponse;
import com.resumepilot.dto.response.OptimizationResultResponse;
import com.resumepilot.service.OptimizationService;
import com.resumepilot.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI optimization endpoint.
 */
@RestController
@RequestMapping("/api/v1/optimize")
@RequiredArgsConstructor
public class OptimizationController {

    private final OptimizationService optimizationService;

    @PostMapping
    public ResponseEntity<ApiResponse<OptimizationResultResponse>> optimize(
            @Valid @RequestBody OptimizeRequest request) {
        OptimizationResultResponse result =
                optimizationService.optimize(SecurityUtils.currentUser(), request);
        return ResponseEntity.ok(ApiResponse.success(
                result.aiGenerated() ? "Resume optimized by AI" : "Resume analyzed (AI unavailable)",
                result));
    }
}
