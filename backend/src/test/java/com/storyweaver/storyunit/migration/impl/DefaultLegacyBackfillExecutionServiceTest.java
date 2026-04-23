package com.storyweaver.storyunit.migration.impl;

import com.storyweaver.config.StoryCompatibilityProperties;
import com.storyweaver.storyunit.event.StoryEvent;
import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;
import com.storyweaver.storyunit.facet.state.ChapterIncrementalState;
import com.storyweaver.storyunit.migration.LegacyBackfillActionPlan;
import com.storyweaver.storyunit.migration.LegacyBackfillDryRun;
import com.storyweaver.storyunit.migration.LegacyBackfillDryRunService;
import com.storyweaver.storyunit.migration.LegacyBackfillExecutionResult;
import com.storyweaver.storyunit.migration.LegacyChapterBackfillAnalysis;
import com.storyweaver.storyunit.patch.StoryPatch;
import com.storyweaver.storyunit.service.ChapterIncrementalStateStore;
import com.storyweaver.storyunit.service.ReaderRevealStateStore;
import com.storyweaver.storyunit.service.SceneExecutionStateQueryService;
import com.storyweaver.storyunit.service.StoryEventStore;
import com.storyweaver.storyunit.service.StoryPatchStore;
import com.storyweaver.storyunit.service.StorySnapshotStore;
import com.storyweaver.storyunit.snapshot.StorySnapshot;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultLegacyBackfillExecutionServiceTest {

    @Test
    void shouldReturnNonExecutedWhenDryRunCannotRun() {
        LegacyBackfillDryRunService dryRunService = mock(LegacyBackfillDryRunService.class);
        when(dryRunService.planChapterBackfill(28L, 31L)).thenReturn(Optional.of(
                new LegacyBackfillDryRun(
                        new LegacyChapterBackfillAnalysis(28L, 31L, "退役者的邀请函", true, false,
                                1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                false, false, false, false, List.of()),
                        false,
                        List.of(),
                        List.of()
                )
        ));
        StoryCompatibilityProperties properties = new StoryCompatibilityProperties();

        DefaultLegacyBackfillExecutionService service = new DefaultLegacyBackfillExecutionService(
                dryRunService,
                mock(SceneExecutionStateQueryService.class),
                mock(StoryEventStore.class),
                mock(StorySnapshotStore.class),
                mock(StoryPatchStore.class),
                mock(ReaderRevealStateStore.class),
                mock(ChapterIncrementalStateStore.class),
                properties
        );

        LegacyBackfillExecutionResult result = service.executeChapterBackfill(28L, 31L).orElseThrow();
        assertFalse(result.executed());
        assertEquals(0, result.createdEventCount());
    }

    @Test
    void shouldBackfillMissingBaselinesIdempotently() {
        LegacyBackfillDryRunService dryRunService = mock(LegacyBackfillDryRunService.class);
        SceneExecutionStateQueryService sceneQueryService = mock(SceneExecutionStateQueryService.class);
        StoryEventStore eventStore = mock(StoryEventStore.class);
        StorySnapshotStore snapshotStore = mock(StorySnapshotStore.class);
        StoryPatchStore patchStore = mock(StoryPatchStore.class);
        ReaderRevealStateStore revealStateStore = mock(ReaderRevealStateStore.class);
        ChapterIncrementalStateStore chapterStateStore = mock(ChapterIncrementalStateStore.class);

        when(dryRunService.planChapterBackfill(28L, 31L)).thenReturn(Optional.of(
                new LegacyBackfillDryRun(
                        new LegacyChapterBackfillAnalysis(28L, 31L, "退役者的邀请函", true, true,
                                3, 2, 1, 1, 2, 1, 1, 0, 0, 0, 0,
                                false, false, true, true, List.of()),
                        true,
                        List.of(new LegacyBackfillActionPlan("derive-scene-state", "补齐 scene 执行基线", "desc", true, false, "")),
                        List.of()
                )
        ));
        when(sceneQueryService.listChapterScenes(28L, 31L)).thenReturn(List.of(
                new SceneExecutionState(28L, 31L, "scene-1", 1, SceneExecutionStatus.COMPLETED, "opening-31",
                        "开场", "", List.of("主角收到邀请"), List.of("scene:scene-2:pending"), List.of(), Map.of("source", "aiWritingRecord"), "", "完成"),
                new SceneExecutionState(28L, 31L, "scene-2", 2, SceneExecutionStatus.FAILED, "transition-31",
                        "犹豫", "", List.of(), List.of(), List.of("scene:scene-1:pending"), Map.of("source", "aiWritingRecord"), "", "失败")
        ));
        when(eventStore.listChapterEvents(28L, 31L)).thenReturn(List.of());
        when(snapshotStore.listChapterSnapshots(28L, 31L)).thenReturn(List.of());
        when(patchStore.listChapterPatches(28L, 31L)).thenReturn(List.of());
        when(revealStateStore.findChapterRevealState(28L, 31L)).thenReturn(Optional.empty());
        when(chapterStateStore.findChapterState(28L, 31L)).thenReturn(Optional.empty());
        when(eventStore.appendEvent(any(StoryEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(snapshotStore.saveSnapshot(any(StorySnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(patchStore.appendPatch(any(), any(), any(StoryPatch.class))).thenAnswer(invocation -> invocation.getArgument(2));
        when(revealStateStore.saveChapterRevealState(any(ReaderRevealState.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chapterStateStore.saveChapterState(any(ChapterIncrementalState.class))).thenAnswer(invocation -> invocation.getArgument(0));
        StoryCompatibilityProperties properties = new StoryCompatibilityProperties();

        DefaultLegacyBackfillExecutionService service = new DefaultLegacyBackfillExecutionService(
                dryRunService,
                sceneQueryService,
                eventStore,
                snapshotStore,
                patchStore,
                revealStateStore,
                chapterStateStore,
                properties
        );

        LegacyBackfillExecutionResult result = service.executeChapterBackfill(28L, 31L).orElseThrow();

        assertTrue(result.executed());
        assertEquals(2, result.createdEventCount());
        assertEquals(3, result.createdSnapshotCount());
        assertEquals(2, result.createdPatchCount());
        assertTrue(result.createdReaderRevealState());
        assertTrue(result.createdChapterState());
        assertTrue(result.writtenKeys().contains("backfill:event:28:31:scene-1"));
        assertTrue(result.warnings().isEmpty());
        verify(eventStore, org.mockito.Mockito.times(2)).appendEvent(any(StoryEvent.class));
        verify(snapshotStore, org.mockito.Mockito.times(3)).saveSnapshot(any(StorySnapshot.class));
        verify(patchStore, org.mockito.Mockito.times(2)).appendPatch(any(), any(), any(StoryPatch.class));
        verify(revealStateStore).saveChapterRevealState(any(ReaderRevealState.class));
        verify(chapterStateStore).saveChapterState(any(ChapterIncrementalState.class));
    }

    @Test
    void shouldRespectBackfillExecutionFeatureFlag() {
        LegacyBackfillDryRunService dryRunService = mock(LegacyBackfillDryRunService.class);
        when(dryRunService.planChapterBackfill(28L, 31L)).thenReturn(Optional.of(
                new LegacyBackfillDryRun(
                        new LegacyChapterBackfillAnalysis(28L, 31L, "退役者的邀请函", true, true,
                                3, 2, 1, 1, 2, 1, 1, 0, 0, 0, 0,
                                false, false, true, true, List.of()),
                        true,
                        List.of(new LegacyBackfillActionPlan("derive-scene-state", "补齐 scene 执行基线", "desc", true, false, "")),
                        List.of()
                )
        ));
        StoryCompatibilityProperties properties = new StoryCompatibilityProperties();
        properties.setBackfillExecuteEnabled(false);

        DefaultLegacyBackfillExecutionService service = new DefaultLegacyBackfillExecutionService(
                dryRunService,
                mock(SceneExecutionStateQueryService.class),
                mock(StoryEventStore.class),
                mock(StorySnapshotStore.class),
                mock(StoryPatchStore.class),
                mock(ReaderRevealStateStore.class),
                mock(ChapterIncrementalStateStore.class),
                properties
        );

        LegacyBackfillExecutionResult result = service.executeChapterBackfill(28L, 31L).orElseThrow();

        assertFalse(result.executed());
        assertTrue(result.warnings().contains("兼容回填开关已关闭，当前仅允许查看分析和 dry-run。"));
    }
}
