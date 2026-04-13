package com.storyweaver.storyunit.facet.relation;

import com.storyweaver.storyunit.facet.StoryFacet;
import com.storyweaver.storyunit.model.FacetType;

import java.util.List;
import java.util.Map;

public interface RelationFacet extends StoryFacet {

    @Override
    default FacetType facetType() {
        return FacetType.RELATION;
    }

    Map<String, List<String>> relationRefs();
}
