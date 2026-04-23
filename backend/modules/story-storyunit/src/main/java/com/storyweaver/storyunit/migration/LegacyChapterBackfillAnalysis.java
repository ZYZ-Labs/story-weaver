package com.storyweaver.storyunit.migration;

import java.util.List;

public record LegacyChapterBackfillAnalysis(
        Long projectId,
        Long chapterId,
        String chapterTitle,
        boolean chapterSummaryPresent,
        boolean chapterContentPresent,
        int legacyRecordCount,
        int legacyGeneratedRecordCount,
        int legacyAcceptedRecordCount,
        int legacyFailedRecordCount,
        int derivedSceneCount,
        int completedSceneCount,
        int failedSceneCount,
        int runtimeOnlySceneCount,
        int eventCount,
        int snapshotCount,
        int patchCount,
        boolean hasReaderRevealState,
        boolean hasChapterState,
        boolean needsSceneBackfill,
        boolean needsStateBackfill,
        List<String> notes) {

    public LegacyChapterBackfillAnalysis {
        chapterTitle = chapterTitle == null ? "" : chapterTitle.trim();
        legacyRecordCount = Math.max(legacyRecordCount, 0);
        legacyGeneratedRecordCount = Math.max(legacyGeneratedRecordCount, 0);
        legacyAcceptedRecordCount = Math.max(legacyAcceptedRecordCount, 0);
        legacyFailedRecordCount = Math.max(legacyFailedRecordCount, 0);
        derivedSceneCount = Math.max(derivedSceneCount, 0);
        completedSceneCount = Math.max(completedSceneCount, 0);
        failedSceneCount = Math.max(failedSceneCount, 0);
        runtimeOnlySceneCount = Math.max(runtimeOnlySceneCount, 0);
        eventCount = Math.max(eventCount, 0);
        snapshotCount = Math.max(snapshotCount, 0);
        patchCount = Math.max(patchCount, 0);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }
}
