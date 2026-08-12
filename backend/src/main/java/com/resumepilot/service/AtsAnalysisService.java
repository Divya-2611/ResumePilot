package com.resumepilot.service;

import com.resumepilot.dto.response.AtsAnalysisResponse;
import com.resumepilot.util.SkillsDictionary;
import com.resumepilot.util.TextNormalizer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Local, deterministic ATS compatibility engine.
 *
 * <p>Computes a transparent score even when no AI provider is configured:
 * <ul>
 *   <li>60% - keyword match against the job description</li>
 *   <li>20% - readability (sentence length / long words)</li>
 *   <li>10% - action-verb usage in bullet points</li>
 *   <li>10% - presence of standard sections (contact, summary, experience, skills)</li>
 * </ul></p>
 */
@Service
public class AtsAnalysisService {

    private static final Pattern BULLET_PATTERN = Pattern.compile("(?m)^\\s*(?:[-*•]|\\d+[.)])\\s+");
    private static final Pattern LONG_WORD = Pattern.compile("\\b\\w{9,}\\b");

    private static final Set<String> REQUIRED_SECTIONS = Set.of(
            "summary", "objective", "experience", "education", "skills");

    private static final List<String> FORMATTING_HINTS = List.of(
            "Use standard section headers (SUMMARY, EXPERIENCE, EDUCATION, SKILLS) - ATS parsers rely on them",
            "Keep the resume to one or two pages",
            "Avoid tables, images, charts and text boxes",
            "Use a clean single-column layout",
            "Include your email and phone number in the header");

    /**
     * Computes the local analysis. {@code resumeText} and {@code jdText} are the
     * plain texts; {@code jdKeywords} come from the stored JD extraction.
     */
    public AtsAnalysisResponse analyze(String resumeText, String jdText, List<String> jdKeywords) {
        String normalizedResume = TextNormalizer.normalize(resumeText);
        String normalizedJd = TextNormalizer.normalize(jdText);

        List<String> jdTokens = jdKeywords != null && !jdKeywords.isEmpty()
                ? jdKeywords.stream().map(String::toLowerCase).toList()
                : TextNormalizer.meaningfulTokens(jdText);

        // --- Keyword matching -------------------------------------------------
        List<String> missing = new ArrayList<>();
        List<String> matched = new ArrayList<>();
        for (String token : jdTokens) {
            if (normalizedResume.contains(token)) {
                matched.add(token);
            } else {
                missing.add(token);
            }
        }

        // Also match multi-word skills from the dictionary.
        for (String skill : SkillsDictionary.SKILLS) {
            if (!normalizedJd.contains(skill)) {
                continue;
            }
            if (normalizedResume.contains(skill)) {
                matched.add(skill);
            } else {
                missing.add(skill);
            }
        }

        List<String> matchedUnique = new ArrayList<>(new LinkedHashSet<>(matched));
        List<String> missingUnique = new ArrayList<>(new LinkedHashSet<>(missing));

        int keywordMatchPercent = (int) Math.round(
                100.0 * matchedUnique.size() / Math.max(1, matchedUnique.size() + missingUnique.size()));

        // --- Readability ------------------------------------------------------
        int readability = readabilityScore(resumeText);

        // --- Action verbs -----------------------------------------------------
        double actionVerbRatio = actionVerbRatio(resumeText);

        // --- Sections ---------------------------------------------------------
        double sectionRatio = sectionRatio(resumeText);

        // --- Composite score --------------------------------------------------
        int atsScore = (int) Math.round(
                keywordMatchPercent * 0.60
                        + readability * 0.20
                        + actionVerbRatio * 100 * 0.10
                        + sectionRatio * 100 * 0.10);
        atsScore = Math.max(0, Math.min(100, atsScore));

        Map<String, String> breakdown = Map.of(
                "keywordMatch", keywordMatchPercent + "% (60% weight)",
                "readability", readability + "/100 (20% weight)",
                "actionVerbs", Math.round(actionVerbRatio * 100) + "% (10% weight)",
                "sections", Math.round(sectionRatio * 100) + "% (10% weight)");

        return new AtsAnalysisResponse(
                atsScore,
                keywordMatchPercent,
                matchedUnique.stream().limit(40).toList(),
                missingUnique.stream().limit(40).toList(),
                weakBullets(resumeText),
                grammarSuggestions(resumeText),
                formattingSuggestions(resumeText),
                readability,
                breakdown);
    }

    // ------------------------------------------------------------------
    // Heuristics
    // ------------------------------------------------------------------

