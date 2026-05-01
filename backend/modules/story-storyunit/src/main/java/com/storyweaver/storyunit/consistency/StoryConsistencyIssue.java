package com.storyweaver.storyunit.consistency;

import java.util.Objects;

public record StoryConsistencyIssue(
        String issueKey,
        ConsistencySeverity severity,
        String message) {

    public StoryConsistencyIssue {
        issueKey = Objects.requireNonNull(issueKey, "issueKey must not be null").trim();
        severity = Objects.requireNonNull(severity, "severity must not be null");
        message = Objects.requireNonNull(message, "message must not be null").trim();
    }
}
