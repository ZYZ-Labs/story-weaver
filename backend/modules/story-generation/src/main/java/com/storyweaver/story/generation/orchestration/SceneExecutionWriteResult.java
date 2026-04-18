package com.storyweaver.story.generation.orchestration;

import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneHandoffSnapshot;
import com.storyweaver.storyunit.event.StoryEvent;
import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;
import com.storyweaver.storyunit.patch.StoryPatch;
import com.storyweaver.storyunit.snapshot.StorySnapshot;

import java.util.Objects;

public record SceneExecutionWriteResult(
        SceneExecutionState sceneExecutionState,
        SceneHandoffSnapshot handoffSnapshot,
        StoryEvent stateEvent,
        StorySnapshot stateSnapshot,
        StoryPatch statePatch,
        ReaderRevealState readerRevealState,
        StorySnapshot chapterStateSnapshot) {

    public SceneExecutionWriteResult {
        sceneExecutionState = Objects.requireNonNull(sceneExecutionState, "sceneExecutionState must not be null");
        handoffSnapshot = Objects.requireNonNull(handoffSnapshot, "handoffSnapshot must not be null");
        stateEvent = Objects.requireNonNull(stateEvent, "stateEvent must not be null");
        stateSnapshot = Objects.requireNonNull(stateSnapshot, "stateSnapshot must not be null");
        statePatch = Objects.requireNonNull(statePatch, "statePatch must not be null");
        readerRevealState = Objects.requireNonNull(readerRevealState, "readerRevealState must not be null");
        chapterStateSnapshot = Objects.requireNonNull(chapterStateSnapshot, "chapterStateSnapshot must not be null");
    }
}
