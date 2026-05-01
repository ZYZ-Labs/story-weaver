package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.DirectorSessionService;
import com.storyweaver.story.generation.orchestration.SceneBindingContext;
import com.storyweaver.story.generation.orchestration.SceneBindingMode;
import com.storyweaver.story.generation.orchestration.SceneExecutionRequest;
import com.storyweaver.story.generation.orchestration.SceneExecutionWriteResult;
import com.storyweaver.story.generation.orchestration.SceneExecutionWriteService;
import com.storyweaver.story.generation.orchestration.SelectorSessionService;
import com.storyweaver.story.generation.orchestration.SessionExecutionTrace;
import com.storyweaver.story.generation.orchestration.SessionTraceStatus;
import com.storyweaver.story.generation.orchestration.StorySessionContextAssembler;
import com.storyweaver.story.generation.orchestration.StorySessionExecution;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.story.generation.orchestration.StorySessionPreview;
import com.storyweaver.story.generation.orchestration.ReviewerSessionService;
import com.storyweaver.story.generation.orchestration.WriterSessionResult;
import com.storyweaver.story.generation.orchestration.WriterSessionService;
import com.storyweaver.story.generation.orchestration.WriterExecutionBriefBuilder;
import com.storyweaver.storyunit.context.ChapterAnchorBundleView;
import com.storyweaver.storyunit.context.ProjectBriefView;
import com.storyweaver.storyunit.context.ReaderKnownStateView;
import com.storyweaver.storyunit.context.RecentStoryProgressView;
import com.storyweaver.storyunit.context.StoryUnitSummaryView;
import com.storyweaver.storyunit.event.StoryEvent;
import com.storyweaver.storyunit.event.StoryEventType;
import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;
import com.storyweaver.storyunit.facet.state.ChapterIncrementalState;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StorySourceTrace;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.patch.PatchOperation;
import com.storyweaver.storyunit.patch.PatchOperationType;
import com.storyweaver.storyunit.patch.PatchStatus;
import com.storyweaver.storyunit.patch.StoryPatch;
import com.storyweaver.storyunit.snapshot.SnapshotScope;
import com.storyweaver.storyunit.snapshot.StorySnapshot;
import com.storyweaver.storyunit.session.DirectorCandidate;
import com.storyweaver.storyunit.session.DirectorCandidateType;
import com.storyweaver.storyunit.session.ReviewDecision;
import com.storyweaver.storyunit.session.ReviewResult;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import com.storyweaver.storyunit.session.SceneHandoffSnapshot;
import com.storyweaver.storyunit.session.SelectionDecision;
import com.storyweaver.storyunit.session.WriterExecutionBrief;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultStorySessionOrchestratorTest {

    @Test
    void shouldBuildPreviewFromContextCandidatesAndSelection() {
        StorySessionContextAssembler contextAssembler = mock(StorySessionContextAssembler.class);
        DirectorSessionService directorSessionService = mock(DirectorSessionService.class);
        SelectorSessionService selectorSessionService = mock(SelectorSessionService.class);
        WriterExecutionBriefBuilder writerExecutionBriefBuilder = mock(WriterExecutionBriefBuilder.class);
        WriterSessionService writerSessionService = mock(WriterSessionService.class);
        ReviewerSessionService reviewerSessionService = mock(ReviewerSessionService.class);
        SceneExecutionWriteService sceneExecutionWriteService = mock(SceneExecutionWriteService.class);

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

        DirectorCandidate chosen = new DirectorCandidate(
                "opening-31",
                DirectorCandidateType.OPENING,
                "先做开场定向",
                List.of("待揭晓"),
                List.of("pov=林沉舟"),
                List.of("不要跳过开场"),
                "完成触发点后停住。",
                900,
                "首段优先开场"
        );
        SelectionDecision selectionDecision = new SelectionDecision(
                "opening-31",
                "当前是第一段，优先开场。",
                List.of(),
                List.of()
        );
        WriterExecutionBrief writerExecutionBrief = new WriterExecutionBrief(
                28L, 31L, "scene-1", "opening-31", "先做开场定向",
                List.of("待揭晓"), List.of("pov=林沉舟"), List.of("不要跳过开场"),
                "完成触发点后停住。", 900, List.of(), "", "", "scene-2", "转入回归决定"
        );
        WriterSessionResult writerSessionResult = new WriterSessionResult(
                "scene-1", "opening-31", "【目标】先做开场定向\n\n【收束点】完成触发点后停住。", "退役者的邀请函 / 先做开场定向"
        );
        ReviewDecision reviewDecision = new ReviewDecision(
                "scene-1", ReviewResult.PASS, "规则审校通过。", List.of(), false, ""
        );

        when(contextAssembler.assemble(28L, 31L, "scene-1")).thenReturn(Optional.of(contextPacket));
        when(directorSessionService.proposeCandidates(contextPacket)).thenReturn(List.of(chosen));
        when(selectorSessionService.selectCandidate(contextPacket, List.of(chosen))).thenReturn(selectionDecision);
        when(writerExecutionBriefBuilder.build(contextPacket, chosen)).thenReturn(writerExecutionBrief);
        when(writerSessionService.write(contextPacket, writerExecutionBrief)).thenReturn(writerSessionResult);
        when(reviewerSessionService.review(contextPacket, writerSessionResult)).thenReturn(reviewDecision);

        DefaultStorySessionOrchestrator orchestrator = new DefaultStorySessionOrchestrator(
                contextAssembler,
                directorSessionService,
                selectorSessionService,
                writerExecutionBriefBuilder,
                writerSessionService,
                reviewerSessionService,
                sceneExecutionWriteService
        );

        Optional<StorySessionPreview> result = orchestrator.preview(28L, 31L, "scene-1");

        assertTrue(result.isPresent());
        assertEquals("opening-31", result.orElseThrow().selectionDecision().chosenCandidateId());
        assertEquals("opening-31", result.orElseThrow().writerExecutionBrief().chosenCandidateId());
        assertEquals("scene-1", result.orElseThrow().writerSessionResult().sceneId());
        assertEquals(ReviewResult.PASS, result.orElseThrow().reviewDecision().result());
        SessionExecutionTrace trace = result.orElseThrow().trace();
        assertEquals(6, trace.items().size());
        assertEquals("context-scene-binding", trace.items().getFirst().stepKey());
        assertEquals(SessionTraceStatus.COMPLETED, trace.items().getFirst().status());
        assertEquals(1, trace.items().getFirst().attempt());
    }

    @Test
    void shouldMarkFallbackBindingAndReviewerRetryabilityInTrace() {
        StorySessionContextAssembler contextAssembler = mock(StorySessionContextAssembler.class);
        DirectorSessionService directorSessionService = mock(DirectorSessionService.class);
        SelectorSessionService selectorSessionService = mock(SelectorSessionService.class);
        WriterExecutionBriefBuilder writerExecutionBriefBuilder = mock(WriterExecutionBriefBuilder.class);
        WriterSessionService writerSessionService = mock(WriterSessionService.class);
        ReviewerSessionService reviewerSessionService = mock(ReviewerSessionService.class);
        SceneExecutionWriteService sceneExecutionWriteService = mock(SceneExecutionWriteService.class);

        StorySessionContextPacket contextPacket = new StorySessionContextPacket(
                28L,
                31L,
                "scene-2",
                new SceneBindingContext("scene-2", "scene-1", SceneBindingMode.SCENE_FALLBACK_TO_LATEST, true, "未找到请求的 sceneId，当前回退到最近 scene 上下文。", null),
                new ProjectBriefView(28L, "旧日王座", "logline", "summary"),
                new StoryUnitSummaryView(new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER), StoryUnitType.CHAPTER, "退役者的邀请函", "章节摘要"),
                new ChapterAnchorBundleView(28L, 31L, "退役者的邀请函", 9L, "第一卷", 15L, "林沉舟", List.of("林沉舟"), List.of("剧情"), List.of("剧情"), "章节摘要"),
                new ReaderKnownStateView(28L, 31L, List.of(), List.of("待揭晓")),
                new RecentStoryProgressView(28L, List.of()),
                List.of(),
                null,
                List.of()
        );

        DirectorCandidate chosen = new DirectorCandidate(
                "mainline-31",
                DirectorCandidateType.MAINLINE_ADVANCE,
                "围绕主线推进",
                List.of("待揭晓"),
                List.of("pov=林沉舟"),
                List.of("不要跳过承接"),
                "完成承接后停住。",
                1100,
                "当前 scene 已回退到最近 scene 上下文，优先做承接。"
        );
        SelectionDecision selectionDecision = new SelectionDecision(
                "mainline-31",
                "请求的 scene 未命中，当前已回退到最近 scene 上下文，优先先做承接。",
                List.of(),
                List.of("当前 sceneId 未命中真实 scene 状态，已回退到最近 scene，上下文承接仍可能偏粗。")
        );
        WriterExecutionBrief writerExecutionBrief = new WriterExecutionBrief(
                28L, 31L, "scene-2", "mainline-31", "围绕主线推进",
                List.of("待揭晓"), List.of("pov=林沉舟"), List.of("不要跳过承接"),
                "完成承接后停住。", 1100, List.of(), "上一镜头已建立开场", "", "scene-3", "转入新手村会合"
        );
        WriterSessionResult writerSessionResult = new WriterSessionResult(
                "scene-2", "mainline-31", "【目标】围绕主线推进\n\n【收束点】完成承接后停住。", "退役者的邀请函 / 围绕主线推进"
        );
        ReviewDecision reviewDecision = new ReviewDecision(
                "scene-2", ReviewResult.REVISE, "规则审校发现需要修正的问题。", List.of(), true, "补齐目标与收束点后重新生成。"
        );

        when(contextAssembler.assemble(28L, 31L, "scene-2")).thenReturn(Optional.of(contextPacket));
        when(directorSessionService.proposeCandidates(contextPacket)).thenReturn(List.of(chosen));
        when(selectorSessionService.selectCandidate(contextPacket, List.of(chosen))).thenReturn(selectionDecision);
        when(writerExecutionBriefBuilder.build(contextPacket, chosen)).thenReturn(writerExecutionBrief);
        when(writerSessionService.write(contextPacket, writerExecutionBrief)).thenReturn(writerSessionResult);
        when(reviewerSessionService.review(contextPacket, writerSessionResult)).thenReturn(reviewDecision);

        DefaultStorySessionOrchestrator orchestrator = new DefaultStorySessionOrchestrator(
                contextAssembler,
                directorSessionService,
                selectorSessionService,
                writerExecutionBriefBuilder,
                writerSessionService,
                reviewerSessionService,
                sceneExecutionWriteService
        );

        StorySessionPreview preview = orchestrator.preview(28L, 31L, "scene-2").orElseThrow();

        assertEquals(SessionTraceStatus.SKIPPED, preview.trace().items().getFirst().status());
        assertTrue(preview.trace().items().getFirst().retryable());
        assertEquals(SessionTraceStatus.FAILED, preview.trace().items().getLast().status());
        assertTrue(preview.trace().items().getLast().retryable());
    }

    @Test
    void shouldWriteSceneRuntimeStateDuringExecute() {
        StorySessionContextAssembler contextAssembler = mock(StorySessionContextAssembler.class);
        DirectorSessionService directorSessionService = mock(DirectorSessionService.class);
        SelectorSessionService selectorSessionService = mock(SelectorSessionService.class);
        WriterExecutionBriefBuilder writerExecutionBriefBuilder = mock(WriterExecutionBriefBuilder.class);
        WriterSessionService writerSessionService = mock(WriterSessionService.class);
        ReviewerSessionService reviewerSessionService = mock(ReviewerSessionService.class);
        SceneExecutionWriteService sceneExecutionWriteService = mock(SceneExecutionWriteService.class);

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
        DirectorCandidate chosen = new DirectorCandidate(
                "opening-31",
                DirectorCandidateType.OPENING,
                "先做开场定向",
                List.of("待揭晓"),
                List.of("pov=林沉舟"),
                List.of("不要跳过开场"),
                "完成触发点后停住。",
                900,
                "首段优先开场"
        );
        SelectionDecision selectionDecision = new SelectionDecision("opening-31", "当前是第一段，优先开场。", List.of(), List.of());
        WriterExecutionBrief writerExecutionBrief = new WriterExecutionBrief(
                28L, 31L, "scene-1", "opening-31", "先做开场定向",
                List.of("待揭晓"), List.of("pov=林沉舟"), List.of("不要跳过开场"),
                "完成触发点后停住。", 900, List.of(), "", "", "scene-2", "转入回归决定"
        );
        WriterSessionResult writerSessionResult = new WriterSessionResult(
                "scene-1", "opening-31", "【目标】先做开场定向\n\n【收束点】完成触发点后停住。", "退役者的邀请函 / 先做开场定向"
        );
        ReviewDecision reviewDecision = new ReviewDecision("scene-1", ReviewResult.PASS, "规则审校通过。", List.of(), false, "");
        SceneExecutionWriteResult writeResult = new SceneExecutionWriteResult(
                new SceneExecutionState(28L, 31L, "scene-1", 1, SceneExecutionStatus.COMPLETED, "opening-31",
                        "先做开场定向", "完成触发点后停住。", List.of("待揭晓"), List.of(), List.of(),
                        java.util.Map.of("source", "test"), "林沉舟推开门。", "主角完成现实状态定向。"),
                new SceneHandoffSnapshot(28L, 31L, "scene-1", "scene-2", "林沉舟推开门。", "主角完成现实状态定向。",
                        List.of("待揭晓"), List.of(), List.of(), java.util.Map.of("source", "test"), "PASS", "规则审校通过。", LocalDateTime.of(2026, 4, 18, 12, 0)),
                new StoryEvent(
                        "event-scene-1",
                        StoryEventType.SCENE_COMPLETED,
                        28L,
                        31L,
                        "scene-1",
                        new StoryUnitRef("scene-1", "scene:31:scene-1", StoryUnitType.SCENE_EXECUTION),
                        "scene scene-1 已完成",
                        java.util.Map.of("source", "test"),
                        new StorySourceTrace("test", "test", "unit-test", "DefaultStorySessionOrchestratorTest")
                ),
                new StorySnapshot(
                        "snapshot-scene-1",
                        SnapshotScope.SCENE,
                        28L,
                        31L,
                        "scene-1",
                        List.of(
                                new StoryUnitRef("scene-1", "scene:31:scene-1", StoryUnitType.SCENE_EXECUTION),
                                new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER)
                        ),
                        "scene-1 snapshot",
                        new StorySourceTrace("test", "test", "unit-test", "DefaultStorySessionOrchestratorTest")
                ),
                new StoryPatch(
                        "patch-scene-1",
                        new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER),
                        FacetType.REVEAL,
                        List.of(new PatchOperation(PatchOperationType.MERGE, "/readerKnown", List.of("待揭晓"))),
                        "scene-1 的 reveal patch",
                        PatchStatus.APPLIED,
                        new StorySourceTrace("test", "test", "unit-test", "DefaultStorySessionOrchestratorTest")
                ),
                new ReaderRevealState(
                        28L,
                        31L,
                        List.of("待揭晓"),
                        List.of("待揭晓"),
                        List.of("待揭晓"),
                        List.of(),
                        "读者已知 1 条，未揭晓 0 条"
                ),
                new StoryPatch(
                        "patch-chapter-state-1",
                        new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER),
                        FacetType.STATE,
                        List.of(new PatchOperation(PatchOperationType.MERGE, "/openLoops", List.of("scene:scene-2:pending"))),
                        "scene-1 的 chapter state patch",
                        PatchStatus.APPLIED,
                        new StorySourceTrace("test", "test", "unit-test", "DefaultStorySessionOrchestratorTest")
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
                new StorySnapshot(
                        "snapshot-chapter-state-1",
                        SnapshotScope.CHAPTER,
                        28L,
                        31L,
                        "scene-1",
                        List.of(new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER)),
                        "读者已知 1 条，未揭晓 0 条",
                        new StorySourceTrace("test", "test", "unit-test", "DefaultStorySessionOrchestratorTest")
                )
        );

        when(contextAssembler.assemble(28L, 31L, "scene-1")).thenReturn(Optional.of(contextPacket));
        when(directorSessionService.proposeCandidates(contextPacket)).thenReturn(List.of(chosen));
        when(selectorSessionService.selectCandidate(contextPacket, List.of(chosen))).thenReturn(selectionDecision);
        when(writerExecutionBriefBuilder.build(contextPacket, chosen)).thenReturn(writerExecutionBrief);
        when(writerSessionService.write(contextPacket, writerExecutionBrief)).thenReturn(writerSessionResult);
        when(reviewerSessionService.review(contextPacket, writerSessionResult)).thenReturn(reviewDecision);
        when(sceneExecutionWriteService.write(contextPacket, writerExecutionBrief, writerSessionResult, reviewDecision)).thenReturn(writeResult);

        DefaultStorySessionOrchestrator orchestrator = new DefaultStorySessionOrchestrator(
                contextAssembler,
                directorSessionService,
                selectorSessionService,
                writerExecutionBriefBuilder,
                writerSessionService,
                reviewerSessionService,
                sceneExecutionWriteService
        );

        StorySessionExecution execution = orchestrator.execute(new SceneExecutionRequest(28L, 31L, "scene-1")).orElseThrow();

        assertEquals("scene-1", execution.writeResult().sceneExecutionState().sceneId());
        assertEquals("scene-2", execution.writeResult().handoffSnapshot().toSceneId());
        assertEquals("scene-writeback", execution.trace().items().getLast().stepKey());
        assertEquals(7, execution.trace().items().size());
    }
}
