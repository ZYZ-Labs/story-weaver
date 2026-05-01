package com.storyweaver.story.generation.orchestration;

import java.util.List;
import java.util.Objects;

public record ChapterNodeSkeleton(
        Long projectId,
        Long chapterId,
        String skeletonId,
        Integer nodeCount,
        String globalObjective,
        List<StoryNodeSkeletonItem> nodes,
        List<String> planningNotes) {

    public ChapterNodeSkeleton {
        skeletonId = Objects.requireNonNull(skeletonId, "skeletonId must not be null").trim();
        nodeCount = nodeCount == null ? 0 : Math.max(nodeCount, 0);
        globalObjective = globalObjective == null ? "" : globalObjective.trim();
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        planningNotes = planningNotes == null ? List.of() : List.copyOf(planningNotes);
    }
}
