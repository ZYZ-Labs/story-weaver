package com.storyweaver.story.generation.orchestration;

import java.util.Objects;

public record NodeActionRequest(
        Long projectId,
        Long chapterId,
        String nodeId,
        String checkpointId,
        String selectedOptionId,
        String customAction) {

    public NodeActionRequest {
        projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        chapterId = Objects.requireNonNull(chapterId, "chapterId must not be null");
        nodeId = nodeId == null ? "" : nodeId.trim();
        checkpointId = checkpointId == null ? "" : checkpointId.trim();
        selectedOptionId = selectedOptionId == null ? "" : selectedOptionId.trim();
        customAction = customAction == null ? "" : customAction.trim();
    }
}
