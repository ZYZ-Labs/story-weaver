package com.storyweaver.story.generation.orchestration;

import java.util.List;
import java.util.Objects;

public record StoryNodeSkeletonItem(
        String nodeId,
        Integer nodeIndex,
        String title,
        String situation,
        String playerGoal,
        List<NodeActionOption> recommendedActions,
        boolean customActionAllowed,
        String stopCondition,
        String checkpointHint,
        List<String> nextNodeHints) {

    public StoryNodeSkeletonItem {
        nodeId = Objects.requireNonNull(nodeId, "nodeId must not be null").trim();
        nodeIndex = nodeIndex == null ? 0 : Math.max(nodeIndex, 0);
        title = title == null ? "" : title.trim();
        situation = situation == null ? "" : situation.trim();
        playerGoal = playerGoal == null ? "" : playerGoal.trim();
        recommendedActions = recommendedActions == null ? List.of() : List.copyOf(recommendedActions);
        stopCondition = stopCondition == null ? "" : stopCondition.trim();
        checkpointHint = checkpointHint == null ? "" : checkpointHint.trim();
        nextNodeHints = nextNodeHints == null ? List.of() : List.copyOf(nextNodeHints);
    }
}
