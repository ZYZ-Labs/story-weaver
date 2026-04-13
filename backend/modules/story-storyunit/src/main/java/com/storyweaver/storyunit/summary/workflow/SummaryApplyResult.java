package com.storyweaver.storyunit.summary.workflow;

import com.storyweaver.storyunit.facet.summary.SummaryFacet;
import com.storyweaver.storyunit.model.StoryUnitRef;

import java.util.List;
import java.util.Objects;

public record SummaryApplyResult(
        boolean applied,
        SummaryFacet updatedSummary,
        StoryUnitRef updatedUnitRef,
        List<String> warnings) {

    public SummaryApplyResult {
        updatedUnitRef = Objects.requireNonNull(updatedUnitRef, "updatedUnitRef must not be null");
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
