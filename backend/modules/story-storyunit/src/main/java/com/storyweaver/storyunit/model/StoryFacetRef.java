package com.storyweaver.storyunit.model;

import java.util.Objects;

public record StoryFacetRef(
        FacetType facetType,
        String refKey) {

    public StoryFacetRef {
        facetType = Objects.requireNonNull(facetType, "facetType must not be null");
        refKey = Objects.requireNonNull(refKey, "refKey must not be null").trim();
    }
}
