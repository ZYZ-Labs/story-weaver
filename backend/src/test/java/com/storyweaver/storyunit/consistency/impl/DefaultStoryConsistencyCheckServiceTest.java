package com.storyweaver.storyunit.consistency.impl;

import com.storyweaver.story.generation.orchestration.ChapterExecutionReview;
import com.storyweaver.story.generation.orchestration.ChapterExecutionReviewService;
import com.storyweaver.story.generation.orchestration.ChapterTraceSummary;
import com.storyweaver.storyunit.consistency.StoryConsistencyCheck;
import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;
import com.storyweaver.storyunit.facet.state.ChapterIncrementalState;
import com.storyweaver.storyunit.service.ChapterIncrementalStateStore;
import com.storyweaver.storyunit.service.ReaderRevealStateStore;
import com.storyweaver.storyunit.service.SceneRuntimeStateStore;
import com.storyweaver.storyunit.service.StoryEventStore;
import com.storyweaver.storyunit.service.StoryPatchStore;
import com.storyweaver.storyunit.service.StorySnapshotStore;
import com.storyweaver.storyunit.session.ReviewIssue;
import com.storyweaver.storyunit.session.ReviewResult;
import com.storyweaver.storyunit.session.ReviewSeverity;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultStoryConsistencyCheckServiceTest {

    @Test
    void shouldBuildConsistencyCheckWithReviewIssues() {
        SceneRuntimeStateStore sceneRuntimeStateStore = mock(SceneRuntimeStateStore.class);
        StoryEventStore storyEventStore = mock(StoryEventStore.class);
        StorySnapshotStore storySnapshotStore = mock(StorySnapshotStore.class);
        StoryPatchStore storyPatchStore = mock(StoryPatchStore.class);
        ReaderRevealStateStore readerRevealStateStore = mock(ReaderRevealStateStore.class);
        ChapterIncrementalStateStore chapterIncrementalStateStore = mock(ChapterIncrementalStateStore.class);
        ChapterExecutionReviewService chapterExecutionReviewService = mock(ChapterExecutionReviewService.class);

        when(sceneRuntimeStateStore.listChapterScenes(28L, 32L)).thenReturn(List.of(
                new SceneExecutionState(28L, 32L, "scene-1", 1, SceneExecutionStatus.COMPLETED, "c1", "目标", "停点", List.of(), List.of(), List.of(), Map.of(), "handoff", "完成")
        ));
        when(sceneRuntimeStateStore.listChapterHandoffs(28L, 32L)).thenReturn(List.of());
        when(storyEventStore.listChapterEvents(28L, 32L)).thenReturn(List.of());
        when(storySnapshotStore.listChapterSnapshots(28L, 32L)).thenReturn(List.of());
        when(storyPatchStore.listChapterPatches(28L, 32L)).thenReturn(List.of());
        when(readerRevealStateStore.findChapterRevealState(28L, 32L)).thenReturn(Optional.of(
                new ReaderRevealState(28L, 32L, List.of(), List.of(), List.of("已知"), List.of("未揭晓"), "summary")
        ));
        when(chapterIncrementalStateStore.findChapterState(28L, 32L)).thenReturn(Optional.of(
                new ChapterIncrementalState(28L, 32L, List.of("loop"), List.of(), List.of(), Map.of(), Map.of(), Map.of(), "state")
        ));
        when(chapterExecutionReviewService.review(28L, 32L)).thenReturn(Optional.of(
                new ChapterExecutionReview(
                        28L,
                        32L,
                        ReviewResult.REVISE,
                        "章节还未收口",
                        List.of(new ReviewIssue("handoff_missing", "缺少 handoff", ReviewSeverity.ERROR, false)),
                        false,
                        new ChapterTraceSummary(
                                28L,
                                32L,
                                "skeleton-32",
                                5,
                                1,
                                1,
                                0,
                                0,
                                4,
                                "scene-1",
                                List.of("scene-1"),
                                List.of("scene-2"),
                                List.of("scene-2")
                        )
                )
        ));

        DefaultStoryConsistencyCheckService service = new DefaultStoryConsistencyCheckService(
                sceneRuntimeStateStore,
                storyEventStore,
                storySnapshotStore,
                storyPatchStore,
                readerRevealStateStore,
                chapterIncrementalStateStore,
                chapterExecutionReviewService
        );

        StoryConsistencyCheck check = service.checkChapter(28L, 32L).orElseThrow();
        assertEquals(1, check.sceneCount());
        assertEquals(1, check.completedSceneCount());
        assertTrue(check.chapterReviewPresent());
        assertEquals("REVISE", check.chapterReviewResult());
        assertTrue(check.issues().stream().anyMatch(issue -> issue.issueKey().equals("missing_events")));
        assertTrue(check.issues().stream().anyMatch(issue -> issue.issueKey().equals("chapter_review:handoff_missing")));
    }
}
