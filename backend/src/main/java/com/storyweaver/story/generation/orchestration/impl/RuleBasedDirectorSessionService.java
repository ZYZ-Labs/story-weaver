package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.ChapterSkeleton;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonPlanner;
import com.storyweaver.story.generation.orchestration.DirectorSessionService;
import com.storyweaver.story.generation.orchestration.SceneSkeletonItem;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.storyunit.session.DirectorCandidate;
import com.storyweaver.storyunit.session.DirectorCandidateType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Service
public class RuleBasedDirectorSessionService implements DirectorSessionService {

    private final ChapterSkeletonPlanner chapterSkeletonPlanner;

    public RuleBasedDirectorSessionService(ChapterSkeletonPlanner chapterSkeletonPlanner) {
        this.chapterSkeletonPlanner = chapterSkeletonPlanner;
    }

    @Override
    public List<DirectorCandidate> proposeCandidates(StorySessionContextPacket contextPacket) {
        Optional<ChapterSkeleton> skeleton = chapterSkeletonPlanner.plan(contextPacket.projectId(), contextPacket.chapterId());
        if (skeleton.isEmpty()) {
            return List.of();
        }

        List<SceneSkeletonItem> scenes = skeleton.get().scenes();
        for (int index = 0; index < scenes.size(); index++) {
            SceneSkeletonItem scene = scenes.get(index);
            if (!scene.sceneId().equals(contextPacket.sceneId())) {
                continue;
            }
            SceneSkeletonItem nextScene = index + 1 < scenes.size() ? scenes.get(index + 1) : null;
            return List.of(toCandidate(scene, nextScene, contextPacket));
        }
        return List.of();
    }

    private DirectorCandidate toCandidate(
            SceneSkeletonItem scene,
            SceneSkeletonItem nextScene,
            StorySessionContextPacket contextPacket) {
        List<String> mustUseAnchors = mergeUnique(buildBaseAnchors(contextPacket), scene.mustUseAnchors());
        return new DirectorCandidate(
                "skeleton-" + scene.sceneId(),
                resolveCandidateType(scene),
                scene.goal(),
                scene.readerReveal(),
                mustUseAnchors,
                buildForbiddenMoves(contextPacket, nextScene),
                scene.stopCondition(),
                scene.targetWords() == null ? 900 : scene.targetWords(),
                buildSceneReason(scene, contextPacket)
        );
    }

    private DirectorCandidateType resolveCandidateType(SceneSkeletonItem scene) {
        if (scene.sceneIndex() <= 1) {
            return DirectorCandidateType.OPENING;
        }
        if (!scene.readerReveal().isEmpty()) {
            return DirectorCandidateType.REVEAL;
        }
        return DirectorCandidateType.MAINLINE_ADVANCE;
    }

    private List<String> buildForbiddenMoves(StorySessionContextPacket contextPacket, SceneSkeletonItem nextScene) {
        List<String> moves = new ArrayList<>();
        moves.add("不要重复上一镜头已经完成的推进。");
        moves.add("不要越过当前镜头停点，抢写到下一镜头。");
        if (contextPacket.readerKnownState().knownFacts().isEmpty()) {
            moves.add("不要默认读者已经知道未在正文揭晓的信息。");
        }
        if (nextScene != null && StringUtils.hasText(nextScene.goal())) {
            moves.add("不要提前完成 " + nextScene.sceneId() + " 的核心推进：" + nextScene.goal());
        }
        return List.copyOf(moves);
    }

    private String buildSceneReason(SceneSkeletonItem scene, StorySessionContextPacket contextPacket) {
        if (StringUtils.hasText(scene.source())) {
            return "当前镜头来自已保存骨架：" + scene.source();
        }
        if (contextPacket.previousSceneHandoff() != null && StringUtils.hasText(contextPacket.previousSceneHandoff().outcomeSummary())) {
            return "当前镜头承接上一镜头 handoff：" + contextPacket.previousSceneHandoff().outcomeSummary();
        }
        return "当前镜头来自章节骨架规划。";
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

    private List<String> mergeUnique(List<String> left, List<String> right) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.addAll(left == null ? List.of() : left);
        values.addAll(right == null ? List.of() : right);
        return List.copyOf(values);
    }
}
