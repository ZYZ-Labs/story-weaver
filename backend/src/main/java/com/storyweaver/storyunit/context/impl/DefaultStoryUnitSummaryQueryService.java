package com.storyweaver.storyunit.context.impl;

import com.storyweaver.storyunit.context.StoryUnitSummaryQueryService;
import com.storyweaver.storyunit.context.StoryUnitSummaryView;
import com.storyweaver.storyunit.facet.summary.SummaryFacet;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.service.ProjectedStoryUnit;
import com.storyweaver.storyunit.service.StoryUnitQueryService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DefaultStoryUnitSummaryQueryService implements StoryUnitSummaryQueryService {

    private final StoryUnitQueryService storyUnitQueryService;

    public DefaultStoryUnitSummaryQueryService(StoryUnitQueryService storyUnitQueryService) {
        this.storyUnitQueryService = storyUnitQueryService;
    }

    @Override
    public Optional<StoryUnitSummaryView> getStoryUnitSummary(StoryUnitRef ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return storyUnitQueryService.getProjected(ref)
                .map(this::toView);
    }

    private StoryUnitSummaryView toView(ProjectedStoryUnit projected) {
        SummaryFacet summaryFacet = projected.summaryFacet().orElse(null);
        String title = summaryFacet == null
                ? projected.unit().ref().unitKey()
                : ContextViewSupport.firstNonBlank(summaryFacet.displayTitle(), projected.unit().ref().unitKey());
        String summary = summaryFacet == null
                ? ""
                : ContextViewSupport.firstNonBlank(
                summaryFacet.longSummary(),
                summaryFacet.oneLineSummary(),
                summaryFacet.stateSummary(),
                summaryFacet.relationSummary(),
                summaryFacet.changeSummary()
        );
        return new StoryUnitSummaryView(projected.unit().ref(), projected.unit().ref().unitType(), title, summary);
    }
}
