package com.resumepilot.config;

import com.resumepilot.entity.Resume;
import com.resumepilot.entity.StoredFile;
import com.resumepilot.entity.User;
import com.resumepilot.repository.ResumeRepository;
import com.resumepilot.repository.StoredFileRepository;
import com.resumepilot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * One-shot migration of files that live on local disk (old uploads directory)
 * into database BLOB storage. Enable with {@code MIGRATE_LOCAL_FILES=true}.
 *
 * <p>For every resume/profile-picture reference that still points at a local
 * path, the file bytes are copied into {@code stored_files} and the reference
 * is rewritten to {@code db://{id}}. Original files are left on disk as a
 * backup. Idempotent: already-migrated references are skipped.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.migrate-local-files", havingValue = "true")
public class LocalFileMigrationRunner implements ApplicationRunner {

    private static final String DB_PREFIX = "db://";

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final StoredFileRepository storedFileRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<String> problems = new ArrayList<>();
        int migrated = 0;

        for (Resume resume : resumeRepository.findAll()) {
            String ref = resume.getStoragePath();
            if (ref == null || ref.startsWith(DB_PREFIX)) {
                continue;
            }
            StoredFile storedFile = storeIfReadable(resume.getUser().getId(), "resumes", ref, problems);
            if (storedFile != null) {
                resume.setStoragePath(DB_PREFIX + storedFile.getId());
                resumeRepository.save(resume);
                migrated++;
            }
        }

        for (User user : userRepository.findAll()) {
            String ref = user.getProfilePicturePath();
            if (ref == null || ref.startsWith(DB_PREFIX)) {
                continue;
            }
            StoredFile storedFile = storeIfReadable(user.getId(), "profile", ref, problems);
            if (storedFile != null) {
                user.setProfilePicturePath(DB_PREFIX + storedFile.getId());
                userRepository.save(user);
                migrated++;
            }
        }

        log.info("Local file migration finished: {} file(s) moved into database", migrated);
        problems.forEach(p -> log.warn("Migration: {}", p));
    }

    private StoredFile storeIfReadable(Long userId, String category, String ref, List<String> problems) {
        try {
            Path path = Path.of(ref);
            if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
                problems.add("not readable, skipped: " + ref);
                return null;
            }
            StoredFile storedFile = new StoredFile();
            storedFile.setUserId(userId);
            storedFile.setCategory(category);
            storedFile.setOriginalName(path.getFileName().toString());
            storedFile.setData(Files.readAllBytes(path));
            storedFileRepository.save(storedFile);
            log.info("Migrated {} -> db://{}", ref, storedFile.getId());
            return storedFile;
        } catch (Exception e) {
            problems.add("failed to migrate " + ref + ": " + e.getMessage());
            return null;
        }
    }
}
