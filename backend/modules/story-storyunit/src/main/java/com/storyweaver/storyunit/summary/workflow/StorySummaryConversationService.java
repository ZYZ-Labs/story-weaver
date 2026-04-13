package com.storyweaver.storyunit.summary.workflow;

public interface StorySummaryConversationService {

    default SummaryWorkflowChatTurnResult reply(Long userId, SummaryWorkflowChatTurnRequest request) {
        return reply(request);
    }

    SummaryWorkflowChatTurnResult reply(SummaryWorkflowChatTurnRequest request);
}
