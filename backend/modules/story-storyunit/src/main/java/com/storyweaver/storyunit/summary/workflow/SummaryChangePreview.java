package com.storyweaver.storyunit.summary.workflow;

import com.storyweaver.storyunit.facet.summary.SummaryFacet;
import com.storyweaver.storyunit.model.FacetType;

import java.util.List;

public record SummaryChangePreview(
        SummaryFacet beforeSummary,
        SummaryFacet afterSummary,
        String changeSummary,
        List<FacetType> affectedFacets,
        boolean requiresConfirmation,
        List<String> warnings) {

    public SummaryChangePreview {
        changeSummary = changeSummary == null ? "" : changeSummary.trim();
        affectedFacets = affectedFacets == null ? List.of() : List.copyOf(affectedFacets);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
