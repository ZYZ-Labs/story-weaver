package com.storyweaver.story.generation.orchestration;

import java.util.Objects;

public record SceneExecutionRequest(
        Long projectId,
        Long chapterId,
        String sceneId) {

    public SceneExecutionRequest {
        projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        chapterId = Objects.requireNonNull(chapterId, "chapterId must not be null");
        sceneId = sceneId == null ? "" : sceneId.trim();
    }
}
