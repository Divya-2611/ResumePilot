package com.resumepilot.controller;

import com.resumepilot.dto.request.CreateResumeRequest;
import com.resumepilot.dto.request.RenameVersionRequest;
import com.resumepilot.dto.request.SaveVersionRequest;
import com.resumepilot.dto.request.UpdateResumeRequest;
import com.resumepilot.dto.response.ApiResponse;
import com.resumepilot.dto.response.ResumeResponse;
import com.resumepilot.dto.response.ResumeVersionResponse;
import com.resumepilot.service.ResumeService;
import com.resumepilot.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Authenticated resume management endpoints (CRUD, upload, versions).
 */
@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    // ------------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------------

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<ResumeResponse>> upload(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam("file") MultipartFile file) {
        ResumeResponse created = resumeService.upload(SecurityUtils.currentUser(), name, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resume uploaded and parsed", created));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<ResumeResponse>> create(
            @Valid @RequestBody CreateResumeRequest request) {
        ResumeResponse created = resumeService.create(SecurityUtils.currentUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resume created", created));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<ResumeResponse>>> getAll(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "9") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "favorites", defaultValue = "false") boolean favorites) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return ResponseEntity.ok(ApiResponse.success(
                resumeService.search(SecurityUtils.currentUserId(), search, favorites, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResumeResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                resumeService.get(id, SecurityUtils.currentUserId())));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<ResumeResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateResumeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Resume updated",
                resumeService.update(id, SecurityUtils.currentUserId(), request)));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        resumeService.delete(id, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.success("Resume deleted"));
    }

    // ------------------------------------------------------------------
    // Utilities
    // ------------------------------------------------------------------

    @PostMapping("/duplicate/{id}")
    public ResponseEntity<ApiResponse<ResumeResponse>> duplicate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Resume duplicated",
                resumeService.duplicate(id, SecurityUtils.currentUserId())));
    }

    @PostMapping("/favorite/{id}")
    public ResponseEntity<ApiResponse<ResumeResponse>> toggleFavorite(@PathVariable Long id) {
        resumeService.toggleFavorite(id, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.success("Favorite toggled",
                resumeService.get(id, SecurityUtils.currentUserId())));
    }

    // ------------------------------------------------------------------
    // Versions
    // ------------------------------------------------------------------

    @GetMapping("/{id}/versions")
    public ResponseEntity<ApiResponse<List<ResumeVersionResponse>>> versions(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                resumeService.getVersions(id, SecurityUtils.currentUserId())));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<ResumeVersionResponse>> saveVersion(
            @Valid @RequestBody SaveVersionRequest request) {
        var version = resumeService.saveVersion(
                request.resumeId(), SecurityUtils.currentUserId(),
                request.name(), request.label(),
                request.content(), request.structure(),
                request.atsScore(), request.keywordMatchPercent(), false);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Version saved",
                        resumeService.getVersions(request.resumeId(),
                                SecurityUtils.currentUserId()).stream()
                                .filter(v -> v.id().equals(version.getId()))
                                .findFirst().orElse(null)));
    }

    @PostMapping("/{id}/restore/{versionId}")
    public ResponseEntity<ApiResponse<ResumeResponse>> restore(@PathVariable Long id,
                                                               @PathVariable Long versionId) {
        return ResponseEntity.ok(ApiResponse.success("Version restored",
                resumeService.restoreVersion(id, versionId, SecurityUtils.currentUserId())));
    }

    @PostMapping("/versions/{versionId}/favorite")
    public ResponseEntity<ApiResponse<Void>> favoriteVersion(@PathVariable Long versionId) {
        resumeService.toggleVersionFavorite(versionId, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.success("Version favorite toggled"));
    }

    @PutMapping("/{resumeId}/versions/{versionId}/rename")
    public ResponseEntity<ApiResponse<ResumeVersionResponse>> renameVersion(
            @PathVariable Long resumeId, @PathVariable Long versionId,
            @Valid @RequestBody RenameVersionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Version renamed",
                resumeService.renameVersion(resumeId, versionId,
                        SecurityUtils.currentUserId(), request.name())));
    }

    @DeleteMapping("/{resumeId}/versions/{versionId}")
    public ResponseEntity<ApiResponse<Void>> deleteVersion(@PathVariable Long resumeId,
                                                           @PathVariable Long versionId) {
        resumeService.deleteVersion(resumeId, versionId, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.success("Version deleted"));
    }
}
