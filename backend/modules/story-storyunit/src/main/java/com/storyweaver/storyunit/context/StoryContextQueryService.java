package com.storyweaver.storyunit.context;

import com.storyweaver.storyunit.model.StoryUnitRef;

import java.util.Optional;

public interface StoryContextQueryService {

    Optional<ProjectBriefView> getProjectBrief(Long projectId);

    Optional<StoryUnitSummaryView> getStoryUnitSummary(StoryUnitRef ref);

    Optional<ChapterAnchorBundleView> getChapterAnchorBundle(Long projectId, Long chapterId);

    Optional<ReaderKnownStateView> getReaderKnownState(Long projectId, Long chapterId);

    Optional<CharacterRuntimeStateView> getCharacterRuntimeState(Long projectId, Long characterId);

    default RecentStoryProgressView getRecentStoryProgress(Long projectId) {
        return getRecentStoryProgress(projectId, 10);
    }

    RecentStoryProgressView getRecentStoryProgress(Long projectId, int limit);
}
