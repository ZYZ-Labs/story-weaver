package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.DirectorSessionService;
import com.storyweaver.story.generation.orchestration.SceneBindingMode;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.storyunit.session.DirectorCandidate;
import com.storyweaver.storyunit.session.DirectorCandidateType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RuleBasedDirectorSessionService implements DirectorSessionService {

    @Override
    public List<DirectorCandidate> proposeCandidates(StorySessionContextPacket contextPacket) {
        List<DirectorCandidate> candidates = new ArrayList<>();
        boolean firstScene = contextPacket.sceneBindingContext().mode() == SceneBindingMode.CHAPTER_COLD_START
                || contextPacket.existingSceneStates().isEmpty();
        String chapterTitle = contextPacket.chapterAnchorBundle().chapterTitle();
        String chapterSummary = contextPacket.chapterAnchorBundle().chapterSummary();
        String povName = contextPacket.chapterAnchorBundle().mainPovCharacterName();
        String firstPlot = contextPacket.chapterAnchorBundle().activePlotTitles().stream().findFirst().orElse("");
        String firstUnrevealed = contextPacket.readerKnownState().unrevealedFacts().stream().findFirst().orElse(chapterSummary);
        List<String> baseAnchors = buildBaseAnchors(contextPacket);

        candidates.add(new DirectorCandidate(
                "opening-" + contextPacket.chapterId(),
                firstScene ? DirectorCandidateType.OPENING : DirectorCandidateType.TRANSITION,
                "先让读者理解" + povName + "在《" + chapterTitle + "》中的当前状态，并建立本段进入点。",
                List.of(firstUnrevealed),
                baseAnchors,
                List.of("不要默认读者知道未在正文揭晓的信息", "不要越过当前章节目标快速推进到后续章节"),
                firstScene ? "完成开场定向并抛出本章触发点后停住。" : "完成与上一段的自然衔接后停住。",
                900,
                firstScene
                        ? "当前章节尚无 scene state，优先建立开场与读者定向。"
                        : (contextPacket.sceneBindingContext().fallbackUsed()
                        ? "当前 scene 已回退到最近 scene 上下文，优先做承接。"
                        : "已绑定请求的 scene state，优先做承接。")
        ));

        candidates.add(new DirectorCandidate(
                "mainline-" + contextPacket.chapterId(),
                DirectorCandidateType.MAINLINE_ADVANCE,
                firstPlot.isBlank() ? "围绕本章摘要推进当前主线。" : "围绕“" + firstPlot + "”推进当前主线。",
                chapterSummary.isBlank() ? List.of(firstUnrevealed) : List.of(chapterSummary),
                baseAnchors,
                List.of("不要引入与本章无关的新冲突", "不要让配角抢走 POV"),
                "完成当前主线推进的一次明确落点后停住。",
                1100,
                "当前章节已有明确主线锚点，适合作为默认推进候选。"
        ));

        candidates.add(new DirectorCandidate(
                "reveal-" + contextPacket.chapterId(),
                DirectorCandidateType.REVEAL,
                "以较小篇幅释放本章最关键的一条未揭晓信息，并控制节奏。",
                List.of(firstUnrevealed),
                baseAnchors,
                List.of("不要一次性揭穿所有信息", "不要抢跑到完整真相"),
                "揭晓一条关键信息并留下后续空间后停住。",
                800,
                "读者已知状态里存在明确未揭晓信息，适合生成揭晓候选。"
        ));

        return candidates;
    }

    private List<String> buildBaseAnchors(StorySessionContextPacket contextPacket) {
        List<String> anchors = new ArrayList<>();
        if (!contextPacket.chapterAnchorBundle().chapterTitle().isBlank()) {
            anchors.add("chapter=" + contextPacket.chapterAnchorBundle().chapterTitle());
        }
        if (!contextPacket.chapterAnchorBundle().outlineTitle().isBlank()) {
            anchors.add("outline=" + contextPacket.chapterAnchorBundle().outlineTitle());
        }
        if (!contextPacket.chapterAnchorBundle().mainPovCharacterName().isBlank()) {
            anchors.add("pov=" + contextPacket.chapterAnchorBundle().mainPovCharacterName());
        }
        if (!contextPacket.chapterAnchorBundle().chapterSummary().isBlank()) {
            anchors.add("summary=" + contextPacket.chapterAnchorBundle().chapterSummary());
        }
        return List.copyOf(anchors);
    }
}
