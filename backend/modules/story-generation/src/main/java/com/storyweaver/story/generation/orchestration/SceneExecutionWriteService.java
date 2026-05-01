package com.storyweaver.story.generation.orchestration;

import com.storyweaver.storyunit.session.ReviewDecision;
import com.storyweaver.story.generation.orchestration.SceneSkeletonItem;
import com.storyweaver.storyunit.session.WriterExecutionBrief;

public interface SceneExecutionWriteService {

    SceneExecutionWriteResult write(
            StorySessionContextPacket contextPacket,
            WriterExecutionBrief writerExecutionBrief,
            WriterSessionResult writerSessionResult,
            ReviewDecision reviewDecision);

    SceneExecutionWriteResult writeAccepted(
            StorySessionContextPacket contextPacket,
            SceneSkeletonItem currentScene,
            SceneSkeletonItem nextScene,
            Long writingRecordId,
            String acceptedContent);
}
