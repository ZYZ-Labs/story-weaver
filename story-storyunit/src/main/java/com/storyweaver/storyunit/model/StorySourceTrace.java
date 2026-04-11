package com.storyweaver.storyunit.model;

public record StorySourceTrace(
        String createdBy,
        String lastUpdatedBy,
        String sourceType,
        String sourceRef) {
}
