package com.storyweaver.storyunit.adapter;

import com.storyweaver.domain.entity.Character;
import com.storyweaver.storyunit.model.StoryUnitType;

public interface CharacterStoryUnitAdapter extends StoryUnitAdapter<Character> {

    @Override
    default StoryUnitType unitType() {
        return StoryUnitType.CHARACTER;
    }

    @Override
    default Class<Character> sourceType() {
        return Character.class;
    }
}
