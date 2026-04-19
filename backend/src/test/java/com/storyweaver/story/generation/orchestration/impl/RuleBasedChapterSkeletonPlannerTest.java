package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.DirectorSessionService;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonStore;
import com.storyweaver.story.generation.orchestration.SceneBindingContext;
import com.storyweaver.story.generation.orchestration.SceneBindingMode;
import com.storyweaver.story.generation.orchestration.SelectorSessionService;
import com.storyweaver.story.generation.orchestration.StorySessionContextAssembler;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.storyunit.context.ChapterAnchorBundleView;
import com.storyweaver.storyunit.context.ProjectBriefView;
import com.storyweaver.storyunit.context.ReaderKnownStateView;
import com.storyweaver.storyunit.context.RecentStoryProgressView;
import com.storyweaver.storyunit.context.StoryUnitSummaryView;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.session.DirectorCandidate;
import com.storyweaver.storyunit.session.DirectorCandidateType;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import com.storyweaver.storyunit.session.SelectionDecision;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleBasedChapterSkeletonPlannerTest {

    @Test
    void shouldBuildColdStartSkeletonFromSelectedCandidateAndDirectorCandidates() {
        StorySessionContextAssembler contextAssembler = mock(StorySessionContextAssembler.class);
        DirectorSessionService directorSessionService = mock(DirectorSessionService.class);
        SelectorSessionService selectorSessionService = mock(SelectorSessionService.class);
        ChapterSkeletonStore chapterSkeletonStore = mock(ChapterSkeletonStore.class);

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

        List<DirectorCandidate> candidates = List.of(
                new DirectorCandidate("opening-31", DirectorCandidateType.OPENING, "完成现实状态定向", List.of("林沉舟已退役两年"), List.of("pov=林沉舟"), List.of("不要直接打斗"), "回到家，进入书房前。", 900, "冷启动优先开场"),
                new DirectorCandidate("mainline-31", DirectorCandidateType.MAINLINE_ADVANCE, "推进邀请函主线", List.of("邀请函来自旧战队"), List.of("summary=章节摘要"), List.of("不要跳章"), "收到邀请并做出决定后停住。", 1100, "围绕本章摘要推进"),
                new DirectorCandidate("reveal-31", DirectorCandidateType.REVEAL, "释放一条关键信息", List.of("林沉舟仍被旧日王座牵动"), List.of("chapter=退役者的邀请函"), List.of("不要一次说完"), "揭晓一条关键信息并留下后续空间后停住。", 800, "作为第三镜头收口")
        );

        when(contextAssembler.assemble(28L, 31L, "scene-1")).thenReturn(Optional.of(contextPacket));
        when(directorSessionService.proposeCandidates(contextPacket)).thenReturn(candidates);
        when(selectorSessionService.selectCandidate(contextPacket, candidates))
                .thenReturn(new SelectionDecision("opening-31", "当前是第一镜头，优先开场。", List.of(), List.of()));
        when(chapterSkeletonStore.find(28L, 31L)).thenReturn(Optional.empty());

        RuleBasedChapterSkeletonPlanner planner = new RuleBasedChapterSkeletonPlanner(
                contextAssembler,
                directorSessionService,
                selectorSessionService,
                chapterSkeletonStore
        );

        var skeleton = planner.plan(28L, 31L).orElseThrow();

        assertEquals("skeleton_31_v1", skeleton.skeletonId());
        assertEquals(3, skeleton.sceneCount());
        assertEquals("scene-1", skeleton.scenes().get(0).sceneId());
        assertEquals(SceneExecutionStatus.PLANNED, skeleton.scenes().get(0).status());
        assertEquals("opening-31", skeleton.planningNotes().stream().filter(note -> note.contains("opening-31")).findFirst().map(note -> "opening-31").orElse(""));
        assertEquals("揭晓一条关键信息并留下后续空间后停住。", skeleton.globalStopCondition());
    }

    @Test
    void shouldPreserveExistingSceneStatesAndAppendPlannedScenes() {
        StorySessionContextAssembler contextAssembler = mock(StorySessionContextAssembler.class);
        DirectorSessionService directorSessionService = mock(DirectorSessionService.class);
        SelectorSessionService selectorSessionService = mock(SelectorSessionService.class);
        ChapterSkeletonStore chapterSkeletonStore = mock(ChapterSkeletonStore.class);

        SceneExecutionState existingScene = new SceneExecutionState(
                28L,
                31L,
                "scene-1",
                1,
                SceneExecutionStatus.COMPLETED,
                "opening-31",
                "完成现实状态定向",
                "回到家，进入书房前。",
                List.of("林沉舟已退役两年"),
                List.of("邀请函是什么来历"),
                List.of(),
                Map.of("recordId", 101),
                "林沉舟走向书房。",
                "林沉舟结束了当晚的日常生活。"
        );

        StorySessionContextPacket contextPacket = new StorySessionContextPacket(
                28L,
                31L,
                "scene-2",
                new SceneBindingContext("scene-2", "scene-1", SceneBindingMode.SCENE_FALLBACK_TO_LATEST, true, "未找到请求的 sceneId，当前回退到最近 scene 上下文。", existingScene),
                new ProjectBriefView(28L, "旧日王座", "logline", "summary"),
                new StoryUnitSummaryView(new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER), StoryUnitType.CHAPTER, "退役者的邀请函", "章节摘要"),
                new ChapterAnchorBundleView(28L, 31L, "退役者的邀请函", 9L, "第一卷", 15L, "林沉舟", List.of("林沉舟"), List.of("剧情"), List.of("剧情"), "章节摘要"),
                new ReaderKnownStateView(28L, 31L, List.of("林沉舟已退役两年"), List.of("邀请函来自旧战队")),
                new RecentStoryProgressView(28L, List.of()),
                List.of(),
                null,
                List.of(existingScene)
        );

        List<DirectorCandidate> candidates = List.of(
                new DirectorCandidate("mainline-31", DirectorCandidateType.MAINLINE_ADVANCE, "推进邀请函主线", List.of("邀请函来自旧战队"), List.of("summary=章节摘要"), List.of("不要跳章"), "收到邀请并做出决定后停住。", 1100, "围绕本章摘要推进")
        );

        when(contextAssembler.assemble(28L, 31L, "scene-1")).thenReturn(Optional.of(contextPacket));
        when(directorSessionService.proposeCandidates(contextPacket)).thenReturn(candidates);
        when(selectorSessionService.selectCandidate(contextPacket, candidates))
                .thenReturn(new SelectionDecision("mainline-31", "当前应优先承接上一镜头。", List.of(), List.of()));
        when(chapterSkeletonStore.find(28L, 31L)).thenReturn(Optional.empty());

        RuleBasedChapterSkeletonPlanner planner = new RuleBasedChapterSkeletonPlanner(
                contextAssembler,
                directorSessionService,
                selectorSessionService,
                chapterSkeletonStore
        );

        var skeleton = planner.plan(28L, 31L).orElseThrow();

        assertEquals(3, skeleton.sceneCount());
        assertEquals(SceneExecutionStatus.COMPLETED, skeleton.scenes().get(0).status());
        assertEquals("existing-scene-state", skeleton.scenes().get(0).source());
        assertEquals("scene-2", skeleton.scenes().get(1).sceneId());
        assertEquals(SceneExecutionStatus.PLANNED, skeleton.scenes().get(1).status());
        assertTrue(skeleton.planningNotes().stream().anyMatch(note -> note.contains("回退")));
    }
}
