package com.storyweaver.storyunit.patch;

import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StorySourceTrace;
import com.storyweaver.storyunit.model.StoryUnitRef;

import java.util.List;
import java.util.Objects;

public record StoryPatch(
        String patchId,
        StoryUnitRef targetUnit,
        FacetType facetType,
        List<PatchOperation> operations,
        String summary,
        PatchStatus status,
        StorySourceTrace sourceTrace) {

    public StoryPatch {
        patchId = Objects.requireNonNull(patchId, "patchId must not be null").trim();
        targetUnit = Objects.requireNonNull(targetUnit, "targetUnit must not be null");
        facetType = Objects.requireNonNull(facetType, "facetType must not be null");
        operations = operations == null ? List.of() : List.copyOf(operations);
        summary = summary == null ? "" : summary.trim();
        status = Objects.requireNonNull(status, "status must not be null");
    }
}
