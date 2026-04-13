package com.storyweaver.storyunit.summary.workflow;

public record SummaryWorkflowChatMessage(
        String role,
        String content) {

    public SummaryWorkflowChatMessage {
        role = normalize(role);
        content = normalize(content);
        if (role.isEmpty()) {
            role = "user";
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
