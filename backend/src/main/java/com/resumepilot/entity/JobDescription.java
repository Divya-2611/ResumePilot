package com.resumepilot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A pasted or uploaded job description with extracted structured insights
 * (skills, responsibilities, keywords, experience, education, tools).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "job_descriptions",
        indexes = {
                @Index(name = "idx_jd_user", columnList = "user_id"),
                @Index(name = "idx_jd_title", columnList = "title")
        })
public class JobDescription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", length = 160)
    private String title;

    @Lob
    @Column(name = "content", columnDefinition = "text", nullable = false)
    private String content;

    /** JSON arrays extracted from the JD. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skills", columnDefinition = "jsonb")
    private String skillsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "responsibilities", columnDefinition = "jsonb")
    private String responsibilitiesJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keywords", columnDefinition = "jsonb")
    private String keywordsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "experience", columnDefinition = "jsonb")
    private String experienceJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "education", columnDefinition = "jsonb")
    private String educationJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tools", columnDefinition = "jsonb")
    private String toolsJson;

    /** SOURCE: PASTE or UPLOAD. */
    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;
}
