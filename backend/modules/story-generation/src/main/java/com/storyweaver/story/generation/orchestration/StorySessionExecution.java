package com.storyweaver.story.generation.orchestration;

import java.util.Objects;

public record StorySessionExecution(
        StorySessionPreview preview,
        SceneExecutionWriteResult writeResult,
        SessionExecutionTrace trace) {

    public StorySessionExecution {
        preview = Objects.requireNonNull(preview, "preview must not be null");
        writeResult = Objects.requireNonNull(writeResult, "writeResult must not be null");
        trace = Objects.requireNonNull(trace, "trace must not be null");
    }
}
