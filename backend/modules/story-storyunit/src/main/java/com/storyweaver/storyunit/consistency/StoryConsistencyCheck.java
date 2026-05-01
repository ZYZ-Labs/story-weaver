package com.storyweaver.storyunit.consistency;

import java.util.List;
import java.util.Objects;

public record StoryConsistencyCheck(
        Long projectId,
        Long chapterId,
        int sceneCount,
        int completedSceneCount,
        int handoffCount,
        int eventCount,
        int snapshotCount,
        int patchCount,
        boolean hasReaderRevealState,
        boolean hasChapterState,
        boolean chapterReviewPresent,
        String chapterReviewResult,
        String chapterReviewSummary,
        List<StoryConsistencyIssue> issues) {

    public StoryConsistencyCheck {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(chapterId, "chapterId must not be null");
        chapterReviewResult = chapterReviewResult == null ? "" : chapterReviewResult.trim();
        chapterReviewSummary = chapterReviewSummary == null ? "" : chapterReviewSummary.trim();
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
