package com.storyweaver.storyunit.snapshot;

import com.storyweaver.storyunit.model.StorySourceTrace;
import com.storyweaver.storyunit.model.StoryUnitRef;

import java.util.List;
import java.util.Objects;

public record StorySnapshot(
        String snapshotId,
        SnapshotScope scope,
        Long projectId,
        Long chapterId,
        String sceneId,
        List<StoryUnitRef> unitRefs,
        String summary,
        StorySourceTrace sourceTrace) {

    public StorySnapshot {
        snapshotId = Objects.requireNonNull(snapshotId, "snapshotId must not be null").trim();
        scope = Objects.requireNonNull(scope, "scope must not be null");
        sceneId = sceneId == null ? null : sceneId.trim();
        unitRefs = unitRefs == null ? List.of() : List.copyOf(unitRefs);
        summary = summary == null ? "" : summary.trim();
    }
}
