package com.storyweaver.story.generation.orchestration;

import com.storyweaver.storyunit.session.DirectorCandidate;
import com.storyweaver.storyunit.session.SelectionDecision;

import java.util.List;

public interface SelectorSessionService {

    SelectionDecision selectCandidate(StorySessionContextPacket contextPacket, List<DirectorCandidate> candidates);
}
