package com.storyweaver.storyunit.adapter;

import com.storyweaver.domain.entity.Plot;
import com.storyweaver.storyunit.model.StoryUnitType;

public interface PlotStoryUnitAdapter extends StoryUnitAdapter<Plot> {

    @Override
    default StoryUnitType unitType() {
        return StoryUnitType.PLOT;
    }

    @Override
    default Class<Plot> sourceType() {
        return Plot.class;
    }
}
