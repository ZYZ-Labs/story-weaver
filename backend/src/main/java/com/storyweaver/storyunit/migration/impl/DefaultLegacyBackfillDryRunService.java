package com.storyweaver.storyunit.migration.impl;

import com.storyweaver.storyunit.migration.LegacyBackfillActionPlan;
import com.storyweaver.storyunit.migration.LegacyBackfillAnalysisService;
import com.storyweaver.storyunit.migration.LegacyBackfillDryRun;
import com.storyweaver.storyunit.migration.LegacyBackfillDryRunService;
import com.storyweaver.storyunit.migration.LegacyChapterBackfillAnalysis;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DefaultLegacyBackfillDryRunService implements LegacyBackfillDryRunService {

    private final LegacyBackfillAnalysisService legacyBackfillAnalysisService;

    public DefaultLegacyBackfillDryRunService(LegacyBackfillAnalysisService legacyBackfillAnalysisService) {
        this.legacyBackfillAnalysisService = legacyBackfillAnalysisService;
    }

    @Override
    public Optional<LegacyBackfillDryRun> planChapterBackfill(Long projectId, Long chapterId) {
        Optional<LegacyChapterBackfillAnalysis> analysisOptional = legacyBackfillAnalysisService.analyzeChapter(projectId, chapterId);
        if (analysisOptional.isEmpty()) {
            return Optional.empty();
        }

        LegacyChapterBackfillAnalysis analysis = analysisOptional.orElseThrow();
        List<LegacyBackfillActionPlan> actions = new ArrayList<>();

        actions.add(new LegacyBackfillActionPlan(
                "scan-legacy-records",
                "扫描旧写作记录",
                "读取当前章节的 AIWritingRecord，建立旧记录数量和状态基线。",
                analysis.legacyRecordCount() > 0,
                false,
                ""
        ));
        actions.add(new LegacyBackfillActionPlan(
                "derive-scene-state",
                "补齐 scene 执行基线",
                "将可用旧正文记录映射为 SceneExecutionState，并补齐缺失的 runtime/event/snapshot 基线。",
                analysis.needsSceneBackfill(),
                analysis.legacyGeneratedRecordCount() == 0,
                analysis.legacyGeneratedRecordCount() == 0 ? "当前章节没有可用于映射的旧正文记录。" : ""
        ));
        actions.add(new LegacyBackfillActionPlan(
                "derive-state-facets",
                "补齐状态与揭晓基线",
                "补齐 StoryPatch、ReaderRevealState 和 ChapterIncrementalState 的兼容状态。",
                analysis.needsStateBackfill(),
                analysis.legacyGeneratedRecordCount() == 0,
                analysis.legacyGeneratedRecordCount() == 0 ? "当前章节没有可用于推导状态的旧正文记录。" : ""
        ));
        actions.add(new LegacyBackfillActionPlan(
                "verify-post-backfill",
                "执行回填后校验",
                "回填后重新核对 scene/event/snapshot/patch/state 的数量与章节摘要状态。",
                true,
                false,
                ""
        ));

        List<String> riskNotes = new ArrayList<>();
        if (analysis.runtimeOnlySceneCount() > 0) {
            riskNotes.add("当前章节已经存在 runtime-only scene，回填时必须避免覆盖新状态。");
        }
        if (analysis.hasChapterState() || analysis.hasReaderRevealState()) {
            riskNotes.add("当前章节已有部分状态链数据，后续真正回填必须采用幂等 merge，而不是整章覆写。");
        }
        if (!analysis.chapterSummaryPresent() && !analysis.chapterContentPresent()) {
            riskNotes.add("当前章节摘要和正文都为空，回填价值有限，应先确认是否为冷启动样本。");
        }

        boolean canRunBackfill = analysis.legacyGeneratedRecordCount() > 0;
        return Optional.of(new LegacyBackfillDryRun(
                analysis,
                canRunBackfill,
                List.copyOf(actions),
                List.copyOf(riskNotes)
        ));
    }
}
