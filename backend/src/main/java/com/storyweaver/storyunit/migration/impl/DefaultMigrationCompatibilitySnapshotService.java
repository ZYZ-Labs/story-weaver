package com.storyweaver.storyunit.migration.impl;

import com.storyweaver.config.StoryCompatibilityProperties;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.service.ChapterService;
import com.storyweaver.storyunit.migration.CompatibilityBoundaryItem;
import com.storyweaver.storyunit.migration.CompatibilityMode;
import com.storyweaver.storyunit.migration.CompatibilityScope;
import com.storyweaver.storyunit.migration.LegacyBackfillAnalysisService;
import com.storyweaver.storyunit.migration.LegacyChapterBackfillAnalysis;
import com.storyweaver.storyunit.migration.MigrationCompatibilitySnapshot;
import com.storyweaver.storyunit.migration.MigrationCompatibilitySnapshotService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DefaultMigrationCompatibilitySnapshotService implements MigrationCompatibilitySnapshotService {

    private final ChapterService chapterService;
    private final LegacyBackfillAnalysisService legacyBackfillAnalysisService;
    private final StoryCompatibilityProperties properties;

    public DefaultMigrationCompatibilitySnapshotService(
            ChapterService chapterService,
            LegacyBackfillAnalysisService legacyBackfillAnalysisService,
            StoryCompatibilityProperties properties) {
        this.chapterService = chapterService;
        this.legacyBackfillAnalysisService = legacyBackfillAnalysisService;
        this.properties = properties;
    }

    @Override
    public Optional<MigrationCompatibilitySnapshot> getChapterSnapshot(Long projectId, Long chapterId) {
        if (projectId == null || chapterId == null) {
            return Optional.empty();
        }

        Chapter chapter = chapterService.getById(chapterId);
        if (chapter == null || !projectId.equals(chapter.getProjectId())) {
            return Optional.empty();
        }

        LegacyChapterBackfillAnalysis analysis = legacyBackfillAnalysisService.analyzeChapter(projectId, chapterId).orElse(null);

        return Optional.of(new MigrationCompatibilitySnapshot(
                projectId,
                chapterId,
                chapter.getTitle(),
                buildPageBoundaries(),
                buildApiBoundaries(),
                buildDataBoundaries(),
                buildFeatureFlags(),
                buildRiskNotes(analysis)
        ));
    }

    private List<CompatibilityBoundaryItem> buildPageBoundaries() {
        List<CompatibilityBoundaryItem> boundaries = new ArrayList<>();
        boundaries.add(new CompatibilityBoundaryItem(
                "chapter-workspace",
                CompatibilityScope.PAGE,
                "章节工作区",
                properties.isChapterWorkspacePrimary() ? CompatibilityMode.NEW_PRIMARY : CompatibilityMode.LEGACY_FALLBACK,
                "章节工作区 / 创作台",
                properties.isLegacyWritingCenterEnabled() ? "旧写作中心" : "",
                true,
                List.of("镜头执行、章节状态、trace 与 chapter review 统一走新工作区。")
        ));
        boundaries.add(new CompatibilityBoundaryItem(
                "legacy-writing-center",
                CompatibilityScope.PAGE,
                "旧写作中心",
                properties.isLegacyWritingCenterEnabled() ? CompatibilityMode.LEGACY_FALLBACK : CompatibilityMode.DISABLED,
                "旧写作中心",
                "",
                properties.isLegacyWritingCenterEnabled(),
                List.of("迁移期保留，用于异常回退和对照验证。")
        ));
        boundaries.add(new CompatibilityBoundaryItem(
                "state-center",
                CompatibilityScope.PAGE,
                "状态台",
                properties.isStateCenterPrimary() ? CompatibilityMode.NEW_PRIMARY : CompatibilityMode.DISABLED,
                "状态台",
                "",
                properties.isStateCenterPrimary(),
                List.of("状态台已聚合 chapter state、reader reveal、兼容回填。")
        ));
        boundaries.add(new CompatibilityBoundaryItem(
                "generation-center",
                CompatibilityScope.PAGE,
                "生成台",
                properties.isGenerationCenterPrimary() ? CompatibilityMode.NEW_PRIMARY : CompatibilityMode.DISABLED,
                "生成台",
                "",
                properties.isGenerationCenterPrimary(),
                List.of("多 session 编排预览和章节审校以新链为准。")
        ));
        boundaries.add(new CompatibilityBoundaryItem(
                "object-management",
                CompatibilityScope.PAGE,
                "人物 / 世界观 / 章节管理",
                CompatibilityMode.DUAL_READ,
                "Summary / Canon / State / History",
                "旧字段与旧对象读模型",
                true,
                List.of("对象页已转为 Summary First，但底层仍同时消费旧对象字段和新状态摘要。")
        ));
        return List.copyOf(boundaries);
    }

    private List<CompatibilityBoundaryItem> buildApiBoundaries() {
        List<CompatibilityBoundaryItem> boundaries = new ArrayList<>();
        boundaries.add(new CompatibilityBoundaryItem(
                "story-context",
                CompatibilityScope.API,
                "story-context",
                properties.isStoryContextDualReadEnabled() ? CompatibilityMode.DUAL_READ : CompatibilityMode.NEW_PRIMARY,
                "StoryContextQueryService + State Server 读模型",
                "Chapter / Character / AIWritingRecord 兼容读模型",
                true,
                List.of("新工作台统一从 story-context 读上下文，仍允许回退旧章节对象信息。")
        ));
        boundaries.add(new CompatibilityBoundaryItem(
                "story-state",
                CompatibilityScope.API,
                "story-state",
                CompatibilityMode.NEW_PRIMARY,
                "StoryEvent / StorySnapshot / StoryPatch / ChapterState",
                "",
                true,
                List.of("状态系统已进入新主链，回填只补缺失基线。")
        ));
        boundaries.add(new CompatibilityBoundaryItem(
                "summary-workflow",
                CompatibilityScope.API,
                "summary-workflow",
                properties.isSummaryWorkflowDualWriteEnabled() ? CompatibilityMode.DUAL_WRITE : CompatibilityMode.NEW_PRIMARY,
                "Summary First workflow",
                "旧对象字段 / 兼容摘要字段",
                true,
                List.of("摘要工作流继续写旧对象真源，同时回生成新摘要读模型。")
        ));
        boundaries.add(new CompatibilityBoundaryItem(
                "legacy-writing-api",
                CompatibilityScope.API,
                "旧写作接口",
                properties.isLegacyWritingApiEnabled() ? CompatibilityMode.LEGACY_FALLBACK : CompatibilityMode.DISABLED,
                "旧写作链",
                "",
                properties.isLegacyWritingApiEnabled(),
                List.of("迁移期仍保留，用于旧写作中心与回归样本对照。")
        ));
        return List.copyOf(boundaries);
    }

    private List<CompatibilityBoundaryItem> buildDataBoundaries() {
        List<CompatibilityBoundaryItem> boundaries = new ArrayList<>();
        boundaries.add(new CompatibilityBoundaryItem(
                "chapter-summary-content",
                CompatibilityScope.DATA,
                "章节摘要 / 正文",
                CompatibilityMode.LEGACY_PRIMARY,
                "Chapter.summary / Chapter.content",
                "StorySnapshot / chapter review 只做辅助读模型",
                true,
                List.of("正文仍以章节实体为真源，迁移期不做强制替换。")
        ));
        boundaries.add(new CompatibilityBoundaryItem(
                "scene-runtime-handoff",
                CompatibilityScope.DATA,
                "SceneExecutionState / Handoff",
                CompatibilityMode.NEW_PRIMARY,
                "State Server scene runtime",
                "AIWritingRecord 派生兼容 scene",
                true,
                List.of("新执行链优先读 runtime/handoff，缺失时才回退 legacy scene 派生。")
        ));
        boundaries.add(new CompatibilityBoundaryItem(
                "events-snapshots-patches",
                CompatibilityScope.DATA,
                "Event / Snapshot / Patch",
                CompatibilityMode.NEW_PRIMARY,
                "State Server manifests",
                "",
                true,
                List.of("兼容回填只补缺失基线，后续新执行直接写入新状态。")
        ));
        boundaries.add(new CompatibilityBoundaryItem(
                "reader-reveal-and-chapter-state",
                CompatibilityScope.DATA,
                "ReaderReveal / ChapterState",
                CompatibilityMode.NEW_PRIMARY,
                "ReaderRevealStateStore / ChapterIncrementalStateStore",
                "legacy patch backfill",
                true,
                List.of("当前读者已知和章节状态优先读新状态存储。")
        ));
        boundaries.add(new CompatibilityBoundaryItem(
                "legacy-ai-writing-record",
                CompatibilityScope.DATA,
                "AIWritingRecord 历史记录",
                CompatibilityMode.LEGACY_FALLBACK,
                "AIWritingRecord",
                "",
                true,
                List.of("保留为迁移样本、回放基线和兼容 scene 派生来源。")
        ));
        return List.copyOf(boundaries);
    }

    private List<String> buildFeatureFlags() {
        return List.of(
                "legacyWritingCenterEnabled=" + properties.isLegacyWritingCenterEnabled(),
                "legacyWritingApiEnabled=" + properties.isLegacyWritingApiEnabled(),
                "chapterWorkspacePrimary=" + properties.isChapterWorkspacePrimary(),
                "stateCenterPrimary=" + properties.isStateCenterPrimary(),
                "generationCenterPrimary=" + properties.isGenerationCenterPrimary(),
                "storyContextDualReadEnabled=" + properties.isStoryContextDualReadEnabled(),
                "summaryWorkflowDualWriteEnabled=" + properties.isSummaryWorkflowDualWriteEnabled(),
                "backfillExecuteEnabled=" + properties.isBackfillExecuteEnabled()
        );
    }

    private List<String> buildRiskNotes(LegacyChapterBackfillAnalysis analysis) {
        List<String> notes = new ArrayList<>();
        if (analysis == null) {
            notes.add("当前章节还没有兼容分析结果，灰度切换前应先跑 backfill-analysis。");
            return List.copyOf(notes);
        }
        if (analysis.needsSceneBackfill()) {
            notes.add("当前章节仍缺 scene 基线，工作区和生成台应保留 legacy scene fallback。");
        }
        if (analysis.needsStateBackfill()) {
            notes.add("当前章节仍缺 reader/state 基线，状态台展示应先补兼容回填。");
        }
        if (analysis.runtimeOnlySceneCount() > 0) {
            notes.add("当前章节存在 runtime-only scene，迁移回填不得覆盖已经生成的新状态。");
        }
        if (properties.isLegacyWritingCenterEnabled()) {
            notes.add("旧写作中心仍保留为回退入口，统一切换前不要提前下线。");
        }
        return List.copyOf(notes);
    }
}
