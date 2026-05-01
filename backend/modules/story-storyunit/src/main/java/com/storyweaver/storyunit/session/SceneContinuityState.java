package com.storyweaver.storyunit.session;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record SceneContinuityState(
        String sourceSceneId,
        String summary,
        String handoffLine,
        List<String> carryForwardFacts,
        List<String> timeAnchors,
        List<String> expectedNames,
        List<String> counterpartNames,
        boolean requiresExplicitTimeTransition,
        String nextSceneId,
        String nextSceneGoal,
        String stopCondition) {

    private static final SceneContinuityState EMPTY = new SceneContinuityState(
            "",
            "",
            "",
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            false,
            "",
            "",
            ""
    );

    public SceneContinuityState {
        sourceSceneId = normalizeText(sourceSceneId);
        summary = normalizeText(summary);
        handoffLine = normalizeText(handoffLine);
        carryForwardFacts = normalizeStrings(carryForwardFacts);
        timeAnchors = normalizeStrings(timeAnchors);
        expectedNames = normalizeStrings(expectedNames);
        counterpartNames = normalizeStrings(counterpartNames);
        requiresExplicitTimeTransition = requiresExplicitTimeTransition;
        nextSceneId = normalizeText(nextSceneId);
        nextSceneGoal = normalizeText(nextSceneGoal);
        stopCondition = normalizeText(stopCondition);
    }

    public static SceneContinuityState empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return !hasText(sourceSceneId)
                && !hasText(summary)
                && !hasText(handoffLine)
                && carryForwardFacts.isEmpty()
                && timeAnchors.isEmpty()
                && expectedNames.isEmpty()
                && counterpartNames.isEmpty()
                && !hasText(nextSceneId)
                && !hasText(nextSceneGoal)
                && !hasText(stopCondition);
    }

    public Map<String, Object> toStateDeltaMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sourceSceneId", sourceSceneId);
        values.put("summary", summary);
        values.put("handoffLine", handoffLine);
        values.put("carryForwardFacts", carryForwardFacts);
        values.put("timeAnchors", timeAnchors);
        values.put("expectedNames", expectedNames);
        values.put("counterpartNames", counterpartNames);
        values.put("requiresExplicitTimeTransition", requiresExplicitTimeTransition);
        values.put("nextSceneId", nextSceneId);
        values.put("nextSceneGoal", nextSceneGoal);
        values.put("stopCondition", stopCondition);
        return Map.copyOf(values);
    }

    public static SceneContinuityState fromStateDeltaValue(Object value) {
        if (value instanceof SceneContinuityState continuityState) {
            return continuityState;
        }
        if (!(value instanceof Map<?, ?> source)) {
            return SceneContinuityState.empty();
        }
        return new SceneContinuityState(
                asString(source.get("sourceSceneId")),
                asString(source.get("summary")),
                asString(source.get("handoffLine")),
                asStringList(source.get("carryForwardFacts")),
                asStringList(source.get("timeAnchors")),
                asStringList(source.get("expectedNames")),
                asStringList(source.get("counterpartNames")),
                asBoolean(source.get("requiresExplicitTimeTransition")),
                asString(source.get("nextSceneId")),
                asString(source.get("nextSceneGoal")),
                asString(source.get("stopCondition"))
        );
    }

    private static String asString(Object value) {
        if (value == null) {
            return "";
        }
        return normalizeText(String.valueOf(value));
    }

    private static List<String> asStringList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        return normalizeStrings(values.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList());
    }

    private static List<String> normalizeStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(SceneContinuityState::normalizeText)
                .filter(SceneContinuityState::hasText)
                .distinct()
                .toList();
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value == null) {
            return false;
        }
        String text = normalizeText(String.valueOf(value)).toLowerCase();
        return "true".equals(text) || "1".equals(text) || "yes".equals(text) || "是".equals(text);
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
