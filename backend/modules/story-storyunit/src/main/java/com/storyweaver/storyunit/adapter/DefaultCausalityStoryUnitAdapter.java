package com.storyweaver.storyunit.adapter;

import com.storyweaver.domain.entity.Causality;
import com.storyweaver.storyunit.model.StoryUnitType;

public class DefaultCausalityStoryUnitAdapter extends AbstractStoryUnitAdapter<Causality> implements CausalityStoryUnitAdapter {

    public DefaultCausalityStoryUnitAdapter() {
        super(StoryUnitType.CAUSALITY, Causality.class);
    }

    @Override
    protected Long extractProjectId(Causality source) {
        return source.getProjectId();
    }

    @Override
    protected Object extractSourceId(Causality source) {
        return source.getId();
    }
}
