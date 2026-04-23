package com.storyweaver.storyunit.migration.impl;

import com.storyweaver.storyunit.migration.LegacyBackfillActionPlan;
import com.storyweaver.storyunit.migration.LegacyBackfillAnalysisService;
import com.storyweaver.storyunit.migration.LegacyBackfillDryRun;
import com.storyweaver.storyunit.migration.LegacyChapterBackfillAnalysis;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultLegacyBackfillDryRunServiceTest {

    @Test
    void shouldReturnEmptyWhenAnalysisMissing() {
        LegacyBackfillAnalysisService analysisService = mock(LegacyBackfillAnalysisService.class);
        when(analysisService.analyzeChapter(28L, 31L)).thenReturn(Optional.empty());

        DefaultLegacyBackfillDryRunService service = new DefaultLegacyBackfillDryRunService(analysisService);

        assertTrue(service.planChapterBackfill(28L, 31L).isEmpty());
    }

    @Test
    void shouldBuildDryRunActionPlanFromAnalysis() {
        LegacyBackfillAnalysisService analysisService = mock(LegacyBackfillAnalysisService.class);
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

        DefaultLegacyBackfillDryRunService service = new DefaultLegacyBackfillDryRunService(analysisService);

        LegacyBackfillDryRun dryRun = service.planChapterBackfill(28L, 31L).orElseThrow();

        assertTrue(dryRun.canRunBackfill());
        assertEquals(4, dryRun.actions().size());
        LegacyBackfillActionPlan deriveScene = dryRun.actions().get(1);
        assertEquals("derive-scene-state", deriveScene.actionKey());
        assertTrue(deriveScene.required());
        assertFalse(deriveScene.blocked());
        assertTrue(dryRun.riskNotes().contains("当前章节已经存在 runtime-only scene，回填时必须避免覆盖新状态。"));
    }
}
