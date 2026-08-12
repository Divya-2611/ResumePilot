package com.resumepilot.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local (non-AI) extraction of structured signals from a job description:
 * skills, tools, education, experience, responsibilities and keywords.
 *
 * <p>Results are merged with AI extraction in the optimization flow when the
 * AI provider is available.</p>
 */
public final class KeywordExtractor {

    private static final Pattern EXPERIENCE_PATTERN =
            Pattern.compile("(\\d{1,2})\\s*\\+?\\s*(years?|yrs?)(?!\\s*of\\s*(age|old))",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern YEAR_RANGE_PATTERN =
            Pattern.compile("(\\d{4})\\s*[-–—]\\s*(\\d{4})");

    private KeywordExtractor() {
    }

    /** Extracts a structured analysis map from raw JD text. */
    public static Map<String, List<String>> extract(String content) {
        String normalized = TextNormalizer.normalize(content);
        String original = content == null ? "" : content;

        return Map.of(
                "skills", matchPhrases(normalized, SkillsDictionary.SKILLS),
                "tools", matchPhrases(normalized, SkillsDictionary.TOOLS),
                "education", matchPhrases(normalized, SkillsDictionary.EDUCATION),
                "experience", extractExperience(original),
                "keywords", extractKeywords(content),
                "responsibilities", extractResponsibilities(original));
    }

    /** Multi-word phrase matching against normalized text. */
    private static List<String> matchPhrases(String normalized, List<String> phrases) {
        List<String> found = new ArrayList<>();
        for (String phrase : phrases) {
            if (normalized.contains(phrase)) {
                found.add(phrase);
            }
        }
        return found;
    }

    private static List<String> extractExperience(String content) {
        List<String> result = new ArrayList<>();
        Matcher m = EXPERIENCE_PATTERN.matcher(content);
        while (m.find()) {
            result.add(m.group(1) + "+ years");
        }
        // Handle "2019 - 2023" ranges.
        Matcher range = YEAR_RANGE_PATTERN.matcher(content);
        if (range.find()) {
            int years = Integer.parseInt(range.group(2)) - Integer.parseInt(range.group(1));
            result.add(Math.max(0, years) + "+ years");
        }
        return result.stream().distinct().limit(3).toList();
    }

    /** Frequency-based keywords: meaningful tokens appearing 2+ times, by frequency. */
    private static List<String> extractKeywords(String content) {
        String normalized = TextNormalizer.normalize(content);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String token : normalized.split(" ")) {
            if (token.length() > 2 && !TextNormalizer.STOPWORDS.contains(token)) {
                counts.merge(token, 1L, Long::sum);
            }
        }
        return counts.entrySet().stream()
                .filter(e -> e.getValue() >= 2)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(15)
                .toList();
    }

    /** Sentences containing action verbs, capped at 10. */
    private static List<String> extractResponsibilities(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        List<String> sentences = new ArrayList<>();
        for (String line : content.split("[.\\n;]")) {
            String clean = line.trim().replaceAll("^[-•*\\d]+\\s*", "");
            if (clean.length() < 15 || clean.length() > 160) {
                continue;
            }
            String lower = clean.toLowerCase(Locale.ROOT);
            if (SkillsDictionary.ACTION_VERBS.stream().anyMatch(lower::contains)) {
                sentences.add(clean);
            }
            if (sentences.size() >= 10) {
                break;
            }
        }
        return sentences;
    }
}
