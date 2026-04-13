package com.storyweaver.storyunit.context;

import java.util.List;
import java.util.Objects;

public record RecentStoryProgressView(
        Long projectId,
        List<RecentStoryProgressItemView> items) {

    public RecentStoryProgressView {
        items = items == null ? List.of() : items.stream()
                .filter(Objects::nonNull)
                .toList();
    }
}
