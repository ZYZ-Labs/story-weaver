package com.storyweaver.controller;

import com.storyweaver.exception.GlobalExceptionHandler;
import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;
import com.storyweaver.storyunit.facet.state.ChapterIncrementalState;
import com.storyweaver.storyunit.migration.LegacyBackfillActionPlan;
import com.storyweaver.storyunit.migration.LegacyBackfillDryRun;
import com.storyweaver.storyunit.migration.LegacyBackfillDryRunService;
import com.storyweaver.storyunit.migration.LegacyBackfillExecutionResult;
import com.storyweaver.storyunit.migration.LegacyBackfillExecutionService;
import com.storyweaver.storyunit.migration.LegacyBackfillAnalysisService;
import com.storyweaver.storyunit.migration.LegacyChapterBackfillAnalysis;
import com.storyweaver.storyunit.migration.LegacyProjectBackfillOverview;
import com.storyweaver.storyunit.migration.LegacyBackfillOverviewService;
import com.storyweaver.storyunit.migration.LegacyChapterBackfillStatusItem;
import com.storyweaver.storyunit.migration.MigrationCompatibilitySnapshot;
import com.storyweaver.storyunit.migration.MigrationCompatibilitySnapshotService;
import com.storyweaver.storyunit.migration.CompatibilityBoundaryItem;
import com.storyweaver.storyunit.migration.CompatibilityMode;
import com.storyweaver.storyunit.migration.CompatibilityScope;
import com.storyweaver.storyunit.event.StoryEvent;
import com.storyweaver.storyunit.event.StoryEventType;
import com.storyweaver.storyunit.model.StorySourceTrace;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.patch.PatchOperation;
import com.storyweaver.storyunit.patch.PatchOperationType;
import com.storyweaver.storyunit.patch.PatchStatus;
import com.storyweaver.storyunit.patch.StoryPatch;
import com.storyweaver.storyunit.service.ChapterIncrementalStateStore;
import com.storyweaver.storyunit.service.ReaderRevealStateStore;
import com.storyweaver.storyunit.service.StoryEventStore;
import com.storyweaver.storyunit.service.StoryPatchStore;
import com.storyweaver.storyunit.service.StorySnapshotStore;
import com.storyweaver.storyunit.snapshot.SnapshotScope;
import com.storyweaver.storyunit.snapshot.StorySnapshot;
import com.storyweaver.storyunit.model.FacetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StoryStateControllerTest {

    private MockMvc mockMvc;
    private StoryEventStore storyEventStore;
    private StorySnapshotStore storySnapshotStore;
    private StoryPatchStore storyPatchStore;
    private ReaderRevealStateStore readerRevealStateStore;
    private ChapterIncrementalStateStore chapterIncrementalStateStore;
    private LegacyBackfillAnalysisService legacyBackfillAnalysisService;
    private LegacyBackfillDryRunService legacyBackfillDryRunService;
    private LegacyBackfillExecutionService legacyBackfillExecutionService;
    private LegacyBackfillOverviewService legacyBackfillOverviewService;
    private MigrationCompatibilitySnapshotService migrationCompatibilitySnapshotService;

    @BeforeEach
    void setUp() {
        storyEventStore = mock(StoryEventStore.class);
        storySnapshotStore = mock(StorySnapshotStore.class);
        storyPatchStore = mock(StoryPatchStore.class);
        readerRevealStateStore = mock(ReaderRevealStateStore.class);
        chapterIncrementalStateStore = mock(ChapterIncrementalStateStore.class);
        legacyBackfillAnalysisService = mock(LegacyBackfillAnalysisService.class);
        legacyBackfillDryRunService = mock(LegacyBackfillDryRunService.class);
        legacyBackfillExecutionService = mock(LegacyBackfillExecutionService.class);
        legacyBackfillOverviewService = mock(LegacyBackfillOverviewService.class);
        migrationCompatibilitySnapshotService = mock(MigrationCompatibilitySnapshotService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new StoryStateController(
                        storyEventStore,
                        storySnapshotStore,
                        storyPatchStore,
                        readerRevealStateStore,
                        chapterIncrementalStateStore,
                        legacyBackfillAnalysisService,
                        legacyBackfillDryRunService,
                        legacyBackfillExecutionService,
                        legacyBackfillOverviewService,
                        migrationCompatibilitySnapshotService
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnUnauthorizedWhenAuthorizationHeaderMissing() throws Exception {
        mockMvc.perform(get("/api/story-state/projects/28/chapters/31/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturnChapterEvents() throws Exception {
        when(storyEventStore.listChapterEvents(28L, 31L)).thenReturn(List.of(
                new StoryEvent(
                        "event-1",
                        StoryEventType.SCENE_COMPLETED,
                        28L,
                        31L,
                        "scene-1",
                        new StoryUnitRef("scene-1", "scene-execution:31:scene-1", StoryUnitType.SCENE_EXECUTION),
                        "scene-1 已写回为 COMPLETED",
                        java.util.Map.of("status", "COMPLETED"),
                        new StorySourceTrace("test", "test", "SceneExecutionWriteService", "scene-1")
                )
        ));

        mockMvc.perform(get("/api/story-state/projects/28/chapters/31/events")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].eventId").value("event-1"))
                .andExpect(jsonPath("$.data[0].eventType").value("SCENE_COMPLETED"));
    }

    @Test
    void shouldReturnChapterSnapshots() throws Exception {
        when(storySnapshotStore.listChapterSnapshots(28L, 31L)).thenReturn(List.of(
                new StorySnapshot(
                        "snapshot-1",
                        SnapshotScope.SCENE,
                        28L,
                        31L,
                        "scene-1",
                        List.of(new StoryUnitRef("scene-1", "scene-execution:31:scene-1", StoryUnitType.SCENE_EXECUTION)),
                        "scene-1 snapshot",
                        new StorySourceTrace("test", "test", "SceneExecutionWriteService", "scene-1")
                )
        ));

        mockMvc.perform(get("/api/story-state/projects/28/chapters/31/snapshots")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].snapshotId").value("snapshot-1"))
                .andExpect(jsonPath("$.data[0].scope").value("SCENE"));
    }

    @Test
    void shouldReturnChapterPatches() throws Exception {
        when(storyPatchStore.listChapterPatches(28L, 31L)).thenReturn(List.of(
                new StoryPatch(
                        "patch-1",
                        new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER),
                        FacetType.REVEAL,
                        List.of(new PatchOperation(PatchOperationType.MERGE, "/readerKnown", List.of("主角决定赴约"))),
                        "scene-2 的 reveal patch",
                        PatchStatus.APPLIED,
                        new StorySourceTrace("test", "test", "SceneExecutionWriteService", "scene-2")
                )
        ));

        mockMvc.perform(get("/api/story-state/projects/28/chapters/31/patches")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].patchId").value("patch-1"))
                .andExpect(jsonPath("$.data[0].facetType").value("REVEAL"));
    }

    @Test
    void shouldReturnReaderRevealState() throws Exception {
        when(readerRevealStateStore.findChapterRevealState(28L, 31L)).thenReturn(Optional.of(
                new ReaderRevealState(
                        28L,
                        31L,
                        List.of("系统已知"),
                        List.of("作者已知"),
                        List.of("主角决定赴约"),
                        List.of("战队邀约背后的真实目的"),
                        "读者已知 1 条，未揭晓 1 条"
                )
        ));

        mockMvc.perform(get("/api/story-state/projects/28/chapters/31/reader-reveal-state")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.readerKnown[0]").value("主角决定赴约"))
                .andExpect(jsonPath("$.data.unrevealed[0]").value("战队邀约背后的真实目的"));
    }

    @Test
    void shouldReturnChapterState() throws Exception {
        when(chapterIncrementalStateStore.findChapterState(28L, 31L)).thenReturn(Optional.of(
                new ChapterIncrementalState(
                        28L,
                        31L,
                        List.of("scene:scene-2:pending"),
                        List.of("scene:scene-1:pending"),
                        List.of("办公室"),
                        java.util.Map.of("林沉舟", "紧张"),
                        java.util.Map.of("林沉舟", "谨慎"),
                        java.util.Map.of("林沉舟", List.of("观察中")),
                        "scene-1 已写回章节状态"
                )
        ));

        mockMvc.perform(get("/api/story-state/projects/28/chapters/31/chapter-state")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.openLoops[0]").value("scene:scene-2:pending"))
                .andExpect(jsonPath("$.data.activeLocations[0]").value("办公室"))
                .andExpect(jsonPath("$.data.characterEmotions.林沉舟").value("紧张"));
    }

    @Test
    void shouldReturnChapterBackfillAnalysis() throws Exception {
        when(legacyBackfillAnalysisService.analyzeChapter(28L, 31L)).thenReturn(Optional.of(
                new LegacyChapterBackfillAnalysis(
                        28L,
                        31L,
                        "退役者的邀请函",
                        true,
                        true,
                        5,
                        4,
                        3,
                        1,
                        4,
                        2,
                        1,
                        1,
                        0,
                        0,
                        0,
                        false,
                        false,
                        true,
                        true,
                        List.of("旧记录尚未形成 StoryEvent 基线。")
                )
        ));

        mockMvc.perform(get("/api/story-state/projects/28/chapters/31/backfill-analysis")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.chapterTitle").value("退役者的邀请函"))
                .andExpect(jsonPath("$.data.legacyRecordCount").value(5))
                .andExpect(jsonPath("$.data.needsSceneBackfill").value(true))
                .andExpect(jsonPath("$.data.notes[0]").value("旧记录尚未形成 StoryEvent 基线。"));
    }

    @Test
    void shouldReturnChapterBackfillDryRun() throws Exception {
        when(legacyBackfillDryRunService.planChapterBackfill(28L, 31L)).thenReturn(Optional.of(
                new LegacyBackfillDryRun(
                        new LegacyChapterBackfillAnalysis(
                                28L, 31L, "退役者的邀请函",
                                true, true,
                                5, 4, 3, 1,
                                4, 2, 1, 1,
                                0, 0, 0,
                                false, false,
                                true, true,
                                List.of("旧记录尚未形成 StoryEvent 基线。")
                        ),
                        true,
                        List.of(new LegacyBackfillActionPlan(
                                "derive-scene-state",
                                "补齐 scene 执行基线",
                                "将旧正文记录映射为 SceneExecutionState，并补齐 runtime/event/snapshot 基线。",
                                true,
                                false,
                                ""
                        )),
                        List.of("当前章节已经存在 runtime-only scene，回填时必须避免覆盖新状态。")
                )
        ));

        mockMvc.perform(get("/api/story-state/projects/28/chapters/31/backfill-dry-run")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.canRunBackfill").value(true))
                .andExpect(jsonPath("$.data.actions[0].actionKey").value("derive-scene-state"))
                .andExpect(jsonPath("$.data.riskNotes[0]").value("当前章节已经存在 runtime-only scene，回填时必须避免覆盖新状态。"));
    }

    @Test
    void shouldExecuteChapterBackfill() throws Exception {
        LegacyBackfillDryRun dryRun = new LegacyBackfillDryRun(
                new LegacyChapterBackfillAnalysis(
                        28L, 31L, "退役者的邀请函",
                        true, true,
                        5, 4, 3, 1,
                        4, 2, 1, 1,
                        0, 0, 0,
                        false, false,
                        true, true,
                        List.of()
                ),
                true,
                List.of(new LegacyBackfillActionPlan(
                        "derive-scene-state",
                        "补齐 scene 执行基线",
                        "将旧正文记录映射为 SceneExecutionState，并补齐 runtime/event/snapshot 基线。",
                        true,
                        false,
                        ""
                )),
                List.of()
        );
        when(legacyBackfillExecutionService.executeChapterBackfill(28L, 31L)).thenReturn(Optional.of(
                new LegacyBackfillExecutionResult(
                        dryRun,
                        true,
                        2,
                        3,
                        2,
                        true,
                        true,
                        List.of("backfill:event:28:31:scene-1"),
                        List.of("reader-reveal-state:28:31"),
                        List.of("当前章节已经存在 runtime-only scene，回填时必须避免覆盖新状态。")
                )
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/story-state/projects/28/chapters/31/backfill-execute")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.executed").value(true))
                .andExpect(jsonPath("$.data.createdEventCount").value(2))
                .andExpect(jsonPath("$.data.writtenKeys[0]").value("backfill:event:28:31:scene-1"));
    }

    @Test
    void shouldReturnCompatibilitySnapshot() throws Exception {
        when(migrationCompatibilitySnapshotService.getChapterSnapshot(28L, 31L)).thenReturn(Optional.of(
                new MigrationCompatibilitySnapshot(
                        28L,
                        31L,
                        "退役者的邀请函",
                        List.of(new CompatibilityBoundaryItem(
                                "chapter-workspace",
                                CompatibilityScope.PAGE,
                                "章节工作区",
                                CompatibilityMode.NEW_PRIMARY,
                                "章节工作区 / 创作台",
                                "旧写作中心",
                                true,
                                List.of("镜头执行走新工作区。")
                        )),
                        List.of(new CompatibilityBoundaryItem(
                                "story-context",
                                CompatibilityScope.API,
                                "story-context",
                                CompatibilityMode.DUAL_READ,
                                "State Server",
                                "legacy chapter read model",
                                true,
                                List.of("上下文仍允许兼容回退。")
                        )),
                        List.of(new CompatibilityBoundaryItem(
                                "chapter-summary-content",
                                CompatibilityScope.DATA,
                                "章节摘要 / 正文",
                                CompatibilityMode.LEGACY_PRIMARY,
                                "Chapter.summary / Chapter.content",
                                "",
                                true,
                                List.of("正文仍是旧真源。")
                        )),
                        List.of("legacyWritingCenterEnabled=true"),
                        List.of("当前章节仍缺 scene 基线。")
                )
        ));

        mockMvc.perform(get("/api/story-state/projects/28/chapters/31/compatibility-snapshot")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pageBoundaries[0].boundaryKey").value("chapter-workspace"))
                .andExpect(jsonPath("$.data.apiBoundaries[0].mode").value("DUAL_READ"))
                .andExpect(jsonPath("$.data.featureFlags[0]").value("legacyWritingCenterEnabled=true"));
    }

    @Test
    void shouldReturnProjectBackfillOverview() throws Exception {
        when(legacyBackfillOverviewService.buildProjectOverview(28L)).thenReturn(Optional.of(
                new LegacyProjectBackfillOverview(
                        28L,
                        4,
                        3,
                        2,
                        1,
                        2,
                        List.of(new LegacyChapterBackfillStatusItem(
                                31L,
                                "退役者的邀请函",
                                4,
                                true,
                                true,
                                true,
                                List.of("当前章节仍缺 scene 基线。")
                        ))
                )
        ));

        mockMvc.perform(get("/api/story-state/projects/28/backfill-overview")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalChapters").value(4))
                .andExpect(jsonPath("$.data.chapters[0].chapterTitle").value("退役者的邀请函"))
                .andExpect(jsonPath("$.data.chapters[0].canRunBackfill").value(true));
    }
}
