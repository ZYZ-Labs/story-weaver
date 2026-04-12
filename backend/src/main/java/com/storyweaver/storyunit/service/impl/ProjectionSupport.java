package com.storyweaver.storyunit.service.impl;

import com.storyweaver.storyunit.facet.StoryFacet;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryFacetRef;
import com.storyweaver.storyunit.model.StoryUnit;

import java.util.LinkedHashMap;
import java.util.Map;

final class ProjectionSupport {

    private ProjectionSupport() {
    }

    static Long parseUnitId(String unitId) {
        if (unitId == null || unitId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(unitId.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static StoryUnit withFacetRefs(StoryUnit unit, Map<FacetType, StoryFacet> facets) {
        Map<FacetType, StoryFacetRef> refs = new LinkedHashMap<>();
        for (FacetType facetType : facets.keySet()) {
            refs.put(facetType, new StoryFacetRef(facetType, unit.ref().unitKey() + "#" + facetType.name().toLowerCase()));
        }
        return new StoryUnit(
                unit.ref(),
                unit.projectId(),
                unit.scope(),
                refs,
                unit.status(),
                unit.version(),
                unit.snapshotId(),
                unit.sourceTrace()
        );
    }
}
