package com.storyweaver.storyunit.session;

import java.util.List;
import java.util.Objects;

public record ReviewDecision(
        String sceneId,
        ReviewResult result,
        String summary,
        List<ReviewIssue> issues,
        boolean canAutoRepair,
        String repairHint) {

    public ReviewDecision {
        sceneId = Objects.requireNonNull(sceneId, "sceneId must not be null").trim();
        result = Objects.requireNonNull(result, "result must not be null");
        summary = Objects.requireNonNull(summary, "summary must not be null").trim();
        issues = issues == null ? List.of() : List.copyOf(issues);
        repairHint = repairHint == null ? "" : repairHint.trim();
    }
}
