package com.storyweaver.controller;

import com.storyweaver.exception.GlobalExceptionHandler;
import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;
import com.storyweaver.storyunit.event.StoryEvent;
import com.storyweaver.storyunit.event.StoryEventType;
import com.storyweaver.storyunit.model.StorySourceTrace;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.patch.PatchOperation;
import com.storyweaver.storyunit.patch.PatchOperationType;
import com.storyweaver.storyunit.patch.PatchStatus;
import com.storyweaver.storyunit.patch.StoryPatch;
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

    @BeforeEach
    void setUp() {
        storyEventStore = mock(StoryEventStore.class);
        storySnapshotStore = mock(StorySnapshotStore.class);
        storyPatchStore = mock(StoryPatchStore.class);
        readerRevealStateStore = mock(ReaderRevealStateStore.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new StoryStateController(
                        storyEventStore,
                        storySnapshotStore,
                        storyPatchStore,
                        readerRevealStateStore
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
}
