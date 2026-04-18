package com.storyweaver.story.generation.orchestration;

import com.storyweaver.storyunit.session.SceneExecutionStatus;

import java.util.List;
import java.util.Objects;

public record SceneSkeletonItem(
        String sceneId,
        Integer sceneIndex,
        SceneExecutionStatus status,
        String goal,
        List<String> readerReveal,
        List<String> mustUseAnchors,
        String stopCondition,
        Integer targetWords,
        String source) {

    public SceneSkeletonItem {
        sceneId = Objects.requireNonNull(sceneId, "sceneId must not be null").trim();
        sceneIndex = sceneIndex == null ? 0 : Math.max(sceneIndex, 0);
        status = Objects.requireNonNull(status, "status must not be null");
        goal = goal == null ? "" : goal.trim();
        readerReveal = readerReveal == null ? List.of() : List.copyOf(readerReveal);
        mustUseAnchors = mustUseAnchors == null ? List.of() : List.copyOf(mustUseAnchors);
        stopCondition = stopCondition == null ? "" : stopCondition.trim();
        targetWords = targetWords == null ? null : Math.max(targetWords, 0);
        source = source == null ? "" : source.trim();
    }
}
