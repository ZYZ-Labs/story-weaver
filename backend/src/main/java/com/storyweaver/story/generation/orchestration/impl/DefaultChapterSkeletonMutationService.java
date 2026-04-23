package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.ChapterSkeleton;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonMutationService;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonPlanner;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonStore;
import com.storyweaver.story.generation.orchestration.SceneSkeletonItem;
import com.storyweaver.story.generation.orchestration.SceneSkeletonMutationCommand;
import com.storyweaver.storyunit.service.SceneRuntimeStateStore;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DefaultChapterSkeletonMutationService implements ChapterSkeletonMutationService {

    private final ChapterSkeletonPlanner chapterSkeletonPlanner;
    private final ChapterSkeletonStore chapterSkeletonStore;
    private final SceneRuntimeStateStore sceneRuntimeStateStore;

    public DefaultChapterSkeletonMutationService(
            ChapterSkeletonPlanner chapterSkeletonPlanner,
            ChapterSkeletonStore chapterSkeletonStore,
            SceneRuntimeStateStore sceneRuntimeStateStore) {
        this.chapterSkeletonPlanner = chapterSkeletonPlanner;
        this.chapterSkeletonStore = chapterSkeletonStore;
        this.sceneRuntimeStateStore = sceneRuntimeStateStore;
    }

    @Override
    public Optional<ChapterSkeleton> updateScene(Long projectId, Long chapterId, SceneSkeletonMutationCommand command) {
        if (!StringUtils.hasText(command.sceneId())) {
            throw new IllegalArgumentException("sceneId 不能为空");
        }
        Optional<ChapterSkeleton> optionalSkeleton = chapterSkeletonPlanner.plan(projectId, chapterId);
        if (optionalSkeleton.isEmpty()) {
            return Optional.empty();
        }
        ChapterSkeleton chapterSkeleton = optionalSkeleton.get();
        List<SceneSkeletonItem> scenes = new ArrayList<>();
        boolean updated = false;
        for (SceneSkeletonItem scene : chapterSkeleton.scenes()) {
            if (!scene.sceneId().equals(command.sceneId())) {
                scenes.add(scene);
                continue;
            }
            if (scene.status() == SceneExecutionStatus.COMPLETED) {
                throw new IllegalStateException("已完成镜头不允许直接修改骨架字段；如需重做，请先删除镜头后重新规划。");
            }
            scenes.add(new SceneSkeletonItem(
                    scene.sceneId(),
                    scene.sceneIndex(),
                    scene.status(),
                    firstNonBlank(command.goal(), scene.goal()),
                    command.readerReveal().isEmpty() ? scene.readerReveal() : command.readerReveal(),
                    command.mustUseAnchors().isEmpty() ? scene.mustUseAnchors() : command.mustUseAnchors(),
                    firstNonBlank(command.stopCondition(), scene.stopCondition()),
                    command.targetWords() == null ? scene.targetWords() : command.targetWords(),
                    "manual-override"
            ));
            updated = true;
        }
        if (!updated) {
            return Optional.empty();
        }
        ChapterSkeleton next = new ChapterSkeleton(
                chapterSkeleton.projectId(),
                chapterSkeleton.chapterId(),
                chapterSkeleton.skeletonId(),
                scenes.size(),
                chapterSkeleton.globalStopCondition(),
                scenes,
                chapterSkeleton.deletedSceneIds(),
                appendPlanningNote(chapterSkeleton.planningNotes(), "已对 " + command.sceneId() + " 应用手动骨架修改。")
        );
        return Optional.of(chapterSkeletonStore.save(next));
    }

    @Override
    public Optional<ChapterSkeleton> deleteScene(Long projectId, Long chapterId, String sceneId) {
        if (!StringUtils.hasText(sceneId)) {
            throw new IllegalArgumentException("sceneId 不能为空");
        }
        Optional<ChapterSkeleton> optionalSkeleton = chapterSkeletonPlanner.plan(projectId, chapterId);
        if (optionalSkeleton.isEmpty()) {
            return Optional.empty();
        }
        ChapterSkeleton chapterSkeleton = optionalSkeleton.get();
        if (chapterSkeleton.scenes().size() <= 1) {
            throw new IllegalStateException("章节骨架至少保留一个镜头");
        }
        List<SceneSkeletonItem> scenes = new ArrayList<>();
        boolean deleted = false;
        for (SceneSkeletonItem scene : chapterSkeleton.scenes()) {
            if (!scene.sceneId().equals(sceneId)) {
                scenes.add(scene);
                continue;
            }
            deleted = true;
        }
        if (!deleted) {
            return Optional.empty();
        }
        sceneRuntimeStateStore.deleteSceneState(projectId, chapterId, sceneId);
        sceneRuntimeStateStore.deleteHandoffsReferencingScene(projectId, chapterId, sceneId);
        List<String> deletedSceneIds = new ArrayList<>(chapterSkeleton.deletedSceneIds());
        if (!deletedSceneIds.contains(sceneId)) {
            deletedSceneIds.add(sceneId);
        }
        ChapterSkeleton next = new ChapterSkeleton(
                chapterSkeleton.projectId(),
                chapterSkeleton.chapterId(),
                chapterSkeleton.skeletonId(),
                scenes.size(),
                resolveGlobalStopCondition(chapterSkeleton, scenes),
                scenes,
                List.copyOf(deletedSceneIds),
                appendPlanningNote(chapterSkeleton.planningNotes(), "已删除 " + sceneId + "，并清理其 runtime/handoff 状态。")
        );
        return Optional.of(chapterSkeletonStore.save(next));
    }

    private List<String> appendPlanningNote(List<String> notes, String note) {
        List<String> result = new ArrayList<>(notes == null ? List.of() : notes);
        result.add(note);
        return List.copyOf(result);
    }

    private String resolveGlobalStopCondition(ChapterSkeleton chapterSkeleton, List<SceneSkeletonItem> scenes) {
        if (!scenes.isEmpty() && StringUtils.hasText(scenes.get(scenes.size() - 1).stopCondition())) {
            return scenes.get(scenes.size() - 1).stopCondition();
        }
        return chapterSkeleton.globalStopCondition();
    }

    private String firstNonBlank(String primary, String fallback) {
        if (StringUtils.hasText(primary)) {
            return primary.trim();
        }
        return fallback == null ? "" : fallback.trim();
    }
}
