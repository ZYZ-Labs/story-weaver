package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.SceneExecutionWriteResult;
import com.storyweaver.story.generation.orchestration.SceneBindingContext;
import com.storyweaver.story.generation.orchestration.SceneBindingMode;
import com.storyweaver.story.generation.orchestration.SceneSkeletonItem;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.story.generation.orchestration.WriterSessionResult;
import com.storyweaver.storyunit.context.ChapterAnchorBundleView;
import com.storyweaver.storyunit.context.CharacterRuntimeStateView;
import com.storyweaver.storyunit.context.ProjectBriefView;
import com.storyweaver.storyunit.context.ReaderKnownStateView;
import com.storyweaver.storyunit.context.RecentStoryProgressView;
import com.storyweaver.storyunit.context.StoryUnitSummaryView;
import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;
import com.storyweaver.storyunit.facet.state.ChapterIncrementalState;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.patch.StoryPatch;
import com.storyweaver.storyunit.service.ChapterIncrementalStateStore;
import com.storyweaver.storyunit.service.ReaderRevealStateStore;
import com.storyweaver.storyunit.service.SceneRuntimeStateStore;
import com.storyweaver.storyunit.service.StoryEventStore;
import com.storyweaver.storyunit.service.StoryPatchStore;
import com.storyweaver.storyunit.service.StorySnapshotStore;
import com.storyweaver.storyunit.session.ReviewDecision;
import com.storyweaver.storyunit.session.ReviewResult;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import com.storyweaver.storyunit.session.WriterExecutionBrief;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultSceneExecutionWriteServiceTest {

    @Test
    void shouldPersistDraftExecutionWithoutUnlockingNextScene() {
        SceneRuntimeStateStore store = mock(SceneRuntimeStateStore.class);
        StoryEventStore eventStore = mock(StoryEventStore.class);
        StorySnapshotStore snapshotStore = mock(StorySnapshotStore.class);
        StoryPatchStore patchStore = mock(StoryPatchStore.class);
        ReaderRevealStateStore revealStateStore = mock(ReaderRevealStateStore.class);
        ChapterIncrementalStateStore chapterIncrementalStateStore = mock(ChapterIncrementalStateStore.class);
        AIContinuityStateService aiContinuityStateService = mock(AIContinuityStateService.class);
        DefaultSceneExecutionWriteService service = new DefaultSceneExecutionWriteService(
                store,
                eventStore,
                snapshotStore,
                patchStore,
                revealStateStore,
                chapterIncrementalStateStore,
                aiContinuityStateService
        );

        StorySessionContextPacket contextPacket = new StorySessionContextPacket(
                28L,
                31L,
                "scene-2",
                new SceneBindingContext("scene-2", "scene-1", SceneBindingMode.SCENE_BOUND, false, "已绑定请求的 scene 执行状态。", null),
                new ProjectBriefView(28L, "旧日王座", "退役者归来", "摘要"),
                new StoryUnitSummaryView(new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER), StoryUnitType.CHAPTER, "退役者的邀请函", "章节摘要"),
                new ChapterAnchorBundleView(28L, 31L, "退役者的邀请函", 9L, "第一卷", 15L, "林沉舟", List.of("林沉舟"), List.of("剧情"), List.of("剧情"), "章节摘要"),
                new ReaderKnownStateView(28L, 31L, List.of("邀请函来自旧战队"), List.of()),
                new RecentStoryProgressView(28L, List.of()),
                List.of(new CharacterRuntimeStateView(
                        28L,
                        15L,
                        "林沉舟",
                        "办公室",
                        "紧张",
                        "谨慎",
                        List.of(),
                        List.of(),
                        List.of("观察中")
                )),
                null,
                List.of()
        );
        WriterExecutionBrief brief = new WriterExecutionBrief(
                28L,
                31L,
                "scene-2",
                "mainline-31",
                "推进邀请函主线",
                List.of("主角决定赴约"),
                List.of("pov=林沉舟"),
                List.of("不要跳章"),
                "主角决定赴约后停住。",
                1100,
                List.of("上一镜头已建立现实状态。"),
                "上一镜头摘要",
                "林沉舟看着已经发出去的确认函。",
                "scene-3",
                "前往集合点"
        );
        WriterSessionResult writerSessionResult = new WriterSessionResult(
                "scene-2",
                "mainline-31",
                "林沉舟看着已经发出去的确认函，意识到自己已经没有退路。",
                "主角最终决定赴约。"
        );
        ReviewDecision reviewDecision = new ReviewDecision(
                "scene-2",
                ReviewResult.PASS,
                "规则审校通过。",
                List.of(),
                false,
                ""
        );

        when(store.saveSceneState(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(store.saveHandoff(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventStore.appendEvent(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(snapshotStore.saveSnapshot(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(patchStore.appendPatch(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(2));
        when(revealStateStore.findChapterRevealState(28L, 31L)).thenReturn(java.util.Optional.empty());
        when(revealStateStore.saveChapterRevealState(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chapterIncrementalStateStore.findChapterState(28L, 31L)).thenReturn(java.util.Optional.empty());
        when(chapterIncrementalStateStore.saveChapterState(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SceneExecutionWriteResult result = service.write(contextPacket, brief, writerSessionResult, reviewDecision);

        assertEquals("scene-2", result.sceneExecutionState().sceneId());
        assertEquals(SceneExecutionStatus.WRITTEN, result.sceneExecutionState().status());
        assertEquals("", result.handoffSnapshot().toSceneId());
        assertEquals("PASS", result.handoffSnapshot().reviewResult());
        assertEquals("scene-2", result.stateEvent().sceneId());
        assertEquals("STATE_CHANGED", result.stateEvent().eventType().name());
        assertEquals("scene-2", result.stateSnapshot().sceneId());
        assertEquals("REVEAL", result.statePatch().facetType().name());
        assertEquals("STATE", result.chapterStatePatch().facetType().name());
        assertEquals("邀请函来自旧战队", result.readerRevealState().readerKnown().getFirst());
        assertTrue(result.chapterIncrementalState().openLoops().isEmpty());
        assertTrue(result.chapterIncrementalState().resolvedLoops().isEmpty());
        assertEquals("办公室", result.chapterIncrementalState().activeLocations().getFirst());
        assertEquals("紧张", result.chapterIncrementalState().characterEmotions().get("林沉舟"));
        assertEquals("CHAPTER", result.chapterStateSnapshot().scope().name());
        assertTrue(result.sceneExecutionState().handoffLine().contains("林沉舟"));
        verify(store).saveSceneState(any());
        verify(store).saveHandoff(any());
        verify(eventStore).appendEvent(any());
        verify(snapshotStore, org.mockito.Mockito.times(2)).saveSnapshot(any());
        verify(patchStore, org.mockito.Mockito.times(2)).appendPatch(any(), any(), any(StoryPatch.class));
        verify(revealStateStore).saveChapterRevealState(any(ReaderRevealState.class));
        verify(chapterIncrementalStateStore).saveChapterState(any(ChapterIncrementalState.class));
    }

    @Test
    void shouldPersistAcceptedSceneAndUnlockNextScene() {
        SceneRuntimeStateStore store = mock(SceneRuntimeStateStore.class);
        StoryEventStore eventStore = mock(StoryEventStore.class);
        StorySnapshotStore snapshotStore = mock(StorySnapshotStore.class);
        StoryPatchStore patchStore = mock(StoryPatchStore.class);
        ReaderRevealStateStore revealStateStore = mock(ReaderRevealStateStore.class);
        ChapterIncrementalStateStore chapterIncrementalStateStore = mock(ChapterIncrementalStateStore.class);
        AIContinuityStateService aiContinuityStateService = mock(AIContinuityStateService.class);
        DefaultSceneExecutionWriteService service = new DefaultSceneExecutionWriteService(
                store,
                eventStore,
                snapshotStore,
                patchStore,
                revealStateStore,
                chapterIncrementalStateStore,
                aiContinuityStateService
        );

        StorySessionContextPacket contextPacket = new StorySessionContextPacket(
                28L,
                31L,
                "scene-2",
                new SceneBindingContext("scene-2", "scene-1", SceneBindingMode.SCENE_BOUND, false, "已绑定请求的 scene 执行状态。", null),
                new ProjectBriefView(28L, "旧日王座", "退役者归来", "摘要"),
                new StoryUnitSummaryView(new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER), StoryUnitType.CHAPTER, "退役者的邀请函", "章节摘要"),
                new ChapterAnchorBundleView(28L, 31L, "退役者的邀请函", 9L, "第一卷", 15L, "林沉舟", List.of("林沉舟"), List.of("剧情"), List.of("剧情"), "章节摘要"),
                new ReaderKnownStateView(28L, 31L, List.of("邀请函来自旧战队"), List.of()),
                new RecentStoryProgressView(28L, List.of()),
                List.of(new CharacterRuntimeStateView(
                        28L,
                        15L,
                        "林沉舟",
                        "办公室",
                        "紧张",
                        "谨慎",
                        List.of(),
                        List.of(),
                        List.of("观察中")
                )),
                null,
                List.of()
        );
        SceneSkeletonItem currentScene = new SceneSkeletonItem(
                "scene-2",
                2,
                SceneExecutionStatus.PLANNED,
                "推进邀请函主线",
                List.of("主角决定赴约"),
                List.of("pov=林沉舟"),
                "主角决定赴约后停住。",
                1100,
                "ai-skeleton"
        );
        SceneSkeletonItem nextScene = new SceneSkeletonItem(
                "scene-3",
                3,
                SceneExecutionStatus.PLANNED,
                "前往集合点",
                List.of("主角与队友会合"),
                List.of("chapter=退役者的邀请函"),
                "抵达集合点后停住。",
                900,
                "ai-skeleton"
        );

        when(store.saveSceneState(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(store.saveHandoff(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventStore.appendEvent(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(snapshotStore.saveSnapshot(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(patchStore.appendPatch(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(2));
        when(revealStateStore.findChapterRevealState(28L, 31L)).thenReturn(java.util.Optional.empty());
        when(revealStateStore.saveChapterRevealState(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chapterIncrementalStateStore.findChapterState(28L, 31L)).thenReturn(java.util.Optional.empty());
        when(chapterIncrementalStateStore.saveChapterState(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiContinuityStateService.extractAcceptedContinuityState(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new com.storyweaver.storyunit.session.SceneContinuityState(
                        "scene-2",
                        "主角决定赴约。",
                        "林沉舟停在正式出发前。",
                        List.of("主角决定赴约。"),
                        List.of(),
                        List.of("林沉舟"),
                        List.of("林沉舟"),
                        false,
                        "scene-3",
                        "前往集合点",
                        "主角决定赴约后停住。"
                ));

        SceneExecutionWriteResult result = service.writeAccepted(
                contextPacket,
                currentScene,
                nextScene,
                901L,
                "林沉舟看着已经发出去的确认函，终于承认自己已经决定赴约。"
        );

        assertEquals(SceneExecutionStatus.COMPLETED, result.sceneExecutionState().status());
        assertEquals("scene-3", result.handoffSnapshot().toSceneId());
        assertEquals("SCENE_COMPLETED", result.stateEvent().eventType().name());
        assertEquals("主角决定赴约", result.readerRevealState().readerKnown().getLast());
        assertEquals("scene:scene-3:pending", result.chapterIncrementalState().openLoops().getFirst());
        assertEquals("scene:scene-2:pending", result.chapterIncrementalState().resolvedLoops().getFirst());
        assertEquals("phase8.accepted-scene-draft", result.sceneExecutionState().stateDelta().get("source"));
        assertEquals(901L, result.sceneExecutionState().stateDelta().get("writingRecordId"));
    }
}
