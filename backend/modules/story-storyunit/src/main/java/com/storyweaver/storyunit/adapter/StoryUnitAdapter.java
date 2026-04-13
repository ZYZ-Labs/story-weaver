package com.storyweaver.storyunit.adapter;

import com.storyweaver.storyunit.facet.StoryFacet;
import com.storyweaver.storyunit.facet.summary.SummaryFacet;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryUnit;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StoryUnitAdapter<S> {

    StoryUnitType unitType();

    Class<S> sourceType();

    StoryUnitRef toUnitRef(S source);

    StoryUnit toStoryUnit(S source);

    default Optional<SummaryFacet> toSummaryFacet(S source) {
        return Optional.empty();
    }

    default Map<FacetType, StoryFacet> toFacets(S source) {
        return Map.of();
    }

    default List<String> supportedFacetKeys() {
        return List.of();
    }
}
