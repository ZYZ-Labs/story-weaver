package com.storyweaver.storyunit.migration;

public record LegacyBackfillActionPlan(
        String actionKey,
        String title,
        String description,
        boolean required,
        boolean blocked,
        String blockReason) {

    public LegacyBackfillActionPlan {
        actionKey = actionKey == null ? "" : actionKey.trim();
        title = title == null ? "" : title.trim();
        description = description == null ? "" : description.trim();
        blockReason = blockReason == null ? "" : blockReason.trim();
    }
}
