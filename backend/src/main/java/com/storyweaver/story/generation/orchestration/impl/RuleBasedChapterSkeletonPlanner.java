package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.ChapterSkeleton;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonPlanner;
import com.storyweaver.story.generation.orchestration.DirectorSessionService;
import com.storyweaver.story.generation.orchestration.SceneSkeletonItem;
import com.storyweaver.story.generation.orchestration.StorySessionContextAssembler;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.story.generation.orchestration.SelectorSessionService;
import com.storyweaver.storyunit.session.DirectorCandidate;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import com.storyweaver.storyunit.session.SelectionDecision;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class RuleBasedChapterSkeletonPlanner implements ChapterSkeletonPlanner {

    private static final int MIN_SCENE_COUNT = 3;
    private static final int MAX_SCENE_COUNT = 5;

    private final StorySessionContextAssembler storySessionContextAssembler;
    private final DirectorSessionService directorSessionService;
    private final SelectorSessionService selectorSessionService;

    public RuleBasedChapterSkeletonPlanner(
            StorySessionContextAssembler storySessionContextAssembler,
            DirectorSessionService directorSessionService,
            SelectorSessionService selectorSessionService) {
        this.storySessionContextAssembler = storySessionContextAssembler;
        this.directorSessionService = directorSessionService;
        this.selectorSessionService = selectorSessionService;
    }

    @Override
    public Optional<ChapterSkeleton> plan(Long projectId, Long chapterId) {
        Optional<StorySessionContextPacket> contextPacket = storySessionContextAssembler.assemble(projectId, chapterId, "scene-1");
        if (contextPacket.isEmpty()) {
            return Optional.empty();
        }

        StorySessionContextPacket context = contextPacket.get();
        List<DirectorCandidate> candidates = directorSessionService.proposeCandidates(context);
        SelectionDecision selectionDecision = candidates.isEmpty()
                ? new SelectionDecision("", "", List.of(), List.of())
                : selectorSessionService.selectCandidate(context, candidates);

        List<SceneSkeletonItem> scenes = new ArrayList<>();
        for (SceneExecutionState state : context.existingSceneStates()) {
            scenes.add(new SceneSkeletonItem(
                    state.sceneId(),
                    state.sceneIndex(),
                    state.status(),
                    firstNonBlank(state.goal(), state.outcomeSummary()),
                    state.readerRevealDelta(),
                    List.of(),
                    state.stopCondition(),
                    null,
                    "existing-scene-state"
            ));
        }

        int targetSceneCount = resolveTargetSceneCount(context.existingSceneStates().size(), candidates.size());
        List<DirectorCandidate> orderedCandidates = orderCandidates(candidates, selectionDecision.chosenCandidateId());
        while (scenes.size() < targetSceneCount) {
            int plannedIndex = scenes.size() - context.existingSceneStates().size();
            DirectorCandidate candidate = pickCandidateForIndex(orderedCandidates, plannedIndex);
            int sceneIndex = scenes.size() + 1;
            scenes.add(new SceneSkeletonItem(
                    "scene-" + sceneIndex,
                    sceneIndex,
                    SceneExecutionStatus.PLANNED,
                    candidate == null ? buildFallbackGoal(context, sceneIndex) : candidate.goal(),
                    candidate == null ? List.of() : candidate.readerReveal(),
                    candidate == null ? baseAnchors(context) : candidate.mustUseAnchors(),
                    candidate == null ? fallbackStopCondition(context) : candidate.stopCondition(),
                    candidate == null ? 800 : candidate.targetWords(),
                    candidate == null ? "fallback-planner" : "director-candidate"
            ));
        }

        List<String> planningNotes = buildPlanningNotes(context, selectionDecision, candidates, scenes);
        String globalStopCondition = scenes.isEmpty()
                ? fallbackStopCondition(context)
                : firstNonBlank(scenes.get(scenes.size() - 1).stopCondition(), fallbackStopCondition(context));

        return Optional.of(new ChapterSkeleton(
                projectId,
                chapterId,
                "skeleton_" + chapterId + "_v1",
                scenes.size(),
                globalStopCondition,
                scenes,
                planningNotes
        ));
    }

    private int resolveTargetSceneCount(int existingSceneCount, int candidateCount) {
        int baseline = Math.max(existingSceneCount + 1, Math.min(MAX_SCENE_COUNT, Math.max(MIN_SCENE_COUNT, candidateCount)));
        return Math.max(MIN_SCENE_COUNT, Math.min(MAX_SCENE_COUNT, baseline));
    }

    private List<DirectorCandidate> orderCandidates(List<DirectorCandidate> candidates, String chosenCandidateId) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<DirectorCandidate> ordered = new ArrayList<>();
        Set<String> added = new LinkedHashSet<>();
        if (chosenCandidateId != null && !chosenCandidateId.isBlank()) {
            candidates.stream()
                    .filter(candidate -> chosenCandidateId.equals(candidate.candidateId()))
                    .findFirst()
                    .ifPresent(candidate -> {
                        ordered.add(candidate);
                        added.add(candidate.candidateId());
                    });
        }
        for (DirectorCandidate candidate : candidates) {
            if (added.add(candidate.candidateId())) {
                ordered.add(candidate);
            }
        }
        return List.copyOf(ordered);
    }

    private DirectorCandidate pickCandidateForIndex(List<DirectorCandidate> orderedCandidates, int index) {
        if (orderedCandidates.isEmpty()) {
            return null;
        }
        if (index < orderedCandidates.size()) {
            return orderedCandidates.get(index);
        }
        return orderedCandidates.get(orderedCandidates.size() - 1);
    }

    private List<String> buildPlanningNotes(
            StorySessionContextPacket context,
            SelectionDecision selectionDecision,
            List<DirectorCandidate> candidates,
            List<SceneSkeletonItem> scenes) {
        List<String> notes = new ArrayList<>();
        if (context.sceneBindingContext().fallbackUsed()) {
            notes.add("当前章节骨架基于最近 scene 上下文回退生成，首个未执行镜头应优先做承接。");
        }
        if (context.existingSceneStates().isEmpty()) {
            notes.add("当前章节暂无已执行镜头，骨架按冷启动三镜头生成。");
        } else {
            notes.add("当前章节已有 " + context.existingSceneStates().size() + " 个兼容型 scene 状态，骨架会保留既有执行结果。");
        }
        if (selectionDecision.chosenCandidateId() != null && !selectionDecision.chosenCandidateId().isBlank()) {
            notes.add("首个未执行镜头优先采用已选候选：" + selectionDecision.chosenCandidateId());
        }
        if (candidates.isEmpty()) {
            notes.add("当前未产生 director candidates，后续镜头使用摘要和锚点做兜底骨架。");
        }
        notes.add("当前章节骨架规划为 " + scenes.size() + " 个镜头。");
        return List.copyOf(notes);
    }

    private List<String> baseAnchors(StorySessionContextPacket context) {
        List<String> anchors = new ArrayList<>();
        if (!context.chapterAnchorBundle().chapterTitle().isBlank()) {
            anchors.add("chapter=" + context.chapterAnchorBundle().chapterTitle());
        }
        if (!context.chapterAnchorBundle().mainPovCharacterName().isBlank()) {
            anchors.add("pov=" + context.chapterAnchorBundle().mainPovCharacterName());
        }
        if (!context.chapterAnchorBundle().chapterSummary().isBlank()) {
            anchors.add("summary=" + context.chapterAnchorBundle().chapterSummary());
        }
        return List.copyOf(anchors);
    }

    private String fallbackStopCondition(StorySessionContextPacket context) {
        return firstNonBlank(
                context.chapterSummary().summary(),
                context.chapterAnchorBundle().chapterSummary(),
                "完成当前章节目标的一次明确落点后停住。"
        );
    }

    private String buildFallbackGoal(StorySessionContextPacket context, int sceneIndex) {
        return firstNonBlank(
                context.chapterAnchorBundle().chapterSummary(),
                context.chapterSummary().summary(),
                "围绕当前章节目标推进镜头 " + sceneIndex + "。"
        );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
