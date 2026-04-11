package com.storyweaver.storyunit.facet.execution;

import com.storyweaver.storyunit.facet.StoryFacet;
import com.storyweaver.storyunit.model.FacetType;

import java.util.List;

public interface ExecutionFacet extends StoryFacet {

    @Override
    default FacetType facetType() {
        return FacetType.EXECUTION;
    }

    String executionSummary();

    List<String> openLoops();

    String handoffLine();
}
