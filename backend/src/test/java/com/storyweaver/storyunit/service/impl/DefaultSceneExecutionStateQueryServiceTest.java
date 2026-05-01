package com.storyweaver.storyunit.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.domain.entity.AIWritingRecord;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.repository.AIWritingRecordMapper;
import com.storyweaver.service.ChapterService;
import com.storyweaver.storyunit.service.SceneRuntimeStateStore;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultSceneExecutionStateQueryServiceTest {

    @Test
    void shouldDeriveSceneStatesFromWritingRecords() {
        AIWritingRecordMapper mapper = mock(AIWritingRecordMapper.class);
        ChapterService chapterService = mock(ChapterService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SceneRuntimeStateStore> runtimeStoreProvider = mock(ObjectProvider.class);

        Chapter chapter = new Chapter();
        chapter.setId(31L);
        chapter.setProjectId(28L);

        AIWritingRecord first = record(
                101L,
                31L,
                "accepted",
                "draft",
                "让主角收到邀请函",
                "第一段正文。林沉舟看见了那封邀请函。",
                """
                {
                  "readerReveal": {
                    "revealTargets": ["收到旧战队邀请"]
                  },
                  "director": {
                    "mode": "opening",
                    "decisionSummary": "先建立开场与触发点"
                  }
                }
                """,
                LocalDateTime.of(2026, 4, 17, 10, 0)
        );
        AIWritingRecord second = record(
                102L,
                31L,
                "rejected",
                "continue",
                "继续推进训练赛前的犹豫",
                "第二段正文。林沉舟没有立刻回复，他只是把邮件关掉。",
                """
                {
                  "readerReveal": {
                    "revealTargets": ["主角没有立刻答应"]
                  },
                  "director": {
                    "mode": "transition"
                  }
                }
                """,
                LocalDateTime.of(2026, 4, 17, 11, 0)
        );

        when(chapterService.getById(31L)).thenReturn(chapter);
        when(mapper.findByChapterId(31L)).thenReturn(List.of(first, second));
        when(runtimeStoreProvider.getIfAvailable()).thenReturn(null);

        DefaultSceneExecutionStateQueryService service = new DefaultSceneExecutionStateQueryService(
                mapper,
                chapterService,
                new ObjectMapper(),
                runtimeStoreProvider
        );

        List<SceneExecutionState> scenes = service.listChapterScenes(28L, 31L);

        assertEquals(2, scenes.size());
        assertEquals("scene-1", scenes.get(0).sceneId());
        assertEquals(SceneExecutionStatus.COMPLETED, scenes.get(0).status());
        assertEquals("scene-2", scenes.get(1).sceneId());
        assertEquals(SceneExecutionStatus.FAILED, scenes.get(1).status());
        assertEquals(List.of("收到旧战队邀请"), scenes.get(0).readerRevealDelta());
        assertTrue(scenes.get(0).handoffLine().contains("邀请函"));
    }

    @Test
    void shouldPreferLatestNonFailedSceneForFallback() {
        AIWritingRecordMapper mapper = mock(AIWritingRecordMapper.class);
        ChapterService chapterService = mock(ChapterService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SceneRuntimeStateStore> runtimeStoreProvider = mock(ObjectProvider.class);

        Chapter chapter = new Chapter();
        chapter.setId(31L);
        chapter.setProjectId(28L);

        AIWritingRecord first = record(
                101L,
                31L,
                "accepted",
                "draft",
                "先做开场",
                "第一段正文。主角收到了邀请。",
                null,
                LocalDateTime.of(2026, 4, 17, 10, 0)
        );
        AIWritingRecord second = record(
                102L,
                31L,
                "rejected",
                "continue",
                "继续推进",
                "第二段正文。主角没有立刻回复。",
                null,
                LocalDateTime.of(2026, 4, 17, 11, 0)
        );

        when(chapterService.getById(31L)).thenReturn(chapter);
        when(mapper.findByChapterId(31L)).thenReturn(List.of(first, second));
        when(runtimeStoreProvider.getIfAvailable()).thenReturn(null);

        DefaultSceneExecutionStateQueryService service = new DefaultSceneExecutionStateQueryService(
                mapper,
                chapterService,
                new ObjectMapper(),
                runtimeStoreProvider
        );

        Optional<SceneExecutionState> latest = service.findLatestChapterScene(28L, 31L);

        assertTrue(latest.isPresent());
        assertEquals("scene-1", latest.orElseThrow().sceneId());
        assertEquals(SceneExecutionStatus.COMPLETED, latest.orElseThrow().status());
    }

    @Test
    void shouldOverlayRuntimeStateOnTopOfLegacyScene() {
        AIWritingRecordMapper mapper = mock(AIWritingRecordMapper.class);
        ChapterService chapterService = mock(ChapterService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SceneRuntimeStateStore> runtimeStoreProvider = mock(ObjectProvider.class);
        SceneRuntimeStateStore runtimeStateStore = mock(SceneRuntimeStateStore.class);

        Chapter chapter = new Chapter();
        chapter.setId(31L);
        chapter.setProjectId(28L);

        AIWritingRecord first = record(
                101L,
                31L,
                "accepted",
                "draft",
                "先做开场",
                "第一段正文。主角收到了邀请。",
                null,
                LocalDateTime.of(2026, 4, 17, 10, 0)
        );

        SceneExecutionState runtimeScene = new SceneExecutionState(
                28L,
                31L,
                "scene-2",
                2,
                SceneExecutionStatus.COMPLETED,
                "mainline-31",
                "推进邀请函主线",
                "主角决定赴约后停住。",
                List.of("主角决定赴约"),
                List.of(),
                List.of(),
                Map.of("source", "runtime"),
                "林沉舟按下发送键。",
                "主角最终决定赴约。"
        );

        when(chapterService.getById(31L)).thenReturn(chapter);
        when(mapper.findByChapterId(31L)).thenReturn(List.of(first));
        when(runtimeStoreProvider.getIfAvailable()).thenReturn(runtimeStateStore);
        when(runtimeStateStore.listChapterScenes(28L, 31L)).thenReturn(List.of(runtimeScene));

        DefaultSceneExecutionStateQueryService service = new DefaultSceneExecutionStateQueryService(
                mapper,
                chapterService,
                new ObjectMapper(),
                runtimeStoreProvider
        );

        List<SceneExecutionState> scenes = service.listChapterScenes(28L, 31L);

        assertEquals(2, scenes.size());
        assertEquals("scene-1", scenes.get(0).sceneId());
        assertEquals("scene-2", scenes.get(1).sceneId());
        assertEquals("主角最终决定赴约。", scenes.get(1).outcomeSummary());
    }

    @Test
    void shouldIgnoreChapterWorkspaceSceneDraftRecordsWhenDerivingLegacyStates() {
        AIWritingRecordMapper mapper = mock(AIWritingRecordMapper.class);
        ChapterService chapterService = mock(ChapterService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SceneRuntimeStateStore> runtimeStoreProvider = mock(ObjectProvider.class);

        Chapter chapter = new Chapter();
        chapter.setId(31L);
        chapter.setProjectId(28L);

        AIWritingRecord record = record(
                103L,
                31L,
                "draft",
                "continue",
                "【镜头ID】scene-2\n继续推进训练赛前的犹豫",
                "镜头草稿正文。",
                """
                {
                  "orchestration": {
                    "entryPoint": "phase8.chapter-workspace.scene-draft",
                    "sceneId": "scene-2"
                  }
                }
                """,
                LocalDateTime.of(2026, 4, 17, 12, 0)
        );

        when(chapterService.getById(31L)).thenReturn(chapter);
        when(mapper.findByChapterId(31L)).thenReturn(List.of(record));
        when(runtimeStoreProvider.getIfAvailable()).thenReturn(null);

        DefaultSceneExecutionStateQueryService service = new DefaultSceneExecutionStateQueryService(
                mapper,
                chapterService,
                new ObjectMapper(),
                runtimeStoreProvider
        );

        assertTrue(service.listChapterScenes(28L, 31L).isEmpty());
    }

    private AIWritingRecord record(
            Long id,
            Long chapterId,
            String status,
            String writingType,
            String userInstruction,
            String generatedContent,
            String traceJson,
            LocalDateTime createTime) {
        AIWritingRecord record = new AIWritingRecord();
        record.setId(id);
        record.setChapterId(chapterId);
        record.setStatus(status);
        record.setWritingType(writingType);
        record.setUserInstruction(userInstruction);
        record.setGeneratedContent(generatedContent);
        record.setGenerationTraceJson(traceJson);
        record.setCreateTime(createTime);
        return record;
    }
}
