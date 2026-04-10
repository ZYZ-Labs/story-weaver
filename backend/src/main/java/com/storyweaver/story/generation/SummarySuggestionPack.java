package com.storyweaver.story.generation;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class SummarySuggestionPack {

    private String scope;

    private String rawInputSummary;

    private Map<String, Object> structuredFields = new LinkedHashMap<>();

    private String canonSummaryText;

    private List<String> readerRevealGoals = new ArrayList<>();

    private List<String> missingQuestions = new ArrayList<>();

    private List<String> confidenceHints = new ArrayList<>();

    private List<StructuredCreationSuggestion> proposedCreates = new ArrayList<>();

    public static SummarySuggestionPack empty(String scope) {
        SummarySuggestionPack pack = new SummarySuggestionPack();
        pack.setScope(scope);
        return pack;
    }

    public void putStructuredField(String key, Object value) {
        if (key == null || value == null) {
            return;
        }
        structuredFields.put(key, value);
    }

    public void addMissingQuestion(String question) {
        if (question == null || question.isBlank()) {
            return;
        }
        missingQuestions.add(question.trim());
    }

    public void addConfidenceHint(String hint) {
        if (hint == null || hint.isBlank()) {
            return;
        }
        confidenceHints.add(hint.trim());
    }

    public void addReaderRevealGoal(String goal) {
        if (goal == null || goal.isBlank()) {
            return;
        }
        readerRevealGoals.add(goal.trim());
    }

    public void addProposedCreate(StructuredCreationSuggestion suggestion) {
        if (suggestion == null) {
            return;
        }
        proposedCreates.add(suggestion);
    }

    public boolean hasStructuredFields() {
        return structuredFields != null && !structuredFields.isEmpty();
    }
}
