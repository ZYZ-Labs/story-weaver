package com.storyweaver.story.generation.orchestration;

import java.util.Optional;

public interface ChapterNodeRuntimeService {

    Optional<ChapterNodeRuntimeView> preview(Long projectId, Long chapterId);

    Optional<NodeResolutionResult> resolve(NodeActionRequest request);
}
