package com.storyweaver.storyunit.session;

import java.util.List;
import java.util.Objects;

public record DirectorCandidate(
        String candidateId,
        DirectorCandidateType candidateType,
        String goal,
        List<String> readerReveal,
        List<String> mustUseAnchors,
        List<String> forbiddenMoves,
        String stopCondition,
        Integer targetWords,
        String reason) {

    public DirectorCandidate {
        candidateId = Objects.requireNonNull(candidateId, "candidateId must not be null").trim();
        candidateType = Objects.requireNonNull(candidateType, "candidateType must not be null");
        goal = Objects.requireNonNull(goal, "goal must not be null").trim();
        readerReveal = readerReveal == null ? List.of() : List.copyOf(readerReveal);
        mustUseAnchors = mustUseAnchors == null ? List.of() : List.copyOf(mustUseAnchors);
        forbiddenMoves = forbiddenMoves == null ? List.of() : List.copyOf(forbiddenMoves);
        stopCondition = Objects.requireNonNull(stopCondition, "stopCondition must not be null").trim();
        targetWords = targetWords == null ? null : Math.max(targetWords, 0);
        reason = reason == null ? "" : reason.trim();
    }
}
