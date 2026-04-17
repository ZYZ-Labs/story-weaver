package com.storyweaver.story.generation.orchestration;

import java.util.Optional;

public interface StorySessionOrchestrator {

    Optional<StorySessionContextPacket> prepareContext(Long projectId, Long chapterId, String sceneId);

    SessionExecutionTrace initializeTrace(StorySessionContextPacket contextPacket);

    Optional<StorySessionPreview> preview(Long projectId, Long chapterId, String sceneId);
}
