package com.storyweaver.storyunit.summary.workflow;

import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.patch.StoryPatch;

import java.util.List;
import java.util.Objects;

public record StructuredPatchProposal(
        String proposalId,
        StoryUnitRef targetRef,
        StoryPatch patch,
        String inputSummary,
        String proposalSummary,
        List<FacetType> affectedFacets,
        List<String> riskNotes,
        List<String> pendingQuestions,
        boolean requiresConfirmation) {

    public StructuredPatchProposal {
        proposalId = Objects.requireNonNull(proposalId, "proposalId must not be null").trim();
        targetRef = Objects.requireNonNull(targetRef, "targetRef must not be null");
        patch = Objects.requireNonNull(patch, "patch must not be null");
        inputSummary = normalize(inputSummary);
        proposalSummary = normalize(proposalSummary);
        affectedFacets = affectedFacets == null ? List.of() : List.copyOf(affectedFacets);
        riskNotes = normalizeList(riskNotes);
        pendingQuestions = normalizeList(pendingQuestions);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }
}
