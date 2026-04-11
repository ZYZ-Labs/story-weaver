package com.storyweaver.storyunit.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record StoryUnit(
        StoryUnitRef ref,
        Long projectId,
        StoryScope scope,
        Map<FacetType, StoryFacetRef> facetRefs,
        StoryUnitStatus status,
        StoryUnitVersion version,
        String snapshotId,
        StorySourceTrace sourceTrace) {

    public StoryUnit {
        ref = Objects.requireNonNull(ref, "ref must not be null");
        scope = Objects.requireNonNull(scope, "scope must not be null");
        facetRefs = facetRefs == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(facetRefs));
        status = Objects.requireNonNull(status, "status must not be null");
        version = Objects.requireNonNull(version, "version must not be null");
        snapshotId = snapshotId == null ? null : snapshotId.trim();
    }
}
