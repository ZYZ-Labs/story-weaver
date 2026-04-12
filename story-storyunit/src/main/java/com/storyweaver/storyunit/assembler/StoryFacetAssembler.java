package com.storyweaver.storyunit.assembler;

import com.storyweaver.storyunit.facet.StoryFacet;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryUnit;

import java.util.Objects;
import java.util.Optional;

public interface StoryFacetAssembler<S, F extends StoryFacet> {

    FacetType facetType();

    Class<F> facetClass();

    Optional<F> assemble(S source, StoryUnit storyUnit);

    default boolean supports(S source, StoryUnit storyUnit) {
        Objects.requireNonNull(storyUnit, "storyUnit must not be null");
        return source != null;
    }
}
