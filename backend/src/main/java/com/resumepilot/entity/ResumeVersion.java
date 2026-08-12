package com.resumepilot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A snapshot of a resume at a point in time (optimization result, manual save,
 * or restore point). Supports version history, favorites and undo.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "resume_versions",
        indexes = {
                @Index(name = "idx_version_resume", columnList = "resume_id"),
                @Index(name = "idx_version_created", columnList = "created_at")
        })
public class ResumeVersion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    /** Human readable label, e.g. "Optimized for Senior Java Engineer @ Acme". */
    @Column(name = "label", length = 255)
    private String label;

    /** Version content (plain text). */
    @Lob
    @Column(name = "content", columnDefinition = "text")
    private String content;

    /** Structured JSON of the optimized sections. */
    @Lob
    @Column(name = "structure", columnDefinition = "text")
    private String structure;

    @Column(name = "ats_score")
    private Integer atsScore;

    @Column(name = "keyword_match_percent")
    private Integer keywordMatchPercent;

    /** Whether this version was produced by AI optimization. */
    @Column(name = "ai_generated", nullable = false)
    private boolean aiGenerated = false;

    @Column(name = "is_favorite", nullable = false)
    private boolean favorite = false;
}
