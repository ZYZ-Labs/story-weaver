package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.facet.StoryFacet;
import com.storyweaver.storyunit.facet.summary.SummaryFacet;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryUnit;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StoryUnitQueryService {

    Optional<ProjectedStoryUnit> getProjected(StoryUnitRef ref);

    List<ProjectedStoryUnit> listProjected(Long projectId, StoryUnitType unitType);

    default Optional<StoryUnit> getUnit(StoryUnitRef ref) {
        return getProjected(ref).map(ProjectedStoryUnit::unit);
    }

    default Map<StoryUnitRef, StoryUnit> getUnits(Collection<StoryUnitRef> refs) {
        Map<StoryUnitRef, StoryUnit> result = new LinkedHashMap<>();
        if (refs == null || refs.isEmpty()) {
            return result;
        }
        for (StoryUnitRef ref : refs) {
            getUnit(ref).ifPresent(unit -> result.put(ref, unit));
        }
        return Map.copyOf(result);
    }

    default List<StoryUnit> listUnits(Long projectId, StoryUnitType unitType) {
        return listProjected(projectId, unitType).stream()
                .map(ProjectedStoryUnit::unit)
                .toList();
    }

    default Optional<SummaryFacet> getSummaryFacet(StoryUnitRef ref) {
        return getProjected(ref).flatMap(ProjectedStoryUnit::summaryFacet);
    }

    default Map<FacetType, StoryFacet> getFacets(StoryUnitRef ref) {
        return getProjected(ref).map(ProjectedStoryUnit::facets).orElseGet(Map::of);
    }
}
