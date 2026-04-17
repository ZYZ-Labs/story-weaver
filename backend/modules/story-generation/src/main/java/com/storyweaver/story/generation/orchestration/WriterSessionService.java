package com.storyweaver.story.generation.orchestration;

import com.storyweaver.storyunit.session.WriterExecutionBrief;

public interface WriterSessionService {

    WriterSessionResult write(StorySessionContextPacket contextPacket, WriterExecutionBrief brief);
}
