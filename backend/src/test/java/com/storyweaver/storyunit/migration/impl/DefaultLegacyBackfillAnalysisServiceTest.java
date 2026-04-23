package com.storyweaver.storyunit.migration.impl;

import com.storyweaver.domain.entity.AIWritingRecord;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.repository.AIWritingRecordMapper;
import com.storyweaver.service.ChapterService;
import com.storyweaver.storyunit.event.StoryEvent;
import com.storyweaver.storyunit.event.StoryEventType;
import com.storyweaver.storyunit.migration.LegacyChapterBackfillAnalysis;
import com.storyweaver.storyunit.model.StorySourceTrace;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.patch.PatchStatus;
import com.storyweaver.storyunit.patch.StoryPatch;
import com.storyweaver.storyunit.service.ChapterIncrementalStateStore;
import com.storyweaver.storyunit.service.ReaderRevealStateStore;
import com.storyweaver.storyunit.service.SceneExecutionStateQueryService;
import com.storyweaver.storyunit.service.StoryEventStore;
import com.storyweaver.storyunit.service.StoryPatchStore;
import com.storyweaver.storyunit.service.StorySnapshotStore;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultLegacyBackfillAnalysisServiceTest {

    @Test
    void shouldReturnEmptyWhenChapterDoesNotBelongToProject() {
        ChapterService chapterService = mock(ChapterService.class);
        when(chapterService.getById(31L)).thenReturn(null);

        DefaultLegacyBackfillAnalysisService service = new DefaultLegacyBackfillAnalysisService(
                chapterService,
                mock(AIWritingRecordMapper.class),
                mock(SceneExecutionStateQueryService.class),
                mock(StoryEventStore.class),
                mock(StorySnapshotStore.class),
                mock(StoryPatchStore.class),
                mock(ReaderRevealStateStore.class),
                mock(ChapterIncrementalStateStore.class)
        );

        assertTrue(service.analyzeChapter(28L, 31L).isEmpty());
    }

    @Test
    void shouldSummarizeLegacyCoverageGap() {
        ChapterService chapterService = mock(ChapterService.class);
        AIWritingRecordMapper mapper = mock(AIWritingRecordMapper.class);
        SceneExecutionStateQueryService sceneQueryService = mock(SceneExecutionStateQueryService.class);
        StoryEventStore eventStore = mock(StoryEventStore.class);
        StorySnapshotStore snapshotStore = mock(StorySnapshotStore.class);
        StoryPatchStore patchStore = mock(StoryPatchStore.class);
        ReaderRevealStateStore revealStateStore = mock(ReaderRevealStateStore.class);
        ChapterIncrementalStateStore chapterStateStore = mock(ChapterIncrementalStateStore.class);

        Chapter chapter = new Chapter();
        chapter.setId(31L);
        chapter.setProjectId(28L);
        chapter.setTitle("退役者的邀请函");
        chapter.setSummary("章节摘要");
        chapter.setContent("章节正文");

        when(chapterService.getById(31L)).thenReturn(chapter);
        when(mapper.findByChapterId(31L)).thenReturn(List.of(
                record(101L, 31L, "accepted", "第一段正文"),
                record(102L, 31L, "failed", "第二段正文"),
                record(103L, 31L, "pending", null)
        ));
        when(sceneQueryService.listChapterScenes(28L, 31L)).thenReturn(List.of(
                new SceneExecutionState(28L, 31L, "scene-1", 1, SceneExecutionStatus.COMPLETED, "opening-31",
                        "开场", "", List.of(), List.of(), List.of(), Map.of("source", "aiWritingRecord"), "", "完成"),
                new SceneExecutionState(28L, 31L, "scene-9", 9, SceneExecutionStatus.WRITTEN, "runtime-9",
                        "新增运行时镜头", "", List.of(), List.of(), List.of(), Map.of("source", "runtime"), "", "运行时补入")
        ));
        when(eventStore.listChapterEvents(28L, 31L)).thenReturn(List.of(
                new StoryEvent("event-1", StoryEventType.SCENE_COMPLETED, 28L, 31L, "scene-9",
                        new StoryUnitRef("scene-9", "scene-execution:31:scene-9", StoryUnitType.SCENE_EXECUTION),
                        "scene-9 completed", Map.of(), new StorySourceTrace("test", "test", "test", "scene-9"))
        ));
        when(snapshotStore.listChapterSnapshots(28L, 31L)).thenReturn(List.of());
        when(patchStore.listChapterPatches(28L, 31L)).thenReturn(List.of(
                new StoryPatch("patch-1", new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER),
                        com.storyweaver.storyunit.model.FacetType.REVEAL, List.of(), "patch", PatchStatus.APPLIED,
                        new StorySourceTrace("test", "test", "test", "scene-9"))
        ));
        when(revealStateStore.findChapterRevealState(28L, 31L)).thenReturn(Optional.empty());
        when(chapterStateStore.findChapterState(28L, 31L)).thenReturn(Optional.empty());

        DefaultLegacyBackfillAnalysisService service = new DefaultLegacyBackfillAnalysisService(
                chapterService,
                mapper,
                sceneQueryService,
                eventStore,
                snapshotStore,
                patchStore,
                revealStateStore,
                chapterStateStore
        );

        LegacyChapterBackfillAnalysis analysis = service.analyzeChapter(28L, 31L).orElseThrow();

        assertEquals("退役者的邀请函", analysis.chapterTitle());
        assertEquals(3, analysis.legacyRecordCount());
        assertEquals(2, analysis.legacyGeneratedRecordCount());
        assertEquals(1, analysis.legacyAcceptedRecordCount());
        assertEquals(1, analysis.legacyFailedRecordCount());
        assertEquals(2, analysis.derivedSceneCount());
        assertEquals(2, analysis.completedSceneCount());
        assertEquals(0, analysis.failedSceneCount());
        assertEquals(1, analysis.runtimeOnlySceneCount());
        assertEquals(1, analysis.eventCount());
        assertEquals(0, analysis.snapshotCount());
        assertEquals(1, analysis.patchCount());
        assertFalse(analysis.hasReaderRevealState());
        assertFalse(analysis.hasChapterState());
        assertTrue(analysis.needsSceneBackfill());
        assertTrue(analysis.needsStateBackfill());
        assertTrue(analysis.notes().contains("旧记录尚未形成 StorySnapshot 基线。"));
        assertTrue(analysis.notes().contains("章节 ReaderRevealState 尚未建立。"));
        assertTrue(analysis.notes().contains("已存在超出旧写作记录映射范围的新 scene runtime state。"));
    }

    private AIWritingRecord record(Long id, Long chapterId, String status, String generatedContent) {
        AIWritingRecord record = new AIWritingRecord();
        record.setId(id);
        record.setChapterId(chapterId);
        record.setStatus(status);
        record.setGeneratedContent(generatedContent);
        return record;
    }
}
