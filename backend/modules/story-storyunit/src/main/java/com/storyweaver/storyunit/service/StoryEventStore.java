package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.event.StoryEvent;

import java.util.List;

public interface StoryEventStore {

    StoryEvent appendEvent(StoryEvent event);

    List<StoryEvent> listChapterEvents(Long projectId, Long chapterId);
}
