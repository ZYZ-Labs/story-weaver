package com.storyweaver.storyunit.facet.state;

import com.storyweaver.storyunit.facet.StoryFacet;
import com.storyweaver.storyunit.model.FacetType;

import java.util.List;
import java.util.Map;

public interface StateFacet extends StoryFacet {

    @Override
    default FacetType facetType() {
        return FacetType.STATE;
    }

    Map<String, Object> stateFields();

    List<String> activeFlags();
}
