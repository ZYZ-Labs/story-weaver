package com.storyweaver.storyunit.model;

public record StoryUnitVersion(long value) {

    public StoryUnitVersion {
        if (value < 0) {
            throw new IllegalArgumentException("value must be >= 0");
        }
    }
}
