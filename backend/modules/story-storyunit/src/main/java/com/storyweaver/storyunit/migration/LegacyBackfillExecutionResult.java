package com.storyweaver.storyunit.migration;

import java.util.List;

public record LegacyBackfillExecutionResult(
        LegacyBackfillDryRun dryRun,
        boolean executed,
        int createdEventCount,
        int createdSnapshotCount,
        int createdPatchCount,
        boolean createdReaderRevealState,
        boolean createdChapterState,
        List<String> writtenKeys,
        List<String> skippedKeys,
        List<String> warnings) {

    public LegacyBackfillExecutionResult {
        createdEventCount = Math.max(createdEventCount, 0);
        createdSnapshotCount = Math.max(createdSnapshotCount, 0);
        createdPatchCount = Math.max(createdPatchCount, 0);
        writtenKeys = writtenKeys == null ? List.of() : List.copyOf(writtenKeys);
        skippedKeys = skippedKeys == null ? List.of() : List.copyOf(skippedKeys);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
