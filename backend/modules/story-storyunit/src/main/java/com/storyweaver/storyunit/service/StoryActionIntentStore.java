package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.runtime.StoryActionIntent;

import java.util.List;

public interface StoryActionIntentStore {

    StoryActionIntent recordIntent(StoryActionIntent intent);

    List<StoryActionIntent> listChapterIntents(Long projectId, Long chapterId);
}
