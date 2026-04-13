package com.storyweaver.storyunit.summary.workflow;

import com.storyweaver.storyunit.model.StoryUnitType;

import java.util.Objects;

public record SummaryInputDraft(
        StoryUnitType targetType,
        Long targetSourceId,
        Long projectId,
        String summaryText,
        SummaryInputIntent intent,
        SummaryOperatorMode operatorMode) {

    public SummaryInputDraft {
        targetType = Objects.requireNonNull(targetType, "targetType must not be null");
        projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        summaryText = Objects.requireNonNull(summaryText, "summaryText must not be null").trim();
        intent = Objects.requireNonNull(intent, "intent must not be null");
        operatorMode = Objects.requireNonNull(operatorMode, "operatorMode must not be null");
    }
}
