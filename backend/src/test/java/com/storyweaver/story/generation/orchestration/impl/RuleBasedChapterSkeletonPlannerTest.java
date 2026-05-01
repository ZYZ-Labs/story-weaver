package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.ChapterSkeleton;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonStore;
import com.storyweaver.story.generation.orchestration.SceneBindingContext;
import com.storyweaver.story.generation.orchestration.SceneBindingMode;
import com.storyweaver.story.generation.orchestration.SceneSkeletonItem;
import com.storyweaver.story.generation.orchestration.StorySessionContextAssembler;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.storyunit.context.ChapterAnchorBundleView;
import com.storyweaver.storyunit.context.ProjectBriefView;
import com.storyweaver.storyunit.context.ReaderKnownStateView;
import com.storyweaver.storyunit.context.RecentStoryProgressView;
import com.storyweaver.storyunit.context.StoryUnitSummaryView;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
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

class RuleBasedChapterSkeletonPlannerTest {

    @Test
    void shouldReturnEmptyWhenNoStoredSkeletonExists() {
        StorySessionContextAssembler contextAssembler = mock(StorySessionContextAssembler.class);
        ChapterSkeletonStore chapterSkeletonStore = mock(ChapterSkeletonStore.class);
        when(chapterSkeletonStore.find(28L, 31L)).thenReturn(Optional.empty());

        RuleBasedChapterSkeletonPlanner planner = new RuleBasedChapterSkeletonPlanner(
                contextAssembler,
                chapterSkeletonStore
        );

        assertTrue(planner.plan(28L, 31L).isEmpty());
    }

    @Test
    void shouldMergeStoredSkeletonWithRuntimeSceneStates() {
        StorySessionContextAssembler contextAssembler = mock(StorySessionContextAssembler.class);
        ChapterSkeletonStore chapterSkeletonStore = mock(ChapterSkeletonStore.class);

        ChapterSkeleton storedSkeleton = new ChapterSkeleton(
                28L,
                31L,
                "skeleton_31_v2",
                2,
                "在新手村会合前停住。",
                List.of(
                        new SceneSkeletonItem("scene-1", 1, SceneExecutionStatus.PLANNED, "接电话并得知重组消息",
                                List.of("老陈联系林沉舟"), List.of("pov=林沉舟"), "答应明天上线前停住。", 900, "ai-skeleton"),
                        new SceneSkeletonItem("scene-2", 2, SceneExecutionStatus.PLANNED, "次日上线前整理装备",
                                List.of("林沉舟决定回归"), List.of("chapter=退役者的邀请"), "进入新手村前停住。", 1000, "ai-skeleton")
                ),
                List.of(),
                List.of("镜头骨架已通过 AI 重新规划，共 2 个镜头。")
        );

        SceneExecutionState completedSceneOne = new SceneExecutionState(
                28L,
                31L,
                "scene-1",
                1,
                SceneExecutionStatus.COMPLETED,
                "skeleton-scene-1",
                "接电话并得知重组消息",
                "答应明天上线前停住。",
                List.of("老陈联系林沉舟"),
                List.of("陆川仍在等他"),
                List.of(),
                Map.of("source", "test"),
                "林沉舟答应明天上线。",
                "林沉舟已经答应回归。"
        );
        SceneExecutionState completedSceneThree = new SceneExecutionState(
                28L,
                31L,
                "scene-3",
                3,
                SceneExecutionStatus.COMPLETED,
                "runtime-scene-3",
                "提前生成出的额外镜头",
                "抵达集合点前停住。",
                List.of("陆川在新手村等他"),
                List.of(),
                List.of(),
                Map.of("source", "test"),
                "林沉舟已经抵达集合点。",
                "runtime 中已存在额外镜头。"
        );

        when(chapterSkeletonStore.find(28L, 31L)).thenReturn(Optional.of(storedSkeleton));
        when(contextAssembler.assemble(28L, 31L, "scene-1"))
                .thenReturn(Optional.of(sampleContext(List.of(completedSceneOne, completedSceneThree))));

        RuleBasedChapterSkeletonPlanner planner = new RuleBasedChapterSkeletonPlanner(
                contextAssembler,
                chapterSkeletonStore
        );

        ChapterSkeleton merged = planner.plan(28L, 31L).orElseThrow();

        assertEquals(3, merged.sceneCount());
        assertEquals(SceneExecutionStatus.COMPLETED, merged.scenes().get(0).status());
        assertEquals("ai-skeleton", merged.scenes().get(0).source());
        assertEquals("scene-2", merged.scenes().get(1).sceneId());
        assertEquals("scene-3", merged.scenes().get(2).sceneId());
        assertEquals("existing-scene-state", merged.scenes().get(2).source());
        assertTrue(merged.planningNotes().contains("当前章节骨架已同步 runtime scene 状态。"));
    }

    private StorySessionContextPacket sampleContext(List<SceneExecutionState> existingScenes) {
        return new StorySessionContextPacket(
                28L,
                31L,
                "scene-1",
                new SceneBindingContext("scene-1", "", SceneBindingMode.SCENE_BOUND, false, "当前骨架与 runtime 已对齐。", null),
                new ProjectBriefView(28L, "旧日王座", "logline", "summary"),
                new StoryUnitSummaryView(
                        new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER),
                        StoryUnitType.CHAPTER,
                        "退役者的邀请",
                        "章节摘要"
                ),
                new ChapterAnchorBundleView(
                        28L,
                        31L,
                        "退役者的邀请",
                        9L,
                        "第一卷",
                        15L,
                        "林沉舟",
                        List.of("林沉舟", "老陈"),
                        List.of("重组邀请"),
                        List.of("重返赛场"),
                        "章节摘要"
                ),
                new ReaderKnownStateView(28L, 31L, List.of("林沉舟已经退役"), List.of("他是否回归")),
                new RecentStoryProgressView(28L, List.of()),
                List.of(),
                null,
                existingScenes
        );
    }
}
