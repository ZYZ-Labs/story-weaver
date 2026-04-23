package com.storyweaver.storyunit.migration.impl;

import com.storyweaver.domain.entity.AIWritingRecord;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.repository.AIWritingRecordMapper;
import com.storyweaver.service.ChapterService;
import com.storyweaver.storyunit.migration.LegacyBackfillAnalysisService;
import com.storyweaver.storyunit.migration.LegacyChapterBackfillAnalysis;
import com.storyweaver.storyunit.service.ChapterIncrementalStateStore;
import com.storyweaver.storyunit.service.ReaderRevealStateStore;
import com.storyweaver.storyunit.service.SceneExecutionStateQueryService;
import com.storyweaver.storyunit.service.StoryEventStore;
import com.storyweaver.storyunit.service.StoryPatchStore;
import com.storyweaver.storyunit.service.StorySnapshotStore;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class DefaultLegacyBackfillAnalysisService implements LegacyBackfillAnalysisService {

    private final ChapterService chapterService;
    private final AIWritingRecordMapper aiWritingRecordMapper;
    private final SceneExecutionStateQueryService sceneExecutionStateQueryService;
    private final StoryEventStore storyEventStore;
    private final StorySnapshotStore storySnapshotStore;
    private final StoryPatchStore storyPatchStore;
    private final ReaderRevealStateStore readerRevealStateStore;
    private final ChapterIncrementalStateStore chapterIncrementalStateStore;

    public DefaultLegacyBackfillAnalysisService(
            ChapterService chapterService,
            AIWritingRecordMapper aiWritingRecordMapper,
            SceneExecutionStateQueryService sceneExecutionStateQueryService,
            StoryEventStore storyEventStore,
            StorySnapshotStore storySnapshotStore,
            StoryPatchStore storyPatchStore,
            ReaderRevealStateStore readerRevealStateStore,
            ChapterIncrementalStateStore chapterIncrementalStateStore) {
        this.chapterService = chapterService;
        this.aiWritingRecordMapper = aiWritingRecordMapper;
        this.sceneExecutionStateQueryService = sceneExecutionStateQueryService;
        this.storyEventStore = storyEventStore;
        this.storySnapshotStore = storySnapshotStore;
        this.storyPatchStore = storyPatchStore;
        this.readerRevealStateStore = readerRevealStateStore;
        this.chapterIncrementalStateStore = chapterIncrementalStateStore;
    }

    @Override
    public Optional<LegacyChapterBackfillAnalysis> analyzeChapter(Long projectId, Long chapterId) {
        if (projectId == null || chapterId == null) {
            return Optional.empty();
        }

        Chapter chapter = chapterService.getById(chapterId);
        if (chapter == null || !projectId.equals(chapter.getProjectId())) {
            return Optional.empty();
        }

        List<AIWritingRecord> records = Optional.ofNullable(aiWritingRecordMapper.findByChapterId(chapterId)).orElse(List.of());
        List<SceneExecutionState> scenes = sceneExecutionStateQueryService.listChapterScenes(projectId, chapterId);
        int generatedRecordCount = (int) records.stream()
                .filter(record -> StringUtils.hasText(record.getGeneratedContent()))
                .count();
        int acceptedRecordCount = (int) records.stream()
                .filter(record -> isAccepted(record.getStatus()))
                .count();
        int failedRecordCount = (int) records.stream()
                .filter(record -> isFailed(record.getStatus()))
                .count();
        int completedSceneCount = (int) scenes.stream()
                .filter(scene -> scene.status() == SceneExecutionStatus.COMPLETED || scene.status() == SceneExecutionStatus.WRITTEN)
                .count();
        int failedSceneCount = (int) scenes.stream()
                .filter(scene -> scene.status() == SceneExecutionStatus.FAILED || scene.status() == SceneExecutionStatus.BLOCKED)
                .count();

        Set<String> legacySceneIds = new HashSet<>();
        for (int index = 1; index <= generatedRecordCount; index++) {
            legacySceneIds.add("scene-" + index);
        }
        int runtimeOnlySceneCount = (int) scenes.stream()
                .map(SceneExecutionState::sceneId)
                .filter(sceneId -> !legacySceneIds.contains(sceneId))
                .count();

        int eventCount = storyEventStore.listChapterEvents(projectId, chapterId).size();
        int snapshotCount = storySnapshotStore.listChapterSnapshots(projectId, chapterId).size();
        int patchCount = storyPatchStore.listChapterPatches(projectId, chapterId).size();
        boolean hasReaderRevealState = readerRevealStateStore.findChapterRevealState(projectId, chapterId).isPresent();
        boolean hasChapterState = chapterIncrementalStateStore.findChapterState(projectId, chapterId).isPresent();

        boolean needsSceneBackfill = generatedRecordCount > 0 && (eventCount == 0 || snapshotCount == 0);
        boolean needsStateBackfill = generatedRecordCount > 0 && (patchCount == 0 || !hasReaderRevealState || !hasChapterState);

        List<String> notes = new ArrayList<>();
        if (generatedRecordCount > 0 && scenes.isEmpty()) {
            notes.add("旧写作记录已有正文，但新 scene 读模型为空。");
        }
        if (generatedRecordCount > 0 && eventCount == 0) {
            notes.add("旧记录尚未形成 StoryEvent 基线。");
        }
        if (generatedRecordCount > 0 && snapshotCount == 0) {
            notes.add("旧记录尚未形成 StorySnapshot 基线。");
        }
        if (generatedRecordCount > 0 && patchCount == 0) {
            notes.add("旧记录尚未形成 StoryPatch 基线。");
        }
        if (generatedRecordCount > 0 && !hasReaderRevealState) {
            notes.add("章节 ReaderRevealState 尚未建立。");
        }
        if (generatedRecordCount > 0 && !hasChapterState) {
            notes.add("章节 ChapterIncrementalState 尚未建立。");
        }
        if (runtimeOnlySceneCount > 0) {
            notes.add("已存在超出旧写作记录映射范围的新 scene runtime state。");
        }

        return Optional.of(new LegacyChapterBackfillAnalysis(
                projectId,
                chapterId,
                chapter.getTitle(),
                StringUtils.hasText(chapter.getSummary()),
                StringUtils.hasText(chapter.getContent()),
                records.size(),
                generatedRecordCount,
                acceptedRecordCount,
                failedRecordCount,
                scenes.size(),
                completedSceneCount,
                failedSceneCount,
                runtimeOnlySceneCount,
                eventCount,
                snapshotCount,
                patchCount,
                hasReaderRevealState,
                hasChapterState,
                needsSceneBackfill,
                needsStateBackfill,
                List.copyOf(notes)
        ));
    }

    private boolean isAccepted(String status) {
        String normalized = normalizeStatus(status);
        return "accepted".equals(normalized) || "completed".equals(normalized) || "success".equals(normalized);
    }

    private boolean isFailed(String status) {
        String normalized = normalizeStatus(status);
        return "rejected".equals(normalized) || "failed".equals(normalized) || "error".equals(normalized);
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
    }
}
