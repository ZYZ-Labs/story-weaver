package com.storyweaver.storyunit.summary.workflow;

import com.storyweaver.storyunit.model.StoryUnitRef;

import java.util.Objects;

public record SummaryApplyCommand(
        String proposalId,
        StoryUnitRef targetRef,
        boolean confirmed,
        Long operatorId) {

    public SummaryApplyCommand {
        proposalId = Objects.requireNonNull(proposalId, "proposalId must not be null").trim();
        targetRef = Objects.requireNonNull(targetRef, "targetRef must not be null");
    }
}
