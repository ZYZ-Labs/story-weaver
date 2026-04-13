package com.storyweaver.storyunit.session;

import java.util.Objects;

public record ReviewIssue(
        String code,
        String message,
        ReviewSeverity severity,
        boolean autoRepairable) {

    public ReviewIssue {
        code = Objects.requireNonNull(code, "code must not be null").trim();
        message = Objects.requireNonNull(message, "message must not be null").trim();
        severity = Objects.requireNonNull(severity, "severity must not be null");
    }
}
