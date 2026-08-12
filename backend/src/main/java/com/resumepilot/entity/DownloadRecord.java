package com.resumepilot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tracks every resume/version download (PDF or DOCX) for analytics.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "downloads",
        indexes = @Index(name = "idx_download_user", columnList = "user_id"))
public class DownloadRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id")
    private ResumeVersion version;

    /** PDF or DOCX. */
    @Column(name = "format", nullable = false, length = 10)
    private String format;

    @Column(name = "file_name", length = 255)
    private String fileName;
}
