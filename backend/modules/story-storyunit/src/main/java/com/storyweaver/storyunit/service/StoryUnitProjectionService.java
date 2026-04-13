package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;

import java.util.List;
import java.util.Optional;

public interface StoryUnitProjectionService {

    Optional<ProjectedStoryUnit> project(StoryUnitRef ref);

    List<ProjectedStoryUnit> listProjected(Long projectId, StoryUnitType unitType);

    default Optional<com.storyweaver.storyunit.model.StoryUnit> projectUnit(StoryUnitRef ref) {
        return project(ref).map(ProjectedStoryUnit::unit);
    }

    default java.util.Map<com.storyweaver.storyunit.model.FacetType, com.storyweaver.storyunit.facet.StoryFacet> projectFacets(StoryUnitRef ref) {
        return project(ref).map(ProjectedStoryUnit::facets).orElseGet(java.util.Map::of);
    }

    default Optional<com.storyweaver.storyunit.facet.StoryFacet> projectFacet(StoryUnitRef ref, com.storyweaver.storyunit.model.FacetType facetType) {
        return project(ref).map(ProjectedStoryUnit::facets).map(facets -> facets.get(facetType));
    }

    default List<com.storyweaver.storyunit.model.StoryUnit> listProjectedUnits(Long projectId, StoryUnitType unitType) {
        return listProjected(projectId, unitType).stream().map(ProjectedStoryUnit::unit).toList();
    }
}
