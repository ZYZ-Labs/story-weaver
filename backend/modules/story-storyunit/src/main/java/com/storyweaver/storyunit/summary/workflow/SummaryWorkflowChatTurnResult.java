package com.storyweaver.storyunit.summary.workflow;

import java.util.List;
import java.util.Objects;

public record SummaryWorkflowChatTurnResult(
        String assistantMessage,
        String draftSummary,
        List<String> pendingQuestions,
        boolean readyForPreview,
        Long selectedProviderId,
        String selectedModel) {

    public SummaryWorkflowChatTurnResult {
        assistantMessage = normalize(assistantMessage);
        draftSummary = normalize(draftSummary);
        pendingQuestions = pendingQuestions == null ? List.of() : pendingQuestions.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
        selectedModel = normalize(selectedModel);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
