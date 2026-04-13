package com.storyweaver.storyunit.session;

import java.util.Objects;

public record SelectionRejection(
        String candidateId,
        String reason) {

    public SelectionRejection {
        candidateId = Objects.requireNonNull(candidateId, "candidateId must not be null").trim();
        reason = reason == null ? "" : reason.trim();
    }
}
