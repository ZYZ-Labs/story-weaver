package com.storyweaver.storyunit.facet.execution;

import java.util.List;

public record DefaultExecutionFacet(
        String executionSummary,
        List<String> openLoops,
        String handoffLine) implements ExecutionFacet {

    public DefaultExecutionFacet {
        executionSummary = normalize(executionSummary);
        openLoops = openLoops == null ? List.of() : List.copyOf(openLoops);
        handoffLine = normalize(handoffLine);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
