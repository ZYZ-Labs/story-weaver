package com.storyweaver.story.generation.orchestration;

import java.util.Optional;

public interface StorySessionContextAssembler {

    Optional<StorySessionContextPacket> assemble(Long projectId, Long chapterId, String sceneId);
}
