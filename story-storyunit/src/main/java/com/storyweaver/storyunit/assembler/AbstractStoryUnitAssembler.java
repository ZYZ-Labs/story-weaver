package com.storyweaver.storyunit.assembler;

import com.storyweaver.storyunit.adapter.StoryUnitAdapter;
import com.storyweaver.storyunit.facet.StoryFacet;
import com.storyweaver.storyunit.facet.summary.SummaryFacet;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryUnit;
import com.storyweaver.storyunit.model.StoryUnitType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class AbstractStoryUnitAssembler<S> implements StoryUnitAssembler<S> {

    private final StoryUnitAdapter<S> adapter;

    private final List<StoryFacetAssembler<S, ? extends StoryFacet>> facetAssemblers;

    protected AbstractStoryUnitAssembler(
            StoryUnitAdapter<S> adapter,
            Collection<? extends StoryFacetAssembler<S, ? extends StoryFacet>> facetAssemblers) {
        this.adapter = Objects.requireNonNull(adapter, "adapter must not be null");
        this.facetAssemblers = List.copyOf(Objects.requireNonNull(facetAssemblers, "facetAssemblers must not be null"));
    }

    @Override
    public StoryUnitType unitType() {
        return adapter.unitType();
    }

    @Override
    public Class<S> sourceType() {
        return adapter.sourceType();
    }

    @Override
    public StoryUnitAdapter<S> adapter() {
        return adapter;
    }

    @Override
    public Collection<StoryFacetAssembler<S, ? extends StoryFacet>> facetAssemblers() {
        return facetAssemblers;
    }

    @Override
    public Map<FacetType, StoryFacet> assembleFacets(S source, StoryUnit storyUnit) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(storyUnit, "storyUnit must not be null");

        Map<FacetType, StoryFacet> assembled = new LinkedHashMap<>();
        for (StoryFacetAssembler<S, ? extends StoryFacet> facetAssembler : facetAssemblers) {
            if (!facetAssembler.supports(source, storyUnit)) {
                continue;
            }
            facetAssembler.assemble(source, storyUnit)
                    .ifPresent(facet -> assembled.put(facet.facetType(), facet));
        }
        return Map.copyOf(assembled);
    }

    @Override
    public Optional<SummaryFacet> assembleSummaryFacet(S source, StoryUnit storyUnit) {
        return Optional.ofNullable(assembleFacets(source, storyUnit).get(FacetType.SUMMARY))
                .filter(SummaryFacet.class::isInstance)
                .map(SummaryFacet.class::cast);
    }
}
