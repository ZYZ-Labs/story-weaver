package com.storyweaver.story.generation;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class StructuredCreationSuggestion {

    private String entityType;

    private String operation = "create";

    private String summary;

    private Map<String, Object> candidateFields = new LinkedHashMap<>();

    private String sourceExcerpt;

    private Long sourceChapterId;

    private boolean requiresConfirmation = true;

    public void putCandidateField(String key, Object value) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }
        candidateFields.put(key.trim(), value);
    }
}
