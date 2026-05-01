package com.storyweaver.story.generation.orchestration;

import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;
import com.storyweaver.storyunit.facet.state.ChapterIncrementalState;
import com.storyweaver.storyunit.runtime.StoryActionIntent;
import com.storyweaver.storyunit.runtime.StoryNodeCheckpoint;
import com.storyweaver.storyunit.runtime.StoryOpenLoop;
import com.storyweaver.storyunit.runtime.StoryResolvedTurn;

import java.util.List;
import java.util.Objects;

public record NodeResolutionResult(
        Long projectId,
        Long chapterId,
        String nodeId,
        String checkpointId,
        StoryActionIntent actionIntent,
        StoryResolvedTurn resolvedTurn,
        StoryNodeCheckpoint nextCheckpoint,
        List<StoryOpenLoop> activeLoops,
        ReaderRevealState readerRevealState,
        ChapterIncrementalState chapterState) {

    public NodeResolutionResult {
        projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        chapterId = Objects.requireNonNull(chapterId, "chapterId must not be null");
        nodeId = nodeId == null ? "" : nodeId.trim();
        checkpointId = checkpointId == null ? "" : checkpointId.trim();
        actionIntent = Objects.requireNonNull(actionIntent, "actionIntent must not be null");
        resolvedTurn = Objects.requireNonNull(resolvedTurn, "resolvedTurn must not be null");
        nextCheckpoint = Objects.requireNonNull(nextCheckpoint, "nextCheckpoint must not be null");
        activeLoops = activeLoops == null ? List.of() : List.copyOf(activeLoops);
        readerRevealState = Objects.requireNonNull(readerRevealState, "readerRevealState must not be null");
        chapterState = Objects.requireNonNull(chapterState, "chapterState must not be null");
    }
}
