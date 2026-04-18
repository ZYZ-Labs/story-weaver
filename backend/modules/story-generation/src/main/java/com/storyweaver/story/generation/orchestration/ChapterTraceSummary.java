package com.storyweaver.story.generation.orchestration;

import java.util.List;
import java.util.Objects;

public record ChapterTraceSummary(
        Long projectId,
        Long chapterId,
        String skeletonId,
        int skeletonSceneCount,
        int executedSceneCount,
        int completedSceneCount,
        int reviewingSceneCount,
        int failedSceneCount,
        int pendingSceneCount,
        String latestSceneId,
        List<String> executedSceneIds,
        List<String> pendingSceneIds,
        List<String> missingHandoffToSceneIds) {

    public ChapterTraceSummary {
        skeletonId = skeletonId == null ? "" : skeletonId.trim();
        latestSceneId = latestSceneId == null ? "" : latestSceneId.trim();
        executedSceneIds = executedSceneIds == null ? List.of() : List.copyOf(executedSceneIds);
        pendingSceneIds = pendingSceneIds == null ? List.of() : List.copyOf(pendingSceneIds);
        missingHandoffToSceneIds = missingHandoffToSceneIds == null ? List.of() : List.copyOf(missingHandoffToSceneIds);
        Objects.requireNonNull(executedSceneIds, "executedSceneIds must not be null");
        Objects.requireNonNull(pendingSceneIds, "pendingSceneIds must not be null");
        Objects.requireNonNull(missingHandoffToSceneIds, "missingHandoffToSceneIds must not be null");
    }
}
