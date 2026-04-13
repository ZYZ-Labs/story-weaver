package com.storyweaver.storyunit.summary;

import com.storyweaver.storyunit.facet.summary.DefaultSummaryFacet;

public interface StorySummaryService {

    DefaultSummaryFacet summarize(StorySummaryDraft draft);
}
