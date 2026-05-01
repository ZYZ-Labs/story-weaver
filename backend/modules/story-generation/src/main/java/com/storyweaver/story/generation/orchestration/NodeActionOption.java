package com.storyweaver.story.generation.orchestration;

import java.util.Objects;

public record NodeActionOption(
        String optionId,
        String label,
        String intentSummary,
        String riskNote,
        String revealHint) {

    public NodeActionOption {
        optionId = Objects.requireNonNull(optionId, "optionId must not be null").trim();
        label = label == null ? "" : label.trim();
        intentSummary = intentSummary == null ? "" : intentSummary.trim();
        riskNote = riskNote == null ? "" : riskNote.trim();
        revealHint = revealHint == null ? "" : revealHint.trim();
    }
}
