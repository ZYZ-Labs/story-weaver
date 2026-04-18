package com.storyweaver.story.generation.orchestration;

import com.storyweaver.storyunit.context.ChapterAnchorBundleView;
import com.storyweaver.storyunit.context.CharacterRuntimeStateView;
import com.storyweaver.storyunit.context.ProjectBriefView;
import com.storyweaver.storyunit.context.ReaderKnownStateView;
import com.storyweaver.storyunit.context.RecentStoryProgressView;
import com.storyweaver.storyunit.context.StoryUnitSummaryView;
import com.storyweaver.storyunit.session.SceneHandoffSnapshot;
import com.storyweaver.storyunit.session.SceneExecutionState;

import java.util.List;
import java.util.Objects;

public record StorySessionContextPacket(
        Long projectId,
        Long chapterId,
        String sceneId,
        SceneBindingContext sceneBindingContext,
        ProjectBriefView projectBrief,
        StoryUnitSummaryView chapterSummary,
        ChapterAnchorBundleView chapterAnchorBundle,
        ReaderKnownStateView readerKnownState,
        RecentStoryProgressView recentStoryProgress,
        List<CharacterRuntimeStateView> characterRuntimeStates,
        SceneHandoffSnapshot previousSceneHandoff,
        List<SceneExecutionState> existingSceneStates) {

    public StorySessionContextPacket {
        sceneBindingContext = Objects.requireNonNull(sceneBindingContext, "sceneBindingContext must not be null");
        projectBrief = Objects.requireNonNull(projectBrief, "projectBrief must not be null");
        chapterSummary = Objects.requireNonNull(chapterSummary, "chapterSummary must not be null");
        chapterAnchorBundle = Objects.requireNonNull(chapterAnchorBundle, "chapterAnchorBundle must not be null");
        readerKnownState = Objects.requireNonNull(readerKnownState, "readerKnownState must not be null");
        recentStoryProgress = Objects.requireNonNull(recentStoryProgress, "recentStoryProgress must not be null");
        sceneId = sceneId == null ? "" : sceneId.trim();
        characterRuntimeStates = characterRuntimeStates == null ? List.of() : List.copyOf(characterRuntimeStates);
        existingSceneStates = existingSceneStates == null ? List.of() : List.copyOf(existingSceneStates);
    }
}
