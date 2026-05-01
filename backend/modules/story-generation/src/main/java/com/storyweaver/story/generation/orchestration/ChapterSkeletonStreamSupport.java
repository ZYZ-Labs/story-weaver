package com.storyweaver.story.generation.orchestration;

import java.util.Optional;
import java.util.function.Consumer;

public interface ChapterSkeletonStreamSupport {

    default void generateStream(
            Long projectId,
            Long chapterId,
            boolean forceRefresh,
            Consumer<ChapterSkeletonStreamEvent> eventConsumer) {
        Optional<ChapterSkeleton> skeleton = generate(projectId, chapterId, forceRefresh);
        if (skeleton.isEmpty()) {
            throw new IllegalStateException("章节不存在或无法生成镜头骨架");
        }
        eventConsumer.accept(ChapterSkeletonStreamEvent.complete(skeleton.get()));
    }

    Optional<ChapterSkeleton> generate(Long projectId, Long chapterId, boolean forceRefresh);
}
