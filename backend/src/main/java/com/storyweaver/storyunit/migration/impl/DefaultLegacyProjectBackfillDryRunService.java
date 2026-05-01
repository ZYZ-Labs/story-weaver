package com.storyweaver.storyunit.migration.impl;

import com.storyweaver.storyunit.migration.LegacyBackfillDryRun;
import com.storyweaver.storyunit.migration.LegacyBackfillDryRunService;
import com.storyweaver.storyunit.migration.LegacyBackfillOverviewService;
import com.storyweaver.storyunit.migration.LegacyChapterBackfillStatusItem;
import com.storyweaver.storyunit.migration.LegacyProjectBackfillDryRun;
import com.storyweaver.storyunit.migration.LegacyProjectBackfillDryRunItem;
import com.storyweaver.storyunit.migration.LegacyProjectBackfillDryRunService;
import com.storyweaver.storyunit.migration.LegacyProjectBackfillOverview;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class DefaultLegacyProjectBackfillDryRunService implements LegacyProjectBackfillDryRunService {

    private final LegacyBackfillOverviewService legacyBackfillOverviewService;
    private final LegacyBackfillDryRunService legacyBackfillDryRunService;

    public DefaultLegacyProjectBackfillDryRunService(
            LegacyBackfillOverviewService legacyBackfillOverviewService,
            LegacyBackfillDryRunService legacyBackfillDryRunService) {
        this.legacyBackfillOverviewService = legacyBackfillOverviewService;
        this.legacyBackfillDryRunService = legacyBackfillDryRunService;
    }

    @Override
    public Optional<LegacyProjectBackfillDryRun> buildProjectDryRun(Long projectId) {
        if (projectId == null) {
            return Optional.empty();
        }

        Optional<LegacyProjectBackfillOverview> overviewOptional = legacyBackfillOverviewService.buildProjectOverview(projectId);
        if (overviewOptional.isEmpty()) {
            return Optional.empty();
        }

        LegacyProjectBackfillOverview overview = overviewOptional.orElseThrow();
        List<LegacyProjectBackfillDryRunItem> chapterPlans = new ArrayList<>();
        Set<String> requiredActions = new LinkedHashSet<>();
        Set<String> riskNotes = new LinkedHashSet<>();
        int runnableChapters = 0;
        int blockedChapters = 0;
        int chaptersNeedingBackfill = 0;

        for (LegacyChapterBackfillStatusItem item : overview.chapters()) {
            if (!item.needsSceneBackfill() && !item.needsStateBackfill()) {
                continue;
            }
            chaptersNeedingBackfill++;

            Optional<LegacyBackfillDryRun> dryRunOptional =
                    legacyBackfillDryRunService.planChapterBackfill(projectId, item.chapterId());
            if (dryRunOptional.isPresent()) {
                LegacyBackfillDryRun dryRun = dryRunOptional.orElseThrow();
                if (dryRun.canRunBackfill()) {
                    runnableChapters++;
                } else {
                    blockedChapters++;
                }
                dryRun.actions().stream()
                        .filter(action -> action.required() && !action.blocked())
                        .map(action -> action.title() + "（" + item.chapterTitle() + "）")
                        .forEach(requiredActions::add);
                riskNotes.addAll(dryRun.riskNotes());
                chapterPlans.add(new LegacyProjectBackfillDryRunItem(
                        item.chapterId(),
                        item.chapterTitle(),
                        dryRun.canRunBackfill(),
                        item.needsSceneBackfill(),
                        item.needsStateBackfill(),
                        dryRun.actions(),
                        dryRun.riskNotes()
                ));
                continue;
            }

            blockedChapters++;
            riskNotes.add("章节 " + item.chapterTitle() + " 暂时无法生成 dry-run 规划，请先检查旧记录基线。");
            chapterPlans.add(new LegacyProjectBackfillDryRunItem(
                    item.chapterId(),
                    item.chapterTitle(),
                    false,
                    item.needsSceneBackfill(),
                    item.needsStateBackfill(),
                    List.of(),
                    List.of("当前章节无法生成 dry-run 规划。")
            ));
        }

        return Optional.of(new LegacyProjectBackfillDryRun(
                projectId,
                overview.totalChapters(),
                chaptersNeedingBackfill,
                runnableChapters,
                blockedChapters,
                List.copyOf(chapterPlans),
                List.copyOf(requiredActions),
                List.copyOf(riskNotes)
        ));
    }
}
