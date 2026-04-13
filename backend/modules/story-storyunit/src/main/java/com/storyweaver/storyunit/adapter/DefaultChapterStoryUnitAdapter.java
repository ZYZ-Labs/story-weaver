package com.storyweaver.storyunit.adapter;

import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.storyunit.model.StoryScope;
import com.storyweaver.storyunit.model.StoryUnitType;

public class DefaultChapterStoryUnitAdapter extends AbstractStoryUnitAdapter<Chapter> implements ChapterStoryUnitAdapter {

    public DefaultChapterStoryUnitAdapter() {
        super(StoryUnitType.CHAPTER, Chapter.class);
    }

    @Override
    protected Long extractProjectId(Chapter source) {
        return source.getProjectId();
    }

    @Override
    protected Object extractSourceId(Chapter source) {
        return source.getId();
    }

    @Override
    protected StoryScope determineScope(Chapter source) {
        return StoryScope.CHAPTER;
    }
}
