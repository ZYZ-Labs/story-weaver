package com.storyweaver.storyunit.facet.canon;

import com.storyweaver.storyunit.facet.StoryFacet;
import com.storyweaver.storyunit.model.FacetType;

import java.util.List;
import java.util.Map;

public interface CanonFacet extends StoryFacet {

    @Override
    default FacetType facetType() {
        return FacetType.CANON;
    }

    Map<String, Object> canonicalFields();

    List<String> canonicalTags();
}
