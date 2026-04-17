package com.storyweaver.story.generation.orchestration;

import com.storyweaver.storyunit.session.DirectorCandidate;
import com.storyweaver.storyunit.session.WriterExecutionBrief;

public interface WriterExecutionBriefBuilder {

    WriterExecutionBrief build(StorySessionContextPacket contextPacket, DirectorCandidate candidate);
}
