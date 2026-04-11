package com.storyweaver.storyunit.facet.summary;

import com.storyweaver.storyunit.facet.StoryFacet;
import com.storyweaver.storyunit.model.FacetType;

import java.util.List;

public interface SummaryFacet extends StoryFacet {

    @Override
    default FacetType facetType() {
        return FacetType.SUMMARY;
    }

    String displayTitle();

    String oneLineSummary();

    String longSummary();

    String stateSummary();

    String relationSummary();

    String changeSummary();

    List<String> pendingQuestions();
}
