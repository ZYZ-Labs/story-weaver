package com.storyweaver.storyunit.adapter;

import com.storyweaver.domain.entity.WorldSetting;
import com.storyweaver.storyunit.model.StoryUnitType;

public interface WorldSettingStoryUnitAdapter extends StoryUnitAdapter<WorldSetting> {

    @Override
    default StoryUnitType unitType() {
        return StoryUnitType.WORLD_SETTING;
    }

    @Override
    default Class<WorldSetting> sourceType() {
        return WorldSetting.class;
    }
}
