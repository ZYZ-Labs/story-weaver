package com.storyweaver.storyunit.migration;

import java.util.List;

public record LegacyProjectBackfillDryRun(
        Long projectId,
        int totalChapters,
        int chaptersNeedingBackfill,
        int runnableChapters,
        int blockedChapters,
        List<LegacyProjectBackfillDryRunItem> chapters,
        List<String> requiredActions,
        List<String> riskNotes) {

    public LegacyProjectBackfillDryRun {
        totalChapters = Math.max(totalChapters, 0);
        chaptersNeedingBackfill = Math.max(chaptersNeedingBackfill, 0);
        runnableChapters = Math.max(runnableChapters, 0);
        blockedChapters = Math.max(blockedChapters, 0);
        chapters = chapters == null ? List.of() : List.copyOf(chapters);
        requiredActions = requiredActions == null ? List.of() : List.copyOf(requiredActions);
        riskNotes = riskNotes == null ? List.of() : List.copyOf(riskNotes);
    }
}