    private int readabilityScore(String text) {
        List<String> sentences = java.util.Arrays.stream(
                        (text == null ? "" : text).split("(?<=[.!?])\\s+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (sentences.isEmpty()) {
            return 50;
        }
        double avgWords = sentences.stream()
                .mapToInt(s -> s.split("\\s+").length)
                .average().orElse(10);

        long longWords = sentences.stream()
                .flatMap(s -> java.util.Arrays.stream(s.split("\\s+")))
                .filter(w -> LONG_WORD.matcher(w).find())
                .count();
        long totalWords = Math.max(1, sentences.stream()
                .mapToInt(s -> s.split("\\s+").length)
                .sum());

        double longWordRatio = (double) longWords / totalWords;

        // Ideal: 10-20 words per sentence, <15% long words.
        double score = 100.0;
        if (avgWords > 20) score -= (avgWords - 20) * 3;
        if (avgWords < 8) score -= (8 - avgWords) * 2;
        if (longWordRatio > 0.15) score -= (longWordRatio - 0.15) * 120;
        return Math.max(20, Math.min(100, (int) Math.round(score)));
    }

    private double actionVerbRatio(String text) {
        List<String> bullets = bulletList(text);
        if (bullets.isEmpty()) {
            return 0.3;
        }
        long strong = bullets.stream()
                .filter(b -> SkillsDictionary.ACTION_VERBS.stream()
                        .anyMatch(verb -> b.toLowerCase().contains(verb)))
                .count();
        return (double) strong / bullets.size();
    }

    private double sectionRatio(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }
        String lower = text.toLowerCase();
        long present = REQUIRED_SECTIONS.stream()
                .filter(section -> Pattern.compile("(?m)^\\s*" + Pattern.quote(section) + "\\s*[:\\n]")
                        .matcher(lower)
                        .find() || lower.contains("\n" + section + "\n"))
                .count();
        return (double) present / REQUIRED_SECTIONS.size();
    }

    private List<String> weakBullets(String text) {
        List<String> weak = new ArrayList<>();
        for (String bullet : bulletList(text)) {
            String b = bullet.toLowerCase();
            boolean hasVerb = SkillsDictionary.ACTION_VERBS.stream().anyMatch(b::contains);
            boolean hasNumbers = Pattern.compile("\\d").matcher(b).find();
            if (!hasVerb && !b.startsWith("responsibilities") && !b.isBlank()) {
                weak.add(bullet + " - starts without a strong action verb");
            } else if (!hasNumbers && bullet.length() < 40) {
                weak.add(bullet + " - consider adding measurable impact");
            }
            if (weak.size() >= 8) {
                break;
            }
        }
        return weak;
    }

    private List<String> grammarSuggestions(String text) {
        List<String> suggestions = new ArrayList<>();
        if (text == null) {
            return suggestions;
        }
        if (Pattern.compile("\\s{2,}").matcher(text).find()) {
            suggestions.add("Multiple consecutive spaces detected - replace with a single space");
        }
        if (Pattern.compile("\\b(\\w+)\\s+\\1\\b", Pattern.CASE_INSENSITIVE).matcher(text).find()) {
            suggestions.add("Repeated words detected - remove duplicates");
        }
        if (Pattern.compile("(?m)^\\s*(i|me|my)\\b").matcher(text).find()) {
            suggestions.add("Avoid first-person pronouns ('I', 'my') in resume bullet points");
        }
        if (text.length() > 0 && !text.trim().endsWith(".") && !text.trim().endsWith("\n")) {
            suggestions.add("End bullet points with consistent punctuation (periods or none)");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("No obvious grammar issues detected by local checks");
        }
        return suggestions;
    }

    private List<String> formattingSuggestions(String text) {
        List<String> hints = new ArrayList<>(FORMATTING_HINTS);
        if (text != null && (text.contains("http://") || text.contains("www."))) {
            hints.add("Add your LinkedIn/GitHub/portfolio links to the header");
        }
        if (text == null || !text.toLowerCase().contains("@")) {
            hints.add("Make sure an email address appears in the resume header");
        }
        return hints;
    }

    private List<String> bulletList(String text) {
        if (text == null) {
            return List.of();
        }
        var matcher = BULLET_PATTERN.matcher(text);
        List<String> bullets = new ArrayList<>();
        while (matcher.find()) {
            int start = matcher.end();
            int end = text.indexOf('\n', start);
            String bullet = text.substring(start, end < 0 ? text.length() : end).trim();
            if (!bullet.isBlank()) {
                bullets.add(bullet);
            }
        }
        return bullets;
    }
}
