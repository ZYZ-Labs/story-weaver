package com.storyweaver.storyunit.context;

public record ProjectBriefView(
        Long projectId,
        String projectTitle,
        String logline,
        String summary) {

    public ProjectBriefView {
        projectTitle = normalize(projectTitle);
        logline = normalize(logline);
        summary = normalize(summary);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
