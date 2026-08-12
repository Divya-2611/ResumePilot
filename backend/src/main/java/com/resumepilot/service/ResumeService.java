package com.resumepilot.service;

import com.resumepilot.dto.request.CreateResumeRequest;
import com.resumepilot.dto.request.UpdateResumeRequest;
import com.resumepilot.dto.response.ResumeResponse;
import com.resumepilot.dto.response.ResumeVersionResponse;
import com.resumepilot.entity.Resume;
import com.resumepilot.entity.ResumeVersion;
import com.resumepilot.entity.User;
import com.resumepilot.exception.BadRequestException;
import com.resumepilot.exception.ResourceNotFoundException;
import com.resumepilot.mapper.ResumeMapper;
import com.resumepilot.repository.DownloadRecordRepository;
import com.resumepilot.repository.OptimizationHistoryRepository;
import com.resumepilot.repository.ResumeRepository;
import com.resumepilot.repository.ResumeVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * Resume lifecycle: create (blank or from template), upload (PDF/DOCX with
 * text extraction), edit, duplicate, rename, favorite, delete, version
 * history and restore.
 */
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository versionRepository;
    private final ResumeParserService parserService;
    private final FileStorageService fileStorageService;
    private final OptimizationHistoryRepository historyRepository;
    private final DownloadRecordRepository downloadRepository;

    // ------------------------------------------------------------------
    // Create / Upload
    // ------------------------------------------------------------------

    @Transactional
    public ResumeResponse create(User user, CreateResumeRequest request) {
        Resume resume = new Resume();
        resume.setUser(user);
        resume.setName(request.name().trim());
        resume.setTemplate(request.template() != null ? request.template() : "modern");
        resume.setContent(request.content() != null ? request.content() : "");
        resume.setStructure(request.structure());
        // Persist first so the version snapshot references a managed resume.
        resume = resumeRepository.save(resume);
        // First snapshot.
        createVersion(resume, "Original", null, resume.getContent(),
                resume.getStructure(), false);
        return ResumeMapper.toResponse(resume);
    }

    /** Uploads a PDF/DOCX resume: stores the original file and extracts text. */
    @Transactional
    public ResumeResponse upload(User user, String name, MultipartFile file) {
        String extension = fileStorageService.extensionOf(file.getOriginalFilename());
        if (!"pdf".equals(extension) && !"docx".equals(extension)) {
            throw new BadRequestException("Only PDF and DOCX files are supported");
        }

        String content = parserService.extractText(file);
        String stored = fileStorageService.store(user.getId(), "resumes", file, "pdf,docx");

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setName(name != null && !name.isBlank()
                ? name.trim() : stripExtension(file.getOriginalFilename()));
        resume.setContent(content);
        resume.setOriginalFileName(file.getOriginalFilename());
        resume.setStoragePath(stored);
        resume.setFileType(extension.toUpperCase());
        resume.setFileSizeBytes(file.getSize());
        resume.setTemplate("modern");

        // Persist first so the version snapshot references a managed resume.
        resume = resumeRepository.save(resume);
        createVersion(resume, "Uploaded original", null, content, null, false);
        return ResumeMapper.toResponse(resume);
    }

    // ------------------------------------------------------------------
    // Read / Search
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ResumeResponse> search(Long userId, String query, boolean favoritesOnly, Pageable pageable) {
        String q = (query == null || query.isBlank()) ? null : query.trim();
        // Pattern built in Java (not in SQL) to avoid PostgreSQL's bytea/|| overload ambiguity.
        String pattern = q == null ? "%" : "%" + q.toLowerCase() + "%";
        return resumeRepository.search(userId, q, pattern, favoritesOnly, pageable)
                .map(ResumeMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ResumeResponse get(Long resumeId, Long userId) {
        return ResumeMapper.toResponse(getOwned(resumeId, userId));
    }

    @Transactional(readOnly = true)
    public java.util.List<ResumeVersionResponse> getVersions(Long resumeId, Long userId) {
        getOwned(resumeId, userId);
        return versionRepository.findByResumeIdOrderByCreatedAtDesc(resumeId)
                .stream().map(ResumeMapper::toVersionResponse).toList();
    }

    // ------------------------------------------------------------------
    // Update / Rename / Duplicate / Favorite
    // ------------------------------------------------------------------

    @Transactional
    public ResumeResponse update(Long resumeId, Long userId, UpdateResumeRequest request) {
        Resume resume = getOwned(resumeId, userId);
        boolean changed = false;

        if (request.name() != null && !request.name().isBlank()) {
            resume.setName(request.name().trim());
            changed = true;
        }
        if (request.template() != null && !request.template().isBlank()) {
            resume.setTemplate(request.template());
            changed = true;
        }
        if (request.content() != null) {
            if (!request.content().equals(resume.getContent())) {
                // Snapshot previous state before overwriting (undo support).
                createVersion(resume, "Before edit", null,
                        resume.getContent(), resume.getStructure(), false);
                resume.setContent(request.content());
                changed = true;
            }
        }
        if (request.structure() != null) {
            resume.setStructure(request.structure());
            changed = true;
        }
        if (request.favorite() != null) {
            resume.setFavorite(Boolean.parseBoolean(request.favorite()));
            changed = true;
        }
        if (!changed) {
            throw new BadRequestException("Nothing to update");
        }
        return ResumeMapper.toResponse(resumeRepository.save(resume));
    }

    @Transactional
    public void toggleFavorite(Long resumeId, Long userId) {
        Resume resume = getOwned(resumeId, userId);
        resume.setFavorite(!resume.isFavorite());
        resumeRepository.save(resume);
    }

    /** Deep-copies the resume including its content (not file references). */
    @Transactional
    public ResumeResponse duplicate(Long resumeId, Long userId) {
        Resume source = getOwned(resumeId, userId);
        Resume copy = new Resume();
        copy.setUser(source.getUser());
        copy.setName(source.getName() + " (copy)");
        copy.setContent(source.getContent());
        copy.setStructure(source.getStructure());
        copy.setTemplate(source.getTemplate());
        copy.setFavorite(false);
        copy.setOptimized(source.isOptimized());
        copy.setAtsScore(source.getAtsScore());
        // Persist first so the version snapshot references a managed resume.
        copy = resumeRepository.save(copy);
        createVersion(copy, "Original", null, copy.getContent(), copy.getStructure(), false);
        return ResumeMapper.toResponse(copy);
    }

    @Transactional
    public void delete(Long resumeId, Long userId) {
        Resume resume = getOwned(resumeId, userId);
        if (resume.getStoragePath() != null) {
            fileStorageService.delete(resume.getStoragePath());
        }
        historyRepository.deleteByResumeId(resumeId);
        downloadRepository.deleteByResumeId(resumeId);
        resumeRepository.delete(resume);
    }

    // ------------------------------------------------------------------
    // Versions / Restore / Undo
    // ------------------------------------------------------------------

    /** Persists a new version snapshot (optimization results, edits, restores). */
    @Transactional
    public ResumeVersion saveVersion(Long resumeId, Long userId, String name, String label,
                                     String content, String structure,
                                     Integer atsScore, Integer keywordMatchPercent,
                                     boolean aiGenerated) {
        Resume resume = getOwned(resumeId, userId);
        return createVersion(resume, name, label, content, structure, aiGenerated);
    }

    /** Applies a version's content back onto the resume (restore / undo). */
    @Transactional
    public ResumeResponse restoreVersion(Long resumeId, Long versionId, Long userId) {
        Resume resume = getOwned(resumeId, userId);
        ResumeVersion version = versionRepository.findByIdAndResumeId(versionId, resumeId)
                .orElseThrow(() -> ResourceNotFoundException.of("Version", versionId));

        // Snapshot current state before restoring.
        createVersion(resume, "Before restore", null, resume.getContent(),
                resume.getStructure(), false);

        resume.setContent(version.getContent());
        resume.setStructure(version.getStructure());
        resume.setAtsScore(version.getAtsScore());
        return ResumeMapper.toResponse(resumeRepository.save(resume));
    }

    @Transactional
    public void toggleVersionFavorite(Long versionId, Long userId) {
        ResumeVersion version = versionRepository.findByIdAndResumeUserId(versionId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Version", versionId));
        version.setFavorite(!version.isFavorite());
        versionRepository.save(version);
    }

    @Transactional
    public void deleteVersion(Long resumeId, Long versionId, Long userId) {
        getOwned(resumeId, userId);
        ResumeVersion version = versionRepository.findByIdAndResumeId(versionId, resumeId)
                .orElseThrow(() -> ResourceNotFoundException.of("Version", versionId));
        if (versionRepository.countByResumeId(resumeId) <= 1) {
            throw new BadRequestException("Cannot delete the last version of a resume");
        }
        downloadRepository.clearVersion(versionId);
        historyRepository.clearResultVersion(versionId);
        versionRepository.delete(version);
    }

    @Transactional
    public ResumeVersionResponse renameVersion(Long resumeId, Long versionId, Long userId, String name) {
        getOwned(resumeId, userId);
        ResumeVersion version = versionRepository.findByIdAndResumeId(versionId, resumeId)
                .orElseThrow(() -> ResourceNotFoundException.of("Version", versionId));
        version.setName(name.trim());
        return ResumeMapper.toVersionResponse(versionRepository.save(version));
    }

    public Resume getOwned(Long resumeId, Long userId) {
        return resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Resume", resumeId));
    }

    /** Convenience for the optimization service: mark score after optimization. */
    @Transactional
    public void updateAtsScore(Long resumeId, Integer score) {
        resumeRepository.findById(resumeId).ifPresent(r -> {
            r.setAtsScore(score);
            r.setOptimized(true);
            resumeRepository.save(r);
        });
    }

    private ResumeVersion createVersion(Resume resume, String name, String label,
                                        String content, String structure, boolean aiGenerated) {
        ResumeVersion version = new ResumeVersion();
        version.setResume(resume);
        version.setName(name);
        version.setLabel(label);
        version.setContent(content);
        version.setStructure(structure);
        version.setAiGenerated(aiGenerated);
        return versionRepository.save(version);
    }

    private String stripExtension(String filename) {
        return filename != null && filename.contains(".")
                ? filename.substring(0, filename.lastIndexOf('.')) : filename;
    }
}
