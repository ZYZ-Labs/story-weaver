package com.storyweaver.story.generation.orchestration;

import java.util.Optional;

public interface ChapterSkeletonStore {

    Optional<ChapterSkeleton> find(Long projectId, Long chapterId);

    ChapterSkeleton save(ChapterSkeleton chapterSkeleton);
}
