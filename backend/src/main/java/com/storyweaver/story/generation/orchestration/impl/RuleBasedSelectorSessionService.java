package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.SceneBindingMode;
import com.storyweaver.story.generation.orchestration.SelectorSessionService;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.storyunit.session.DirectorCandidate;
import com.storyweaver.storyunit.session.DirectorCandidateType;
import com.storyweaver.storyunit.session.SelectionDecision;
import com.storyweaver.storyunit.session.SelectionRejection;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RuleBasedSelectorSessionService implements SelectorSessionService {

    @Override
    public SelectionDecision selectCandidate(StorySessionContextPacket contextPacket, List<DirectorCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new SelectionDecision("", "当前没有可选候选。", List.of(), List.of("director 未产出候选"));
        }

        DirectorCandidate chosen = choose(contextPacket, candidates);
        List<SelectionRejection> rejected = candidates.stream()
                .filter(candidate -> !candidate.candidateId().equals(chosen.candidateId()))
                .map(candidate -> new SelectionRejection(candidate.candidateId(), buildRejectionReason(contextPacket, candidate)))
                .toList();

        return new SelectionDecision(
                chosen.candidateId(),
                buildChosenReason(contextPacket, chosen),
                rejected,
                buildRisks(contextPacket, chosen)
        );
    }

    private DirectorCandidate choose(StorySessionContextPacket contextPacket, List<DirectorCandidate> candidates) {
        boolean firstScene = contextPacket.sceneBindingContext().mode() == SceneBindingMode.CHAPTER_COLD_START
                || contextPacket.existingSceneStates().isEmpty();
        if (firstScene) {
            return candidates.stream()
                    .filter(candidate -> candidate.candidateType() == DirectorCandidateType.OPENING)
                    .findFirst()
                    .orElse(candidates.getFirst());
        }
        return candidates.stream()
                .filter(candidate -> candidate.candidateType() == DirectorCandidateType.MAINLINE_ADVANCE)
                .findFirst()
                .orElse(candidates.getFirst());
    }

    private String buildChosenReason(StorySessionContextPacket contextPacket, DirectorCandidate chosen) {
        if (contextPacket.existingSceneStates().isEmpty() && chosen.candidateType() == DirectorCandidateType.OPENING) {
            return "当前章节还没有 scene 执行状态，优先先做开场定向。";
        }
        if (contextPacket.sceneBindingContext().fallbackUsed()) {
            return "请求的 scene 未命中，当前已回退到最近 scene 上下文，优先先做承接。";
        }
        if (chosen.candidateType() == DirectorCandidateType.MAINLINE_ADVANCE) {
            return "当前章节已有明确主线锚点，优先选择主线推进候选。";
        }
        return "该候选与当前章节上下文最匹配。";
    }

    private String buildRejectionReason(StorySessionContextPacket contextPacket, DirectorCandidate candidate) {
        if (contextPacket.existingSceneStates().isEmpty() && candidate.candidateType() != DirectorCandidateType.OPENING) {
            return "当前仍处于首段起手，优先级低于开场定向候选。";
        }
        if (candidate.candidateType() == DirectorCandidateType.REVEAL) {
            return "当前更适合先铺出章节承接，再释放揭晓信息。";
        }
        return "当前轮次优先级低于已选候选。";
    }

    private List<String> buildRisks(StorySessionContextPacket contextPacket, DirectorCandidate chosen) {
        List<String> risks = new ArrayList<>();
        if (contextPacket.readerKnownState().knownFacts().isEmpty()) {
            risks.add("读者已知事实较少，必须避免把系统上下文默认成正文已揭晓信息。");
        }
        if (contextPacket.sceneBindingContext().fallbackUsed()) {
            risks.add("当前 sceneId 未命中真实 scene 状态，已回退到最近 scene，上下文承接仍可能偏粗。");
        }
        if (chosen.targetWords() != null && chosen.targetWords() > 1000) {
            risks.add("目标篇幅偏长，后续 writer 阶段需要严格控制收束。");
        }
        return List.copyOf(risks);
    }
}
