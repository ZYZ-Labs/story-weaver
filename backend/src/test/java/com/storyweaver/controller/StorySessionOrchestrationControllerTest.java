package com.storyweaver.controller;

import com.storyweaver.exception.GlobalExceptionHandler;
import com.storyweaver.config.StoryCompatibilityProperties;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonMutationService;
import com.storyweaver.story.generation.orchestration.ChapterExecutionReview;
import com.storyweaver.story.generation.orchestration.ChapterExecutionReviewService;
import com.storyweaver.story.generation.orchestration.ChapterNodeRuntimeService;
import com.storyweaver.story.generation.orchestration.ChapterNodeRuntimeView;
import com.storyweaver.story.generation.orchestration.ChapterNodeSkeleton;
import com.storyweaver.story.generation.orchestration.ChapterSkeleton;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonGenerationService;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonPlanner;
import com.storyweaver.story.generation.orchestration.ChapterTraceSummary;
import com.storyweaver.story.generation.orchestration.NodeActionOption;
import com.storyweaver.story.generation.orchestration.NodeActionRequest;
import com.storyweaver.story.generation.orchestration.NodeResolutionResult;
import com.storyweaver.story.generation.orchestration.SessionExecutionTrace;
import com.storyweaver.story.generation.orchestration.SessionExecutionTraceItem;
import com.storyweaver.story.generation.orchestration.SceneBindingContext;
import com.storyweaver.story.generation.orchestration.SceneBindingMode;
import com.storyweaver.story.generation.orchestration.SceneExecutionRequest;
import com.storyweaver.story.generation.orchestration.SceneExecutionWriteResult;
import com.storyweaver.story.generation.orchestration.SceneSkeletonItem;
import com.storyweaver.story.generation.orchestration.SessionTraceStatus;
import com.storyweaver.story.generation.orchestration.StorySessionExecution;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.story.generation.orchestration.StorySessionOrchestrator;
import com.storyweaver.story.generation.orchestration.StoryNodeSkeletonItem;
import com.storyweaver.story.generation.orchestration.StorySessionPreview;
import com.storyweaver.story.generation.orchestration.WriterSessionResult;
import com.storyweaver.story.generation.orchestration.impl.ChapterSceneWorkflowGuardService;
import com.storyweaver.storyunit.context.ChapterAnchorBundleView;
import com.storyweaver.storyunit.context.ProjectBriefView;
import com.storyweaver.storyunit.context.ReaderKnownStateView;
import com.storyweaver.storyunit.context.RecentStoryProgressView;
import com.storyweaver.storyunit.context.StoryUnitSummaryView;
import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;
import com.storyweaver.storyunit.facet.state.ChapterIncrementalState;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.patch.PatchOperation;
import com.storyweaver.storyunit.patch.PatchOperationType;
import com.storyweaver.storyunit.patch.PatchStatus;
import com.storyweaver.storyunit.patch.StoryPatch;
import com.storyweaver.storyunit.session.DirectorCandidate;
import com.storyweaver.storyunit.session.DirectorCandidateType;
import com.storyweaver.storyunit.session.ReviewDecision;
import com.storyweaver.storyunit.session.ReviewResult;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import com.storyweaver.storyunit.session.SceneHandoffSnapshot;
import com.storyweaver.storyunit.session.SelectionDecision;
import com.storyweaver.storyunit.session.SessionRole;
import com.storyweaver.storyunit.session.WriterExecutionBrief;
import com.storyweaver.storyunit.event.StoryEvent;
import com.storyweaver.storyunit.event.StoryEventType;
import com.storyweaver.storyunit.model.StorySourceTrace;
import com.storyweaver.storyunit.snapshot.SnapshotScope;
import com.storyweaver.storyunit.snapshot.StorySnapshot;
import com.storyweaver.storyunit.runtime.StoryActionIntent;
import com.storyweaver.storyunit.runtime.StoryLoopStatus;
import com.storyweaver.storyunit.runtime.StoryNodeCheckpoint;
import com.storyweaver.storyunit.runtime.StoryOpenLoop;
import com.storyweaver.storyunit.runtime.StoryResolvedTurn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StorySessionOrchestrationControllerTest {

    private MockMvc mockMvc;
    private StorySessionOrchestrator storySessionOrchestrator;
    private ChapterSkeletonPlanner chapterSkeletonPlanner;
    private ChapterSkeletonGenerationService chapterSkeletonGenerationService;
    private ChapterExecutionReviewService chapterExecutionReviewService;
    private ChapterSkeletonMutationService chapterSkeletonMutationService;
    private ChapterNodeRuntimeService chapterNodeRuntimeService;
    private ChapterSceneWorkflowGuardService chapterSceneWorkflowGuardService;
    private StoryCompatibilityProperties storyCompatibilityProperties;

    @BeforeEach
    void setUp() {
        storySessionOrchestrator = mock(StorySessionOrchestrator.class);
        chapterSkeletonPlanner = mock(ChapterSkeletonPlanner.class);
        chapterSkeletonGenerationService = mock(ChapterSkeletonGenerationService.class);
        chapterExecutionReviewService = mock(ChapterExecutionReviewService.class);
        chapterSkeletonMutationService = mock(ChapterSkeletonMutationService.class);
        chapterNodeRuntimeService = mock(ChapterNodeRuntimeService.class);
        chapterSceneWorkflowGuardService = mock(ChapterSceneWorkflowGuardService.class);
        storyCompatibilityProperties = new StoryCompatibilityProperties();
        storyCompatibilityProperties.setChapterWorkspaceNodePreviewEnabled(true);
        storyCompatibilityProperties.setChapterWorkspaceNodeResolveEnabled(true);
        mockMvc = MockMvcBuilders.standaloneSetup(new StorySessionOrchestrationController(
                        storySessionOrchestrator,
                        chapterSkeletonPlanner,
                        chapterSkeletonGenerationService,
                        chapterExecutionReviewService,
                        chapterSkeletonMutationService,
                        chapterNodeRuntimeService,
                        chapterSceneWorkflowGuardService,
                        storyCompatibilityProperties))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnUnauthorizedWhenAuthorizationHeaderMissing() throws Exception {
        mockMvc.perform(get("/api/story-orchestration/projects/28/chapters/31/preview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturnPreviewWhenAvailable() throws Exception {
        StorySessionPreview preview = samplePreview();
        when(storySessionOrchestrator.preview(28L, 31L, "scene-1")).thenReturn(Optional.of(preview));

        mockMvc.perform(get("/api/story-orchestration/projects/28/chapters/31/preview")
                        .param("sceneId", "scene-1")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.selectionDecision.chosenCandidateId").value("opening-31"))
                .andExpect(jsonPath("$.data.reviewDecision.result").value("PASS"))
                .andExpect(jsonPath("$.data.trace.items[0].role").value("ORCHESTRATOR"))
                .andExpect(jsonPath("$.data.trace.items[0].attempt").value(1))
                .andExpect(jsonPath("$.data.trace.items[1].role").value("DIRECTOR"));
    }

    @Test
    void shouldReturnExecutionWhenAvailable() throws Exception {
        StorySessionPreview preview = samplePreview();
        StorySessionExecution execution = new StorySessionExecution(
                preview,
                new SceneExecutionWriteResult(
                        new SceneExecutionState(28L, 31L, "scene-1", 1, SceneExecutionStatus.COMPLETED, "opening-31",
                                "先做开场定向", "完成触发点后停住。", List.of("待揭晓"), List.of(), List.of(),
                                java.util.Map.of("source", "test"), "林沉舟推开门。", "主角完成现实状态定向。"),
                        new SceneHandoffSnapshot(28L, 31L, "scene-1", "scene-2", "林沉舟推开门。", "主角完成现实状态定向。",
                                List.of("待揭晓"), List.of(), List.of(), java.util.Map.of("source", "test"), "PASS", "规则审校通过。", java.time.LocalDateTime.of(2026, 4, 18, 12, 0)),
                        new StoryEvent("event-1", StoryEventType.SCENE_COMPLETED, 28L, 31L, "scene-1",
                                new StoryUnitRef("scene-1", "scene-execution:31:scene-1", StoryUnitType.SCENE_EXECUTION),
                                "scene-1 已写回为 COMPLETED", java.util.Map.of("status", "COMPLETED"),
                                new StorySourceTrace("test", "test", "SceneExecutionWriteService", "scene-1")),
                        new StorySnapshot("snapshot-1", SnapshotScope.SCENE, 28L, 31L, "scene-1",
                                List.of(new StoryUnitRef("scene-1", "scene-execution:31:scene-1", StoryUnitType.SCENE_EXECUTION)),
                                "scene-1 snapshot", new StorySourceTrace("test", "test", "SceneExecutionWriteService", "scene-1")),
                        new StoryPatch(
                                "patch-1",
                                new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER),
                                FacetType.REVEAL,
                                List.of(new PatchOperation(PatchOperationType.MERGE, "/readerKnown", List.of("待揭晓"))),
                                "scene-1 的 reveal patch",
                                PatchStatus.APPLIED,
                                new StorySourceTrace("test", "test", "SceneExecutionWriteService", "scene-1")
                        ),
                        new ReaderRevealState(
                                28L, 31L, List.of("待揭晓"), List.of("待揭晓"), List.of("待揭晓"), List.of(), "读者已知 1 条，未揭晓 0 条"
                        ),
                        new StoryPatch(
                                "patch-chapter-state-1",
                                new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER),
                                FacetType.STATE,
                                List.of(new PatchOperation(PatchOperationType.MERGE, "/openLoops", List.of("scene:scene-2:pending"))),
                                "scene-1 的 chapter state patch",
                                PatchStatus.APPLIED,
                                new StorySourceTrace("test", "test", "SceneExecutionWriteService", "scene-1")
                        ),
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
                        ),
                        new StorySnapshot("snapshot-chapter-state-1", SnapshotScope.CHAPTER, 28L, 31L, "scene-1",
                                List.of(new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER)),
                                "读者已知 1 条，未揭晓 0 条", new StorySourceTrace("test", "test", "SceneExecutionWriteService", "scene-1"))
                ),
                preview.trace().append(new SessionExecutionTraceItem(
                        SessionRole.ORCHESTRATOR, "scene-writeback", SessionTraceStatus.COMPLETED,
                        "已写回 scene runtime state 与 handoff 快照。", "sceneExecutionWriteResult", 1, false, java.util.Map.of("sceneId", "scene-1")
                ))
        );
        when(storySessionOrchestrator.execute(eq(new SceneExecutionRequest(28L, 31L, "scene-1")))).thenReturn(Optional.of(execution));

        mockMvc.perform(post("/api/story-orchestration/projects/28/chapters/31/execute")
                        .param("sceneId", "scene-1")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.writeResult.sceneExecutionState.sceneId").value("scene-1"))
                .andExpect(jsonPath("$.data.writeResult.handoffSnapshot.toSceneId").value("scene-2"))
                .andExpect(jsonPath("$.data.writeResult.stateEvent.eventId").value("event-1"))
                .andExpect(jsonPath("$.data.writeResult.stateSnapshot.snapshotId").value("snapshot-1"))
                .andExpect(jsonPath("$.data.writeResult.statePatch.patchId").value("patch-1"))
                .andExpect(jsonPath("$.data.writeResult.readerRevealState.readerKnown[0]").value("待揭晓"))
                .andExpect(jsonPath("$.data.writeResult.chapterStatePatch.patchId").value("patch-chapter-state-1"))
                .andExpect(jsonPath("$.data.writeResult.chapterIncrementalState.openLoops[0]").value("scene:scene-2:pending"))
                .andExpect(jsonPath("$.data.writeResult.chapterStateSnapshot.snapshotId").value("snapshot-chapter-state-1"))
                .andExpect(jsonPath("$.data.trace.items[2].stepKey").value("scene-writeback"));
    }

    @Test
    void shouldReturnSkeletonPreviewWhenAvailable() throws Exception {
        ChapterSkeleton skeleton = new ChapterSkeleton(
                28L,
                31L,
                "skeleton_31_v1",
                3,
                "揭晓一条关键信息并留下后续空间后停住。",
                List.of(
                        new SceneSkeletonItem("scene-1", 1, SceneExecutionStatus.PLANNED, "完成现实状态定向", List.of("林沉舟已退役两年"), List.of("pov=林沉舟"), "回到家，进入书房前。", 900, "director-candidate")
                ),
                List.of(),
                List.of("当前章节暂无已执行镜头，骨架按冷启动三镜头生成。")
        );
        when(chapterSkeletonPlanner.plan(28L, 31L)).thenReturn(Optional.of(skeleton));

        mockMvc.perform(get("/api/story-orchestration/projects/28/chapters/31/skeleton-preview")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.skeletonId").value("skeleton_31_v1"))
                .andExpect(jsonPath("$.data.sceneCount").value(3))
                .andExpect(jsonPath("$.data.scenes[0].sceneId").value("scene-1"))
                .andExpect(jsonPath("$.data.scenes[0].status").value("PLANNED"));
    }

    @Test
    void shouldGenerateSkeletonWhenRequested() throws Exception {
        ChapterSkeleton skeleton = new ChapterSkeleton(
                28L,
                31L,
                "skeleton_31_v3",
                3,
                "在新手村会合前停住。",
                List.of(
                        new SceneSkeletonItem("scene-1", 1, SceneExecutionStatus.PLANNED, "重新规划后的开场", List.of("重返赛场的召回"), List.of("pov=林沉舟"), "答应回归后停住。", 900, "ai-skeleton")
                ),
                List.of(),
                List.of("镜头骨架已通过 AI 重新规划，共 3 个镜头。")
        );
        when(chapterSkeletonGenerationService.generate(28L, 31L, true)).thenReturn(Optional.of(skeleton));

        mockMvc.perform(post("/api/story-orchestration/projects/28/chapters/31/skeleton-generate")
                        .param("forceRefresh", "true")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("镜头骨架已生成"))
                .andExpect(jsonPath("$.data.skeletonId").value("skeleton_31_v3"))
                .andExpect(jsonPath("$.data.scenes[0].source").value("ai-skeleton"));
    }

    @Test
    void shouldReturnChapterReviewWhenAvailable() throws Exception {
        ChapterExecutionReview review = new ChapterExecutionReview(
                28L,
                31L,
                ReviewResult.REVISE,
                "章节级审校发现仍有未完成镜头或 handoff 缺口。",
                List.of(),
                false,
                new ChapterTraceSummary(28L, 31L, "skeleton_31_v1", 4, 3, 3, 0, 0, 1, "scene-3", List.of("scene-1", "scene-2", "scene-3"), List.of("scene-4"), List.of("scene-4"))
        );
        when(chapterExecutionReviewService.review(28L, 31L)).thenReturn(Optional.of(review));

        mockMvc.perform(get("/api/story-orchestration/projects/28/chapters/31/chapter-review")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.result").value("REVISE"))
                .andExpect(jsonPath("$.data.traceSummary.pendingSceneCount").value(1))
                .andExpect(jsonPath("$.data.traceSummary.pendingSceneIds[0]").value("scene-4"));
    }

    @Test
    void shouldReturnNodePreviewWhenAvailable() throws Exception {
        ChapterNodeRuntimeView runtimeView = new ChapterNodeRuntimeView(
                28L,
                31L,
                new ChapterNodeSkeleton(
                        28L,
                        31L,
                        "node-skeleton-31",
                        2,
                        "完成回归决定并推进到登录前。",
                        List.of(
                                new StoryNodeSkeletonItem(
                                        "node-1",
                                        1,
                                        "节点 1",
                                        "沿用锚点推进现实召回。",
                                        "做出回归决定",
                                        List.of(new NodeActionOption("advance-goal", "正面推进", "答应回归", "可能推进过快", "读者确认主角准备回归")),
                                        true,
                                        "停在登录前。",
                                        "完成本节点结算后写入新的 checkpoint。",
                                        List.of("进入 node-2")
                                )
                        ),
                        List.of("当前 node skeleton 由 scene skeleton 适配而来。")
                ),
                "node-1",
                "",
                List.of(),
                List.of(),
                List.of()
        );
        when(chapterNodeRuntimeService.preview(28L, 31L)).thenReturn(Optional.of(runtimeView));

        mockMvc.perform(get("/api/story-orchestration/projects/28/chapters/31/node-preview")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.currentNodeId").value("node-1"))
                .andExpect(jsonPath("$.data.skeleton.nodes[0].nodeId").value("node-1"))
                .andExpect(jsonPath("$.data.skeleton.nodes[0].recommendedActions[0].optionId").value("advance-goal"));
    }

    @Test
    void shouldResolveNodeActionWhenAvailable() throws Exception {
        NodeResolutionResult resolutionResult = new NodeResolutionResult(
                28L,
                31L,
                "node-1",
                "",
                new StoryActionIntent(
                        "intent-1",
                        28L,
                        31L,
                        "",
                        "node-1",
                        "林沉舟",
                        "player",
                        "advance-goal",
                        "正面推进",
                        "答应回归",
                        java.util.Map.of("mode", "recommended"),
                        new StorySourceTrace("system", "system", "NodeRuntimeService", "node-1")
                ),
                new StoryResolvedTurn(
                        "turn-1",
                        28L,
                        31L,
                        "",
                        "node-1",
                        "intent-1",
                        "答应回归并把局面送到 node-2 的入口前。",
                        List.of("event-1", "event-2"),
                        java.util.Map.of("currentNodeId", "node-1"),
                        List.of("读者确认主角准备回归"),
                        List.of("node-loop:node-2"),
                        List.of("node-loop:node-1"),
                        "checkpoint-2",
                        new StorySourceTrace("system", "system", "NodeRuntimeService", "node-1")
                ),
                new StoryNodeCheckpoint(
                        "checkpoint-2",
                        28L,
                        31L,
                        "node-2",
                        "",
                        2,
                        "答应回归并把局面送到 node-2 的入口前。",
                        "读者当前已知 1 条节点级信息。",
                        List.of("node-loop:node-2"),
                        java.util.Map.of("林沉舟", "退役者的邀请函"),
                        java.util.Map.of("林沉舟", "进入游戏"),
                        List.of("advance-goal"),
                        new StorySourceTrace("system", "system", "NodeRuntimeService", "node-1")
                ),
                List.of(
                        new StoryOpenLoop(
                                "node-loop:node-2",
                                28L,
                                31L,
                                "node-2",
                                "进入节点 node-2",
                                StoryLoopStatus.OPEN,
                                "chapter-runtime",
                                "在推进到 node-2 并完成结算后回收。",
                                null,
                                null,
                                List.of("chapter:31"),
                                new StorySourceTrace("system", "system", "NodeRuntimeService", "node-1")
                        )
                ),
                new ReaderRevealState(
                        28L,
                        31L,
                        List.of(),
                        List.of(),
                        List.of("读者确认主角准备回归"),
                        List.of(),
                        "读者当前已知 1 条节点级信息。"
                ),
                new ChapterIncrementalState(
                        28L,
                        31L,
                        List.of("node-loop:node-2"),
                        List.of("node-loop:node-1"),
                        List.of("退役者的邀请函"),
                        java.util.Map.of("林沉舟", "推进中"),
                        java.util.Map.of("林沉舟", "准备进入下一节点"),
                        java.util.Map.of("林沉舟", List.of("node:node-1")),
                        "当前章节等待推进 node-2。"
                )
        );
        when(chapterNodeRuntimeService.resolve(eq(new NodeActionRequest(
                28L,
                31L,
                "node-1",
                "",
                "advance-goal",
                ""
        )))).thenReturn(Optional.of(resolutionResult));

        mockMvc.perform(post("/api/story-orchestration/projects/28/chapters/31/node-actions/resolve")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "node-1",
                                  "checkpointId": "",
                                  "selectedOptionId": "advance-goal",
                                  "customAction": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("节点推进成功"))
                .andExpect(jsonPath("$.data.actionIntent.intentId").value("intent-1"))
                .andExpect(jsonPath("$.data.resolvedTurn.turnId").value("turn-1"))
                .andExpect(jsonPath("$.data.nextCheckpoint.checkpointId").value("checkpoint-2"))
                .andExpect(jsonPath("$.data.chapterState.openLoops[0]").value("node-loop:node-2"));
    }

    @Test
    void shouldUpdateSkeletonSceneWhenAvailable() throws Exception {
        ChapterSkeleton skeleton = new ChapterSkeleton(
                28L,
                31L,
                "skeleton_31_v1",
                3,
                "停在收口前。",
                List.of(
                        new SceneSkeletonItem("scene-1", 1, SceneExecutionStatus.PLANNED, "新的 goal", List.of("揭晓 A"), List.of("anchor=A"), "新的停点", 950, "manual-override")
                ),
                List.of(),
                List.of("已对 scene-1 应用手动骨架修改。")
        );
        when(chapterSkeletonMutationService.updateScene(eq(28L), eq(31L), eq(new com.storyweaver.story.generation.orchestration.SceneSkeletonMutationCommand(
                "scene-1",
                "新的 goal",
                List.of("揭晓 A"),
                List.of("anchor=A"),
                "新的停点",
                950
        )))).thenReturn(Optional.of(skeleton));

        mockMvc.perform(put("/api/story-orchestration/projects/28/chapters/31/skeleton-scenes/scene-1")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goal": "新的 goal",
                                  "readerReveal": ["揭晓 A"],
                                  "mustUseAnchors": ["anchor=A"],
                                  "stopCondition": "新的停点",
                                  "targetWords": 950
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scenes[0].goal").value("新的 goal"))
                .andExpect(jsonPath("$.data.scenes[0].source").value("manual-override"));
    }

    @Test
    void shouldDeleteSkeletonSceneWhenAvailable() throws Exception {
        ChapterSkeleton skeleton = new ChapterSkeleton(
                28L,
                31L,
                "skeleton_31_v1",
                2,
                "停在收口前。",
                List.of(
                        new SceneSkeletonItem("scene-1", 1, SceneExecutionStatus.PLANNED, "开场", List.of(), List.of(), "停在开场后。", 900, "manual-override"),
                        new SceneSkeletonItem("scene-3", 3, SceneExecutionStatus.PLANNED, "收口", List.of(), List.of(), "停在收口前。", 1000, "manual-override")
                ),
                List.of("scene-2"),
                List.of("已删除 scene-2。")
        );
        when(chapterSkeletonMutationService.deleteScene(28L, 31L, "scene-2")).thenReturn(Optional.of(skeleton));

        mockMvc.perform(delete("/api/story-orchestration/projects/28/chapters/31/skeleton-scenes/scene-2")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sceneCount").value(2))
                .andExpect(jsonPath("$.data.scenes[1].sceneId").value("scene-3"));
    }

    private StorySessionPreview samplePreview() {
        StorySessionContextPacket contextPacket = new StorySessionContextPacket(
                28L,
                31L,
                "scene-1",
                new SceneBindingContext("scene-1", "", SceneBindingMode.CHAPTER_COLD_START, false, "当前章节暂无 scene 执行状态，按冷启动处理。", null),
                new ProjectBriefView(28L, "旧日王座", "logline", "summary"),
                new StoryUnitSummaryView(new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER), StoryUnitType.CHAPTER, "退役者的邀请函", "章节摘要"),
                new ChapterAnchorBundleView(28L, 31L, "退役者的邀请函", 9L, "第一卷", 15L, "林沉舟", List.of("林沉舟"), List.of("剧情"), List.of("剧情"), "章节摘要"),
                new ReaderKnownStateView(28L, 31L, List.of(), List.of("待揭晓")),
                new RecentStoryProgressView(28L, List.of()),
                List.of(),
                null,
                List.of()
        );
        return new StorySessionPreview(
                contextPacket,
                List.of(new DirectorCandidate("opening-31", DirectorCandidateType.OPENING, "先做开场定向", List.of("待揭晓"), List.of("pov=林沉舟"), List.of("不要跳过开场"), "完成触发点后停住。", 900, "首段优先开场")),
                new SelectionDecision("opening-31", "当前是第一段，优先开场。", List.of(), List.of()),
                new WriterExecutionBrief(28L, 31L, "scene-1", "opening-31", "先做开场定向", List.of("待揭晓"), List.of("pov=林沉舟"), List.of("不要跳过开场"), "完成触发点后停住。", 900, List.of(), "", "", "scene-2", "推进到下一次交汇"),
                new WriterSessionResult("scene-1", "opening-31", "【目标】先做开场定向\n\n【收束点】完成触发点后停住。", "退役者的邀请函 / 先做开场定向"),
                new ReviewDecision("scene-1", ReviewResult.PASS, "规则审校通过。", List.of(), false, ""),
                new SessionExecutionTrace(28L, 31L, "scene-1", List.of(
                        new SessionExecutionTraceItem(SessionRole.ORCHESTRATOR, "context-scene-binding", SessionTraceStatus.COMPLETED, "当前章节暂无 scene 执行状态，按冷启动处理。", "sceneBindingContext", 1, false, java.util.Map.of()),
                        new SessionExecutionTraceItem(SessionRole.DIRECTOR, "director-candidates", SessionTraceStatus.COMPLETED, "已生成 1 个候选。", "directorCandidates", 1, false, java.util.Map.of("candidateCount", 1))
                ))
        );
    }
}
