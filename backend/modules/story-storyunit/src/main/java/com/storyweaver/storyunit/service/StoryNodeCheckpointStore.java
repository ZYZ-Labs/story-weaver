package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.runtime.StoryNodeCheckpoint;

import java.util.List;
import java.util.Optional;

public interface StoryNodeCheckpointStore {

    StoryNodeCheckpoint saveCheckpoint(StoryNodeCheckpoint checkpoint);

    Optional<StoryNodeCheckpoint> findCheckpoint(Long projectId, Long chapterId, String checkpointId);

    List<StoryNodeCheckpoint> listChapterCheckpoints(Long projectId, Long chapterId);
}
