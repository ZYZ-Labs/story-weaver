package com.storyweaver.story.generation.orchestration;

import java.util.Objects;

public record WriterSessionResult(
        String sceneId,
        String candidateId,
        String draftText,
        String summary) {

    public WriterSessionResult {
        sceneId = Objects.requireNonNull(sceneId, "sceneId must not be null").trim();
        candidateId = Objects.requireNonNull(candidateId, "candidateId must not be null").trim();
        draftText = draftText == null ? "" : draftText.trim();
        summary = summary == null ? "" : summary.trim();
    }
}
