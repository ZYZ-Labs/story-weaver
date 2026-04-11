package com.storyweaver.storyunit.adapter;

import com.storyweaver.domain.entity.WorldSetting;
import com.storyweaver.storyunit.model.StoryUnitType;

public class DefaultWorldSettingStoryUnitAdapter extends AbstractStoryUnitAdapter<WorldSetting> implements WorldSettingStoryUnitAdapter {

    public DefaultWorldSettingStoryUnitAdapter() {
        super(StoryUnitType.WORLD_SETTING, WorldSetting.class);
    }

    @Override
    protected Long extractProjectId(WorldSetting source) {
        return source.getProjectId();
    }

    @Override
    protected Object extractSourceId(WorldSetting source) {
        return source.getId();
    }
}
