package com.storyweaver.storyunit.migration.impl;

import com.storyweaver.config.StoryCompatibilityProperties;
import com.storyweaver.storyunit.event.StoryEvent;
import com.storyweaver.storyunit.event.StoryEventType;
import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;
import com.storyweaver.storyunit.facet.state.ChapterIncrementalState;
import com.storyweaver.storyunit.migration.LegacyBackfillDryRun;
import com.storyweaver.storyunit.migration.LegacyBackfillDryRunService;
import com.storyweaver.storyunit.migration.LegacyBackfillExecutionResult;
import com.storyweaver.storyunit.migration.LegacyBackfillExecutionService;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StorySourceTrace;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.patch.PatchOperation;
import com.storyweaver.storyunit.patch.PatchOperationType;
import com.storyweaver.storyunit.patch.PatchStatus;
import com.storyweaver.storyunit.patch.StoryPatch;
import com.storyweaver.storyunit.service.ChapterIncrementalStateStore;
import com.storyweaver.storyunit.service.ReaderRevealStateStore;
import com.storyweaver.storyunit.service.SceneExecutionStateQueryService;
import com.storyweaver.storyunit.service.StoryEventStore;
import com.storyweaver.storyunit.service.StoryPatchStore;
import com.storyweaver.storyunit.service.StorySnapshotStore;
import com.storyweaver.storyunit.snapshot.SnapshotScope;
import com.storyweaver.storyunit.snapshot.StorySnapshot;
import com.storyweaver.storyunit.session.SceneExecutionState;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DefaultLegacyBackfillExecutionService implements LegacyBackfillExecutionService {

    private final LegacyBackfillDryRunService legacyBackfillDryRunService;
    private final SceneExecutionStateQueryService sceneExecutionStateQueryService;
    private final StoryEventStore storyEventStore;
    private final StorySnapshotStore storySnapshotStore;
    private final StoryPatchStore storyPatchStore;
    private final ReaderRevealStateStore readerRevealStateStore;
    private final ChapterIncrementalStateStore chapterIncrementalStateStore;
    private final StoryCompatibilityProperties properties;

    public DefaultLegacyBackfillExecutionService(
            LegacyBackfillDryRunService legacyBackfillDryRunService,
            SceneExecutionStateQueryService sceneExecutionStateQueryService,
            StoryEventStore storyEventStore,
            StorySnapshotStore storySnapshotStore,
            StoryPatchStore storyPatchStore,
            ReaderRevealStateStore readerRevealStateStore,
            ChapterIncrementalStateStore chapterIncrementalStateStore,
            StoryCompatibilityProperties properties) {
        this.legacyBackfillDryRunService = legacyBackfillDryRunService;
        this.sceneExecutionStateQueryService = sceneExecutionStateQueryService;
        this.storyEventStore = storyEventStore;
        this.storySnapshotStore = storySnapshotStore;
        this.storyPatchStore = storyPatchStore;
        this.readerRevealStateStore = readerRevealStateStore;
        this.chapterIncrementalStateStore = chapterIncrementalStateStore;
        this.properties = properties;
    }

    @Override
    public Optional<LegacyBackfillExecutionResult> executeChapterBackfill(Long projectId, Long chapterId) {
        Optional<LegacyBackfillDryRun> dryRunOptional = legacyBackfillDryRunService.planChapterBackfill(projectId, chapterId);
        if (dryRunOptional.isEmpty()) {
            return Optional.empty();
        }

        LegacyBackfillDryRun dryRun = dryRunOptional.orElseThrow();
        if (!properties.isBackfillExecuteEnabled()) {
            return Optional.of(new LegacyBackfillExecutionResult(
                    dryRun,
                    false,
                    0,
                    0,
                    0,
                    false,
                    false,
                    List.of(),
                    List.of(),
                    List.of("兼容回填开关已关闭，当前仅允许查看分析和 dry-run。")
            ));
        }
        if (!dryRun.canRunBackfill()) {
            return Optional.of(new LegacyBackfillExecutionResult(
                    dryRun,
                    false,
                    0,
                    0,
                    0,
                    false,
                    false,
                    List.of(),
                    List.of(),
                    List.of("当前 dry-run 判定不可执行回填。")
            ));
        }

        List<SceneExecutionState> legacyScenes = sceneExecutionStateQueryService.listChapterScenes(projectId, chapterId).stream()
                .filter(this::isLegacyDerivedScene)
                .toList();

        Set<String> existingEventIds = storyEventStore.listChapterEvents(projectId, chapterId).stream()
                .map(StoryEvent::eventId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> existingSnapshotIds = storySnapshotStore.listChapterSnapshots(projectId, chapterId).stream()
                .map(StorySnapshot::snapshotId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> existingPatchIds = storyPatchStore.listChapterPatches(projectId, chapterId).stream()
                .map(StoryPatch::patchId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> writtenKeys = new ArrayList<>();
        List<String> skippedKeys = new ArrayList<>();
        List<String> warnings = new ArrayList<>(dryRun.riskNotes());

        int createdEventCount = 0;
        int createdSnapshotCount = 0;
        int createdPatchCount = 0;

        for (SceneExecutionState scene : legacyScenes) {
            String eventId = buildEventId(projectId, chapterId, scene.sceneId());
            if (existingEventIds.contains(eventId)) {
                skippedKeys.add(eventId);
            } else {
                storyEventStore.appendEvent(buildEvent(projectId, chapterId, scene, eventId));
                writtenKeys.add(eventId);
                existingEventIds.add(eventId);
                createdEventCount++;
            }

            String snapshotId = buildSceneSnapshotId(projectId, chapterId, scene.sceneId());
            if (existingSnapshotIds.contains(snapshotId)) {
                skippedKeys.add(snapshotId);
            } else {
                storySnapshotStore.saveSnapshot(buildSceneSnapshot(projectId, chapterId, scene, snapshotId));
                writtenKeys.add(snapshotId);
                existingSnapshotIds.add(snapshotId);
                createdSnapshotCount++;
            }
        }

        String revealPatchId = buildRevealPatchId(projectId, chapterId);
        if (existingPatchIds.contains(revealPatchId)) {
            skippedKeys.add(revealPatchId);
        } else {
            StoryPatch revealPatch = buildRevealPatch(projectId, chapterId, legacyScenes, revealPatchId);
            if (revealPatch != null) {
                storyPatchStore.appendPatch(projectId, chapterId, revealPatch);
                writtenKeys.add(revealPatchId);
                existingPatchIds.add(revealPatchId);
                createdPatchCount++;
            }
        }

        String statePatchId = buildStatePatchId(projectId, chapterId);
        if (existingPatchIds.contains(statePatchId)) {
            skippedKeys.add(statePatchId);
        } else {
            StoryPatch statePatch = buildStatePatch(projectId, chapterId, legacyScenes, statePatchId);
            if (statePatch != null) {
                storyPatchStore.appendPatch(projectId, chapterId, statePatch);
                writtenKeys.add(statePatchId);
                existingPatchIds.add(statePatchId);
                createdPatchCount++;
            }
        }

        boolean createdReaderRevealState = false;
        if (readerRevealStateStore.findChapterRevealState(projectId, chapterId).isEmpty()) {
            ReaderRevealState revealState = buildReaderRevealState(projectId, chapterId, legacyScenes);
            if (revealState != null) {
                readerRevealStateStore.saveChapterRevealState(revealState);
                writtenKeys.add("reader-reveal-state:" + projectId + ":" + chapterId);
                createdReaderRevealState = true;
            }
        } else {
            skippedKeys.add("reader-reveal-state:" + projectId + ":" + chapterId);
        }

        boolean createdChapterState = false;
        if (chapterIncrementalStateStore.findChapterState(projectId, chapterId).isEmpty()) {
            ChapterIncrementalState state = buildChapterState(projectId, chapterId, legacyScenes);
            if (state != null) {
                chapterIncrementalStateStore.saveChapterState(state);
                writtenKeys.add("chapter-state:" + projectId + ":" + chapterId);
                createdChapterState = true;
            }
        } else {
            skippedKeys.add("chapter-state:" + projectId + ":" + chapterId);
        }

        String chapterSnapshotId = buildChapterSnapshotId(projectId, chapterId);
        if (existingSnapshotIds.contains(chapterSnapshotId)) {
            skippedKeys.add(chapterSnapshotId);
        } else {
            StorySnapshot chapterSnapshot = buildChapterSnapshot(projectId, chapterId, legacyScenes, chapterSnapshotId);
            if (chapterSnapshot != null) {
                storySnapshotStore.saveSnapshot(chapterSnapshot);
                writtenKeys.add(chapterSnapshotId);
                createdSnapshotCount++;
            }
        }

        if (legacyScenes.isEmpty()) {
            warnings.add("当前章节没有可用的 legacy scene，未写入任何兼容基线。");
        }

        return Optional.of(new LegacyBackfillExecutionResult(
                dryRun,
                true,
                createdEventCount,
                createdSnapshotCount,
                createdPatchCount,
                createdReaderRevealState,
                createdChapterState,
                List.copyOf(writtenKeys),
                List.copyOf(skippedKeys),
                List.copyOf(warnings)
        ));
    }

    private boolean isLegacyDerivedScene(SceneExecutionState scene) {
        Object source = scene.stateDelta().get("source");
        return source != null && "aiWritingRecord".equals(source.toString());
    }

    private StoryEvent buildEvent(Long projectId, Long chapterId, SceneExecutionState scene, String eventId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sceneId", scene.sceneId());
        payload.put("status", scene.status().name());
        payload.put("goal", scene.goal());
        payload.put("candidateId", scene.chosenCandidateId());
        payload.put("backfill", true);
        return new StoryEvent(
                eventId,
                (scene.status() == com.storyweaver.storyunit.session.SceneExecutionStatus.FAILED
                        || scene.status() == com.storyweaver.storyunit.session.SceneExecutionStatus.BLOCKED)
                        ? StoryEventType.STATE_CHANGED
                        : StoryEventType.SCENE_COMPLETED,
                projectId,
                chapterId,
                scene.sceneId(),
                sceneUnitRef(chapterId, scene.sceneId()),
                "兼容回填：导入 " + scene.sceneId() + " 的历史执行事件",
                Map.copyOf(payload),
                sourceTrace(scene.sceneId())
        );
    }

    private StorySnapshot buildSceneSnapshot(Long projectId, Long chapterId, SceneExecutionState scene, String snapshotId) {
        return new StorySnapshot(
                snapshotId,
                SnapshotScope.SCENE,
                projectId,
                chapterId,
                scene.sceneId(),
                List.of(sceneUnitRef(chapterId, scene.sceneId()), chapterUnitRef(chapterId)),
                firstNonBlank(scene.outcomeSummary(), scene.goal(), "兼容回填 scene snapshot"),
                sourceTrace(scene.sceneId())
        );
    }

    private StoryPatch buildRevealPatch(Long projectId, Long chapterId, List<SceneExecutionState> legacyScenes, String patchId) {
        List<String> readerKnown = legacyScenes.stream()
                .flatMap(scene -> scene.readerRevealDelta().stream())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (readerKnown.isEmpty()) {
            return null;
        }
        return new StoryPatch(
                patchId,
                chapterUnitRef(chapterId),
                FacetType.REVEAL,
                List.of(new PatchOperation(PatchOperationType.MERGE, "/readerKnown", readerKnown)),
                "兼容回填：补齐章节 reader reveal 基线",
                PatchStatus.APPLIED,
                sourceTrace("chapter-" + chapterId)
        );
    }

    private StoryPatch buildStatePatch(Long projectId, Long chapterId, List<SceneExecutionState> legacyScenes, String patchId) {
        List<String> openLoops = legacyScenes.stream()
                .flatMap(scene -> scene.openLoops().stream())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        List<String> resolvedLoops = legacyScenes.stream()
                .flatMap(scene -> scene.resolvedLoops().stream())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (openLoops.isEmpty() && resolvedLoops.isEmpty()) {
            return null;
        }
        List<PatchOperation> operations = new ArrayList<>();
        if (!openLoops.isEmpty()) {
            operations.add(new PatchOperation(PatchOperationType.MERGE, "/openLoops", openLoops));
        }
        if (!resolvedLoops.isEmpty()) {
            operations.add(new PatchOperation(PatchOperationType.MERGE, "/resolvedLoops", resolvedLoops));
        }
        return new StoryPatch(
                patchId,
                chapterUnitRef(chapterId),
                FacetType.STATE,
                List.copyOf(operations),
                "兼容回填：补齐章节 state 基线",
                PatchStatus.APPLIED,
                sourceTrace("chapter-" + chapterId)
        );
    }

    private ReaderRevealState buildReaderRevealState(Long projectId, Long chapterId, List<SceneExecutionState> legacyScenes) {
        List<String> readerKnown = legacyScenes.stream()
                .flatMap(scene -> scene.readerRevealDelta().stream())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (readerKnown.isEmpty()) {
            return null;
        }
        return new ReaderRevealState(
                projectId,
                chapterId,
                readerKnown,
                readerKnown,
                readerKnown,
                List.of(),
                "兼容回填后，读者已知 " + readerKnown.size() + " 条。"
        );
    }

    private ChapterIncrementalState buildChapterState(Long projectId, Long chapterId, List<SceneExecutionState> legacyScenes) {
        List<String> openLoops = legacyScenes.stream()
                .flatMap(scene -> scene.openLoops().stream())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        List<String> resolvedLoops = legacyScenes.stream()
                .flatMap(scene -> scene.resolvedLoops().stream())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (openLoops.isEmpty() && resolvedLoops.isEmpty()) {
            return null;
        }
        return new ChapterIncrementalState(
                projectId,
                chapterId,
                openLoops,
                resolvedLoops,
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                "兼容回填后，open loop " + openLoops.size() + " 条，resolved loop " + resolvedLoops.size() + " 条。"
        );
    }

    private StorySnapshot buildChapterSnapshot(Long projectId, Long chapterId, List<SceneExecutionState> legacyScenes, String snapshotId) {
        if (legacyScenes.isEmpty()) {
            return null;
        }
        return new StorySnapshot(
                snapshotId,
                SnapshotScope.CHAPTER,
                projectId,
                chapterId,
                null,
                List.of(chapterUnitRef(chapterId)),
                "兼容回填：章节已导入 " + legacyScenes.size() + " 个历史镜头基线。",
                sourceTrace("chapter-" + chapterId)
        );
    }

    private String buildEventId(Long projectId, Long chapterId, String sceneId) {
        return "backfill:event:" + projectId + ":" + chapterId + ":" + sceneId;
    }

    private String buildSceneSnapshotId(Long projectId, Long chapterId, String sceneId) {
        return "backfill:snapshot:scene:" + projectId + ":" + chapterId + ":" + sceneId;
    }

    private String buildChapterSnapshotId(Long projectId, Long chapterId) {
        return "backfill:snapshot:chapter:" + projectId + ":" + chapterId;
    }

    private String buildRevealPatchId(Long projectId, Long chapterId) {
        return "backfill:patch:reveal:" + projectId + ":" + chapterId;
    }

    private String buildStatePatchId(Long projectId, Long chapterId) {
        return "backfill:patch:state:" + projectId + ":" + chapterId;
    }

    private StoryUnitRef chapterUnitRef(Long chapterId) {
        return new StoryUnitRef(String.valueOf(chapterId), "chapter:" + chapterId, StoryUnitType.CHAPTER);
    }

    private StoryUnitRef sceneUnitRef(Long chapterId, String sceneId) {
        return new StoryUnitRef(sceneId, "scene-execution:" + chapterId + ":" + sceneId, StoryUnitType.SCENE_EXECUTION);
    }

    private StorySourceTrace sourceTrace(String sourceRef) {
        return new StorySourceTrace("phase9-backfill", "phase9-backfill", "LegacyBackfillExecutionService", sourceRef);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }
}
