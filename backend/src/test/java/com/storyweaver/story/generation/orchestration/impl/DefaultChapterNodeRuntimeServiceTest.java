package com.storyweaver.story.generation.orchestration.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.config.StoryStateProperties;
import com.storyweaver.story.generation.orchestration.ChapterNodeRuntimeView;
import com.storyweaver.story.generation.orchestration.ChapterSkeleton;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonPlanner;
import com.storyweaver.story.generation.orchestration.NodeActionRequest;
import com.storyweaver.story.generation.orchestration.NodeResolutionResult;
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
import com.storyweaver.storyunit.service.impl.ResilientStoryStateStore;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultChapterNodeRuntimeServiceTest {

    @Test
    void shouldResolveCurrentNodeAndAdvanceCheckpoint() {
        ChapterSkeleton skeleton = new ChapterSkeleton(
                28L,
                31L,
                "skeleton-31",
                2,
                "停在进入游戏大厅前。",
                List.of(
                        new SceneSkeletonItem("scene-1", 1, SceneExecutionStatus.PLANNED, "做出回归决定", List.of("主角决定回归"), List.of("pov=林沉舟"), "停在正式登录前。", 900, "ai-skeleton"),
                        new SceneSkeletonItem("scene-2", 2, SceneExecutionStatus.PLANNED, "进入新纪元大厅", List.of("主角进入大厅"), List.of("pov=林沉舟"), "停在见到老队友前。", 900, "ai-skeleton")
                ),
                List.of(),
                List.of("用于 node mode 第一阶段测试。")
        );
        ChapterSkeletonPlanner planner = (projectId, chapterId) -> Optional.of(skeleton);
        StorySessionContextAssembler contextAssembler = (projectId, chapterId, sceneId) -> Optional.of(new StorySessionContextPacket(
                projectId,
                chapterId,
                sceneId,
                new SceneBindingContext(sceneId, sceneId, SceneBindingMode.CHAPTER_COLD_START, false, "冷启动", null),
                new ProjectBriefView(projectId, "旧日王座", "退役者归来", "围绕回归与重启展开。"),
                new StoryUnitSummaryView(new StoryUnitRef(String.valueOf(chapterId), "chapter:" + chapterId, StoryUnitType.CHAPTER), StoryUnitType.CHAPTER, "退役者的邀请函", "林沉舟收到旧队召回。"),
                new ChapterAnchorBundleView(projectId, chapterId, "退役者的邀请函", null, "", 101L, "林沉舟", List.of("老陈"), List.of("回归"), List.of("退役者的邀请"), "林沉舟收到旧队召回。"),
                new ReaderKnownStateView(projectId, chapterId, List.of("林沉舟已退役两年"), List.of("退役真相")),
                new RecentStoryProgressView(projectId, List.of()),
                List.of(),
                null,
                List.of()
        ));
        StoryStateProperties properties = new StoryStateProperties();
        properties.setRedisStoreEnabled(true);
        ResilientStoryStateStore store = new ResilientStoryStateStore(new NullStringRedisTemplateProvider(), new ObjectMapper(), properties);

        DefaultChapterNodeRuntimeService service = new DefaultChapterNodeRuntimeService(
                planner,
                contextAssembler,
                store,
                store,
                store,
                store,
                store,
                store,
                store
        );

        ChapterNodeRuntimeView before = service.preview(28L, 31L).orElseThrow();
        assertEquals("node-1", before.currentNodeId());
        assertTrue(before.completedNodeIds().isEmpty());

        NodeResolutionResult resolved = service.resolve(new NodeActionRequest(
                28L,
                31L,
                "node-1",
                "",
                "advance-goal",
                ""
        )).orElseThrow();
        assertEquals("node-1", resolved.nodeId());
        assertEquals("node-2", resolved.nextCheckpoint().nodeId());
        assertEquals("林沉舟", resolved.actionIntent().actorId());
        assertEquals("turn-1".substring(0, 4), resolved.resolvedTurn().turnId().substring(0, 4));
        assertEquals("node-loop:node-2", resolved.chapterState().openLoops().getFirst());
        assertEquals("主角决定回归", resolved.readerRevealState().readerKnown().getFirst());

        ChapterNodeRuntimeView after = service.preview(28L, 31L).orElseThrow();
        assertEquals("node-2", after.currentNodeId());
        assertEquals(List.of("node-1"), after.completedNodeIds());
        assertEquals(1, after.checkpoints().size());
    }

    private static class NullStringRedisTemplateProvider implements ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> {

        @Override
        public org.springframework.data.redis.core.StringRedisTemplate getObject(Object... args) {
            return null;
        }

        @Override
        public org.springframework.data.redis.core.StringRedisTemplate getIfAvailable() {
            return null;
        }

        @Override
        public org.springframework.data.redis.core.StringRedisTemplate getIfUnique() {
            return null;
        }

        @Override
        public org.springframework.data.redis.core.StringRedisTemplate getObject() {
            return null;
        }
    }
}
