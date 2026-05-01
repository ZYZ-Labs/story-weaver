package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.SceneExecutionWriteResult;
import com.storyweaver.story.generation.orchestration.SceneExecutionWriteService;
import com.storyweaver.story.generation.orchestration.SceneSkeletonItem;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.story.generation.orchestration.WriterSessionResult;
import com.storyweaver.storyunit.context.ReaderKnownStateView;
import com.storyweaver.storyunit.event.StoryEvent;
import com.storyweaver.storyunit.event.StoryEventType;
import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;
import com.storyweaver.storyunit.facet.state.ChapterIncrementalState;
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
import com.storyweaver.storyunit.service.StoryEventStore;
import com.storyweaver.storyunit.service.StoryPatchStore;
import com.storyweaver.storyunit.service.StorySnapshotStore;
import com.storyweaver.storyunit.snapshot.SnapshotScope;
import com.storyweaver.storyunit.snapshot.StorySnapshot;
import com.storyweaver.storyunit.service.SceneRuntimeStateStore;
import com.storyweaver.storyunit.session.ReviewDecision;
import com.storyweaver.storyunit.session.ReviewResult;
import com.storyweaver.storyunit.session.SceneContinuityState;
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
    private final ChapterIncrementalStateStore chapterIncrementalStateStore;
    private final AIContinuityStateService aiContinuityStateService;

    public DefaultSceneExecutionWriteService(
            SceneRuntimeStateStore sceneRuntimeStateStore,
            StoryEventStore storyEventStore,
            StorySnapshotStore storySnapshotStore,
            StoryPatchStore storyPatchStore,
            ReaderRevealStateStore readerRevealStateStore,
            ChapterIncrementalStateStore chapterIncrementalStateStore,
            AIContinuityStateService aiContinuityStateService) {
        this.sceneRuntimeStateStore = sceneRuntimeStateStore;
        this.storyEventStore = storyEventStore;
        this.storySnapshotStore = storySnapshotStore;
        this.storyPatchStore = storyPatchStore;
        this.readerRevealStateStore = readerRevealStateStore;
        this.chapterIncrementalStateStore = chapterIncrementalStateStore;
        this.aiContinuityStateService = aiContinuityStateService;
    }

    @Override
    public SceneExecutionWriteResult write(
            StorySessionContextPacket contextPacket,
            WriterExecutionBrief writerExecutionBrief,
            WriterSessionResult writerSessionResult,
            ReviewDecision reviewDecision) {
        SceneExecutionStatus persistedStatus = mapDraftStatus(reviewDecision);
        return persistWrite(
                contextPacket,
                writerExecutionBrief,
                writerSessionResult,
                reviewDecision,
                persistedStatus,
                "",
                "phase6.runtime-store",
                Map.of()
        );
    }

    @Override
    public SceneExecutionWriteResult writeAccepted(
            StorySessionContextPacket contextPacket,
            SceneSkeletonItem currentScene,
            SceneSkeletonItem nextScene,
            Long writingRecordId,
            String acceptedContent) {
        SceneContinuityState previousContinuityState = SceneContinuitySupport.resolveContinuityState(
                contextPacket.previousSceneHandoff(),
                contextPacket.sceneBindingContext().resolvedSceneState(),
                contextPacket.existingSceneStates(),
                nextScene == null ? "" : nextScene.sceneId(),
                nextScene == null ? "" : nextScene.goal(),
                firstNonBlank(currentScene.stopCondition(), "完成当前镜头目标后停住。")
        );
        String acceptedSummary = summarizeAcceptedContent(acceptedContent);
        String acceptedHandoff = extractTailSentence(acceptedContent, HANDOFF_LIMIT);
        SceneContinuityState continuityState = aiContinuityStateService.extractAcceptedContinuityState(
                currentScene.sceneId(),
                acceptedContent,
                acceptedSummary,
                acceptedHandoff,
                currentScene.readerReveal(),
                nextScene == null ? "" : nextScene.sceneId(),
                nextScene == null ? "" : nextScene.goal(),
                firstNonBlank(currentScene.stopCondition(), "完成当前镜头目标后停住。"),
                previousContinuityState
        );
        WriterExecutionBrief acceptedBrief = buildAcceptedBrief(
                contextPacket,
                currentScene,
                nextScene,
                writingRecordId,
                continuityState
        );
        WriterSessionResult writerSessionResult = new WriterSessionResult(
                acceptedBrief.sceneId(),
                acceptedBrief.chosenCandidateId(),
                acceptedContent,
                acceptedSummary
        );
        ReviewDecision reviewDecision = new ReviewDecision(
                acceptedBrief.sceneId(),
                ReviewResult.PASS,
                "章节工作区已接受当前镜头正文，runtime/handoff 已按真实正文写回。",
                List.of(),
                false,
                ""
        );
        return persistWrite(
                contextPacket,
                acceptedBrief,
                writerSessionResult,
                reviewDecision,
                SceneExecutionStatus.COMPLETED,
                acceptedBrief.nextSceneId(),
                "phase8.accepted-scene-draft",
                buildAcceptedStateDelta(writingRecordId, continuityState)
        );
    }

    private SceneExecutionWriteResult persistWrite(
            StorySessionContextPacket contextPacket,
            WriterExecutionBrief writerExecutionBrief,
            WriterSessionResult writerSessionResult,
            ReviewDecision reviewDecision,
            SceneExecutionStatus persistedStatus,
            String nextSceneId,
            String source,
            Map<String, Object> extraStateDelta) {
        SceneExecutionState sceneExecutionState = buildSceneExecutionState(
                contextPacket,
                writerExecutionBrief,
                writerSessionResult,
                persistedStatus,
                source,
                extraStateDelta
        );
        SceneHandoffSnapshot handoffSnapshot = buildHandoffSnapshot(
                contextPacket,
                reviewDecision,
                sceneExecutionState,
                nextSceneId
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
        StoryPatch chapterStatePatch = storyPatchStore.appendPatch(
                contextPacket.projectId(),
                contextPacket.chapterId(),
                buildChapterStatePatch(contextPacket, sceneExecutionState, handoffSnapshot)
        );
        ChapterIncrementalState chapterIncrementalState = chapterIncrementalStateStore.saveChapterState(
                applyChapterStatePatch(contextPacket, sceneExecutionState, handoffSnapshot, chapterStatePatch)
        );
        StorySnapshot chapterStateSnapshot = storySnapshotStore.saveSnapshot(buildChapterStateSnapshot(
                contextPacket,
                sceneExecutionState,
                readerRevealState,
                chapterIncrementalState
        ));
        return new SceneExecutionWriteResult(
                sceneExecutionState,
                handoffSnapshot,
                storyEvent,
                storySnapshot,
                storyPatch,
                readerRevealState,
                chapterStatePatch,
                chapterIncrementalState,
                chapterStateSnapshot
        );
    }

    private SceneExecutionState buildSceneExecutionState(
            StorySessionContextPacket contextPacket,
            WriterExecutionBrief writerExecutionBrief,
            WriterSessionResult writerSessionResult,
            SceneExecutionStatus persistedStatus,
            String source,
            Map<String, Object> extraStateDelta) {
        String sceneId = sceneIdentity(writerSessionResult.sceneId(), contextPacket.sceneId());
        int sceneIndex = parseSceneIndex(sceneId, contextPacket.existingSceneStates().size() + 1);
        Map<String, Object> stateDelta = new LinkedHashMap<>();
        stateDelta.put("source", firstNonBlank(source, "phase6.runtime-store"));
        stateDelta.put("candidateId", writerSessionResult.candidateId());
        stateDelta.put("reviewResult", persistedStatus.name());
        stateDelta.put("targetWords", writerExecutionBrief.targetWords());
        stateDelta.put("createdAt", LocalDateTime.now().toString());
        if (extraStateDelta != null && !extraStateDelta.isEmpty()) {
            stateDelta.putAll(extraStateDelta);
        }

        return new SceneExecutionState(
                contextPacket.projectId(),
                contextPacket.chapterId(),
                sceneId,
                sceneIndex,
                persistedStatus,
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
            ReviewDecision reviewDecision,
            SceneExecutionState sceneExecutionState,
            String nextSceneId) {
        return new SceneHandoffSnapshot(
                contextPacket.projectId(),
                contextPacket.chapterId(),
                sceneExecutionState.sceneId(),
                sceneExecutionState.status() == SceneExecutionStatus.COMPLETED ? firstNonBlank(nextSceneId) : "",
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
        List<String> revealDelta = sceneExecutionState.status() == SceneExecutionStatus.COMPLETED
                ? sanitizeDistinct(sceneExecutionState.readerRevealDelta())
                : List.of();
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
            ReaderRevealState readerRevealState,
            ChapterIncrementalState chapterIncrementalState) {
        return new StorySnapshot(
                buildChapterStateSnapshotId(contextPacket, sceneExecutionState.sceneId()),
                SnapshotScope.CHAPTER,
                contextPacket.projectId(),
                contextPacket.chapterId(),
                sceneExecutionState.sceneId(),
                List.of(chapterUnitRef(contextPacket.chapterId())),
                buildChapterStateSnapshotSummary(readerRevealState, chapterIncrementalState),
                sourceTrace(sceneExecutionState.sceneId())
        );
    }

    private StoryPatch buildChapterStatePatch(
            StorySessionContextPacket contextPacket,
            SceneExecutionState sceneExecutionState,
            SceneHandoffSnapshot handoffSnapshot) {
        String currentSceneLoop = scenePendingLoop(sceneExecutionState.sceneId());
        boolean completed = sceneExecutionState.status() == SceneExecutionStatus.COMPLETED;
        String nextSceneLoop = StringUtils.hasText(handoffSnapshot.toSceneId())
                ? scenePendingLoop(handoffSnapshot.toSceneId())
                : "";
        List<String> activeLocations = sanitizeDistinct(contextPacket.characterRuntimeStates().stream()
                .map(com.storyweaver.storyunit.context.CharacterRuntimeStateView::currentLocation)
                .toList());
        Map<String, String> emotions = sanitizeStringMap(contextPacket.characterRuntimeStates().stream()
                .filter(character -> StringUtils.hasText(character.emotionalState()))
                .collect(LinkedHashMap::new, (map, character) -> map.put(character.characterName(), character.emotionalState()), Map::putAll));
        Map<String, String> attitudes = sanitizeStringMap(contextPacket.characterRuntimeStates().stream()
                .filter(character -> StringUtils.hasText(character.attitudeSummary()))
                .collect(LinkedHashMap::new, (map, character) -> map.put(character.characterName(), character.attitudeSummary()), Map::putAll));
        Map<String, List<String>> stateTags = sanitizeTagMap(contextPacket.characterRuntimeStates().stream()
                .filter(character -> !character.stateTags().isEmpty())
                .collect(LinkedHashMap::new, (map, character) -> map.put(character.characterName(), character.stateTags()), Map::putAll));
        List<PatchOperation> operations = new ArrayList<>();
        if (completed) {
            operations.add(new PatchOperation(PatchOperationType.MERGE, "/resolvedLoops", List.of(currentSceneLoop)));
        }
        if (completed && StringUtils.hasText(nextSceneLoop)) {
            operations.add(new PatchOperation(PatchOperationType.MERGE, "/openLoops", List.of(nextSceneLoop)));
        }
        operations.add(new PatchOperation(PatchOperationType.REPLACE, "/activeLocations", activeLocations));
        operations.add(new PatchOperation(PatchOperationType.REPLACE, "/characterEmotions", emotions));
        operations.add(new PatchOperation(PatchOperationType.REPLACE, "/characterAttitudes", attitudes));
        operations.add(new PatchOperation(PatchOperationType.REPLACE, "/characterStateTags", stateTags));
        return new StoryPatch(
                buildChapterStatePatchId(contextPacket, sceneExecutionState.sceneId()),
                chapterUnitRef(contextPacket.chapterId()),
                FacetType.STATE,
                List.copyOf(operations),
                "scene " + sceneExecutionState.sceneId() + " 写回章节状态：openLoops="
                        + (completed && StringUtils.hasText(nextSceneLoop) ? 1 : 0)
                        + "，locations=" + activeLocations.size(),
                PatchStatus.APPLIED,
                sourceTrace(sceneExecutionState.sceneId())
        );
    }

    private ChapterIncrementalState applyChapterStatePatch(
            StorySessionContextPacket contextPacket,
            SceneExecutionState sceneExecutionState,
            SceneHandoffSnapshot handoffSnapshot,
            StoryPatch storyPatch) {
        ChapterIncrementalState current = chapterIncrementalStateStore.findChapterState(contextPacket.projectId(), contextPacket.chapterId())
                .orElseGet(() -> baseChapterIncrementalState(contextPacket));
        List<String> openLoops = sanitizeDistinct(mergeDistinct(
                current.openLoops(),
                extractStringList(storyPatch, "/openLoops")
        ));
        List<String> resolvedLoops = sanitizeDistinct(mergeDistinct(
                current.resolvedLoops(),
                extractStringList(storyPatch, "/resolvedLoops")
        ));
        openLoops = openLoops.stream()
                .filter(loop -> !resolvedLoops.contains(loop))
                .toList();
        Map<String, String> emotions = firstNonEmptyStringMap(extractStringMap(storyPatch, "/characterEmotions"), current.characterEmotions());
        Map<String, String> attitudes = firstNonEmptyStringMap(extractStringMap(storyPatch, "/characterAttitudes"), current.characterAttitudes());
        Map<String, List<String>> stateTags = firstNonEmptyTagMap(extractTagMap(storyPatch, "/characterStateTags"), current.characterStateTags());
        List<String> activeLocations = firstNonEmptyList(extractStringList(storyPatch, "/activeLocations"), current.activeLocations());
        return new ChapterIncrementalState(
                contextPacket.projectId(),
                contextPacket.chapterId(),
                openLoops,
                resolvedLoops,
                activeLocations,
                emotions,
                attitudes,
                stateTags,
                buildChapterStateSummary(sceneExecutionState, handoffSnapshot, openLoops, activeLocations)
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

    private List<String> extractStringList(StoryPatch patch, String path) {
        return patch.operations().stream()
                .filter(operation -> path.equals(operation.path()))
                .map(PatchOperation::value)
                .filter(Collection.class::isInstance)
                .map(Collection.class::cast)
                .flatMap(Collection::stream)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    private Map<String, String> extractStringMap(StoryPatch patch, String path) {
        Map<String, String> extracted = new LinkedHashMap<>();
        patch.operations().stream()
                .filter(operation -> path.equals(operation.path()))
                .map(PatchOperation::value)
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .forEach(map -> map.forEach((key, value) -> {
                    if (!(key instanceof String keyString) || !(value instanceof String valueString)) {
                        return;
                    }
                    extracted.put(keyString.trim(), valueString.trim());
                }));
        return sanitizeStringMap(extracted);
    }

    private Map<String, List<String>> extractTagMap(StoryPatch patch, String path) {
        Map<String, List<String>> extracted = new LinkedHashMap<>();
        patch.operations().stream()
                .filter(operation -> path.equals(operation.path()))
                .map(PatchOperation::value)
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .forEach(map -> map.forEach((key, value) -> {
                    if (!(key instanceof String keyString) || !(value instanceof Collection<?> collection)) {
                        return;
                    }
                    List<String> tags = collection.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .toList();
                    if (!tags.isEmpty()) {
                        extracted.put(keyString.trim(), sanitizeDistinct(tags));
                    }
                }));
        return sanitizeTagMap(extracted);
    }

    private ChapterIncrementalState baseChapterIncrementalState(StorySessionContextPacket contextPacket) {
        List<String> activeLocations = sanitizeDistinct(contextPacket.characterRuntimeStates().stream()
                .map(com.storyweaver.storyunit.context.CharacterRuntimeStateView::currentLocation)
                .toList());
        Map<String, String> emotions = sanitizeStringMap(contextPacket.characterRuntimeStates().stream()
                .filter(character -> StringUtils.hasText(character.emotionalState()))
                .collect(LinkedHashMap::new, (map, character) -> map.put(character.characterName(), character.emotionalState()), Map::putAll));
        Map<String, String> attitudes = sanitizeStringMap(contextPacket.characterRuntimeStates().stream()
                .filter(character -> StringUtils.hasText(character.attitudeSummary()))
                .collect(LinkedHashMap::new, (map, character) -> map.put(character.characterName(), character.attitudeSummary()), Map::putAll));
        Map<String, List<String>> tags = sanitizeTagMap(contextPacket.characterRuntimeStates().stream()
                .filter(character -> !character.stateTags().isEmpty())
                .collect(LinkedHashMap::new, (map, character) -> map.put(character.characterName(), character.stateTags()), Map::putAll));
        return new ChapterIncrementalState(
                contextPacket.projectId(),
                contextPacket.chapterId(),
                List.of(),
                List.of(),
                activeLocations,
                emotions,
                attitudes,
                tags,
                "章节状态基线已建立。"
        );
    }

    private String buildChapterStateSnapshotSummary(ReaderRevealState readerRevealState, ChapterIncrementalState chapterIncrementalState) {
        return readerRevealState.summary()
                + " / 开放回路 " + chapterIncrementalState.openLoops().size() + " 条"
                + " / 活动地点 " + chapterIncrementalState.activeLocations().size() + " 个";
    }

    private String buildChapterStateSummary(
            SceneExecutionState sceneExecutionState,
            SceneHandoffSnapshot handoffSnapshot,
            List<String> openLoops,
            List<String> activeLocations) {
        if (sceneExecutionState.status() != SceneExecutionStatus.COMPLETED) {
            return "scene " + sceneExecutionState.sceneId()
                    + " 已写入运行态草稿，但尚未正式接纳；开放回路 "
                    + openLoops.size()
                    + " 条，活动地点 "
                    + activeLocations.size()
                    + " 个";
        }
        return "scene " + sceneExecutionState.sceneId()
                + " 已写回章节状态，下一镜头 "
                + firstNonBlank(handoffSnapshot.toSceneId(), "无")
                + " 待执行，开放回路 "
                + openLoops.size()
                + " 条，活动地点 "
                + activeLocations.size()
                + " 个";
    }

    private List<String> firstNonEmptyList(List<String> preferred, List<String> fallback) {
        return preferred == null || preferred.isEmpty() ? sanitizeDistinct(fallback) : sanitizeDistinct(preferred);
    }

    private Map<String, String> firstNonEmptyStringMap(Map<String, String> preferred, Map<String, String> fallback) {
        return preferred == null || preferred.isEmpty() ? sanitizeStringMap(fallback) : sanitizeStringMap(preferred);
    }

    private Map<String, List<String>> firstNonEmptyTagMap(Map<String, List<String>> preferred, Map<String, List<String>> fallback) {
        return preferred == null || preferred.isEmpty() ? sanitizeTagMap(fallback) : sanitizeTagMap(preferred);
    }

    private Map<String, String> sanitizeStringMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, String> sanitized = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (!StringUtils.hasText(key) || !StringUtils.hasText(value)) {
                return;
            }
            sanitized.put(key.trim(), value.trim());
        });
        return Map.copyOf(sanitized);
    }

    private Map<String, List<String>> sanitizeTagMap(Map<String, List<String>> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> sanitized = new LinkedHashMap<>();
        values.forEach((key, tags) -> {
            if (!StringUtils.hasText(key)) {
                return;
            }
            List<String> normalizedTags = sanitizeDistinct(tags);
            if (!normalizedTags.isEmpty()) {
                sanitized.put(key.trim(), normalizedTags);
            }
        });
        return Map.copyOf(sanitized);
    }

    private String scenePendingLoop(String sceneId) {
        return "scene:" + sceneId + ":pending";
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

    private SceneExecutionStatus mapDraftStatus(ReviewDecision reviewDecision) {
        if (reviewDecision.result() == ReviewResult.PASS) {
            return SceneExecutionStatus.WRITTEN;
        }
        if (reviewDecision.result() == ReviewResult.BLOCK) {
            return SceneExecutionStatus.BLOCKED;
        }
        if (reviewDecision.canAutoRepair()) {
            return SceneExecutionStatus.REVIEWING;
        }
        return SceneExecutionStatus.FAILED;
    }

    private String resolveHandoffLine(WriterSessionResult writerSessionResult, WriterExecutionBrief writerExecutionBrief) {
        String draftText = extractTailSentence(writerSessionResult.draftText(), HANDOFF_LIMIT);
        if (StringUtils.hasText(draftText)) {
            return draftText;
        }
        return extractTailSentence(writerExecutionBrief.handoffLine(), HANDOFF_LIMIT);
    }

    private WriterExecutionBrief buildAcceptedBrief(
            StorySessionContextPacket contextPacket,
            SceneSkeletonItem currentScene,
            SceneSkeletonItem nextScene,
            Long writingRecordId,
            SceneContinuityState continuityState) {
        return new WriterExecutionBrief(
                contextPacket.projectId(),
                contextPacket.chapterId(),
                currentScene.sceneId(),
                writingRecordId == null ? "accepted-scene-draft" : "accepted-record-" + writingRecordId,
                firstNonBlank(currentScene.goal(), contextPacket.chapterSummary().summary()),
                currentScene.readerReveal(),
                currentScene.mustUseAnchors(),
                List.of("当前镜头已经正式接纳，禁止回退到上一镜头。"),
                firstNonBlank(currentScene.stopCondition(), "完成当前镜头目标后停住。"),
                currentScene.targetWords(),
                SceneContinuitySupport.buildConstraintLines(continuityState),
                continuityState.summary(),
                continuityState.handoffLine(),
                nextScene == null ? "" : nextScene.sceneId(),
                nextScene == null ? "" : nextScene.goal(),
                continuityState
        );
    }

    private Map<String, Object> buildAcceptedStateDelta(Long writingRecordId, SceneContinuityState continuityState) {
        Map<String, Object> stateDelta = new LinkedHashMap<>();
        if (writingRecordId != null) {
            stateDelta.put("writingRecordId", writingRecordId);
        }
        if (continuityState != null && !continuityState.isEmpty()) {
            stateDelta.put("continuity", continuityState.toStateDeltaMap());
        }
        return stateDelta.isEmpty() ? Map.of() : Map.copyOf(stateDelta);
    }

    private String summarizeAcceptedContent(String acceptedContent) {
        return truncate(acceptedContent, SUMMARY_LIMIT);
    }

    private String extractTailSentence(String value, int limit) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        String[] segments = normalized.split("(?<=[。！？!?])");
        for (int index = segments.length - 1; index >= 0; index--) {
            if (StringUtils.hasText(segments[index])) {
                return truncate(segments[index], limit);
            }
        }
        return truncate(normalized, limit);
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

    private String buildChapterStatePatchId(StorySessionContextPacket contextPacket, String sceneId) {
        return "patch-chapter-state-" + contextPacket.projectId() + "-" + contextPacket.chapterId() + "-" + sceneId + "-" + UUID.randomUUID();
    }

    private String buildChapterStateSnapshotId(StorySessionContextPacket contextPacket, String sceneId) {
        return "snapshot-chapter-state-" + contextPacket.projectId() + "-" + contextPacket.chapterId() + "-" + sceneId + "-" + UUID.randomUUID();
    }
}
