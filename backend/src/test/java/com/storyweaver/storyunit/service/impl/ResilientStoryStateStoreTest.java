package com.storyweaver.storyunit.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;
import com.storyweaver.storyunit.facet.state.ChapterIncrementalState;
import com.storyweaver.config.StoryStateProperties;
import com.storyweaver.storyunit.event.StoryEvent;
import com.storyweaver.storyunit.event.StoryEventType;
import com.storyweaver.storyunit.model.StorySourceTrace;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.patch.PatchOperation;
import com.storyweaver.storyunit.patch.PatchOperationType;
import com.storyweaver.storyunit.patch.PatchStatus;
import com.storyweaver.storyunit.patch.StoryPatch;
import com.storyweaver.storyunit.runtime.StoryActionIntent;
import com.storyweaver.storyunit.runtime.StoryLoopStatus;
import com.storyweaver.storyunit.runtime.StoryNodeCheckpoint;
import com.storyweaver.storyunit.runtime.StoryOpenLoop;
import com.storyweaver.storyunit.runtime.StoryResolvedTurn;
import com.storyweaver.storyunit.snapshot.SnapshotScope;
import com.storyweaver.storyunit.snapshot.StorySnapshot;
import com.storyweaver.storyunit.model.FacetType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResilientStoryStateStoreTest {

    @Test
    void shouldFallbackToInMemoryWhenRedisTemplateMissing() {
        StoryStateProperties properties = new StoryStateProperties();
        properties.setRedisStoreEnabled(true);
        ResilientStoryStateStore store = new ResilientStoryStateStore(
                new NullStringRedisTemplateProvider(),
                new ObjectMapper(),
                properties
        );

        StoryEvent event = new StoryEvent(
                "event-1",
                StoryEventType.SCENE_COMPLETED,
                28L,
                31L,
                "scene-1",
                new StoryUnitRef("scene-1", "scene-execution:31:scene-1", StoryUnitType.SCENE_EXECUTION),
                "scene-1 已写回为 COMPLETED",
                java.util.Map.of("status", "COMPLETED"),
                new StorySourceTrace("test", "test", "SceneExecutionWriteService", "scene-1")
        );
        StorySnapshot snapshot = new StorySnapshot(
                "snapshot-1",
                SnapshotScope.SCENE,
                28L,
                31L,
                "scene-1",
                List.of(new StoryUnitRef("scene-1", "scene-execution:31:scene-1", StoryUnitType.SCENE_EXECUTION)),
                "scene-1 snapshot",
                new StorySourceTrace("test", "test", "SceneExecutionWriteService", "scene-1")
        );

        store.appendEvent(event);
        store.saveSnapshot(snapshot);
        store.appendPatch(28L, 31L, new StoryPatch(
                "patch-1",
                new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER),
                FacetType.REVEAL,
                List.of(new PatchOperation(PatchOperationType.MERGE, "/readerKnown", List.of("主角决定赴约"))),
                "scene-1 的 reveal patch",
                PatchStatus.APPLIED,
                new StorySourceTrace("test", "test", "SceneExecutionWriteService", "scene-1")
        ));
        store.saveChapterRevealState(new ReaderRevealState(
                28L,
                31L,
                List.of("系统已知"),
                List.of("作者已知"),
                List.of("主角决定赴约"),
                List.of("更大的阴谋"),
                "读者已知 1 条，未揭晓 1 条"
        ));
        store.saveChapterState(new ChapterIncrementalState(
                28L,
                31L,
                List.of("scene:scene-2:pending"),
                List.of("scene:scene-1:pending"),
                List.of("办公室"),
                java.util.Map.of("林沉舟", "紧张"),
                java.util.Map.of("林沉舟", "谨慎"),
                java.util.Map.of("林沉舟", List.of("观察中")),
                "scene-1 已写回章节状态"
        ));
        store.recordIntent(new StoryActionIntent(
                "intent-1",
                28L,
                31L,
                "checkpoint-1",
                "node-1",
                "林沉舟",
                "player",
                "option-return",
                "接受老陈的邀约",
                "确认回归并登录新纪元",
                java.util.Map.of("tone", "restrained"),
                new StorySourceTrace("test", "test", "NodeDecisionService", "intent-1")
        ));
        store.recordTurn(new StoryResolvedTurn(
                "turn-1",
                28L,
                31L,
                "checkpoint-1",
                "node-1",
                "intent-1",
                "林沉舟决定登录游戏，旧战队线重新启动。",
                List.of("event-1"),
                java.util.Map.of("loginState", "ready"),
                List.of("读者知道他准备回归"),
                List.of("loop-return"),
                List.of(),
                "checkpoint-2",
                new StorySourceTrace("test", "test", "WorldResolverService", "turn-1")
        ));
        store.saveCheckpoint(new StoryNodeCheckpoint(
                "checkpoint-2",
                28L,
                31L,
                "node-2",
                "checkpoint-1",
                2,
                "主角已决定回归，世界仍停在开服前夜。",
                "读者知道主角将于下一节点进入游戏大厅。",
                List.of("loop-return"),
                java.util.Map.of("林沉舟", "出租屋"),
                java.util.Map.of("林沉舟", "进入新纪元"),
                List.of("option-enter-game", "option-call-old-team"),
                new StorySourceTrace("test", "test", "CheckpointService", "checkpoint-2")
        ));
        store.saveLoop(new StoryOpenLoop(
                "loop-return",
                28L,
                31L,
                "node-1",
                "林沉舟是否正式回归旧战队",
                StoryLoopStatus.OPEN,
                "林沉舟",
                "在见到老陈和旧队友后回收",
                "turn-1",
                null,
                List.of("chapter:31", "character:林沉舟"),
                new StorySourceTrace("test", "test", "OpenLoopService", "loop-return")
        ));

        assertEquals(1, store.listChapterEvents(28L, 31L).size());
        assertEquals("event-1", store.listChapterEvents(28L, 31L).getFirst().eventId());
        assertEquals(1, store.listChapterSnapshots(28L, 31L).size());
        assertEquals("snapshot-1", store.listChapterSnapshots(28L, 31L).getFirst().snapshotId());
        assertEquals(1, store.listChapterPatches(28L, 31L).size());
        assertEquals("patch-1", store.listChapterPatches(28L, 31L).getFirst().patchId());
        assertEquals("主角决定赴约", store.findChapterRevealState(28L, 31L).orElseThrow().readerKnown().getFirst());
        assertEquals("scene:scene-2:pending", store.findChapterState(28L, 31L).orElseThrow().openLoops().getFirst());
        assertEquals("intent-1", store.listChapterIntents(28L, 31L).getFirst().intentId());
        assertEquals("turn-1", store.listChapterTurns(28L, 31L).getFirst().turnId());
        assertEquals("checkpoint-2", store.findCheckpoint(28L, 31L, "checkpoint-2").orElseThrow().checkpointId());
        assertEquals("loop-return", store.listChapterLoops(28L, 31L).getFirst().loopId());
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
