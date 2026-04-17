package com.storyweaver.story.generation.orchestration;

import java.util.List;
import java.util.Objects;

public record SessionExecutionTrace(
        Long projectId,
        Long chapterId,
        String sceneId,
        List<SessionExecutionTraceItem> items) {

    public SessionExecutionTrace {
        sceneId = sceneId == null ? "" : sceneId.trim();
        items = items == null ? List.of() : List.copyOf(items);
    }

    public SessionExecutionTrace append(SessionExecutionTraceItem item) {
        Objects.requireNonNull(item, "item must not be null");
        List<SessionExecutionTraceItem> nextItems = new java.util.ArrayList<>(items);
        nextItems.add(item);
        return new SessionExecutionTrace(projectId, chapterId, sceneId, nextItems);
    }
}
