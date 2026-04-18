package com.storyweaver.storyunit.session;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record SceneHandoffSnapshot(
        Long projectId,
        Long chapterId,
        String fromSceneId,
        String toSceneId,
        String handoffLine,
        String outcomeSummary,
        List<String> readerRevealDelta,
        List<String> openLoops,
        List<String> resolvedLoops,
        Map<String, Object> stateDelta,
        String reviewResult,
        String reviewSummary,
        LocalDateTime createdAt) {

    public SceneHandoffSnapshot {
        fromSceneId = Objects.requireNonNull(fromSceneId, "fromSceneId must not be null").trim();
        toSceneId = toSceneId == null ? "" : toSceneId.trim();
        handoffLine = handoffLine == null ? "" : handoffLine.trim();
        outcomeSummary = outcomeSummary == null ? "" : outcomeSummary.trim();
        readerRevealDelta = readerRevealDelta == null ? List.of() : List.copyOf(readerRevealDelta);
        openLoops = openLoops == null ? List.of() : List.copyOf(openLoops);
        resolvedLoops = resolvedLoops == null ? List.of() : List.copyOf(resolvedLoops);
        stateDelta = stateDelta == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(stateDelta));
        reviewResult = reviewResult == null ? "" : reviewResult.trim();
        reviewSummary = reviewSummary == null ? "" : reviewSummary.trim();
    }
}
