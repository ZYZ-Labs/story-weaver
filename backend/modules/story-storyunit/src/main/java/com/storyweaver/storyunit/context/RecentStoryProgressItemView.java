package com.storyweaver.storyunit.context;

import java.time.LocalDateTime;

public record RecentStoryProgressItemView(
        String itemType,
        Long refId,
        String title,
        String summary,
        String status,
        LocalDateTime createTime) {

    public RecentStoryProgressItemView {
        itemType = normalize(itemType);
        title = normalize(title);
        summary = normalize(summary);
        status = normalize(status);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
