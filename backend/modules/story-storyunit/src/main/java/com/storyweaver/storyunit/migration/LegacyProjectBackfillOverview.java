package com.storyweaver.storyunit.migration;

import java.util.List;

public record LegacyProjectBackfillOverview(
        Long projectId,
        int totalChapters,
        int analyzedChapters,
        int chaptersNeedingSceneBackfill,
        int chaptersNeedingStateBackfill,
        int chaptersReadyForBackfill,
        List<LegacyChapterBackfillStatusItem> chapters) {

    public LegacyProjectBackfillOverview {
        totalChapters = Math.max(totalChapters, 0);
        analyzedChapters = Math.max(analyzedChapters, 0);
        chaptersNeedingSceneBackfill = Math.max(chaptersNeedingSceneBackfill, 0);
        chaptersNeedingStateBackfill = Math.max(chaptersNeedingStateBackfill, 0);
        chaptersReadyForBackfill = Math.max(chaptersReadyForBackfill, 0);
        chapters = chapters == null ? List.of() : List.copyOf(chapters);
    }
}
