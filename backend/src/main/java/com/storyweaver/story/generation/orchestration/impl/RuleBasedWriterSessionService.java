package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.story.generation.orchestration.WriterSessionResult;
import com.storyweaver.story.generation.orchestration.WriterSessionService;
import com.storyweaver.storyunit.session.WriterExecutionBrief;
import org.springframework.stereotype.Service;

import java.util.StringJoiner;

@Service
public class RuleBasedWriterSessionService implements WriterSessionService {

    @Override
    public WriterSessionResult write(StorySessionContextPacket contextPacket, WriterExecutionBrief brief) {
        StringJoiner draft = new StringJoiner("\n\n");
        draft.add("【目标】" + brief.goal());
        if (!brief.readerReveal().isEmpty()) {
            draft.add("【本段需要向读者揭晓】" + String.join("；", brief.readerReveal()));
        }
        if (!brief.mustUseAnchors().isEmpty()) {
            draft.add("【必须使用的锚点】" + String.join("；", brief.mustUseAnchors()));
        }
        if (!brief.continuityNotes().isEmpty()) {
            draft.add("【承接说明】" + String.join("；", brief.continuityNotes()));
        }
        draft.add("【收束点】" + brief.stopCondition());

        String summary = contextPacket.chapterAnchorBundle().chapterTitle() + " / " + brief.goal();

        return new WriterSessionResult(
                brief.sceneId(),
                brief.chosenCandidateId(),
                draft.toString(),
                summary
        );
    }
}
