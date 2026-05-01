package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.ChapterSkeleton;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonPlanner;
import com.storyweaver.story.generation.orchestration.SceneSkeletonItem;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.story.generation.orchestration.WriterExecutionBriefBuilder;
import com.storyweaver.storyunit.session.DirectorCandidate;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneContinuityState;
import com.storyweaver.storyunit.session.SceneHandoffSnapshot;
import com.storyweaver.storyunit.session.WriterExecutionBrief;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DefaultWriterExecutionBriefBuilder implements WriterExecutionBriefBuilder {

    private final ChapterSkeletonPlanner chapterSkeletonPlanner;

    public DefaultWriterExecutionBriefBuilder(ChapterSkeletonPlanner chapterSkeletonPlanner) {
        this.chapterSkeletonPlanner = chapterSkeletonPlanner;
    }

    @Override
    public WriterExecutionBrief build(StorySessionContextPacket contextPacket, DirectorCandidate candidate) {
        Optional<ChapterSkeleton> skeleton = chapterSkeletonPlanner.plan(contextPacket.projectId(), contextPacket.chapterId());
        SceneSkeletonItem nextScene = resolveNextScene(skeleton.orElse(null), contextPacket.sceneId());
        List<String> continuityNotes = new ArrayList<>();
        SceneExecutionState resolvedSceneState = contextPacket.sceneBindingContext().resolvedSceneState();
        SceneHandoffSnapshot previousSceneHandoff = contextPacket.previousSceneHandoff();
        SceneContinuityState continuityState = SceneContinuitySupport.resolveContinuityState(
                previousSceneHandoff,
                resolvedSceneState,
                contextPacket.existingSceneStates(),
                nextScene == null ? "" : nextScene.sceneId(),
                nextScene == null ? "" : nextScene.goal(),
                candidate.stopCondition()
        );
        String previousSceneSummary = firstNonBlank(
                continuityState.summary(),
                previousSceneHandoff == null ? "" : previousSceneHandoff.outcomeSummary(),
                resolvedSceneState == null ? "" : resolvedSceneState.outcomeSummary(),
                contextPacket.existingSceneStates().isEmpty() ? "" : contextPacket.existingSceneStates().getLast().outcomeSummary()
        );
        if (!contextPacket.readerKnownState().knownFacts().isEmpty()) {
            continuityNotes.add("读者已知：" + contextPacket.readerKnownState().knownFacts().getFirst());
        }
        if (!contextPacket.recentStoryProgress().items().isEmpty()) {
            continuityNotes.add("最近进度：" + contextPacket.recentStoryProgress().items().getFirst().summary());
        }
        continuityNotes.addAll(SceneContinuitySupport.buildConstraintLines(continuityState));
        if (contextPacket.sceneBindingContext().fallbackUsed()) {
            continuityNotes.add("scene 绑定提示：" + contextPacket.sceneBindingContext().summary());
        }
        if (nextScene != null && StringUtils.hasText(nextScene.goal())) {
            continuityNotes.add("下一镜头入口：" + nextScene.sceneId() + " 将转入 " + nextScene.goal());
        }
        String handoffLine = StringUtils.hasText(continuityState.handoffLine())
                ? continuityState.handoffLine()
                : (previousSceneHandoff != null && !previousSceneHandoff.handoffLine().isBlank()
                ? previousSceneHandoff.handoffLine()
                : (resolvedSceneState != null ? resolvedSceneState.handoffLine()
                : (contextPacket.existingSceneStates().isEmpty() ? "" : contextPacket.existingSceneStates().getLast().handoffLine())));

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
                previousSceneSummary,
                handoffLine,
                nextScene == null ? "" : nextScene.sceneId(),
                nextScene == null ? "" : nextScene.goal(),
                continuityState
        );
    }

    private SceneSkeletonItem resolveNextScene(ChapterSkeleton skeleton, String sceneId) {
        if (skeleton == null || !StringUtils.hasText(sceneId)) {
            return null;
        }
        List<SceneSkeletonItem> scenes = skeleton.scenes();
        for (int index = 0; index < scenes.size(); index++) {
            SceneSkeletonItem scene = scenes.get(index);
            if (!scene.sceneId().equals(sceneId.trim())) {
                continue;
            }
            return index + 1 < scenes.size() ? scenes.get(index + 1) : null;
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }
}
