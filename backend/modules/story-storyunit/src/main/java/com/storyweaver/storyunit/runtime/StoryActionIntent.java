package com.storyweaver.storyunit.runtime;

import com.storyweaver.storyunit.model.StorySourceTrace;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record StoryActionIntent(
        String intentId,
        Long projectId,
        Long chapterId,
        String checkpointId,
        String nodeId,
        String actorId,
        String actorRole,
        String selectedOptionId,
        String rawAction,
        String normalizedAction,
        Map<String, Object> constraints,
        StorySourceTrace sourceTrace) {

    public StoryActionIntent {
        intentId = requireText(intentId, "intentId");
        checkpointId = trim(checkpointId);
        nodeId = trim(nodeId);
        actorId = trim(actorId);
        actorRole = trim(actorRole);
        selectedOptionId = trim(selectedOptionId);
        rawAction = trimToEmpty(rawAction);
        normalizedAction = trimToEmpty(normalizedAction);
        constraints = constraints == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(constraints));
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
}
