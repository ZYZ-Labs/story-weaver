package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.ChapterExecutionReview;
import com.storyweaver.story.generation.orchestration.ChapterExecutionReviewService;
import com.storyweaver.story.generation.orchestration.ChapterSkeleton;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonPlanner;
import com.storyweaver.story.generation.orchestration.ChapterTraceSummary;
import com.storyweaver.story.generation.orchestration.SceneSkeletonItem;
import com.storyweaver.storyunit.service.SceneExecutionStateQueryService;
import com.storyweaver.storyunit.service.SceneRuntimeStateStore;
import com.storyweaver.storyunit.session.ReviewIssue;
import com.storyweaver.storyunit.session.ReviewResult;
import com.storyweaver.storyunit.session.ReviewSeverity;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RuleBasedChapterExecutionReviewService implements ChapterExecutionReviewService {

    private final ChapterSkeletonPlanner chapterSkeletonPlanner;
    private final SceneExecutionStateQueryService sceneExecutionStateQueryService;
    private final SceneRuntimeStateStore sceneRuntimeStateStore;

    public RuleBasedChapterExecutionReviewService(
            ChapterSkeletonPlanner chapterSkeletonPlanner,
            SceneExecutionStateQueryService sceneExecutionStateQueryService,
            SceneRuntimeStateStore sceneRuntimeStateStore) {
        this.chapterSkeletonPlanner = chapterSkeletonPlanner;
        this.sceneExecutionStateQueryService = sceneExecutionStateQueryService;
        this.sceneRuntimeStateStore = sceneRuntimeStateStore;
    }

    @Override
    public Optional<ChapterExecutionReview> review(Long projectId, Long chapterId) {
        return chapterSkeletonPlanner.plan(projectId, chapterId)
                .map(skeleton -> buildReview(projectId, chapterId, skeleton));
    }

    private ChapterExecutionReview buildReview(Long projectId, Long chapterId, ChapterSkeleton skeleton) {
        List<SceneExecutionState> sceneStates = sceneExecutionStateQueryService.listChapterScenes(projectId, chapterId);
        Map<String, SceneExecutionState> stateBySceneId = new LinkedHashMap<>();
        sceneStates.forEach(state -> stateBySceneId.put(state.sceneId(), state));

        List<String> executedSceneIds = new ArrayList<>();
        List<String> pendingSceneIds = new ArrayList<>();
        List<String> missingHandoffToSceneIds = new ArrayList<>();
        List<ReviewIssue> issues = new ArrayList<>();

        int completedSceneCount = 0;
        int reviewingSceneCount = 0;
        int failedSceneCount = 0;

        List<SceneSkeletonItem> scenes = skeleton.scenes();
        for (int index = 0; index < scenes.size(); index++) {
            SceneSkeletonItem item = scenes.get(index);
            SceneExecutionState state = stateBySceneId.get(item.sceneId());
            if (state == null) {
                pendingSceneIds.add(item.sceneId());
            } else {
                executedSceneIds.add(item.sceneId());
                switch (state.status()) {
                    case COMPLETED -> completedSceneCount++;
                    case REVIEWING, WRITING, WRITTEN -> {
                        reviewingSceneCount++;
                        pendingSceneIds.add(item.sceneId());
                    }
                    case FAILED, BLOCKED -> {
                        failedSceneCount++;
                        pendingSceneIds.add(item.sceneId());
                    }
                    default -> pendingSceneIds.add(item.sceneId());
                }
            }

            if (index == 0) {
                continue;
            }
            String sceneId = item.sceneId();
            String previousSceneId = scenes.get(index - 1).sceneId();
            SceneExecutionState previousScene = stateBySceneId.get(previousSceneId);
            if (previousScene != null
                    && previousScene.status() == SceneExecutionStatus.COMPLETED
                    && sceneRuntimeStateStore.findHandoffToScene(projectId, chapterId, sceneId).isEmpty()) {
                missingHandoffToSceneIds.add(sceneId);
            }
        }

        if (failedSceneCount > 0) {
            issues.add(new ReviewIssue(
                    "scene_failed",
                    "当前章节存在执行失败的镜头，需要先修复失败镜头再继续章节级收口。",
                    ReviewSeverity.ERROR,
                    false
            ));
        }
        if (!pendingSceneIds.isEmpty()) {
            issues.add(new ReviewIssue(
                    "scene_pending",
                    "当前章节仍有未执行或未完成的镜头：" + String.join(", ", pendingSceneIds),
                    ReviewSeverity.WARNING,
                    false
            ));
        }
        if (!missingHandoffToSceneIds.isEmpty()) {
            issues.add(new ReviewIssue(
                    "handoff_missing",
                    "当前章节存在已完成镜头但缺少下一镜头 handoff 的场景：" + String.join(", ", missingHandoffToSceneIds),
                    ReviewSeverity.WARNING,
                    true
            ));
        }

        boolean chapterExecutionComplete = completedSceneCount == scenes.size() && failedSceneCount == 0;
        ReviewResult result = issues.stream().anyMatch(issue -> issue.severity() == ReviewSeverity.ERROR || issue.severity() == ReviewSeverity.BLOCKER)
                ? ReviewResult.REVISE
                : (chapterExecutionComplete ? ReviewResult.PASS : ReviewResult.REVISE);

        ChapterTraceSummary traceSummary = new ChapterTraceSummary(
                projectId,
                chapterId,
                skeleton.skeletonId(),
                skeleton.sceneCount(),
                executedSceneIds.size(),
                completedSceneCount,
                reviewingSceneCount,
                failedSceneCount,
                pendingSceneIds.size(),
                sceneStates.isEmpty() ? "" : sceneStates.get(sceneStates.size() - 1).sceneId(),
                List.copyOf(executedSceneIds),
                List.copyOf(pendingSceneIds),
                List.copyOf(missingHandoffToSceneIds)
        );

        String summary = result == ReviewResult.PASS
                ? "章节级审校通过，当前镜头执行链与 handoff 状态完整。"
                : "章节级审校发现仍有未完成镜头或 handoff 缺口。";

        return new ChapterExecutionReview(
                projectId,
                chapterId,
                result,
                summary,
                List.copyOf(issues),
                chapterExecutionComplete,
                traceSummary
        );
    }
}
