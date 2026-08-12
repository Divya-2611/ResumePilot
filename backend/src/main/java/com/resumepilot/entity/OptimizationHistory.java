package com.resumepilot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Record of a single optimization run: which resume was optimized against
 * which job description, the resulting score and which AI provider produced it.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "optimization_history",
        indexes = {
                @Index(name = "idx_hist_user", columnList = "user_id"),
                @Index(name = "idx_hist_created", columnList = "created_at")
        })
public class OptimizationHistory extends BaseEntity {

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
    @JoinColumn(name = "job_description_id")
    private JobDescription jobDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_version_id")
    private ResumeVersion resultVersion;

    @Column(name = "ats_score")
    private Integer atsScore;

    @Column(name = "keyword_match_percent")
    private Integer keywordMatchPercent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_keywords", columnDefinition = "jsonb")
    private String missingKeywordsJson;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.COMPLETED;

    @Column(name = "ai_provider", length = 20)
    private String aiProvider;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum Status {
        PENDING, RUNNING, COMPLETED, FAILED
    }
}
