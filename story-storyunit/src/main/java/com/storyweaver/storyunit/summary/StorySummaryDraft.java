package com.storyweaver.storyunit.summary;

import com.storyweaver.storyunit.model.StoryUnitType;

import java.util.List;
import java.util.Objects;

public record StorySummaryDraft(
        StoryUnitType unitType,
        String displayTitle,
        List<String> oneLineCandidates,
        List<String> longSummaryCandidates,
        List<String> stateFacts,
        List<String> relationFacts,
        List<String> changeFacts,
        List<String> pendingQuestions) {

    public StorySummaryDraft {
        unitType = Objects.requireNonNull(unitType, "unitType must not be null");
        displayTitle = normalize(displayTitle);
        oneLineCandidates = normalizeList(oneLineCandidates);
        longSummaryCandidates = normalizeList(longSummaryCandidates);
        stateFacts = normalizeList(stateFacts);
        relationFacts = normalizeList(relationFacts);
        changeFacts = normalizeList(changeFacts);
        pendingQuestions = normalizeList(pendingQuestions);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(StorySummaryDraft::normalize)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }
}
