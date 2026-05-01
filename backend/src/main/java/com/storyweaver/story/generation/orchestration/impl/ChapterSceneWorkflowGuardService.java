package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.exception.SceneWorkflowConflictException;
import com.storyweaver.story.generation.orchestration.ChapterSkeleton;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonPlanner;
import com.storyweaver.story.generation.orchestration.SceneSkeletonItem;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ChapterSceneWorkflowGuardService {

    private final ChapterSkeletonPlanner chapterSkeletonPlanner;

    public ChapterSceneWorkflowGuardService(ChapterSkeletonPlanner chapterSkeletonPlanner) {
        this.chapterSkeletonPlanner = chapterSkeletonPlanner;
    }

    public SceneWorkflowState inspect(Long projectId, Long chapterId) {
        ChapterSkeleton skeleton = chapterSkeletonPlanner.plan(projectId, chapterId)
                .orElseThrow(() -> new SceneWorkflowConflictException("当前章节还没有镜头骨架，请先生成镜头骨架。"));
        List<SceneSkeletonItem> orderedScenes = skeleton.scenes().stream()
                .sorted(Comparator.comparingInt(SceneSkeletonItem::sceneIndex).thenComparing(SceneSkeletonItem::sceneId))
                .toList();
        if (orderedScenes.isEmpty()) {
            throw new SceneWorkflowConflictException("当前章节骨架为空，请先重新生成镜头骨架。");
        }

        SceneSkeletonItem unlockedScene = orderedScenes.stream()
                .filter(scene -> scene.status() != SceneExecutionStatus.COMPLETED)
                .findFirst()
                .orElse(null);
        List<String> completedSceneIds = orderedScenes.stream()
                .filter(scene -> scene.status() == SceneExecutionStatus.COMPLETED)
                .map(SceneSkeletonItem::sceneId)
                .toList();
        List<String> selectableSceneIds = orderedScenes.stream()
                .filter(scene -> scene.status() == SceneExecutionStatus.COMPLETED
                        || (unlockedScene != null && scene.sceneId().equals(unlockedScene.sceneId())))
                .map(SceneSkeletonItem::sceneId)
                .toList();
        List<String> lockedSceneIds = orderedScenes.stream()
                .map(SceneSkeletonItem::sceneId)
                .filter(sceneId -> !selectableSceneIds.contains(sceneId))
                .toList();

        return new SceneWorkflowState(
                projectId,
                chapterId,
                skeleton.skeletonId(),
                orderedScenes,
                unlockedScene == null ? "" : unlockedScene.sceneId(),
                completedSceneIds,
                selectableSceneIds,
                lockedSceneIds,
                unlockedScene == null
        );
    }

    public SceneWorkflowState assertSceneSelectable(Long projectId, Long chapterId, String sceneId) {
        SceneWorkflowState state = inspect(projectId, chapterId);
        if (!StringUtils.hasText(sceneId)) {
            throw new SceneWorkflowConflictException("当前镜头不能为空。");
        }
        String normalizedSceneId = sceneId.trim();
        if (state.selectableSceneIds().contains(normalizedSceneId)) {
            return state;
        }
        if (!state.containsScene(normalizedSceneId)) {
            throw new SceneWorkflowConflictException("镜头 " + normalizedSceneId + " 不存在于当前章节骨架中。");
        }
        throw new SceneWorkflowConflictException(lockMessage(normalizedSceneId, state.unlockedSceneId()));
    }

    public SceneWorkflowState assertCurrentUnlockedScene(Long projectId, Long chapterId, String sceneId, String actionLabel) {
        SceneWorkflowState state = inspect(projectId, chapterId);
        if (state.chapterCompleted()) {
            throw new SceneWorkflowConflictException("当前章节所有镜头都已接纳完成，无需继续" + actionLabel + "。");
        }
        if (!StringUtils.hasText(sceneId)) {
            throw new SceneWorkflowConflictException("当前只能处理第一个未接纳镜头 " + state.unlockedSceneId() + "。");
        }
        String normalizedSceneId = sceneId.trim();
        if (!state.containsScene(normalizedSceneId)) {
            throw new SceneWorkflowConflictException("镜头 " + normalizedSceneId + " 不存在于当前章节骨架中。");
        }
        if (normalizedSceneId.equals(state.unlockedSceneId())) {
            return state;
        }
        if (state.completedSceneIds().contains(normalizedSceneId)) {
            throw new SceneWorkflowConflictException("镜头 " + normalizedSceneId + " 已接纳完成，当前只能处理 " + state.unlockedSceneId() + "。");
        }
        throw new SceneWorkflowConflictException(lockMessage(normalizedSceneId, state.unlockedSceneId()));
    }

    public record SceneWorkflowState(
            Long projectId,
            Long chapterId,
            String skeletonId,
            List<SceneSkeletonItem> scenes,
            String unlockedSceneId,
            List<String> completedSceneIds,
            List<String> selectableSceneIds,
            List<String> lockedSceneIds,
            boolean chapterCompleted) {

        public SceneWorkflowState {
            skeletonId = skeletonId == null ? "" : skeletonId.trim();
            unlockedSceneId = unlockedSceneId == null ? "" : unlockedSceneId.trim();
            scenes = scenes == null ? List.of() : List.copyOf(scenes);
            completedSceneIds = completedSceneIds == null ? List.of() : List.copyOf(completedSceneIds);
            selectableSceneIds = selectableSceneIds == null ? List.of() : List.copyOf(selectableSceneIds);
            lockedSceneIds = lockedSceneIds == null ? List.of() : List.copyOf(lockedSceneIds);
        }

        public boolean containsScene(String sceneId) {
            return findScene(sceneId).isPresent();
        }

        public Optional<SceneSkeletonItem> findScene(String sceneId) {
            if (!StringUtils.hasText(sceneId)) {
                return Optional.empty();
            }
            String normalizedSceneId = sceneId.trim();
            return scenes.stream()
                    .filter(scene -> scene.sceneId().equals(normalizedSceneId))
                    .findFirst();
        }

        public Optional<SceneSkeletonItem> nextScene(String sceneId) {
            Optional<SceneSkeletonItem> current = findScene(sceneId);
            if (current.isEmpty()) {
                return Optional.empty();
            }
            int currentIndex = current.get().sceneIndex();
            return scenes.stream()
                    .filter(scene -> scene.sceneIndex() > currentIndex)
                    .min(Comparator.comparingInt(SceneSkeletonItem::sceneIndex).thenComparing(SceneSkeletonItem::sceneId));
        }
    }

    private String lockMessage(String requestedSceneId, String unlockedSceneId) {
        if (!StringUtils.hasText(unlockedSceneId)) {
            return "当前章节没有可继续处理的镜头。";
        }
        return "镜头 " + requestedSceneId + " 尚未解锁，请先接纳 " + unlockedSceneId + "。";
    }
}
