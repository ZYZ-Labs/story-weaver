package com.storyweaver.storyunit.migration;

import java.util.List;

public record CompatibilityBoundaryItem(
        String boundaryKey,
        CompatibilityScope scope,
        String displayName,
        CompatibilityMode mode,
        String primaryChain,
        String fallbackChain,
        boolean enabled,
        List<String> notes) {

    public CompatibilityBoundaryItem {
        boundaryKey = boundaryKey == null ? "" : boundaryKey.trim();
        displayName = displayName == null ? "" : displayName.trim();
        primaryChain = primaryChain == null ? "" : primaryChain.trim();
        fallbackChain = fallbackChain == null ? "" : fallbackChain.trim();
        notes = notes == null ? List.of() : List.copyOf(notes);
    }
}
