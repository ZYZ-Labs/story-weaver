package com.storyweaver.storyunit.session;

import java.util.List;
import java.util.Objects;

public record SelectionDecision(
        String chosenCandidateId,
        String whyChosen,
        List<SelectionRejection> rejectedCandidates,
        List<String> risks) {

    public SelectionDecision {
        chosenCandidateId = Objects.requireNonNull(chosenCandidateId, "chosenCandidateId must not be null").trim();
        whyChosen = Objects.requireNonNull(whyChosen, "whyChosen must not be null").trim();
        rejectedCandidates = rejectedCandidates == null ? List.of() : List.copyOf(rejectedCandidates);
        risks = risks == null ? List.of() : List.copyOf(risks);
    }
}
