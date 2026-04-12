package com.storyweaver.storyunit.service.impl;

import com.storyweaver.storyunit.facet.StoryFacet;
import com.storyweaver.storyunit.facet.summary.SummaryFacet;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryUnit;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.service.StoryUnitProjectionService;
import com.storyweaver.storyunit.service.StoryUnitQueryService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DefaultStoryUnitQueryService implements StoryUnitQueryService {

    private final StoryUnitProjectionService projectionService;

    public DefaultStoryUnitQueryService(StoryUnitProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @Override
    public Optional<StoryUnit> getUnit(StoryUnitRef ref) {
        return projectionService.projectUnit(ref);
    }

    @Override
    public Map<StoryUnitRef, StoryUnit> getUnits(Collection<StoryUnitRef> refs) {
        Map<StoryUnitRef, StoryUnit> result = new LinkedHashMap<>();
        if (refs == null || refs.isEmpty()) {
            return result;
        }
        for (StoryUnitRef ref : refs) {
            getUnit(ref).ifPresent(unit -> result.put(ref, unit));
        }
        return Map.copyOf(result);
    }

    @Override
    public List<StoryUnit> listUnits(Long projectId, StoryUnitType unitType) {
        return projectionService.listProjectedUnits(projectId, unitType);
    }

    @Override
    public Optional<SummaryFacet> getSummaryFacet(StoryUnitRef ref) {
        return projectionService.project(ref).flatMap(projected -> projected.summaryFacet());
    }

    @Override
    public Map<FacetType, StoryFacet> getFacets(StoryUnitRef ref) {
        return projectionService.projectFacets(ref);
    }
}
