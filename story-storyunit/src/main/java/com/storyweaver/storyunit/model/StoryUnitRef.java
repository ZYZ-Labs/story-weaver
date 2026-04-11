package com.storyweaver.storyunit.model;

import java.util.Objects;

public record StoryUnitRef(
        String unitId,
        String unitKey,
        StoryUnitType unitType) {

    public StoryUnitRef {
        unitId = Objects.requireNonNull(unitId, "unitId must not be null").trim();
        unitKey = Objects.requireNonNull(unitKey, "unitKey must not be null").trim();
        unitType = Objects.requireNonNull(unitType, "unitType must not be null");
    }
}
