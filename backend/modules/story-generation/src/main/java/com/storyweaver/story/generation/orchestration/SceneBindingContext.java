package com.storyweaver.story.generation.orchestration;

import com.storyweaver.storyunit.session.SceneExecutionState;

import java.util.Objects;

public record SceneBindingContext(
        String requestedSceneId,
        String resolvedSceneId,
        SceneBindingMode mode,
        boolean fallbackUsed,
        String summary,
        SceneExecutionState resolvedSceneState) {

    public SceneBindingContext {
        requestedSceneId = requestedSceneId == null ? "" : requestedSceneId.trim();
        resolvedSceneId = resolvedSceneId == null ? "" : resolvedSceneId.trim();
        mode = Objects.requireNonNull(mode, "mode must not be null");
        summary = summary == null ? "" : summary.trim();
    }
}
