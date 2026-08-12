package com.resumepilot;

import com.resumepilot.util.KeywordExtractor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies local JD extraction (skills, tools, experience, keywords).
 */
class KeywordExtractorTest {

    private static final String JD = """
            Senior Java Engineer needed. Must know Java, Spring Boot, Docker,
            Kubernetes, MySQL, Redis, Kafka and AWS. 5+ years experience.
            Build microservices. Bachelor's degree preferred. Docker and MySQL
            are critical. Must design REST APIs and build microservices.
            """;

    @Test
    void extractsSkillsAndTools() {
        Map<String, List<String>> result = KeywordExtractor.extract(JD);

        assertThat(result.get("skills"))
                .contains("java", "spring boot", "mysql", "kafka", "docker", "kubernetes", "aws");
    }

    @Test
    void extractsExperienceYears() {
        Map<String, List<String>> result = KeywordExtractor.extract(JD);
        assertThat(result.get("experience")).contains("5+ years");
    }

    @Test
    void extractsRepeatedKeywords() {
        Map<String, List<String>> result = KeywordExtractor.extract(JD);
        // "microservices" and "build" appear twice -> keyword candidates.
        assertThat(result.get("keywords")).contains("microservices", "build");
    }

    @Test
    void extractsEducationHints() {
        Map<String, List<String>> result = KeywordExtractor.extract(JD);
        assertThat(result.get("education")).contains("bachelor");
    }

    @Test
    void emptyInputYieldsEmptyLists() {
        Map<String, List<String>> result = KeywordExtractor.extract("");
        assertThat(result.get("keywords")).isEmpty();
        assertThat(result.get("skills")).isEmpty();
    }
}
