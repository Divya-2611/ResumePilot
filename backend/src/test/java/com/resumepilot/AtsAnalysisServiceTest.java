package com.resumepilot;

import com.resumepilot.dto.response.AtsAnalysisResponse;
import com.resumepilot.service.AtsAnalysisService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the local ATS heuristic engine.
 */
class AtsAnalysisServiceTest {

    private final AtsAnalysisService service = new AtsAnalysisService();

    private static final String JD = """
            Senior Java Engineer
            Responsibilities: build microservices with Spring Boot, work with Docker
            and Kubernetes, design REST APIs. Requires MySQL, Redis, Kafka and AWS.
            Bachelor's degree in Computer Science. 5+ years experience.
            """;

    private static final String RESUME = """
            SUMMARY
            Backend engineer with 6 years of Java and Spring Boot experience.

            EXPERIENCE
            - Built microservices with Spring Boot and MySQL
            - Designed REST APIs used by 40k daily users
            - Deployed Docker containers on AWS

            SKILLS
            Java, Spring Boot, MySQL, Docker, AWS

            EDUCATION
            Bachelor of Science in Computer Science
            """;

    @Test
    void keywordMatchCountsJdTokens() {
        AtsAnalysisResponse analysis = service.analyze(RESUME, JD, List.of());

        assertThat(analysis.keywordMatchPercent()).isBetween(0, 100);
        // Skills present in both resume and JD.
        assertThat(analysis.matchedKeywords()).contains("spring boot", "mysql", "docker", "aws");
        // Kafka / Redis / Kubernetes are in the JD but missing from the resume.
        assertThat(analysis.missingKeywords()).contains("kafka", "redis", "kubernetes");
    }

    @Test
    void atsScoreIncreasesWithBetterMatch() {
        AtsAnalysisResponse weak = service.analyze("Front desk assistant at a hotel",
                JD, List.of());
        AtsAnalysisResponse strong = service.analyze(RESUME, JD, List.of());

        assertThat(strong.atsScore()).isGreaterThan(weak.atsScore());
        assertThat(strong.scoreBreakdown()).isNotEmpty();
    }

    @Test
    void readabilityIsClampedToRange() {
        AtsAnalysisResponse analysis = service.analyze(RESUME, JD, List.of());
        assertThat(analysis.readabilityScore()).isBetween(0, 100);
    }

    @Test
    void weakBulletsFlagNonActionBullets() {
        AtsAnalysisResponse analysis = service.analyze(
                "EXPERIENCE\n- was responsible for some things\n- Built a system",
                JD, List.of());
        assertThat(analysis.weakBulletPoints()).isNotEmpty();
    }

    @Test
    void grammarChecksDetectRepeatedWords() {
        AtsAnalysisResponse analysis = service.analyze(
                "SUMMARY\nExperienced Java developer with with 5 years.",
                JD, List.of());
        assertThat(analysis.grammarSuggestions()).isNotEmpty();
    }
}
