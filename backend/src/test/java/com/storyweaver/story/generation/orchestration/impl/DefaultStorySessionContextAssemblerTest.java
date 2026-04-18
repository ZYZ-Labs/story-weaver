package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.story.generation.orchestration.SceneBindingMode;
import com.storyweaver.storyunit.context.ChapterAnchorBundleView;
import com.storyweaver.storyunit.context.CharacterRuntimeStateView;
import com.storyweaver.storyunit.context.ProjectBriefView;
import com.storyweaver.storyunit.context.ReaderKnownStateView;
import com.storyweaver.storyunit.context.RecentStoryProgressView;
import com.storyweaver.storyunit.context.StoryContextQueryService;
import com.storyweaver.storyunit.context.StoryUnitSummaryView;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.service.SceneExecutionStateQueryService;
import com.storyweaver.storyunit.service.SceneRuntimeStateStore;
import com.storyweaver.storyunit.session.SceneHandoffSnapshot;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultStorySessionContextAssemblerTest {

    @Test
    void shouldAssembleContextPacketFromReadonlyQueries() {
        StoryContextQueryService storyContextQueryService = mock(StoryContextQueryService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SceneExecutionStateQueryService> sceneStateProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SceneRuntimeStateStore> runtimeStoreProvider = mock(ObjectProvider.class);

        ProjectBriefView projectBriefView = new ProjectBriefView(28L, "旧日王座", "退役者归来", "摘要");
        StoryUnitSummaryView chapterSummaryView = new StoryUnitSummaryView(
                new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER),
                StoryUnitType.CHAPTER,
                "退役者的邀请函",
                "两年沉寂后，主角收到旧战队邀请，命运再次启动。"
        );
        ChapterAnchorBundleView anchorBundleView = new ChapterAnchorBundleView(
                28L, 31L, "退役者的邀请函", 9L, "第一卷：新纪元开服", 15L, "林沉舟",
                List.of("林沉舟"), List.of("围绕《退役者的邀请函》的剧情节点"),
                List.of("围绕《退役者的邀请函》的剧情节点"), "章节摘要"
        );
        ReaderKnownStateView readerKnownStateView = new ReaderKnownStateView(28L, 31L, List.of(), List.of("本章待揭晓：邀请函"));
        CharacterRuntimeStateView runtimeStateView = new CharacterRuntimeStateView(
                28L, 15L, "林沉舟", "", "低谷回归期", "重返职业赛场并证明自己", List.of(), List.of(), List.of("主角")
        );
        RecentStoryProgressView recentStoryProgressView = new RecentStoryProgressView(28L, List.of());

        when(storyContextQueryService.getProjectBrief(28L)).thenReturn(Optional.of(projectBriefView));
        when(storyContextQueryService.getStoryUnitSummary(any(StoryUnitRef.class))).thenReturn(Optional.of(chapterSummaryView));
        when(storyContextQueryService.getChapterAnchorBundle(28L, 31L)).thenReturn(Optional.of(anchorBundleView));
        when(storyContextQueryService.getReaderKnownState(28L, 31L)).thenReturn(Optional.of(readerKnownStateView));
        when(storyContextQueryService.getCharacterRuntimeState(28L, 15L)).thenReturn(Optional.of(runtimeStateView));
        when(storyContextQueryService.getRecentStoryProgress(28L, 5)).thenReturn(recentStoryProgressView);
        when(sceneStateProvider.getIfAvailable()).thenReturn(null);
        when(runtimeStoreProvider.getIfAvailable()).thenReturn(null);

        DefaultStorySessionContextAssembler assembler = new DefaultStorySessionContextAssembler(
                storyContextQueryService,
                sceneStateProvider,
                runtimeStoreProvider
        );

        Optional<StorySessionContextPacket> result = assembler.assemble(28L, 31L, "scene-1");

        assertTrue(result.isPresent());
        assertEquals("旧日王座", result.orElseThrow().projectBrief().projectTitle());
        assertEquals("退役者的邀请函", result.orElseThrow().chapterSummary().title());
        assertEquals(1, result.orElseThrow().characterRuntimeStates().size());
        assertEquals("林沉舟", result.orElseThrow().characterRuntimeStates().getFirst().characterName());
        assertEquals("scene-1", result.orElseThrow().sceneId());
        assertEquals(SceneBindingMode.SCENE_QUERY_UNAVAILABLE, result.orElseThrow().sceneBindingContext().mode());
        assertEquals(null, result.orElseThrow().previousSceneHandoff());
    }

    @Test
    void shouldFallbackToLatestSceneWhenRequestedSceneMissing() {
        StoryContextQueryService storyContextQueryService = mock(StoryContextQueryService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SceneExecutionStateQueryService> sceneStateProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SceneRuntimeStateStore> runtimeStoreProvider = mock(ObjectProvider.class);
        SceneExecutionStateQueryService sceneExecutionStateQueryService = mock(SceneExecutionStateQueryService.class);
        SceneRuntimeStateStore runtimeStateStore = mock(SceneRuntimeStateStore.class);

        ProjectBriefView projectBriefView = new ProjectBriefView(28L, "旧日王座", "退役者归来", "摘要");
        StoryUnitSummaryView chapterSummaryView = new StoryUnitSummaryView(
                new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER),
                StoryUnitType.CHAPTER,
                "退役者的邀请函",
                "两年沉寂后，主角收到旧战队邀请，命运再次启动。"
        );
        ChapterAnchorBundleView anchorBundleView = new ChapterAnchorBundleView(
                28L, 31L, "退役者的邀请函", 9L, "第一卷：新纪元开服", 15L, "林沉舟",
                List.of("林沉舟"), List.of("围绕《退役者的邀请函》的剧情节点"),
                List.of("围绕《退役者的邀请函》的剧情节点"), "章节摘要"
        );
        ReaderKnownStateView readerKnownStateView = new ReaderKnownStateView(28L, 31L, List.of(), List.of("本章待揭晓：邀请函"));
        CharacterRuntimeStateView runtimeStateView = new CharacterRuntimeStateView(
                28L, 15L, "林沉舟", "", "低谷回归期", "重返职业赛场并证明自己", List.of(), List.of(), List.of("主角")
        );
        RecentStoryProgressView recentStoryProgressView = new RecentStoryProgressView(28L, List.of());
        SceneExecutionState latestSceneState = new SceneExecutionState(
                28L, 31L, "scene-1", 1, SceneExecutionStatus.COMPLETED, "opening-31",
                "先做开场定向", "完成触发点后停住。", List.of(), List.of(), List.of(), java.util.Map.of(), "旧场景交接", "旧场景结果"
        );

        when(storyContextQueryService.getProjectBrief(28L)).thenReturn(Optional.of(projectBriefView));
        when(storyContextQueryService.getStoryUnitSummary(any(StoryUnitRef.class))).thenReturn(Optional.of(chapterSummaryView));
        when(storyContextQueryService.getChapterAnchorBundle(28L, 31L)).thenReturn(Optional.of(anchorBundleView));
        when(storyContextQueryService.getReaderKnownState(28L, 31L)).thenReturn(Optional.of(readerKnownStateView));
        when(storyContextQueryService.getCharacterRuntimeState(28L, 15L)).thenReturn(Optional.of(runtimeStateView));
        when(storyContextQueryService.getRecentStoryProgress(28L, 5)).thenReturn(recentStoryProgressView);
        when(sceneStateProvider.getIfAvailable()).thenReturn(sceneExecutionStateQueryService);
        when(runtimeStoreProvider.getIfAvailable()).thenReturn(runtimeStateStore);
        when(sceneExecutionStateQueryService.listChapterScenes(28L, 31L)).thenReturn(List.of(latestSceneState));
        when(sceneExecutionStateQueryService.getSceneState(28L, 31L, "scene-2")).thenReturn(Optional.empty());
        when(sceneExecutionStateQueryService.findLatestChapterScene(28L, 31L)).thenReturn(Optional.of(latestSceneState));
        SceneHandoffSnapshot handoffSnapshot = new SceneHandoffSnapshot(
                28L, 31L, "scene-1", "scene-2", "林沉舟走进书房。", "上一镜头已结束。", List.of(), List.of(), List.of(), java.util.Map.of(), "PASS", "规则审校通过。", java.time.LocalDateTime.now()
        );
        when(runtimeStateStore.findHandoffToScene(28L, 31L, "scene-2")).thenReturn(Optional.of(handoffSnapshot));

        DefaultStorySessionContextAssembler assembler = new DefaultStorySessionContextAssembler(
                storyContextQueryService,
                sceneStateProvider,
                runtimeStoreProvider
        );

        Optional<StorySessionContextPacket> result = assembler.assemble(28L, 31L, "scene-2");

        assertTrue(result.isPresent());
        assertEquals(SceneBindingMode.SCENE_FALLBACK_TO_LATEST, result.orElseThrow().sceneBindingContext().mode());
        assertEquals("scene-1", result.orElseThrow().sceneBindingContext().resolvedSceneId());
        assertTrue(result.orElseThrow().sceneBindingContext().fallbackUsed());
        assertEquals("scene-1", result.orElseThrow().previousSceneHandoff().fromSceneId());
    }
}
