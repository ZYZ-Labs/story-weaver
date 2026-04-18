package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.SceneExecutionWriteResult;
import com.storyweaver.story.generation.orchestration.SceneExecutionWriteService;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.story.generation.orchestration.WriterSessionResult;
import com.storyweaver.storyunit.context.ReaderKnownStateView;
import com.storyweaver.storyunit.event.StoryEvent;
import com.storyweaver.storyunit.event.StoryEventType;
import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StorySourceTrace;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.patch.PatchOperation;
import com.storyweaver.storyunit.patch.PatchOperationType;
import com.storyweaver.storyunit.patch.PatchStatus;
import com.storyweaver.storyunit.patch.StoryPatch;
import com.storyweaver.storyunit.service.ReaderRevealStateStore;
import com.storyweaver.storyunit.service.StoryEventStore;
import com.storyweaver.storyunit.service.StoryPatchStore;
import com.storyweaver.storyunit.service.StorySnapshotStore;
import com.storyweaver.storyunit.snapshot.SnapshotScope;
import com.storyweaver.storyunit.snapshot.StorySnapshot;
import com.storyweaver.storyunit.service.SceneRuntimeStateStore;
import com.storyweaver.storyunit.session.ReviewDecision;
import com.storyweaver.storyunit.session.ReviewResult;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import com.storyweaver.storyunit.session.SceneHandoffSnapshot;
import com.storyweaver.storyunit.session.WriterExecutionBrief;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DefaultSceneExecutionWriteService implements SceneExecutionWriteService {

    private static final int SUMMARY_LIMIT = 160;
    private static final int HANDOFF_LIMIT = 120;

    private final SceneRuntimeStateStore sceneRuntimeStateStore;
    private final StoryEventStore storyEventStore;
    private final StorySnapshotStore storySnapshotStore;
    private final StoryPatchStore storyPatchStore;
    private final ReaderRevealStateStore readerRevealStateStore;

    public DefaultSceneExecutionWriteService(
            SceneRuntimeStateStore sceneRuntimeStateStore,
            StoryEventStore storyEventStore,
            StorySnapshotStore storySnapshotStore,
            StoryPatchStore storyPatchStore,
            ReaderRevealStateStore readerRevealStateStore) {
        this.sceneRuntimeStateStore = sceneRuntimeStateStore;
        this.storyEventStore = storyEventStore;
        this.storySnapshotStore = storySnapshotStore;
        this.storyPatchStore = storyPatchStore;
        this.readerRevealStateStore = readerRevealStateStore;
    }

    @Override
    public SceneExecutionWriteResult write(
            StorySessionContextPacket contextPacket,
            WriterExecutionBrief writerExecutionBrief,
            WriterSessionResult writerSessionResult,
            ReviewDecision reviewDecision) {
        SceneExecutionState sceneExecutionState = buildSceneExecutionState(
                contextPacket,
                writerExecutionBrief,
                writerSessionResult,
                reviewDecision
        );
        SceneHandoffSnapshot handoffSnapshot = buildHandoffSnapshot(
                contextPacket,
                writerSessionResult,
                reviewDecision,
                sceneExecutionState
        );
        sceneRuntimeStateStore.saveSceneState(sceneExecutionState);
        sceneRuntimeStateStore.saveHandoff(handoffSnapshot);
        StoryEvent storyEvent = storyEventStore.appendEvent(buildStoryEvent(
                contextPacket,
                sceneExecutionState,
                handoffSnapshot,
                writerSessionResult,
                reviewDecision
        ));
        StorySnapshot storySnapshot = storySnapshotStore.saveSnapshot(buildStorySnapshot(
                contextPacket,
                sceneExecutionState,
                handoffSnapshot,
                writerSessionResult
        ));
        StoryPatch storyPatch = storyPatchStore.appendPatch(
                contextPacket.projectId(),
                contextPacket.chapterId(),
                buildRevealStatePatch(contextPacket, sceneExecutionState)
        );
        ReaderRevealState readerRevealState = readerRevealStateStore.saveChapterRevealState(
                applyRevealStatePatch(contextPacket, sceneExecutionState, storyPatch)
        );
        StorySnapshot chapterStateSnapshot = storySnapshotStore.saveSnapshot(buildChapterStateSnapshot(
                contextPacket,
                sceneExecutionState,
                readerRevealState
        ));
        return new SceneExecutionWriteResult(
                sceneExecutionState,
                handoffSnapshot,
                storyEvent,
                storySnapshot,
                storyPatch,
                readerRevealState,
                chapterStateSnapshot
        );
    }

    private SceneExecutionState buildSceneExecutionState(
            StorySessionContextPacket contextPacket,
            WriterExecutionBrief writerExecutionBrief,
            WriterSessionResult writerSessionResult,
            ReviewDecision reviewDecision) {
        String sceneId = sceneIdentity(writerSessionResult.sceneId(), contextPacket.sceneId());
        int sceneIndex = parseSceneIndex(sceneId, contextPacket.existingSceneStates().size() + 1);
        Map<String, Object> stateDelta = new LinkedHashMap<>();
        stateDelta.put("source", "phase6.runtime-store");
        stateDelta.put("candidateId", writerSessionResult.candidateId());
        stateDelta.put("reviewResult", reviewDecision.result().name());
        stateDelta.put("targetWords", writerExecutionBrief.targetWords());
        stateDelta.put("createdAt", LocalDateTime.now().toString());

        return new SceneExecutionState(
                contextPacket.projectId(),
                contextPacket.chapterId(),
                sceneId,
                sceneIndex,
                mapStatus(reviewDecision),
                writerSessionResult.candidateId(),
                writerExecutionBrief.goal(),
                writerExecutionBrief.stopCondition(),
                writerExecutionBrief.readerReveal(),
                List.of(),
                List.of(),
                Map.copyOf(stateDelta),
                resolveHandoffLine(writerSessionResult, writerExecutionBrief),
                truncate(firstNonBlank(writerSessionResult.summary(), writerSessionResult.draftText()), SUMMARY_LIMIT)
        );
    }

    private SceneHandoffSnapshot buildHandoffSnapshot(
            StorySessionContextPacket contextPacket,
            WriterSessionResult writerSessionResult,
            ReviewDecision reviewDecision,
            SceneExecutionState sceneExecutionState) {
        String nextSceneId = "scene-" + (sceneExecutionState.sceneIndex() + 1);
        return new SceneHandoffSnapshot(
                contextPacket.projectId(),
                contextPacket.chapterId(),
                sceneExecutionState.sceneId(),
                nextSceneId,
                sceneExecutionState.handoffLine(),
                sceneExecutionState.outcomeSummary(),
                sceneExecutionState.readerRevealDelta(),
                sceneExecutionState.openLoops(),
                sceneExecutionState.resolvedLoops(),
                sceneExecutionState.stateDelta(),
                reviewDecision.result().name(),
                reviewDecision.summary(),
                LocalDateTime.now()
        );
    }

    private StoryEvent buildStoryEvent(
            StorySessionContextPacket contextPacket,
            SceneExecutionState sceneExecutionState,
            SceneHandoffSnapshot handoffSnapshot,
            WriterSessionResult writerSessionResult,
            ReviewDecision reviewDecision) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sceneId", sceneExecutionState.sceneId());
        payload.put("status", sceneExecutionState.status().name());
        payload.put("candidateId", sceneExecutionState.chosenCandidateId());
        payload.put("reviewResult", reviewDecision.result().name());
        payload.put("nextSceneId", handoffSnapshot.toSceneId());
        payload.put("outcomeSummary", sceneExecutionState.outcomeSummary());
        payload.put("handoffLine", sceneExecutionState.handoffLine());
        payload.put("writerSummary", writerSessionResult.summary());
        payload.put("readerRevealCount", sceneExecutionState.readerRevealDelta().size());
        payload.put("openLoopCount", sceneExecutionState.openLoops().size());
        payload.put("resolvedLoopCount", sceneExecutionState.resolvedLoops().size());

        return new StoryEvent(
                buildEventId(contextPacket, sceneExecutionState.sceneId()),
                sceneExecutionState.status() == SceneExecutionStatus.COMPLETED
                        ? StoryEventType.SCENE_COMPLETED
                        : StoryEventType.STATE_CHANGED,
                contextPacket.projectId(),
                contextPacket.chapterId(),
                sceneExecutionState.sceneId(),
                sceneUnitRef(contextPacket.chapterId(), sceneExecutionState.sceneId()),
                "scene " + sceneExecutionState.sceneId() + " 已写回为 " + sceneExecutionState.status().name(),
                Map.copyOf(payload),
                sourceTrace(sceneExecutionState.sceneId())
        );
    }

    private StorySnapshot buildStorySnapshot(
            StorySessionContextPacket contextPacket,
            SceneExecutionState sceneExecutionState,
            SceneHandoffSnapshot handoffSnapshot,
            WriterSessionResult writerSessionResult) {
        return new StorySnapshot(
                buildSnapshotId(contextPacket, sceneExecutionState.sceneId()),
                SnapshotScope.SCENE,
                contextPacket.projectId(),
                contextPacket.chapterId(),
                sceneExecutionState.sceneId(),
                List.of(
                        sceneUnitRef(contextPacket.chapterId(), sceneExecutionState.sceneId()),
                        chapterUnitRef(contextPacket.chapterId())
                ),
                firstNonBlank(
                        writerSessionResult.summary(),
                        sceneExecutionState.outcomeSummary(),
                        handoffSnapshot.outcomeSummary(),
                        "scene " + sceneExecutionState.sceneId() + " snapshot"
                ),
                sourceTrace(sceneExecutionState.sceneId())
        );
    }

    private StoryPatch buildRevealStatePatch(
            StorySessionContextPacket contextPacket,
            SceneExecutionState sceneExecutionState) {
        List<String> revealDelta = sanitizeDistinct(sceneExecutionState.readerRevealDelta());
        List<PatchOperation> operations = revealDelta.isEmpty()
                ? List.of()
                : List.of(new PatchOperation(PatchOperationType.MERGE, "/readerKnown", revealDelta));
        return new StoryPatch(
                buildPatchId(contextPacket, sceneExecutionState.sceneId()),
                chapterUnitRef(contextPacket.chapterId()),
                FacetType.REVEAL,
                operations,
                revealDelta.isEmpty()
                        ? "scene " + sceneExecutionState.sceneId() + " 未产生新的 reader reveal delta"
                        : "scene " + sceneExecutionState.sceneId() + " 新增读者已知信息 " + revealDelta.size() + " 条",
                PatchStatus.APPLIED,
                sourceTrace(sceneExecutionState.sceneId())
        );
    }

    private ReaderRevealState applyRevealStatePatch(
            StorySessionContextPacket contextPacket,
            SceneExecutionState sceneExecutionState,
            StoryPatch storyPatch) {
        ReaderRevealState current = readerRevealStateStore.findChapterRevealState(contextPacket.projectId(), contextPacket.chapterId())
                .orElseGet(() -> baseReaderRevealState(contextPacket.readerKnownState()));
        List<String> revealDelta = extractReaderRevealDelta(storyPatch);
        List<String> nextReaderKnown = mergeDistinct(current.readerKnown(), revealDelta);
        List<String> nextUnrevealed = current.unrevealed().stream()
                .filter(item -> !nextReaderKnown.contains(item))
                .toList();
        List<String> nextSystemKnown = mergeDistinct(current.systemKnown(), revealDelta);
        List<String> nextAuthorKnown = mergeDistinct(current.authorKnown(), revealDelta);
        return new ReaderRevealState(
                current.projectId(),
                current.chapterId(),
                nextSystemKnown,
                nextAuthorKnown,
                nextReaderKnown,
                nextUnrevealed,
                "scene " + sceneExecutionState.sceneId() + " 写回后，读者已知 " + nextReaderKnown.size() + " 条，未揭晓 " + nextUnrevealed.size() + " 条"
        );
    }

    private StorySnapshot buildChapterStateSnapshot(
            StorySessionContextPacket contextPacket,
            SceneExecutionState sceneExecutionState,
            ReaderRevealState readerRevealState) {
        return new StorySnapshot(
                buildChapterStateSnapshotId(contextPacket, sceneExecutionState.sceneId()),
                SnapshotScope.CHAPTER,
                contextPacket.projectId(),
                contextPacket.chapterId(),
                sceneExecutionState.sceneId(),
                List.of(chapterUnitRef(contextPacket.chapterId())),
                readerRevealState.summary(),
                sourceTrace(sceneExecutionState.sceneId())
        );
    }

    private ReaderRevealState baseReaderRevealState(ReaderKnownStateView baseState) {
        List<String> knownFacts = sanitizeDistinct(baseState.knownFacts());
        List<String> unrevealedFacts = baseState.unrevealedFacts().stream()
                .filter(item -> !knownFacts.contains(item))
                .toList();
        List<String> systemAndAuthorKnown = mergeDistinct(knownFacts, unrevealedFacts);
        return new ReaderRevealState(
                baseState.projectId(),
                baseState.chapterId(),
                systemAndAuthorKnown,
                systemAndAuthorKnown,
                knownFacts,
                unrevealedFacts,
                "初始读者已知 " + knownFacts.size() + " 条，未揭晓 " + unrevealedFacts.size() + " 条"
        );
    }

    private List<String> extractReaderRevealDelta(StoryPatch patch) {
        return patch.operations().stream()
                .filter(operation -> operation.op() == PatchOperationType.MERGE)
                .filter(operation -> "/readerKnown".equals(operation.path()))
                .map(PatchOperation::value)
                .filter(Collection.class::isInstance)
                .map(Collection.class::cast)
                .flatMap(Collection::stream)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    private List<String> mergeDistinct(List<String> current, List<String> delta) {
        List<String> merged = new ArrayList<>();
        merged.addAll(current == null ? List.of() : current);
        merged.addAll(delta == null ? List.of() : delta);
        return sanitizeDistinct(merged);
    }

    private List<String> sanitizeDistinct(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private SceneExecutionStatus mapStatus(ReviewDecision reviewDecision) {
        if (reviewDecision.result() == ReviewResult.PASS) {
            return SceneExecutionStatus.COMPLETED;
        }
        if (reviewDecision.canAutoRepair()) {
            return SceneExecutionStatus.REVIEWING;
        }
        return SceneExecutionStatus.FAILED;
    }

    private String resolveHandoffLine(WriterSessionResult writerSessionResult, WriterExecutionBrief writerExecutionBrief) {
        String draftText = truncate(writerSessionResult.draftText(), HANDOFF_LIMIT);
        if (StringUtils.hasText(draftText)) {
            return draftText;
        }
        return truncate(writerExecutionBrief.handoffLine(), HANDOFF_LIMIT);
    }

    private String sceneIdentity(String writerSceneId, String contextSceneId) {
        if (StringUtils.hasText(writerSceneId)) {
            return writerSceneId.trim();
        }
        if (StringUtils.hasText(contextSceneId)) {
            return contextSceneId.trim();
        }
        return "scene-1";
    }

    private int parseSceneIndex(String sceneId, int fallback) {
        if (!StringUtils.hasText(sceneId)) {
            return Math.max(1, fallback);
        }
        if (!sceneId.startsWith("scene-")) {
            return Math.max(1, fallback);
        }
        try {
            return Math.max(1, Integer.parseInt(sceneId.substring("scene-".length())));
        } catch (NumberFormatException exception) {
            return Math.max(1, fallback);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String truncate(String value, int limit) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= limit ? normalized : normalized.substring(0, limit) + "...";
    }

    private StoryUnitRef sceneUnitRef(Long chapterId, String sceneId) {
        return new StoryUnitRef(sceneId, "scene-execution:" + chapterId + ":" + sceneId, StoryUnitType.SCENE_EXECUTION);
    }

    private StoryUnitRef chapterUnitRef(Long chapterId) {
        return new StoryUnitRef(String.valueOf(chapterId), "chapter:" + chapterId, StoryUnitType.CHAPTER);
    }

    private StorySourceTrace sourceTrace(String sceneId) {
        return new StorySourceTrace("phase7-state-system", "phase7-state-system", "SceneExecutionWriteService", sceneId);
    }

    private String buildEventId(StorySessionContextPacket contextPacket, String sceneId) {
        return "event-" + contextPacket.projectId() + "-" + contextPacket.chapterId() + "-" + sceneId + "-" + UUID.randomUUID();
    }

    private String buildSnapshotId(StorySessionContextPacket contextPacket, String sceneId) {
        return "snapshot-" + contextPacket.projectId() + "-" + contextPacket.chapterId() + "-" + sceneId + "-" + UUID.randomUUID();
    }

    private String buildPatchId(StorySessionContextPacket contextPacket, String sceneId) {
        return "patch-" + contextPacket.projectId() + "-" + contextPacket.chapterId() + "-" + sceneId + "-" + UUID.randomUUID();
    }

    private String buildChapterStateSnapshotId(StorySessionContextPacket contextPacket, String sceneId) {
        return "snapshot-chapter-state-" + contextPacket.projectId() + "-" + contextPacket.chapterId() + "-" + sceneId + "-" + UUID.randomUUID();
    }
}
