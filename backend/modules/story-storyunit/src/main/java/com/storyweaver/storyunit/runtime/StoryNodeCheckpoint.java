package com.storyweaver.storyunit.runtime;

import com.storyweaver.storyunit.model.StorySourceTrace;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record StoryNodeCheckpoint(
        String checkpointId,
        Long projectId,
        Long chapterId,
        String nodeId,
        String previousCheckpointId,
        Integer nodeIndex,
        String worldSummary,
        String readerSummary,
        List<String> activeOpenLoopIds,
        Map<String, String> actorPositions,
        Map<String, String> actorGoals,
        List<String> availableOptionIds,
        StorySourceTrace sourceTrace) {

    public StoryNodeCheckpoint {
        checkpointId = requireText(checkpointId, "checkpointId");
        nodeId = trim(nodeId);
        previousCheckpointId = trim(previousCheckpointId);
        worldSummary = trimToEmpty(worldSummary);
        readerSummary = trimToEmpty(readerSummary);
        activeOpenLoopIds = sanitizeList(activeOpenLoopIds);
        actorPositions = sanitizeStringMap(actorPositions);
        actorGoals = sanitizeStringMap(actorGoals);
        availableOptionIds = sanitizeList(availableOptionIds);
    }

    private static String requireText(String value, String field) {
        String normalized = trim(value);
        return Objects.requireNonNull(normalized, field + " must not be null");
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String trimToEmpty(String value) {
        String normalized = trim(value);
        return normalized == null ? "" : normalized;
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
}
