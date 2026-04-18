package com.storyweaver.storyunit.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;
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

        assertEquals(1, store.listChapterEvents(28L, 31L).size());
        assertEquals("event-1", store.listChapterEvents(28L, 31L).getFirst().eventId());
        assertEquals(1, store.listChapterSnapshots(28L, 31L).size());
        assertEquals("snapshot-1", store.listChapterSnapshots(28L, 31L).getFirst().snapshotId());
        assertEquals(1, store.listChapterPatches(28L, 31L).size());
        assertEquals("patch-1", store.listChapterPatches(28L, 31L).getFirst().patchId());
        assertEquals("主角决定赴约", store.findChapterRevealState(28L, 31L).orElseThrow().readerKnown().getFirst());
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
