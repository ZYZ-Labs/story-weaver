package com.storyweaver.storyunit.migration.impl;

import com.storyweaver.storyunit.migration.LegacyBackfillActionPlan;
import com.storyweaver.storyunit.migration.LegacyBackfillDryRun;
import com.storyweaver.storyunit.migration.LegacyBackfillDryRunService;
import com.storyweaver.storyunit.migration.LegacyBackfillOverviewService;
import com.storyweaver.storyunit.migration.LegacyChapterBackfillStatusItem;
import com.storyweaver.storyunit.migration.LegacyProjectBackfillDryRun;
import com.storyweaver.storyunit.migration.LegacyProjectBackfillOverview;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultLegacyProjectBackfillDryRunServiceTest {

    @Test
    void shouldReturnEmptyForMissingProjectId() {
        DefaultLegacyProjectBackfillDryRunService service = new DefaultLegacyProjectBackfillDryRunService(
                mock(LegacyBackfillOverviewService.class),
                mock(LegacyBackfillDryRunService.class)
        );

        assertTrue(service.buildProjectDryRun(null).isEmpty());
    }

    @Test
    void shouldBuildProjectDryRunFromOverviewAndChapterPlans() {
        LegacyBackfillOverviewService overviewService = mock(LegacyBackfillOverviewService.class);
        LegacyBackfillDryRunService dryRunService = mock(LegacyBackfillDryRunService.class);

        when(overviewService.buildProjectOverview(28L)).thenReturn(Optional.of(
                new LegacyProjectBackfillOverview(
                        28L,
                        4,
                        4,
                        2,
                        1,
                        1,
                        List.of(
                                new LegacyChapterBackfillStatusItem(
                                        31L,
                                        "退役者的邀请函",
                                        4,
                                        true,
                                        false,
                                        true,
                                        List.of("scene 基线缺失")
                                ),
                                new LegacyChapterBackfillStatusItem(
                                        32L,
                                        "新战队试探",
                                        0,
                                        false,
                                        true,
                                        false,
                                        List.of("旧记录为空")
                                ),
                                new LegacyChapterBackfillStatusItem(
                                        33L,
                                        "不需回填章节",
                                        2,
                                        false,
                                        false,
                                        false,
                                        List.of()
                                )
                        )
                )
        ));

        when(dryRunService.planChapterBackfill(28L, 31L)).thenReturn(Optional.of(
                new LegacyBackfillDryRun(
                        null,
                        true,
                        List.of(new LegacyBackfillActionPlan(
                                "derive-scene-state",
                                "补齐 scene 执行基线",
                                "将旧正文记录映射为 scene。",
                                true,
                                false,
                                ""
                        )),
                        List.of("scene 基线缺失")
                )
        ));
        when(dryRunService.planChapterBackfill(28L, 32L)).thenReturn(Optional.of(
                new LegacyBackfillDryRun(
                        null,
                        false,
                        List.of(new LegacyBackfillActionPlan(
                                "derive-state-facets",
                                "补齐状态与揭晓基线",
                                "补齐 reveal/state。",
                                true,
                                true,
                                "没有可用旧正文记录"
                        )),
                        List.of("旧记录为空")
                )
        ));

        DefaultLegacyProjectBackfillDryRunService service =
                new DefaultLegacyProjectBackfillDryRunService(overviewService, dryRunService);

        LegacyProjectBackfillDryRun dryRun = service.buildProjectDryRun(28L).orElseThrow();

        assertEquals(4, dryRun.totalChapters());
        assertEquals(2, dryRun.chaptersNeedingBackfill());
        assertEquals(1, dryRun.runnableChapters());
        assertEquals(1, dryRun.blockedChapters());
        assertEquals(2, dryRun.chapters().size());
        assertEquals("退役者的邀请函", dryRun.chapters().get(0).chapterTitle());
        assertEquals("补齐 scene 执行基线（退役者的邀请函）", dryRun.requiredActions().get(0));
        assertTrue(dryRun.riskNotes().contains("旧记录为空"));
    }
}
