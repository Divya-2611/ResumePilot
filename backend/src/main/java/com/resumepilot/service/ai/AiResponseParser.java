package com.resumepilot.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses provider responses into {@link AiOptimizationResult}.
 *
 * <p>Handles the common quirks: markdown code fences around JSON, missing keys,
 * and string-or-array fields.</p>
 */
@Component
public class AiResponseParser {

    private final ObjectMapper objectMapper;

    public AiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Extracts a JSON object from a provider completion.
     * Strips markdown fences and leading prose before the first '{'.
     */
    public JsonNode extractJsonObject(String raw) {
        String text = raw == null ? "" : raw.trim();
        int start = text.indexOf('{');
        if (start < 0) {
            throw new IllegalArgumentException("No JSON object found in AI response");
        }
        int end = text.lastIndexOf('}');
        if (end <= start) {
            throw new IllegalArgumentException("Malformed JSON in AI response");
        }
        text = text.substring(start, end + 1);
        try {
            return objectMapper.readTree(text);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON from AI provider: " + e.getMessage());
        }
    }

    public AiOptimizationResult toResult(JsonNode root) {
        return new AiOptimizationResult(
                root.path("atsScore").asInt(50),
                root.path("keywordMatchPercent").asInt(50),
                stringList(root, "matchedKeywords"),
                stringList(root, "missingKeywords"),
                stringList(root, "weakBulletPoints"),
                stringList(root, "grammarSuggestions"),
                stringList(root, "formattingSuggestions"),
                root.path("readabilityScore").asInt(70),
                objectMap(root.path("optimizedSections")),
                root.path("optimizedContent").asText(null));
    }

    private List<String> stringList(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return List.of();
        }
        if (value.isTextual()) {
            return List.of(value.asText());
        }
        if (value.isArray()) {
            List<String> result = new ArrayList<>();
            value.forEach(item -> result.add(item.asText()));
            return result;
        }
        return List.of();
    }

    private Map<String, Object> objectMap(JsonNode node) {
        if (node == null || node.isNull()) {
            return Map.of();
        }
        try {
            return objectMapper.convertValue(node, new TypeReference<>() {
            });
        } catch (IllegalArgumentException e) {
            return Map.of();
        }
    }
}
