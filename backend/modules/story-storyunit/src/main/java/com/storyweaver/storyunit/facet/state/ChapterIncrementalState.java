package com.storyweaver.storyunit.facet.state;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ChapterIncrementalState(
        Long projectId,
        Long chapterId,
        List<String> openLoops,
        List<String> resolvedLoops,
        List<String> activeLocations,
        Map<String, String> characterEmotions,
        Map<String, String> characterAttitudes,
        Map<String, List<String>> characterStateTags,
        String summary) implements StateFacet {

    public ChapterIncrementalState {
        openLoops = sanitizeList(openLoops);
        resolvedLoops = sanitizeList(resolvedLoops);
        activeLocations = sanitizeList(activeLocations);
        characterEmotions = sanitizeStringMap(characterEmotions);
        characterAttitudes = sanitizeStringMap(characterAttitudes);
        characterStateTags = sanitizeTagMap(characterStateTags);
        summary = summary == null ? "" : summary.trim();
    }

    @Override
    public Map<String, Object> stateFields() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("openLoops", openLoops);
        values.put("resolvedLoops", resolvedLoops);
        values.put("activeLocations", activeLocations);
        values.put("characterEmotions", characterEmotions);
        values.put("characterAttitudes", characterAttitudes);
        values.put("characterStateTags", characterStateTags);
        return Map.copyOf(values);
    }

    @Override
    public List<String> activeFlags() {
        return openLoops;
    }

    private static List<String> sanitizeList(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    private static Map<String, String> sanitizeStringMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, String> sanitized = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }
            String normalizedKey = key.trim();
            String normalizedValue = value.trim();
            if (!normalizedKey.isEmpty() && !normalizedValue.isEmpty()) {
                sanitized.put(normalizedKey, normalizedValue);
            }
        });
        return Map.copyOf(sanitized);
    }

    private static Map<String, List<String>> sanitizeTagMap(Map<String, List<String>> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> sanitized = new LinkedHashMap<>();
        values.forEach((key, tags) -> {
            if (key == null) {
                return;
            }
            String normalizedKey = key.trim();
            List<String> normalizedTags = sanitizeList(tags);
            if (!normalizedKey.isEmpty() && !normalizedTags.isEmpty()) {
                sanitized.put(normalizedKey, normalizedTags);
            }
        });
        return Map.copyOf(sanitized);
    }
}
