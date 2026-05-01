package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.ChapterSkeleton;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonPlanner;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonStore;
import com.storyweaver.story.generation.orchestration.SceneSkeletonItem;
import com.storyweaver.story.generation.orchestration.StorySessionContextAssembler;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.storyunit.session.SceneExecutionState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class RuleBasedChapterSkeletonPlanner implements ChapterSkeletonPlanner {

    private final StorySessionContextAssembler storySessionContextAssembler;
    private final ChapterSkeletonStore chapterSkeletonStore;

    public RuleBasedChapterSkeletonPlanner(
            StorySessionContextAssembler storySessionContextAssembler,
            ChapterSkeletonStore chapterSkeletonStore) {
        this.storySessionContextAssembler = storySessionContextAssembler;
        this.chapterSkeletonStore = chapterSkeletonStore;
    }

    @Override
    public Optional<ChapterSkeleton> plan(Long projectId, Long chapterId) {
        Optional<ChapterSkeleton> storedSkeleton = chapterSkeletonStore.find(projectId, chapterId);
        if (storedSkeleton.isEmpty()) {
            return Optional.empty();
        }

        Optional<StorySessionContextPacket> contextPacket = storySessionContextAssembler.assemble(projectId, chapterId, "scene-1");
        if (contextPacket.isEmpty()) {
            return storedSkeleton;
        }

        ChapterSkeleton mergedSkeleton = mergeStoredSkeleton(contextPacket.get(), storedSkeleton.get());
        return Optional.of(new ChapterSkeleton(
                projectId,
                chapterId,
                mergedSkeleton.skeletonId(),
                mergedSkeleton.scenes().size(),
                mergedSkeleton.globalStopCondition(),
                mergedSkeleton.scenes(),
                mergedSkeleton.deletedSceneIds(),
                appendPlanningNote(mergedSkeleton.planningNotes(), "当前章节骨架已同步 runtime scene 状态。")
        ));
    }

    private ChapterSkeleton mergeStoredSkeleton(StorySessionContextPacket context, ChapterSkeleton storedSkeleton) {
        Set<String> deletedSceneIds = new LinkedHashSet<>(storedSkeleton.deletedSceneIds());
        Map<String, SceneExecutionState> stateBySceneId = new LinkedHashMap<>();
        context.existingSceneStates().forEach(state -> {
            if (!deletedSceneIds.contains(state.sceneId())) {
                stateBySceneId.put(state.sceneId(), state);
            }
        });

        List<SceneSkeletonItem> mergedScenes = new ArrayList<>();
        for (SceneSkeletonItem scene : storedSkeleton.scenes()) {
            if (deletedSceneIds.contains(scene.sceneId())) {
                continue;
            }
            SceneExecutionState state = stateBySceneId.remove(scene.sceneId());
            mergedScenes.add(mergeScene(scene, state, context));
        }

        for (SceneExecutionState state : stateBySceneId.values()) {
            mergedScenes.add(new SceneSkeletonItem(
                    state.sceneId(),
                    state.sceneIndex(),
                    state.status(),
                    firstNonBlank(state.goal(), state.outcomeSummary()),
                    state.readerRevealDelta(),
                    baseAnchors(context),
                    state.stopCondition(),
                    null,
                    "existing-scene-state"
            ));
        }

        List<SceneSkeletonItem> orderedScenes = mergedScenes.stream()
                .sorted(java.util.Comparator.comparingInt(SceneSkeletonItem::sceneIndex).thenComparing(SceneSkeletonItem::sceneId))
                .toList();
        String globalStopCondition = orderedScenes.isEmpty()
                ? fallbackStopCondition(context)
                : firstNonBlank(storedSkeleton.globalStopCondition(), orderedScenes.get(orderedScenes.size() - 1).stopCondition(), fallbackStopCondition(context));
        return new ChapterSkeleton(
                storedSkeleton.projectId(),
                storedSkeleton.chapterId(),
                storedSkeleton.skeletonId(),
                orderedScenes.size(),
                globalStopCondition,
                orderedScenes,
                List.copyOf(deletedSceneIds),
                storedSkeleton.planningNotes()
        );
    }

    private SceneSkeletonItem mergeScene(SceneSkeletonItem scene, SceneExecutionState state, StorySessionContextPacket context) {
        if (state == null) {
            return new SceneSkeletonItem(
                    scene.sceneId(),
                    scene.sceneIndex(),
                    scene.status(),
                    scene.goal(),
                    scene.readerReveal(),
                    scene.mustUseAnchors().isEmpty() ? baseAnchors(context) : scene.mustUseAnchors(),
                    firstNonBlank(scene.stopCondition(), fallbackStopCondition(context)),
                    scene.targetWords(),
                    scene.source()
            );
        }
        return new SceneSkeletonItem(
                scene.sceneId(),
                scene.sceneIndex(),
                state.status(),
                firstNonBlank(scene.goal(), state.goal(), state.outcomeSummary()),
                scene.readerReveal().isEmpty() ? state.readerRevealDelta() : scene.readerReveal(),
                scene.mustUseAnchors().isEmpty() ? baseAnchors(context) : scene.mustUseAnchors(),
                firstNonBlank(scene.stopCondition(), state.stopCondition(), fallbackStopCondition(context)),
                scene.targetWords(),
                scene.source()
        );
    }

    private List<String> appendPlanningNote(List<String> notes, String note) {
        List<String> result = new ArrayList<>(notes == null ? List.of() : notes);
        result.add(note);
        return List.copyOf(result);
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
