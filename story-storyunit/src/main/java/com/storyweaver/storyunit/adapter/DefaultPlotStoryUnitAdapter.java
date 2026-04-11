package com.storyweaver.storyunit.adapter;

import com.storyweaver.domain.entity.Plot;
import com.storyweaver.storyunit.model.StoryScope;
import com.storyweaver.storyunit.model.StoryUnitType;

public class DefaultPlotStoryUnitAdapter extends AbstractStoryUnitAdapter<Plot> implements PlotStoryUnitAdapter {

    public DefaultPlotStoryUnitAdapter() {
        super(StoryUnitType.PLOT, Plot.class);
    }

    @Override
    protected Long extractProjectId(Plot source) {
        return source.getProjectId();
    }

    @Override
    protected Object extractSourceId(Plot source) {
        return source.getId();
    }

    @Override
    protected StoryScope determineScope(Plot source) {
        return source.getChapterId() == null ? StoryScope.PROJECT : StoryScope.CHAPTER;
    }
}
