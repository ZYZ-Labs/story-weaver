package com.storyweaver.storyunit.assembler;

import com.storyweaver.storyunit.adapter.StoryUnitAdapter;
import com.storyweaver.storyunit.facet.StoryFacet;
import com.storyweaver.storyunit.facet.summary.SummaryFacet;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryUnit;
import com.storyweaver.storyunit.model.StoryUnitType;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public interface StoryUnitAssembler<S> {

    StoryUnitType unitType();

    Class<S> sourceType();

    StoryUnitAdapter<S> adapter();

    Collection<StoryFacetAssembler<S, ? extends StoryFacet>> facetAssemblers();

    default StoryUnit assembleUnit(S source) {
        Objects.requireNonNull(source, "source must not be null");
        return adapter().toStoryUnit(source);
    }

    default Map<FacetType, StoryFacet> assembleFacets(S source, StoryUnit storyUnit) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(storyUnit, "storyUnit must not be null");
        return Map.of();
    }

    default Optional<SummaryFacet> assembleSummaryFacet(S source, StoryUnit storyUnit) {
        return Optional.empty();
    }
}
