package com.storyweaver.storyunit.context;

import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;

import java.util.Objects;

public record StoryUnitSummaryView(
        StoryUnitRef unitRef,
        StoryUnitType unitType,
        String title,
        String summary) {

    public StoryUnitSummaryView {
        unitRef = Objects.requireNonNull(unitRef, "unitRef must not be null");
        unitType = Objects.requireNonNull(unitType, "unitType must not be null");
        title = normalize(title);
        summary = normalize(summary);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
