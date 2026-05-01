package com.storyweaver.story.generation.orchestration;

import com.storyweaver.storyunit.runtime.StoryNodeCheckpoint;
import com.storyweaver.storyunit.runtime.StoryOpenLoop;

import java.util.List;
import java.util.Objects;

public record ChapterNodeRuntimeView(
        Long projectId,
        Long chapterId,
        ChapterNodeSkeleton skeleton,
        String currentNodeId,
        String latestCheckpointId,
        List<String> completedNodeIds,
        List<StoryNodeCheckpoint> checkpoints,
        List<StoryOpenLoop> activeLoops) {

    public ChapterNodeRuntimeView {
        projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        chapterId = Objects.requireNonNull(chapterId, "chapterId must not be null");
        skeleton = Objects.requireNonNull(skeleton, "skeleton must not be null");
        currentNodeId = currentNodeId == null ? "" : currentNodeId.trim();
        latestCheckpointId = latestCheckpointId == null ? "" : latestCheckpointId.trim();
        completedNodeIds = completedNodeIds == null ? List.of() : List.copyOf(completedNodeIds);
        checkpoints = checkpoints == null ? List.of() : List.copyOf(checkpoints);
        activeLoops = activeLoops == null ? List.of() : List.copyOf(activeLoops);
    }
}
