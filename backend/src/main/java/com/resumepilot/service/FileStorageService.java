package com.resumepilot.service;

import com.resumepilot.entity.StoredFile;
import com.resumepilot.exception.BadRequestException;
import com.resumepilot.exception.FileStorageException;
import com.resumepilot.repository.StoredFileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * File storage with two interchangeable backends, chosen by configuration
 * ({@code app.storage.mode}):
 *
 * <ul>
 *   <li><b>Database</b> (default, {@code db}): file bytes are stored as BLOBs in the
 *       {@code stored_files} table. References look like {@code db://{id}} and survive
 *       restarts, redeploys and scale-to-zero — no external storage service needed.</li>
 *   <li><b>Local disk</b> ({@code local}): {@code uploads/{userId}/{category}/{uuid}.{ext}}
 *       with plain absolute paths in the database.</li>
 * </ul>
 *
 * <p>References start with {@code db://} or are absolute paths, so the database can
 * hold a mix of both without ambiguity.</p>
 */
@Slf4j
@Service
public class FileStorageService {

    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final String DB_PREFIX = "db://";

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.storage.mode:db}")
    private String storageMode;

    private final StoredFileRepository storedFileRepository;

    public FileStorageService(StoredFileRepository storedFileRepository) {
        this.storedFileRepository = storedFileRepository;
    }

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "docx", "jpg", "jpeg", "png", "webp", "svg");

    private boolean isDbMode() {
        return "db".equalsIgnoreCase(storageMode);
    }

    /**
     * Stores an uploaded file. Returns either a {@code db://{id}} reference
     * (database mode) or an absolute local path (persisted as-is in the database).
     */
    @Transactional
    public String store(Long userId, String category, MultipartFile file, String allowedExtensions) {
        validateFile(file, allowedExtensions);
        String originalName = file.getOriginalFilename();

        if (isDbMode()) {
            try (InputStream in = file.getInputStream()) {
                StoredFile storedFile = new StoredFile();
                storedFile.setUserId(userId);
                storedFile.setCategory(category);
                storedFile.setOriginalName(originalName);
                storedFile.setContentType(file.getContentType());
                storedFile.setData(in.readAllBytes());
                storedFileRepository.save(storedFile);
                log.debug("Stored file {} in DB (user {}, category {})", storedFile.getId(), userId, category);
                return DB_PREFIX + storedFile.getId();
            } catch (IOException e) {
                throw new FileStorageException("Could not read uploaded file", e);
            }
        }

        try {
            Path userDir = Paths.get(uploadDir, String.valueOf(userId), category);
            Files.createDirectories(userDir);
            Path target = userDir.resolve(UUID.randomUUID() + "." + extensionOf(originalName));
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target.toString();
        } catch (IOException e) {
            throw new FileStorageException("Could not store file", e);
        }
    }

    /** Returns true when the reference points at database storage. */
    public boolean isDbReference(String storedPath) {
        return storedPath != null && storedPath.startsWith(DB_PREFIX);
    }

    /**
     * Loads a stored file into a local file. For DB references the bytes are
     * materialized into a temp file (callers serve it directly afterwards).
     */
    public Path resolve(String storedPath) {
        if (isDbReference(storedPath)) {
            long id = parseDbId(storedPath);
            StoredFile storedFile = storedFileRepository.findById(id)
                    .orElseThrow(() -> new FileStorageException("Stored file not found: " + storedPath));
            try {
                String name = storedFile.getOriginalName() != null
                        ? storedFile.getOriginalName() : id + "." + storedFile.getContentType();
                Path temp = Files.createTempFile("ro-", "-" + sanitize(name));
                Files.write(temp, storedFile.getData());
                temp.toFile().deleteOnExit();
                return temp;
            } catch (IOException e) {
                throw new FileStorageException("Could not materialize stored file: " + storedPath, e);
            }
        }
        Path path = Paths.get(storedPath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new FileStorageException("Stored file not found: " + storedPath);
        }
        return path;
    }

    /** Deletes a stored file, whether in the database or on disk. */
    @Transactional
    public void delete(String storedPath) {
        if (isDbReference(storedPath)) {
            storedFileRepository.deleteById(parseDbId(storedPath));
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(storedPath));
        } catch (IOException e) {
            // Non-critical: orphaned files are cleaned by housekeeping.
        }
    }

    /** Removes everything a user owns (account deletion). */
    @Transactional
    public void deleteUserDirectory(Long userId) {
        if (isDbMode()) {
            storedFileRepository.deleteByUserId(userId);
            log.debug("Deleted DB files for user {}", userId);
            return;
        }
        try {
            Path userDir = Paths.get(uploadDir, String.valueOf(userId));
            if (Files.exists(userDir)) {
                try (var walk = Files.walk(userDir)) {
                    walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
                }
            }
        } catch (IOException e) {
            // Non-critical.
        }
    }

    public String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private long parseDbId(String storedPath) {
        try {
            return Long.parseLong(storedPath.substring(DB_PREFIX.length()));
        } catch (NumberFormatException e) {
            throw new FileStorageException("Invalid stored file reference: " + storedPath);
        }
    }

    private String sanitize(String name) {
        String cleaned = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (cleaned.length() > 80) {
            cleaned = cleaned.substring(cleaned.length() - 80);
        }
        return cleaned.isBlank() ? "file" : cleaned;
    }

    private void validateFile(MultipartFile file, String allowedExtensions) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file provided");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File too large (max 10 MB)");
        }
        String extension = extensionOf(file.getOriginalFilename());
        Set<String> allowed = Set.of(allowedExtensions.toLowerCase(Locale.ROOT).split(","));
        if (!allowed.contains(extension) || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Unsupported file type: " + extension);
        }
    }
}
