package com.storyweaver.story.generation.orchestration;

import java.util.Optional;

public interface ChapterSkeletonGenerationService extends ChapterSkeletonStreamSupport {

    Optional<ChapterSkeleton> generate(Long projectId, Long chapterId, boolean forceRefresh);
}
