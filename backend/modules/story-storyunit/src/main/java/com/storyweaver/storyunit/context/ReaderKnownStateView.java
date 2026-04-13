package com.storyweaver.storyunit.context;

import java.util.List;
import java.util.Objects;

public record ReaderKnownStateView(
        Long projectId,
        Long chapterId,
        List<String> knownFacts,
        List<String> unrevealedFacts) {

    public ReaderKnownStateView {
        knownFacts = sanitize(knownFacts);
        unrevealedFacts = sanitize(unrevealedFacts);
    }

    private static List<String> sanitize(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }
}
