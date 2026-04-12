package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.facet.StoryFacet;
import com.storyweaver.storyunit.facet.summary.SummaryFacet;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryUnit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record ProjectedStoryUnit(
        StoryUnit unit,
        Map<FacetType, StoryFacet> facets) {

    public ProjectedStoryUnit {
        unit = Objects.requireNonNull(unit, "unit must not be null");
        facets = facets == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(facets));
    }

    public Optional<SummaryFacet> summaryFacet() {
        return Optional.ofNullable(facets.get(FacetType.SUMMARY))
                .filter(SummaryFacet.class::isInstance)
                .map(SummaryFacet.class::cast);
    }
}
