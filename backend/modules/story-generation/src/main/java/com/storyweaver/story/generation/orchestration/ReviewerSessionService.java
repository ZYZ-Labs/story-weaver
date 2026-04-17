package com.storyweaver.story.generation.orchestration;

import com.storyweaver.storyunit.session.ReviewDecision;

public interface ReviewerSessionService {

    ReviewDecision review(StorySessionContextPacket contextPacket, WriterSessionResult writerSessionResult);
}
