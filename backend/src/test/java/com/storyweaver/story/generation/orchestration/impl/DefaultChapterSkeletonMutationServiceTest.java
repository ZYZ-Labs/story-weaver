package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.ChapterSkeleton;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonPlanner;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonStore;
import com.storyweaver.story.generation.orchestration.SceneSkeletonItem;
import com.storyweaver.storyunit.service.SceneRuntimeStateStore;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultChapterSkeletonMutationServiceTest {

    @Test
    void shouldDeleteCompletedSceneAndClearRuntimeState() {
        ChapterSkeletonPlanner chapterSkeletonPlanner = mock(ChapterSkeletonPlanner.class);
        ChapterSkeletonStore chapterSkeletonStore = mock(ChapterSkeletonStore.class);
        SceneRuntimeStateStore sceneRuntimeStateStore = mock(SceneRuntimeStateStore.class);

        ChapterSkeleton skeleton = new ChapterSkeleton(
                28L,
                31L,
                "skeleton_31_v1",
                2,
                "scene-2 停住。",
                List.of(
                        new SceneSkeletonItem(
                                "scene-1",
                                1,
                                SceneExecutionStatus.COMPLETED,
                                "完成开场",
                                List.of("主角登场"),
                                List.of("POV=林沉舟"),
                                "scene-1 停住。",
                                900,
                                "existing-scene-state"
                        ),
                        new SceneSkeletonItem(
                                "scene-2",
                                2,
                                SceneExecutionStatus.PLANNED,
                                "推进邀请函",
                                List.of("邀请函来自旧战队"),
                                List.of("summary=退役者的邀请函"),
                                "scene-2 停住。",
                                1000,
                                "planned"
                        )
                ),
                List.of(),
                List.of("note")
        );

        when(chapterSkeletonPlanner.plan(28L, 31L)).thenReturn(Optional.of(skeleton));
        when(chapterSkeletonStore.save(any(ChapterSkeleton.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DefaultChapterSkeletonMutationService service = new DefaultChapterSkeletonMutationService(
                chapterSkeletonPlanner,
                chapterSkeletonStore,
                sceneRuntimeStateStore
        );

        ChapterSkeleton updated = service.deleteScene(28L, 31L, "scene-1").orElseThrow();

        assertEquals(1, updated.sceneCount());
        assertEquals("scene-2", updated.scenes().getFirst().sceneId());
        assertTrue(updated.deletedSceneIds().contains("scene-1"));
        assertTrue(updated.planningNotes().stream().anyMatch(note -> note.contains("已删除 scene-1")));
        verify(sceneRuntimeStateStore).deleteSceneState(28L, 31L, "scene-1");
        verify(sceneRuntimeStateStore).deleteHandoffsReferencingScene(28L, 31L, "scene-1");
    }
}
