package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.runtime.StoryOpenLoop;

import java.util.List;

public interface StoryOpenLoopStore {

    StoryOpenLoop saveLoop(StoryOpenLoop loop);

    List<StoryOpenLoop> listChapterLoops(Long projectId, Long chapterId);
}
