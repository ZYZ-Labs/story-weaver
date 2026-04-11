package com.storyweaver.storyunit.session;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record SceneExecutionState(
        Long projectId,
        Long chapterId,
        String sceneId,
        Integer sceneIndex,
        SceneExecutionStatus status,
        String chosenCandidateId,
        String goal,
        String stopCondition,
        List<String> readerRevealDelta,
        List<String> openLoops,
        List<String> resolvedLoops,
        Map<String, Object> stateDelta,
        String handoffLine,
        String outcomeSummary) {

    public SceneExecutionState {
        sceneId = Objects.requireNonNull(sceneId, "sceneId must not be null").trim();
        sceneIndex = sceneIndex == null ? 0 : Math.max(sceneIndex, 0);
        status = Objects.requireNonNull(status, "status must not be null");
        chosenCandidateId = chosenCandidateId == null ? "" : chosenCandidateId.trim();
        goal = goal == null ? "" : goal.trim();
        stopCondition = stopCondition == null ? "" : stopCondition.trim();
        readerRevealDelta = readerRevealDelta == null ? List.of() : List.copyOf(readerRevealDelta);
        openLoops = openLoops == null ? List.of() : List.copyOf(openLoops);
        resolvedLoops = resolvedLoops == null ? List.of() : List.copyOf(resolvedLoops);
        stateDelta = stateDelta == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(stateDelta));
        handoffLine = handoffLine == null ? "" : handoffLine.trim();
        outcomeSummary = outcomeSummary == null ? "" : outcomeSummary.trim();
    }
}
