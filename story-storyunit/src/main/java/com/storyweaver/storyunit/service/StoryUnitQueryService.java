package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.facet.StoryFacet;
import com.storyweaver.storyunit.facet.summary.SummaryFacet;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryUnit;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StoryUnitQueryService {

    Optional<StoryUnit> getUnit(StoryUnitRef ref);

    Map<StoryUnitRef, StoryUnit> getUnits(Collection<StoryUnitRef> refs);

    List<StoryUnit> listUnits(Long projectId, StoryUnitType unitType);

    Optional<SummaryFacet> getSummaryFacet(StoryUnitRef ref);

    Map<FacetType, StoryFacet> getFacets(StoryUnitRef ref);
}
