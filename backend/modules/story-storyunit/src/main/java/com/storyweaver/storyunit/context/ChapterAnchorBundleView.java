package com.storyweaver.storyunit.context;

import java.util.List;
import java.util.Objects;

public record ChapterAnchorBundleView(
        Long projectId,
        Long chapterId,
        String chapterTitle,
        Long outlineId,
        String outlineTitle,
        Long mainPovCharacterId,
        String mainPovCharacterName,
        List<String> activeCharacterNames,
        List<String> activePlotTitles,
        List<String> storyBeats,
        String chapterSummary) {

    public ChapterAnchorBundleView {
        chapterTitle = normalize(chapterTitle);
        outlineTitle = normalize(outlineTitle);
        mainPovCharacterName = normalize(mainPovCharacterName);
        activeCharacterNames = sanitize(activeCharacterNames);
        activePlotTitles = sanitize(activePlotTitles);
        storyBeats = sanitize(storyBeats);
        chapterSummary = normalize(chapterSummary);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
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
