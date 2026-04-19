package com.storyweaver.story.generation.orchestration;

import java.util.List;

public record SceneSkeletonMutationCommand(
        String sceneId,
        String goal,
        List<String> readerReveal,
        List<String> mustUseAnchors,
        String stopCondition,
        Integer targetWords) {

    public SceneSkeletonMutationCommand {
        sceneId = normalize(sceneId);
        goal = normalize(goal);
        readerReveal = sanitize(readerReveal);
        mustUseAnchors = sanitize(mustUseAnchors);
        stopCondition = normalize(stopCondition);
        targetWords = targetWords == null ? null : Math.max(targetWords, 0);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> sanitize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}
