package com.storyweaver.story.generation.orchestration;

import com.storyweaver.storyunit.session.DirectorCandidate;
import com.storyweaver.storyunit.session.SelectionDecision;
import com.storyweaver.storyunit.session.ReviewDecision;
import com.storyweaver.storyunit.session.WriterExecutionBrief;

import java.util.List;
import java.util.Objects;

public record StorySessionPreview(
        StorySessionContextPacket contextPacket,
        List<DirectorCandidate> candidates,
        SelectionDecision selectionDecision,
        WriterExecutionBrief writerExecutionBrief,
        WriterSessionResult writerSessionResult,
        ReviewDecision reviewDecision,
        SessionExecutionTrace trace) {

    public StorySessionPreview {
        contextPacket = Objects.requireNonNull(contextPacket, "contextPacket must not be null");
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
        selectionDecision = Objects.requireNonNull(selectionDecision, "selectionDecision must not be null");
        writerExecutionBrief = Objects.requireNonNull(writerExecutionBrief, "writerExecutionBrief must not be null");
        writerSessionResult = Objects.requireNonNull(writerSessionResult, "writerSessionResult must not be null");
        reviewDecision = Objects.requireNonNull(reviewDecision, "reviewDecision must not be null");
        trace = Objects.requireNonNull(trace, "trace must not be null");
    }
}
