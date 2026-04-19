package com.storyweaver.story.generation.orchestration;

import java.util.Optional;

public interface ChapterSkeletonMutationService {

    Optional<ChapterSkeleton> updateScene(Long projectId, Long chapterId, SceneSkeletonMutationCommand command);

    Optional<ChapterSkeleton> deleteScene(Long projectId, Long chapterId, String sceneId);
}
