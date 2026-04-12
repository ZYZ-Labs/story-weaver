package com.storyweaver.storyunit.mapping;

import com.storyweaver.storyunit.model.FacetType;

import java.util.Objects;

public record StoryFieldMapping(
        FacetType facetType,
        String fieldPath,
        MappingSourceType sourceType,
        String sourceKey,
        String assemblerKey,
        FallbackStrategy fallbackStrategy,
        MappingPhase phase,
        String notes) {

    public StoryFieldMapping {
        facetType = Objects.requireNonNull(facetType, "facetType must not be null");
        fieldPath = normalize(fieldPath, "fieldPath");
        sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        sourceKey = normalize(sourceKey, "sourceKey");
        assemblerKey = normalize(assemblerKey, "assemblerKey");
        fallbackStrategy = Objects.requireNonNull(fallbackStrategy, "fallbackStrategy must not be null");
        phase = Objects.requireNonNull(phase, "phase must not be null");
        notes = notes == null ? null : notes.trim();
    }

    private static String normalize(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
