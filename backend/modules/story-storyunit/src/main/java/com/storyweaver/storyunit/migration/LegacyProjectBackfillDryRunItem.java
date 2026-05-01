package com.storyweaver.storyunit.migration;

import java.util.List;

public record LegacyProjectBackfillDryRunItem(
        Long chapterId,
        String chapterTitle,
        boolean canRunBackfill,
        boolean needsSceneBackfill,
        boolean needsStateBackfill,
        List<LegacyBackfillActionPlan> actions,
        List<String> riskNotes) {

    public LegacyProjectBackfillDryRunItem {
        chapterTitle = chapterTitle == null ? "" : chapterTitle.trim();
        actions = actions == null ? List.of() : List.copyOf(actions);
        riskNotes = riskNotes == null ? List.of() : List.copyOf(riskNotes);
    }
}
