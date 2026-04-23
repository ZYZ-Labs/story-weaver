package com.storyweaver.storyunit.migration.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.service.ChapterService;
import com.storyweaver.storyunit.migration.LegacyBackfillAnalysisService;
import com.storyweaver.storyunit.migration.LegacyBackfillDryRun;
import com.storyweaver.storyunit.migration.LegacyBackfillDryRunService;
import com.storyweaver.storyunit.migration.LegacyChapterBackfillAnalysis;
import com.storyweaver.storyunit.migration.LegacyProjectBackfillOverview;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultLegacyBackfillOverviewServiceTest {

    @Test
    void shouldReturnEmptyOverviewForMissingProjectId() {
        DefaultLegacyBackfillOverviewService service = new DefaultLegacyBackfillOverviewService(
                mock(ChapterService.class),
                mock(LegacyBackfillAnalysisService.class),
                mock(LegacyBackfillDryRunService.class)
        );

        assertTrue(service.buildProjectOverview(null).isEmpty());
    }

    @Test
    void shouldAggregateChapterBackfillStatus() {
        ChapterService chapterService = mock(ChapterService.class);
        LegacyBackfillAnalysisService analysisService = mock(LegacyBackfillAnalysisService.class);
        LegacyBackfillDryRunService dryRunService = mock(LegacyBackfillDryRunService.class);

        Chapter chapter31 = new Chapter();
        chapter31.setId(31L);
        chapter31.setTitle("退役者的邀请函");
        chapter31.setProjectId(28L);
        Chapter chapter32 = new Chapter();
        chapter32.setId(32L);
        chapter32.setTitle("新战队试探");
        chapter32.setProjectId(28L);

        when(chapterService.list(any(QueryWrapper.class))).thenReturn(List.of(chapter31, chapter32));
        LegacyChapterBackfillAnalysis chapter31Analysis = new LegacyChapterBackfillAnalysis(
                28L, 31L, "退役者的邀请函",
                true, true,
                5, 4, 3, 1,
                4, 2, 1, 0,
                0, 0, 0,
                false, false,
                true, true,
                List.of("当前章节仍缺 scene 基线。")
        );
        LegacyChapterBackfillAnalysis chapter32Analysis = new LegacyChapterBackfillAnalysis(
                28L, 32L, "新战队试探",
                true, true,
                2, 2, 2, 0,
                2, 2, 0, 0,
                2, 2, 1,
                true, true,
                false, false,
                List.of()
        );
        when(analysisService.analyzeChapter(28L, 31L)).thenReturn(Optional.of(chapter31Analysis));
        when(analysisService.analyzeChapter(28L, 32L)).thenReturn(Optional.of(chapter32Analysis));
        when(dryRunService.planChapterBackfill(28L, 31L)).thenReturn(Optional.of(
                new LegacyBackfillDryRun(
                        chapter31Analysis,
                        true,
                        List.of(),
                        List.of()
                )
        ));
        when(dryRunService.planChapterBackfill(28L, 32L)).thenReturn(Optional.of(
                new LegacyBackfillDryRun(
                        chapter32Analysis,
                        false,
                        List.of(),
                        List.of()
                )
        ));

        DefaultLegacyBackfillOverviewService service = new DefaultLegacyBackfillOverviewService(
                chapterService,
                analysisService,
                dryRunService
        );

        LegacyProjectBackfillOverview overview = service.buildProjectOverview(28L).orElseThrow();

        assertEquals(2, overview.totalChapters());
        assertEquals(2, overview.analyzedChapters());
        assertEquals(1, overview.chaptersNeedingSceneBackfill());
        assertEquals(1, overview.chaptersNeedingStateBackfill());
        assertEquals(1, overview.chaptersReadyForBackfill());
        assertEquals("退役者的邀请函", overview.chapters().get(0).chapterTitle());
    }
}
