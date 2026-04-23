package com.storyweaver.story.generation.orchestration;

import java.util.List;
import java.util.Objects;

public record ChapterSkeleton(
        Long projectId,
        Long chapterId,
        String skeletonId,
        Integer sceneCount,
        String globalStopCondition,
        List<SceneSkeletonItem> scenes,
        List<String> deletedSceneIds,
        List<String> planningNotes) {

    public ChapterSkeleton {
        skeletonId = Objects.requireNonNull(skeletonId, "skeletonId must not be null").trim();
        sceneCount = sceneCount == null ? 0 : Math.max(sceneCount, 0);
        globalStopCondition = globalStopCondition == null ? "" : globalStopCondition.trim();
        scenes = scenes == null ? List.of() : List.copyOf(scenes);
        deletedSceneIds = deletedSceneIds == null ? List.of() : List.copyOf(deletedSceneIds);
        planningNotes = planningNotes == null ? List.of() : List.copyOf(planningNotes);
    }
}
