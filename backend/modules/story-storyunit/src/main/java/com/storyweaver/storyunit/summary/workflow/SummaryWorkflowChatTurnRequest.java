package com.storyweaver.storyunit.summary.workflow;

import com.storyweaver.storyunit.model.StoryUnitType;

import java.util.List;
import java.util.Objects;

public record SummaryWorkflowChatTurnRequest(
        StoryUnitType targetType,
        Long targetSourceId,
        Long projectId,
        String title,
        String existingSummary,
        String currentDraftSummary,
        SummaryInputIntent intent,
        SummaryOperatorMode operatorMode,
        List<SummaryWorkflowChatMessage> messages,
        Long selectedProviderId,
        String selectedModel) {

    public SummaryWorkflowChatTurnRequest {
        targetType = Objects.requireNonNull(targetType, "targetType must not be null");
        projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        title = normalize(title);
        existingSummary = normalize(existingSummary);
        currentDraftSummary = normalize(currentDraftSummary);
        intent = intent == null ? SummaryInputIntent.REFINE : intent;
        operatorMode = operatorMode == null ? SummaryOperatorMode.DEFAULT : operatorMode;
        messages = messages == null ? List.of() : messages.stream()
                .filter(Objects::nonNull)
                .toList();
        selectedModel = normalize(selectedModel);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
