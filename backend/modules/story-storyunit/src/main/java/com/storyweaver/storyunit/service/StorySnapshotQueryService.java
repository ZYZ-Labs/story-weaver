package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.snapshot.SnapshotScope;
import com.storyweaver.storyunit.snapshot.StorySnapshot;

import java.util.List;
import java.util.Optional;

public interface StorySnapshotQueryService {

    Optional<StorySnapshot> getSnapshot(String snapshotId);

    Optional<StorySnapshot> findLatestSnapshot(Long projectId, SnapshotScope scope);

    List<StorySnapshot> listChapterSnapshots(Long projectId, Long chapterId);
}
