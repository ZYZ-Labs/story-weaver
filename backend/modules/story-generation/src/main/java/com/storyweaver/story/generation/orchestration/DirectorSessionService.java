package com.storyweaver.story.generation.orchestration;

import com.storyweaver.storyunit.session.DirectorCandidate;

import java.util.List;

public interface DirectorSessionService {

    List<DirectorCandidate> proposeCandidates(StorySessionContextPacket contextPacket);
}
