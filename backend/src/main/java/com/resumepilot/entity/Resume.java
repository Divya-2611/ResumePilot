package com.resumepilot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A resume owned by a user. Stores the editable plain-text content plus an
 * optional structured JSON representation (sections) and the original file
 * for uploaded resumes.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "resumes",
        indexes = {
                @Index(name = "idx_resume_user", columnList = "user_id"),
                @Index(name = "idx_resume_name", columnList = "name")
        })
public class Resume extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    /** Plain-text resume content (editor friendly, ATS readable). */
    @Lob
    @Column(name = "content", columnDefinition = "text")
    private String content;

    /** Structured JSON of sections {summary, skills, experience, projects, education, achievements}. */
    @Lob
    @Column(name = "structure", columnDefinition = "text")
    private String structure;

    /** Original uploaded file name. */
    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    /** Storage path of the original uploaded file. */
    @Column(name = "storage_path", length = 500)
    private String storagePath;

    @Column(name = "file_type", length = 10)
    private String fileType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "template", length = 40)
    private String template = "modern";

    @Column(name = "is_favorite", nullable = false)
    private boolean favorite = false;

    /** True when the content has been optimized by AI at least once. */
    @Column(name = "optimized", nullable = false)
    private boolean optimized = false;

    @Column(name = "ats_score")
    private Integer atsScore;

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<ResumeVersion> versions = new ArrayList<>();
}
