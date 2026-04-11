package com.storyweaver.storyunit.adapter;

import com.storyweaver.domain.entity.Causality;
import com.storyweaver.storyunit.model.StoryUnitType;

public interface CausalityStoryUnitAdapter extends StoryUnitAdapter<Causality> {

    @Override
    default StoryUnitType unitType() {
        return StoryUnitType.CAUSALITY;
    }

    @Override
    default Class<Causality> sourceType() {
        return Causality.class;
    }
}
