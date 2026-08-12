package com.resumepilot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumepilot.entity.JobDescription;
import com.resumepilot.entity.User;
import com.resumepilot.exception.BadRequestException;
import com.resumepilot.exception.ResourceNotFoundException;
import com.resumepilot.repository.JobDescriptionRepository;
import com.resumepilot.util.KeywordExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Stores pasted or uploaded job descriptions and runs structured extraction
 * (skills, responsibilities, keywords, experience, education, tools).
 */
@Service
@RequiredArgsConstructor
public class JobDescriptionService {

    private final JobDescriptionRepository jdRepository;
    private final ResumeParserService parserService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    /** Stores a pasted JD, extracts structure and persists it. */
    @Transactional
    public JobDescription paste(User user, String content, String title) {
        JobDescription jd = new JobDescription();
        jd.setUser(user);
        jd.setContent(content);
        jd.setTitle(title != null && !title.isBlank()
                ? title.trim() : inferTitle(content));
        jd.setSource("PASTE");
        applyExtraction(jd, content);
        return jdRepository.save(jd);
    }

    /** Stores an uploaded JD file (PDF/DOCX), extracts text + structure. */
    @Transactional
    public JobDescription upload(User user, MultipartFile file) {
        String extension = fileStorageService.extensionOf(file.getOriginalFilename());
        if (!"pdf".equals(extension) && !"docx".equals(extension)) {
            throw new BadRequestException("Only PDF and DOCX files are supported");
        }
        String content = parserService.extractText(file);
        String stored = fileStorageService.store(user.getId(), "jd", file, "pdf,docx");

        JobDescription jd = new JobDescription();
        jd.setUser(user);
        jd.setContent(content);
        jd.setTitle(inferTitle(content));
        jd.setSource("UPLOAD");
        jd.setOriginalFileName(file.getOriginalFilename());
        applyExtraction(jd, content);
        return jdRepository.save(jd);
    }

    public JobDescription getOwned(Long jdId, Long userId) {
        return jdRepository.findByIdAndUserId(jdId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Job description", jdId));
    }

    /** Runs local extraction and serializes the lists into JSON columns. */
    private void applyExtraction(JobDescription jd, String content) {
        Map<String, List<String>> extracted = KeywordExtractor.extract(content);
        jd.setSkillsJson(toJson(extracted.get("skills")));
        jd.setResponsibilitiesJson(toJson(extracted.get("responsibilities")));
        jd.setKeywordsJson(toJson(extracted.get("keywords")));
        jd.setExperienceJson(toJson(extracted.get("experience")));
        jd.setEducationJson(toJson(extracted.get("education")));
        jd.setToolsJson(toJson(extracted.get("tools")));
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /** First 80 chars of content as a human-readable title. */
    private String inferTitle(String content) {
        String firstLine = content.lines()
                .map(String::trim)
                .filter(l -> !l.isBlank())
                .findFirst().orElse("Job Description");
        return firstLine.length() > 80 ? firstLine.substring(0, 80) : firstLine;
    }
}
