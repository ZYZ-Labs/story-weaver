package com.storyweaver.storyunit.service.impl;

import com.storyweaver.storyunit.facet.summary.DefaultSummaryFacet;
import com.storyweaver.storyunit.summary.StorySummaryDraft;
import com.storyweaver.storyunit.summary.StorySummaryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class RuleBasedStorySummaryService implements StorySummaryService {

    @Override
    public DefaultSummaryFacet summarize(StorySummaryDraft draft) {
        Objects.requireNonNull(draft, "draft must not be null");

        String oneLineSummary = firstNonBlank(draft.oneLineCandidates(), draft.displayTitle());
        String longSummary = firstNonBlank(draft.longSummaryCandidates(), oneLineSummary, draft.displayTitle());
        String displayTitle = firstNonBlank(List.of(draft.displayTitle(), oneLineSummary, longSummary));

        return new DefaultSummaryFacet(
                displayTitle,
                oneLineSummary,
                longSummary,
                joinFacts(draft.stateFacts()),
                joinFacts(draft.relationFacts()),
                joinFacts(draft.changeFacts()),
                draft.pendingQuestions()
        );
    }

    private static String joinFacts(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isEmpty() && !normalized.contains(trimmed)) {
                normalized.add(trimmed);
            }
        }
        return String.join("；", normalized);
    }

    private static String firstNonBlank(List<String> candidates, String... fallbacks) {
        if (candidates != null) {
            for (String candidate : candidates) {
                if (hasText(candidate)) {
                    return candidate.trim();
                }
            }
        }
        if (fallbacks != null) {
            for (String fallback : fallbacks) {
                if (hasText(fallback)) {
                    return fallback.trim();
                }
            }
        }
        return "";
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
