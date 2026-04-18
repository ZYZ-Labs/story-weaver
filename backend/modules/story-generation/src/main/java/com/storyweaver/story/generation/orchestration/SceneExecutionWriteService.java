package com.storyweaver.story.generation.orchestration;

import com.storyweaver.storyunit.session.ReviewDecision;
import com.storyweaver.storyunit.session.WriterExecutionBrief;

public interface SceneExecutionWriteService {

    SceneExecutionWriteResult write(
            StorySessionContextPacket contextPacket,
            WriterExecutionBrief writerExecutionBrief,
            WriterSessionResult writerSessionResult,
            ReviewDecision reviewDecision);
}
