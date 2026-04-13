package com.storyweaver.storyunit.context;

import java.util.List;
import java.util.Objects;

public record CharacterRuntimeStateView(
        Long projectId,
        Long characterId,
        String characterName,
        String currentLocation,
        String emotionalState,
        String attitudeSummary,
        List<String> inventoryNames,
        List<String> skillNames,
        List<String> stateTags) {

    public CharacterRuntimeStateView {
        characterName = normalize(characterName);
        currentLocation = normalize(currentLocation);
        emotionalState = normalize(emotionalState);
        attitudeSummary = normalize(attitudeSummary);
        inventoryNames = sanitize(inventoryNames);
        skillNames = sanitize(skillNames);
        stateTags = sanitize(stateTags);
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
