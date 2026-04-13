package com.storyweaver.storyunit.facet.summary;

import java.util.List;

public record DefaultSummaryFacet(
        String displayTitle,
        String oneLineSummary,
        String longSummary,
        String stateSummary,
        String relationSummary,
        String changeSummary,
        List<String> pendingQuestions) implements SummaryFacet {

    public DefaultSummaryFacet {
        displayTitle = normalize(displayTitle);
        oneLineSummary = normalize(oneLineSummary);
        longSummary = normalize(longSummary);
        stateSummary = normalize(stateSummary);
        relationSummary = normalize(relationSummary);
        changeSummary = normalize(changeSummary);
        pendingQuestions = pendingQuestions == null ? List.of() : List.copyOf(pendingQuestions);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
