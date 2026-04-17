package com.storyweaver.storyunit.context;

import com.storyweaver.storyunit.model.StoryUnitRef;

import java.util.Optional;

public interface StoryUnitSummaryQueryService {

    Optional<StoryUnitSummaryView> getStoryUnitSummary(StoryUnitRef ref);
}
