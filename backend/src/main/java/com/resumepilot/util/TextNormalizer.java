package com.resumepilot.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/**
 * Text normalization + stopwords shared by keyword extraction and ATS analysis.
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /** English stopwords that carry no keyword signal. */
    public static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "if", "of", "for", "to", "in", "on",
            "at", "by", "with", "from", "as", "is", "are", "was", "were", "be", "been",
            "being", "have", "has", "had", "do", "does", "did", "will", "would", "shall",
            "should", "can", "could", "may", "might", "must", "not", "no", "so", "than",
            "that", "this", "these", "those", "it", "its", "we", "you", "your", "they",
            "their", "he", "she", "his", "her", "our", "i", "me", "my", "all",
            "any", "both", "each", "few", "more", "most", "other", "some", "such", "only",
            "own", "same", "very", "just", "about", "into", "over", "after", "before",
            "under", "between", "out", "up", "down", "off", "during", "within", "without",
            "through", "among", "along", "which", "who", "whom", "whose", "when", "where",
            "why", "how", "what", "per", "via", "due", "etc", "e.g", "i.e", "also", "too");

    /** Lowercases, strips accents and collapses whitespace. */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String stripped = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return stripped.toLowerCase(Locale.ROOT)
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("[^a-z0-9+#\\- ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Splits normalized text into individual tokens (stopwords removed). */
    public static java.util.List<String> meaningfulTokens(String text) {
        return java.util.Arrays.stream(normalize(text).split(" "))
                .filter(t -> t.length() > 1)
                .filter(t -> !STOPWORDS.contains(t))
                .distinct()
                .toList();
    }
}
