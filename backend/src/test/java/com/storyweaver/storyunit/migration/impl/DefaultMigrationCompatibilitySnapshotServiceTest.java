package com.storyweaver.storyunit.migration.impl;

import com.storyweaver.config.StoryCompatibilityProperties;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.service.ChapterService;
import com.storyweaver.storyunit.migration.LegacyBackfillAnalysisService;
import com.storyweaver.storyunit.migration.LegacyChapterBackfillAnalysis;
import com.storyweaver.storyunit.migration.MigrationCompatibilitySnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultMigrationCompatibilitySnapshotServiceTest {

    @Test
    void shouldReturnEmptyWhenChapterMissing() {
        ChapterService chapterService = mock(ChapterService.class);
        LegacyBackfillAnalysisService analysisService = mock(LegacyBackfillAnalysisService.class);
        StoryCompatibilityProperties properties = new StoryCompatibilityProperties();
        when(chapterService.getById(31L)).thenReturn(null);

        DefaultMigrationCompatibilitySnapshotService service = new DefaultMigrationCompatibilitySnapshotService(
                chapterService,
                analysisService,
                properties
        );

        assertTrue(service.getChapterSnapshot(28L, 31L).isEmpty());
    }

    @Test
    void shouldBuildCompatibilitySnapshotFromPropertiesAndAnalysis() {
        ChapterService chapterService = mock(ChapterService.class);
        LegacyBackfillAnalysisService analysisService = mock(LegacyBackfillAnalysisService.class);
        StoryCompatibilityProperties properties = new StoryCompatibilityProperties();
        properties.setLegacyWritingCenterEnabled(true);
        properties.setStoryContextDualReadEnabled(true);
        properties.setSummaryWorkflowDualWriteEnabled(true);
        properties.setBackfillExecuteEnabled(true);
        properties.setChapterWorkspaceNodePreviewEnabled(true);
        properties.setChapterWorkspaceNodeResolveEnabled(false);

        Chapter chapter = new Chapter();
        chapter.setId(31L);
        chapter.setProjectId(28L);
        chapter.setTitle("退役者的邀请函");

        when(chapterService.getById(31L)).thenReturn(chapter);
        when(analysisService.analyzeChapter(28L, 31L)).thenReturn(Optional.of(
                new LegacyChapterBackfillAnalysis(
                        28L, 31L, "退役者的邀请函",
                        true, true,
                        5, 4, 3, 1,
                        4, 2, 1, 1,
                        0, 0, 0,
                        false, false,
                        true, true,
                        List.of("旧记录尚未形成 StoryEvent 基线。")
                )
        ));

        DefaultMigrationCompatibilitySnapshotService service = new DefaultMigrationCompatibilitySnapshotService(
                chapterService,
                analysisService,
                properties
        );

        MigrationCompatibilitySnapshot snapshot = service.getChapterSnapshot(28L, 31L).orElseThrow();

        assertEquals("退役者的邀请函", snapshot.chapterTitle());
        assertEquals("chapter-workspace", snapshot.pageBoundaries().get(0).boundaryKey());
        assertEquals("story-context", snapshot.apiBoundaries().get(0).boundaryKey());
        assertTrue(snapshot.featureFlags().contains("storyContextDualReadEnabled=true"));
        assertTrue(snapshot.featureFlags().contains("chapterWorkspaceNodeResolveEnabled=false"));
        assertTrue(snapshot.riskNotes().stream().anyMatch(note -> note.contains("scene 基线")));
        assertTrue(snapshot.riskNotes().stream().anyMatch(note -> note.contains("node runtime 当前仅开放预览")));
    }
}
