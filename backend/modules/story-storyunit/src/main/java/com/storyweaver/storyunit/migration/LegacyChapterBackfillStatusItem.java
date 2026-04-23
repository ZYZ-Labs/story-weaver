package com.storyweaver.storyunit.migration;

import java.util.List;

public record LegacyChapterBackfillStatusItem(
        Long chapterId,
        String chapterTitle,
        int legacyGeneratedRecordCount,
        boolean needsSceneBackfill,
        boolean needsStateBackfill,
        boolean canRunBackfill,
        List<String> riskNotes) {

    public LegacyChapterBackfillStatusItem {
        chapterTitle = chapterTitle == null ? "" : chapterTitle.trim();
        legacyGeneratedRecordCount = Math.max(legacyGeneratedRecordCount, 0);
        riskNotes = riskNotes == null ? List.of() : List.copyOf(riskNotes);
    }
}
