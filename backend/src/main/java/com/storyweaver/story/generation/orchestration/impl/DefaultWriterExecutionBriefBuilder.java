package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.story.generation.orchestration.WriterExecutionBriefBuilder;
import com.storyweaver.storyunit.session.DirectorCandidate;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.WriterExecutionBrief;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultWriterExecutionBriefBuilder implements WriterExecutionBriefBuilder {

    @Override
    public WriterExecutionBrief build(StorySessionContextPacket contextPacket, DirectorCandidate candidate) {
        List<String> continuityNotes = new ArrayList<>();
        SceneExecutionState resolvedSceneState = contextPacket.sceneBindingContext().resolvedSceneState();
        if (!contextPacket.readerKnownState().knownFacts().isEmpty()) {
            continuityNotes.add("读者已知：" + contextPacket.readerKnownState().knownFacts().getFirst());
        }
        if (!contextPacket.recentStoryProgress().items().isEmpty()) {
            continuityNotes.add("最近进度：" + contextPacket.recentStoryProgress().items().getFirst().summary());
        }
        if (resolvedSceneState != null && !resolvedSceneState.outcomeSummary().isBlank()) {
            continuityNotes.add("承接 scene：" + resolvedSceneState.outcomeSummary());
        } else if (!contextPacket.existingSceneStates().isEmpty()) {
            continuityNotes.add("上一 scene：" + contextPacket.existingSceneStates().getLast().outcomeSummary());
        }
        if (contextPacket.sceneBindingContext().fallbackUsed()) {
            continuityNotes.add("scene 绑定提示：" + contextPacket.sceneBindingContext().summary());
        }

        return new WriterExecutionBrief(
                contextPacket.projectId(),
                contextPacket.chapterId(),
                contextPacket.sceneId().isBlank() ? "scene-1" : contextPacket.sceneId(),
                candidate.candidateId(),
                candidate.goal(),
                candidate.readerReveal(),
                candidate.mustUseAnchors(),
                candidate.forbiddenMoves(),
                candidate.stopCondition(),
                candidate.targetWords(),
                List.copyOf(continuityNotes),
                resolvedSceneState != null ? resolvedSceneState.handoffLine()
                        : (contextPacket.existingSceneStates().isEmpty() ? "" : contextPacket.existingSceneStates().getLast().handoffLine())
        );
    }
}
