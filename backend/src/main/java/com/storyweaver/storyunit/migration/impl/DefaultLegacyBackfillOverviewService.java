package com.storyweaver.storyunit.migration.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.service.ChapterService;
import com.storyweaver.storyunit.migration.LegacyBackfillAnalysisService;
import com.storyweaver.storyunit.migration.LegacyBackfillDryRun;
import com.storyweaver.storyunit.migration.LegacyBackfillDryRunService;
import com.storyweaver.storyunit.migration.LegacyBackfillOverviewService;
import com.storyweaver.storyunit.migration.LegacyChapterBackfillAnalysis;
import com.storyweaver.storyunit.migration.LegacyChapterBackfillStatusItem;
import com.storyweaver.storyunit.migration.LegacyProjectBackfillOverview;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class DefaultLegacyBackfillOverviewService implements LegacyBackfillOverviewService {

    private final ChapterService chapterService;
    private final LegacyBackfillAnalysisService legacyBackfillAnalysisService;
    private final LegacyBackfillDryRunService legacyBackfillDryRunService;

    public DefaultLegacyBackfillOverviewService(
            ChapterService chapterService,
            LegacyBackfillAnalysisService legacyBackfillAnalysisService,
            LegacyBackfillDryRunService legacyBackfillDryRunService) {
        this.chapterService = chapterService;
        this.legacyBackfillAnalysisService = legacyBackfillAnalysisService;
        this.legacyBackfillDryRunService = legacyBackfillDryRunService;
    }

    @Override
    public Optional<LegacyProjectBackfillOverview> buildProjectOverview(Long projectId) {
        if (projectId == null) {
            return Optional.empty();
        }

        List<Chapter> chapters = chapterService.list(new QueryWrapper<Chapter>()
                .eq("project_id", projectId)
                .eq("deleted", 0)
                .orderByAsc("order_num")
                .orderByAsc("create_time"));
        if (chapters == null || chapters.isEmpty()) {
            return Optional.of(new LegacyProjectBackfillOverview(projectId, 0, 0, 0, 0, 0, List.of()));
        }

        List<LegacyChapterBackfillStatusItem> items = new ArrayList<>();
        int analyzedChapters = 0;
        int chaptersNeedingSceneBackfill = 0;
        int chaptersNeedingStateBackfill = 0;
        int chaptersReadyForBackfill = 0;

        for (Chapter chapter : chapters) {
            Optional<LegacyChapterBackfillAnalysis> analysisOptional = legacyBackfillAnalysisService.analyzeChapter(projectId, chapter.getId());
            if (analysisOptional.isEmpty()) {
                continue;
            }
            analyzedChapters++;
            LegacyChapterBackfillAnalysis analysis = analysisOptional.orElseThrow();
            boolean needsSceneBackfill = analysis.needsSceneBackfill();
            boolean needsStateBackfill = analysis.needsStateBackfill();
            if (needsSceneBackfill) {
                chaptersNeedingSceneBackfill++;
            }
            if (needsStateBackfill) {
                chaptersNeedingStateBackfill++;
            }
            boolean canRunBackfill = legacyBackfillDryRunService.planChapterBackfill(projectId, chapter.getId())
                    .map(LegacyBackfillDryRun::canRunBackfill)
                    .orElse(false);
            if (canRunBackfill) {
                chaptersReadyForBackfill++;
            }
            items.add(new LegacyChapterBackfillStatusItem(
                    chapter.getId(),
                    chapter.getTitle(),
                    analysis.legacyGeneratedRecordCount(),
                    needsSceneBackfill,
                    needsStateBackfill,
                    canRunBackfill,
                    analysis.notes()
            ));
        }

        items.sort(Comparator.comparing(LegacyChapterBackfillStatusItem::chapterId));
        return Optional.of(new LegacyProjectBackfillOverview(
                projectId,
                chapters.size(),
                analyzedChapters,
                chaptersNeedingSceneBackfill,
                chaptersNeedingStateBackfill,
                chaptersReadyForBackfill,
                List.copyOf(items)
        ));
    }
}
