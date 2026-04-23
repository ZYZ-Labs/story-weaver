package com.storyweaver.storyunit.migration;

import java.util.List;

public record MigrationCompatibilitySnapshot(
        Long projectId,
        Long chapterId,
        String chapterTitle,
        List<CompatibilityBoundaryItem> pageBoundaries,
        List<CompatibilityBoundaryItem> apiBoundaries,
        List<CompatibilityBoundaryItem> dataBoundaries,
        List<String> featureFlags,
        List<String> riskNotes) {

    public MigrationCompatibilitySnapshot {
        chapterTitle = chapterTitle == null ? "" : chapterTitle.trim();
        pageBoundaries = pageBoundaries == null ? List.of() : List.copyOf(pageBoundaries);
        apiBoundaries = apiBoundaries == null ? List.of() : List.copyOf(apiBoundaries);
        dataBoundaries = dataBoundaries == null ? List.of() : List.copyOf(dataBoundaries);
        featureFlags = featureFlags == null ? List.of() : List.copyOf(featureFlags);
        riskNotes = riskNotes == null ? List.of() : List.copyOf(riskNotes);
    }
}
