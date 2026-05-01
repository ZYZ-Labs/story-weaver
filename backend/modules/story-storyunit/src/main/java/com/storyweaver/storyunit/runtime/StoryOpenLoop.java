package com.storyweaver.storyunit.runtime;

import com.storyweaver.storyunit.model.StorySourceTrace;

import java.util.List;
import java.util.Objects;

public record StoryOpenLoop(
        String loopId,
        Long projectId,
        Long chapterId,
        String sourceNodeId,
        String label,
        StoryLoopStatus status,
        String owner,
        String payoffHint,
        String sourceTurnId,
        String resolvedByTurnId,
        List<String> relatedUnitRefs,
        StorySourceTrace sourceTrace) {

    public StoryOpenLoop {
        loopId = requireText(loopId, "loopId");
        sourceNodeId = trim(sourceNodeId);
        label = trimToEmpty(label);
        status = status == null ? StoryLoopStatus.OPEN : status;
        owner = trim(owner);
        payoffHint = trimToEmpty(payoffHint);
        sourceTurnId = trim(sourceTurnId);
        resolvedByTurnId = trim(resolvedByTurnId);
        relatedUnitRefs = relatedUnitRefs == null ? List.of() : relatedUnitRefs.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
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
