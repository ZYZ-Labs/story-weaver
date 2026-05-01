package com.storyweaver.storyunit.runtime;

import com.storyweaver.storyunit.model.StorySourceTrace;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record StoryResolvedTurn(
        String turnId,
        Long projectId,
        Long chapterId,
        String checkpointId,
        String nodeId,
        String actionIntentId,
        String resolutionSummary,
        List<String> eventIds,
        Map<String, Object> stateDelta,
        List<String> readerRevealDelta,
        List<String> openedLoopIds,
        List<String> resolvedLoopIds,
        String nextCheckpointId,
        StorySourceTrace sourceTrace) {

    public StoryResolvedTurn {
        turnId = requireText(turnId, "turnId");
        checkpointId = trim(checkpointId);
        nodeId = trim(nodeId);
        actionIntentId = trim(actionIntentId);
        resolutionSummary = trimToEmpty(resolutionSummary);
        eventIds = sanitizeList(eventIds);
        stateDelta = stateDelta == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(stateDelta));
        readerRevealDelta = sanitizeList(readerRevealDelta);
        openedLoopIds = sanitizeList(openedLoopIds);
        resolvedLoopIds = sanitizeList(resolvedLoopIds);
        nextCheckpointId = trim(nextCheckpointId);
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
}
