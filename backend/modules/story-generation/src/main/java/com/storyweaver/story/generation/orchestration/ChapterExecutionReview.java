package com.storyweaver.story.generation.orchestration;

import com.storyweaver.storyunit.session.ReviewIssue;
import com.storyweaver.storyunit.session.ReviewResult;

import java.util.List;
import java.util.Objects;

public record ChapterExecutionReview(
        Long projectId,
        Long chapterId,
        ReviewResult result,
        String summary,
        List<ReviewIssue> issues,
        boolean chapterExecutionComplete,
        ChapterTraceSummary traceSummary) {

    public ChapterExecutionReview {
        result = Objects.requireNonNull(result, "result must not be null");
        summary = summary == null ? "" : summary.trim();
        issues = issues == null ? List.of() : List.copyOf(issues);
        traceSummary = Objects.requireNonNull(traceSummary, "traceSummary must not be null");
    }
}
