package com.storyweaver.story.generation.orchestration;

import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneHandoffSnapshot;

import java.util.Objects;

public record SceneExecutionWriteResult(
        SceneExecutionState sceneExecutionState,
        SceneHandoffSnapshot handoffSnapshot) {

    public SceneExecutionWriteResult {
        sceneExecutionState = Objects.requireNonNull(sceneExecutionState, "sceneExecutionState must not be null");
        handoffSnapshot = Objects.requireNonNull(handoffSnapshot, "handoffSnapshot must not be null");
    }
}
