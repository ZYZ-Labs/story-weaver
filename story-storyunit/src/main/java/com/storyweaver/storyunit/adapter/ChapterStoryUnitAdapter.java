package com.storyweaver.storyunit.adapter;

import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.storyunit.model.StoryUnitType;

public interface ChapterStoryUnitAdapter extends StoryUnitAdapter<Chapter> {

    @Override
    default StoryUnitType unitType() {
        return StoryUnitType.CHAPTER;
    }

    @Override
    default Class<Chapter> sourceType() {
        return Chapter.class;
    }
}
