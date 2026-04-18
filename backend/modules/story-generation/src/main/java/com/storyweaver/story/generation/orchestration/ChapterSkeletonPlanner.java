package com.storyweaver.story.generation.orchestration;

import java.util.Optional;

public interface ChapterSkeletonPlanner {

    Optional<ChapterSkeleton> plan(Long projectId, Long chapterId);
}
