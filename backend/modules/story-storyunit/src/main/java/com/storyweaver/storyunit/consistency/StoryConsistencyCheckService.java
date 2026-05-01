package com.storyweaver.storyunit.consistency;

import java.util.Optional;

public interface StoryConsistencyCheckService {

    Optional<StoryConsistencyCheck> checkChapter(Long projectId, Long chapterId);
}
