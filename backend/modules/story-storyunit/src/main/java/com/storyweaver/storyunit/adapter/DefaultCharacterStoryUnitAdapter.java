package com.storyweaver.storyunit.adapter;

import com.storyweaver.domain.entity.Character;
import com.storyweaver.storyunit.model.StoryUnitType;

public class DefaultCharacterStoryUnitAdapter extends AbstractStoryUnitAdapter<Character> implements CharacterStoryUnitAdapter {

    public DefaultCharacterStoryUnitAdapter() {
        super(StoryUnitType.CHARACTER, Character.class);
    }

    @Override
    protected Long extractProjectId(Character source) {
        return source.getProjectId();
    }

    @Override
    protected Object extractSourceId(Character source) {
        return source.getId();
    }
}
