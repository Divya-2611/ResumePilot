package com.resumepilot.controller;

import com.resumepilot.dto.request.PasteJdRequest;
import com.resumepilot.dto.response.ApiResponse;
import com.resumepilot.dto.response.JobDescriptionResponse;
import com.resumepilot.mapper.OptimizationMapper;
import com.resumepilot.service.JobDescriptionService;
import com.resumepilot.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Job description endpoints: paste or upload, with structured extraction.
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobDescriptionController {

    private final JobDescriptionService jdService;
    private final OptimizationMapper mapper;

    @PostMapping("/paste-jd")
    public ResponseEntity<ApiResponse<JobDescriptionResponse>> pasteJd(
            @Valid @RequestBody PasteJdRequest request) {
        var saved = jdService.paste(SecurityUtils.currentUser(), request.content(), request.title());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Job description analyzed", mapper.toJdResponse(saved)));
    }

    @PostMapping("/upload-jd")
    public ResponseEntity<ApiResponse<JobDescriptionResponse>> uploadJd(
            @RequestParam("file") MultipartFile file) {
        var saved = jdService.upload(SecurityUtils.currentUser(), file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Job description analyzed", mapper.toJdResponse(saved)));
    }
}
