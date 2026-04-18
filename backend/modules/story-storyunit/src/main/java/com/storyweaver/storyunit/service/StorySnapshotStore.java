package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.snapshot.StorySnapshot;

import java.util.List;

public interface StorySnapshotStore {

    StorySnapshot saveSnapshot(StorySnapshot snapshot);

    List<StorySnapshot> listChapterSnapshots(Long projectId, Long chapterId);
}
