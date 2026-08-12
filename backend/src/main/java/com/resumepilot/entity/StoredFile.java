package com.resumepilot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A stored file (resume PDF/DOCX, JD file, profile picture) kept in the
 * database as binary data. This makes uploads survive server restarts and
 * redeploys without needing a separate object-storage service.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stored_files",
        indexes = {
                @Index(name = "idx_stored_user", columnList = "user_id")
        })
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** resumes | jd | profile */
    @Column(name = "category", nullable = false, length = 20)
    private String category;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    /** The actual file content (bytea on PostgreSQL). */
    @Column(name = "data", nullable = false, columnDefinition = "bytea")
    private byte[] data;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
