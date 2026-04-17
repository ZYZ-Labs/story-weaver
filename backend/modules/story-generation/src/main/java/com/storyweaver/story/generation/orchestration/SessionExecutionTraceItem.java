package com.storyweaver.story.generation.orchestration;

import com.storyweaver.storyunit.session.SessionRole;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record SessionExecutionTraceItem(
        SessionRole role,
        String stepKey,
        SessionTraceStatus status,
        String summary,
        String outputType,
        Integer attempt,
        boolean retryable,
        Map<String, Object> details) {

    public SessionExecutionTraceItem {
        role = Objects.requireNonNull(role, "role must not be null");
        stepKey = Objects.requireNonNull(stepKey, "stepKey must not be null").trim();
        status = Objects.requireNonNull(status, "status must not be null");
        summary = summary == null ? "" : summary.trim();
        outputType = outputType == null ? "" : outputType.trim();
        attempt = attempt == null ? 1 : Math.max(attempt, 1);
        details = details == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(details));
    }
}
