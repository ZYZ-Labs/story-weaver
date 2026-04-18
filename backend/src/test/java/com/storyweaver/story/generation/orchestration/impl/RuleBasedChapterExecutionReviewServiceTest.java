package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.ChapterExecutionReview;
import com.storyweaver.story.generation.orchestration.ChapterSkeleton;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonPlanner;
import com.storyweaver.story.generation.orchestration.SceneSkeletonItem;
import com.storyweaver.storyunit.service.SceneExecutionStateQueryService;
import com.storyweaver.storyunit.service.SceneRuntimeStateStore;
import com.storyweaver.storyunit.session.ReviewResult;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import com.storyweaver.storyunit.session.SceneHandoffSnapshot;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleBasedChapterExecutionReviewServiceTest {

    @Test
    void shouldReturnPassWhenScenesAndHandoffsAreComplete() {
        ChapterSkeletonPlanner chapterSkeletonPlanner = mock(ChapterSkeletonPlanner.class);
        SceneExecutionStateQueryService sceneExecutionStateQueryService = mock(SceneExecutionStateQueryService.class);
        SceneRuntimeStateStore sceneRuntimeStateStore = mock(SceneRuntimeStateStore.class);

        ChapterSkeleton skeleton = new ChapterSkeleton(
                28L,
                31L,
                "skeleton_31_v1",
                2,
                "完成章节收口。",
                List.of(
                        new SceneSkeletonItem("scene-1", 1, SceneExecutionStatus.COMPLETED, "开场", List.of(), List.of(), "停在 scene-1", 800, "c1"),
                        new SceneSkeletonItem("scene-2", 2, SceneExecutionStatus.COMPLETED, "承接", List.of(), List.of(), "停在 scene-2", 900, "c2")
                ),
                List.of()
        );

        when(chapterSkeletonPlanner.plan(28L, 31L)).thenReturn(Optional.of(skeleton));
        when(sceneExecutionStateQueryService.listChapterScenes(28L, 31L)).thenReturn(List.of(
                new SceneExecutionState(28L, 31L, "scene-1", 1, SceneExecutionStatus.COMPLETED, "c1", "开场", "停在 scene-1", List.of(), List.of(), List.of(), java.util.Map.of(), "handoff-1", "scene-1 已完成"),
                new SceneExecutionState(28L, 31L, "scene-2", 2, SceneExecutionStatus.COMPLETED, "c2", "承接", "停在 scene-2", List.of(), List.of(), List.of(), java.util.Map.of(), "handoff-2", "scene-2 已完成")
        ));
        when(sceneRuntimeStateStore.findHandoffToScene(28L, 31L, "scene-2")).thenReturn(Optional.of(
                new SceneHandoffSnapshot(28L, 31L, "scene-1", "scene-2", "handoff-1", "scene-1 已完成", List.of(), List.of(), List.of(), java.util.Map.of(), "PASS", "规则审校通过。", LocalDateTime.of(2026, 4, 18, 12, 0))
        ));

        RuleBasedChapterExecutionReviewService service = new RuleBasedChapterExecutionReviewService(
                chapterSkeletonPlanner,
                sceneExecutionStateQueryService,
                sceneRuntimeStateStore
        );

        ChapterExecutionReview review = service.review(28L, 31L).orElseThrow();

        assertEquals(ReviewResult.PASS, review.result());
        assertTrue(review.chapterExecutionComplete());
        assertEquals(2, review.traceSummary().completedSceneCount());
        assertTrue(review.traceSummary().missingHandoffToSceneIds().isEmpty());
    }

    @Test
    void shouldReturnReviseWhenPendingScenesOrMissingHandoffExist() {
        ChapterSkeletonPlanner chapterSkeletonPlanner = mock(ChapterSkeletonPlanner.class);
        SceneExecutionStateQueryService sceneExecutionStateQueryService = mock(SceneExecutionStateQueryService.class);
        SceneRuntimeStateStore sceneRuntimeStateStore = mock(SceneRuntimeStateStore.class);

        ChapterSkeleton skeleton = new ChapterSkeleton(
                28L,
                31L,
                "skeleton_31_v2",
                3,
                "继续执行。",
                List.of(
                        new SceneSkeletonItem("scene-1", 1, SceneExecutionStatus.COMPLETED, "开场", List.of(), List.of(), "停在 scene-1", 800, "c1"),
                        new SceneSkeletonItem("scene-2", 2, SceneExecutionStatus.COMPLETED, "承接", List.of(), List.of(), "停在 scene-2", 900, "c2"),
                        new SceneSkeletonItem("scene-3", 3, SceneExecutionStatus.PLANNED, "收口", List.of(), List.of(), "停在 scene-3", 1000, "c3")
                ),
                List.of()
        );

        when(chapterSkeletonPlanner.plan(28L, 31L)).thenReturn(Optional.of(skeleton));
        when(sceneExecutionStateQueryService.listChapterScenes(28L, 31L)).thenReturn(List.of(
                new SceneExecutionState(28L, 31L, "scene-1", 1, SceneExecutionStatus.COMPLETED, "c1", "开场", "停在 scene-1", List.of(), List.of(), List.of(), java.util.Map.of(), "handoff-1", "scene-1 已完成"),
                new SceneExecutionState(28L, 31L, "scene-2", 2, SceneExecutionStatus.COMPLETED, "c2", "承接", "停在 scene-2", List.of(), List.of(), List.of(), java.util.Map.of(), "handoff-2", "scene-2 已完成")
        ));
        when(sceneRuntimeStateStore.findHandoffToScene(28L, 31L, "scene-2")).thenReturn(Optional.of(
                new SceneHandoffSnapshot(28L, 31L, "scene-1", "scene-2", "handoff-1", "scene-1 已完成", List.of(), List.of(), List.of(), java.util.Map.of(), "PASS", "规则审校通过。", LocalDateTime.of(2026, 4, 18, 12, 0))
        ));
        when(sceneRuntimeStateStore.findHandoffToScene(28L, 31L, "scene-3")).thenReturn(Optional.empty());

        RuleBasedChapterExecutionReviewService service = new RuleBasedChapterExecutionReviewService(
                chapterSkeletonPlanner,
                sceneExecutionStateQueryService,
                sceneRuntimeStateStore
        );

        ChapterExecutionReview review = service.review(28L, 31L).orElseThrow();

        assertEquals(ReviewResult.REVISE, review.result());
        assertEquals(1, review.traceSummary().pendingSceneCount());
        assertEquals(List.of("scene-3"), review.traceSummary().pendingSceneIds());
        assertEquals(List.of("scene-3"), review.traceSummary().missingHandoffToSceneIds());
    }
}
