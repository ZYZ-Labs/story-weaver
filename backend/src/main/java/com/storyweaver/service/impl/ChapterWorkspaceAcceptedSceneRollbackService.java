package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.storyweaver.domain.entity.AIWritingRecord;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.KnowledgeDocument;
import com.storyweaver.domain.vo.AIWritingRollbackResponseVO;
import com.storyweaver.exception.SceneWorkflowConflictException;
import com.storyweaver.repository.AIWritingRecordMapper;
import com.storyweaver.service.ChapterService;
import com.storyweaver.service.KnowledgeDocumentService;
import com.storyweaver.story.generation.orchestration.ChapterNarrativeRuntimeModeService;
import com.storyweaver.story.generation.orchestration.SceneSkeletonItem;
import com.storyweaver.story.generation.orchestration.impl.ChapterSceneWorkflowGuardService;
import com.storyweaver.storyunit.context.ChapterAnchorBundleView;
import com.storyweaver.storyunit.context.CharacterRuntimeStateView;
import com.storyweaver.storyunit.context.StoryContextQueryService;
import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;
import com.storyweaver.storyunit.facet.state.ChapterIncrementalState;
import com.storyweaver.storyunit.service.ChapterIncrementalStateStore;
import com.storyweaver.storyunit.service.ReaderRevealStateStore;
import com.storyweaver.storyunit.service.SceneRuntimeStateStore;
import com.storyweaver.storyunit.session.SceneExecutionState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ChapterWorkspaceAcceptedSceneRollbackService {

    private static final String CHAPTER_WORKSPACE_ENTRY_POINT = "phase8.chapter-workspace.scene-draft";

    private final AIWritingRecordMapper aiWritingRecordMapper;
    private final ChapterService chapterService;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final ChapterNarrativeRuntimeModeService chapterNarrativeRuntimeModeService;
    private final ChapterSceneWorkflowGuardService chapterSceneWorkflowGuardService;
    private final SceneRuntimeStateStore sceneRuntimeStateStore;
    private final ReaderRevealStateStore readerRevealStateStore;
    private final ChapterIncrementalStateStore chapterIncrementalStateStore;
    private final StoryContextQueryService storyContextQueryService;
    private final ObjectMapper objectMapper;

    public ChapterWorkspaceAcceptedSceneRollbackService(
            AIWritingRecordMapper aiWritingRecordMapper,
            ChapterService chapterService,
            KnowledgeDocumentService knowledgeDocumentService,
            ChapterNarrativeRuntimeModeService chapterNarrativeRuntimeModeService,
            ChapterSceneWorkflowGuardService chapterSceneWorkflowGuardService,
            SceneRuntimeStateStore sceneRuntimeStateStore,
            ReaderRevealStateStore readerRevealStateStore,
            ChapterIncrementalStateStore chapterIncrementalStateStore,
            StoryContextQueryService storyContextQueryService,
            ObjectMapper objectMapper) {
        this.aiWritingRecordMapper = aiWritingRecordMapper;
        this.chapterService = chapterService;
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.chapterNarrativeRuntimeModeService = chapterNarrativeRuntimeModeService;
        this.chapterSceneWorkflowGuardService = chapterSceneWorkflowGuardService;
        this.sceneRuntimeStateStore = sceneRuntimeStateStore;
        this.readerRevealStateStore = readerRevealStateStore;
        this.chapterIncrementalStateStore = chapterIncrementalStateStore;
        this.storyContextQueryService = storyContextQueryService;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public AIWritingRollbackResponseVO rollbackLatestAcceptedScene(Long chapterId) {
        return rollbackAcceptedScenes(chapterId, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public AIWritingRollbackResponseVO rollbackAllAcceptedScenes(Long chapterId) {
        return rollbackAcceptedScenes(chapterId, true);
    }

    private AIWritingRollbackResponseVO rollbackAcceptedScenes(Long chapterId, boolean rollbackAll) {
        Chapter chapter = chapterService.getById(chapterId);
        if (chapter == null || Integer.valueOf(1).equals(chapter.getDeleted())) {
            throw new SceneWorkflowConflictException("当前章节不存在，无法撤回已接纳镜头。");
        }
        if (chapter.getProjectId() == null) {
            throw new SceneWorkflowConflictException("当前章节缺少项目归属，无法撤回已接纳镜头。");
        }
        chapterNarrativeRuntimeModeService.assertSceneMode(chapter, "撤回已接纳 scene");

        Long projectId = chapter.getProjectId();
        ChapterSceneWorkflowGuardService.SceneWorkflowState workflowState = chapterSceneWorkflowGuardService.inspect(projectId, chapterId);
        List<AcceptedSceneRecord> activeAcceptedRecords = resolveActiveAcceptedSceneRecords(projectId, chapterId, workflowState);
        if (activeAcceptedRecords.isEmpty()) {
            throw new SceneWorkflowConflictException("当前章节还没有已接纳镜头，无法执行撤回。");
        }

        String expectedCurrentContent = resolveExpectedCurrentContent(activeAcceptedRecords);
        String actualCurrentContent = normalizeContentValue(chapter.getContent());
        if (!actualCurrentContent.equals(expectedCurrentContent)) {
            throw new SceneWorkflowConflictException("当前章节正文在镜头接纳后又被手动改动，无法安全撤回已接纳镜头。请先处理正文漂移，再执行撤回。");
        }

        List<AcceptedSceneRecord> targetRecords = rollbackAll
                ? List.copyOf(activeAcceptedRecords)
                : List.of(activeAcceptedRecords.getLast());
        List<AcceptedSceneRecord> remainingRecords = rollbackAll
                ? List.of()
                : List.copyOf(activeAcceptedRecords.subList(0, activeAcceptedRecords.size() - 1));

        String restoredContent = rollbackAll
                ? resolveContentBeforeAccept(activeAcceptedRecords.getFirst())
                : resolveContentBeforeAccept(targetRecords.getFirst());

        chapter.setContent(restoredContent);
        chapter.setWordCount(restoredContent.length());
        chapterService.updateById(chapter);
        syncKnowledgeDocument(chapter);

        String rollbackMode = rollbackAll ? "all" : "latest";
        LocalDateTime rolledBackAt = LocalDateTime.now();
        for (AcceptedSceneRecord targetRecord : targetRecords) {
            targetRecord.record().setStatus("rolled_back");
            targetRecord.record().setGenerationTraceJson(buildRollbackTraceJson(
                    targetRecord.traceRoot(),
                    rollbackMode,
                    targetRecord.sceneId(),
                    rolledBackAt,
                    restoredContent.length()
            ));
            aiWritingRecordMapper.updateById(targetRecord.record());
            sceneRuntimeStateStore.deleteSceneState(projectId, chapterId, targetRecord.sceneId());
            sceneRuntimeStateStore.deleteHandoffsFromScene(projectId, chapterId, targetRecord.sceneId());
        }

        readerRevealStateStore.saveChapterRevealState(buildReaderRevealState(projectId, chapter, remainingRecords));
        chapterIncrementalStateStore.saveChapterState(buildChapterIncrementalState(projectId, chapterId, workflowState, remainingRecords));

        List<Long> rolledBackRecordIds = targetRecords.stream()
                .map(item -> item.record().getId())
                .toList();
        List<String> rolledBackSceneIds = targetRecords.stream()
                .map(AcceptedSceneRecord::sceneId)
                .toList();
        List<String> remainingSceneIds = remainingRecords.stream()
                .map(AcceptedSceneRecord::sceneId)
                .toList();
        String unlockedSceneId = rollbackAll
                ? workflowState.scenes().getFirst().sceneId()
                : targetRecords.getFirst().sceneId();

        return new AIWritingRollbackResponseVO(
                chapterId,
                rollbackMode,
                rolledBackRecordIds,
                rolledBackSceneIds,
                remainingSceneIds,
                unlockedSceneId,
                restoredContent.length(),
                buildRollbackMessage(rollbackAll, rolledBackSceneIds, unlockedSceneId, remainingSceneIds.isEmpty())
        );
    }

    private List<AcceptedSceneRecord> resolveActiveAcceptedSceneRecords(
            Long projectId,
            Long chapterId,
            ChapterSceneWorkflowGuardService.SceneWorkflowState workflowState) {
        Map<String, AcceptedSceneRecord> acceptedBySceneId = new LinkedHashMap<>();
        for (AIWritingRecord record : aiWritingRecordMapper.findByChapterId(chapterId)) {
            if (!"accepted".equalsIgnoreCase(normalizeStatus(record.getStatus()))) {
                continue;
            }
            JsonNode traceRoot = readJson(record.getGenerationTraceJson());
            String entryPoint = resolveEntryPoint(traceRoot);
            if (!CHAPTER_WORKSPACE_ENTRY_POINT.equals(entryPoint)) {
                continue;
            }
            String sceneId = resolveSceneId(record, traceRoot);
            if (!StringUtils.hasText(sceneId)) {
                throw new SceneWorkflowConflictException("存在缺少 sceneId 的已接纳镜头记录，无法安全撤回。");
            }
            if (acceptedBySceneId.containsKey(sceneId)) {
                throw new SceneWorkflowConflictException("镜头 " + sceneId + " 存在多条已接纳记录，无法安全撤回。");
            }
            SceneSkeletonItem scene = workflowState.findScene(sceneId)
                    .orElseThrow(() -> new SceneWorkflowConflictException("已接纳镜头 " + sceneId + " 不存在于当前章节骨架中。"));
            SceneExecutionState runtimeState = sceneRuntimeStateStore.getSceneState(projectId, chapterId, sceneId).orElse(null);
            acceptedBySceneId.put(sceneId, new AcceptedSceneRecord(record, traceRoot, sceneId, scene, runtimeState, readAcceptanceSnapshot(traceRoot)));
        }

        for (String completedSceneId : workflowState.completedSceneIds()) {
            if (!acceptedBySceneId.containsKey(completedSceneId)) {
                throw new SceneWorkflowConflictException("镜头 " + completedSceneId + " 已标记完成，但缺少已接纳记录，无法安全撤回。");
            }
        }

        List<AcceptedSceneRecord> ordered = new ArrayList<>();
        List<String> orderedAcceptedSceneIds = workflowState.scenes().stream()
                .map(SceneSkeletonItem::sceneId)
                .filter(acceptedBySceneId::containsKey)
                .toList();
        List<String> expectedPrefixSceneIds = workflowState.scenes().stream()
                .limit(orderedAcceptedSceneIds.size())
                .map(SceneSkeletonItem::sceneId)
                .toList();
        if (!orderedAcceptedSceneIds.equals(expectedPrefixSceneIds)) {
            throw new SceneWorkflowConflictException("已接纳镜头不是当前骨架的连续前缀，无法安全撤回。");
        }
        for (String sceneId : orderedAcceptedSceneIds) {
            AcceptedSceneRecord acceptedSceneRecord = acceptedBySceneId.get(sceneId);
            if (acceptedSceneRecord == null) {
                throw new SceneWorkflowConflictException("镜头 " + sceneId + " 的已接纳记录缺失，无法安全撤回。");
            }
            ordered.add(acceptedSceneRecord);
        }
        return List.copyOf(ordered);
    }

    private String resolveExpectedCurrentContent(List<AcceptedSceneRecord> acceptedRecords) {
        AcceptedSceneRecord latestRecord = acceptedRecords.getLast();
        String latestSnapshot = latestRecord.acceptanceSnapshot().contentAfterAccept();
        if (latestSnapshot != null) {
            return latestSnapshot;
        }

        String expected = resolveContentBeforeAccept(acceptedRecords.getFirst());
        for (AcceptedSceneRecord acceptedRecord : acceptedRecords) {
            String before = acceptedRecord.acceptanceSnapshot().contentBeforeAccept();
            if (before != null) {
                expected = before;
            }
            expected = resolveContentAfterAccept(acceptedRecord, expected);
        }
        return expected;
    }

    private String resolveContentBeforeAccept(AcceptedSceneRecord acceptedRecord) {
        String snapshot = acceptedRecord.acceptanceSnapshot().contentBeforeAccept();
        return snapshot != null ? snapshot : normalizeContentValue(acceptedRecord.record().getOriginalContent());
    }

    private String resolveContentAfterAccept(AcceptedSceneRecord acceptedRecord, String fallbackBeforeAccept) {
        String snapshot = acceptedRecord.acceptanceSnapshot().contentAfterAccept();
        if (snapshot != null) {
            return snapshot;
        }
        return applyAcceptedContent(fallbackBeforeAccept, acceptedRecord.record().getGeneratedContent(), acceptedRecord.record().getWritingType());
    }

    private String applyAcceptedContent(String beforeAccept, String generatedContent, String writingType) {
        String currentContent = normalizeContentValue(beforeAccept);
        String acceptedContent = normalizeContentValue(generatedContent);
        if ("continue".equalsIgnoreCase(normalizeStatus(writingType))) {
            return currentContent.isBlank() ? acceptedContent : currentContent + "\n\n" + acceptedContent;
        }
        return acceptedContent;
    }

    private ReaderRevealState buildReaderRevealState(Long projectId, Chapter chapter, List<AcceptedSceneRecord> remainingRecords) {
        List<String> knownFacts = buildBaseKnownFacts(projectId, chapter);
        List<String> unrevealedFacts = buildBaseUnrevealedFacts(projectId, chapter);
        List<String> revealDelta = remainingRecords.stream()
                .map(this::resolveReaderRevealDelta)
                .flatMap(List::stream)
                .toList();

        List<String> readerKnown = mergeDistinct(knownFacts, revealDelta);
        List<String> remainingUnrevealed = unrevealedFacts.stream()
                .filter(item -> !readerKnown.contains(item))
                .toList();
        List<String> systemAndAuthorKnown = mergeDistinct(mergeDistinct(knownFacts, unrevealedFacts), revealDelta);

        return new ReaderRevealState(
                projectId,
                chapter.getId(),
                systemAndAuthorKnown,
                systemAndAuthorKnown,
                readerKnown,
                remainingUnrevealed,
                "已按剩余已接纳镜头重建读者已知 " + readerKnown.size() + " 条，未揭晓 " + remainingUnrevealed.size() + " 条"
        );
    }

    private List<String> resolveReaderRevealDelta(AcceptedSceneRecord acceptedSceneRecord) {
        if (acceptedSceneRecord.runtimeState() != null && acceptedSceneRecord.runtimeState().readerRevealDelta() != null) {
            return acceptedSceneRecord.runtimeState().readerRevealDelta();
        }
        return acceptedSceneRecord.scene().readerReveal();
    }

    private ChapterIncrementalState buildChapterIncrementalState(
            Long projectId,
            Long chapterId,
            ChapterSceneWorkflowGuardService.SceneWorkflowState workflowState,
            List<AcceptedSceneRecord> remainingRecords) {
        Optional<CharacterRuntimeStateView> mainPovState = storyContextQueryService.getChapterAnchorBundle(projectId, chapterId)
                .map(ChapterAnchorBundleView::mainPovCharacterId)
                .filter(id -> id != null)
                .flatMap(characterId -> storyContextQueryService.getCharacterRuntimeState(projectId, characterId));

        List<String> activeLocations = sanitizeDistinct(mainPovState.map(CharacterRuntimeStateView::currentLocation)
                .map(List::of)
                .orElseGet(List::of));
        Map<String, String> emotions = mainPovState
                .filter(state -> StringUtils.hasText(state.emotionalState()))
                .map(state -> Map.of(state.characterName(), state.emotionalState().trim()))
                .orElseGet(Map::of);
        Map<String, String> attitudes = mainPovState
                .filter(state -> StringUtils.hasText(state.attitudeSummary()))
                .map(state -> Map.of(state.characterName(), state.attitudeSummary().trim()))
                .orElseGet(Map::of);
        Map<String, List<String>> stateTags = mainPovState
                .filter(state -> state.stateTags() != null && !state.stateTags().isEmpty())
                .map(state -> Map.of(state.characterName(), sanitizeDistinct(state.stateTags())))
                .orElseGet(Map::of);

        List<String> resolvedLoops = remainingRecords.stream()
                .map(AcceptedSceneRecord::sceneId)
                .map(this::scenePendingLoop)
                .toList();
        List<String> openLoops = resolveOpenLoopsAfterRollback(workflowState, remainingRecords);
        String summary = remainingRecords.isEmpty()
                ? "章节状态已回退到镜头执行前基线。"
                : "已回退到 " + remainingRecords.getLast().sceneId()
                + " 接纳后状态，下一镜头 "
                + (openLoops.isEmpty() ? "无" : openLoops.getFirst().replace("scene:", "").replace(":pending", ""))
                + " 待执行。";

        return new ChapterIncrementalState(
                projectId,
                chapterId,
                openLoops,
                resolvedLoops,
                activeLocations,
                emotions,
                attitudes,
                stateTags,
                summary
        );
    }

    private List<String> resolveOpenLoopsAfterRollback(
            ChapterSceneWorkflowGuardService.SceneWorkflowState workflowState,
            List<AcceptedSceneRecord> remainingRecords) {
        if (remainingRecords.isEmpty()) {
            return List.of();
        }
        return workflowState.nextScene(remainingRecords.getLast().sceneId())
                .map(SceneSkeletonItem::sceneId)
                .filter(StringUtils::hasText)
                .map(this::scenePendingLoop)
                .map(List::of)
                .orElseGet(List::of);
    }

    private List<String> buildBaseKnownFacts(Long projectId, Chapter currentChapter) {
        List<Chapter> chapters = chapterService.list(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, projectId)
                .eq(Chapter::getDeleted, 0));
        List<String> rawKnownFacts = chapters.stream()
                .filter(chapter -> chapter != null && chapter.getId() != null && !chapter.getId().equals(currentChapter.getId()))
                .filter(chapter -> isEarlierChapter(chapter, currentChapter))
                .sorted(Comparator
                        .comparing((Chapter item) -> item.getOrderNum() == null ? Integer.MAX_VALUE : item.getOrderNum())
                        .thenComparing(Chapter::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::buildChapterFact)
                .filter(StringUtils::hasText)
                .toList();
        if (rawKnownFacts.size() <= 5) {
            return rawKnownFacts;
        }
        return rawKnownFacts.subList(Math.max(0, rawKnownFacts.size() - 5), rawKnownFacts.size());
    }

    private List<String> buildBaseUnrevealedFacts(Long projectId, Chapter currentChapter) {
        return storyContextQueryService.getChapterAnchorBundle(projectId, currentChapter.getId())
                .map(bundle -> buildUnrevealedFacts(currentChapter, bundle))
                .orElseGet(List::of);
    }

    private List<String> buildUnrevealedFacts(Chapter currentChapter, ChapterAnchorBundleView bundle) {
        if (StringUtils.hasText(currentChapter.getContent()) && currentChapter.getWordCount() != null && currentChapter.getWordCount() > 0) {
            return List.of();
        }
        List<String> unrevealed = new ArrayList<>();
        if (StringUtils.hasText(bundle.chapterSummary())) {
            unrevealed.add("本章待揭晓：" + bundle.chapterSummary().trim());
        }
        if (bundle.storyBeats() != null && !bundle.storyBeats().isEmpty()) {
            unrevealed.add("待推进剧情：" + String.join("、", bundle.storyBeats()));
        }
        if (StringUtils.hasText(bundle.mainPovCharacterName())) {
            unrevealed.add("当前视角人物：" + bundle.mainPovCharacterName().trim());
        }
        return sanitizeDistinct(unrevealed);
    }

    private void syncKnowledgeDocument(Chapter chapter) {
        if (chapter.getProjectId() == null) {
            return;
        }
        QueryWrapper<KnowledgeDocument> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_id", chapter.getProjectId())
                .eq("source_type", "chapter")
                .eq("source_ref_id", String.valueOf(chapter.getId()))
                .eq("deleted", 0);

        KnowledgeDocument document = knowledgeDocumentService.getOne(queryWrapper, false);
        if (document == null) {
            document = new KnowledgeDocument();
            document.setProjectId(chapter.getProjectId());
            document.setSourceType("chapter");
            document.setSourceRefId(String.valueOf(chapter.getId()));
        }
        document.setTitle(chapter.getTitle());
        document.setContentText(chapter.getContent());
        document.setSummary(buildSummary(chapter.getContent()));
        document.setStatus("indexed");
        if (document.getId() == null) {
            knowledgeDocumentService.save(document);
        } else {
            knowledgeDocumentService.updateById(document);
        }
    }

    private String buildRollbackTraceJson(
            JsonNode originalTraceRoot,
            String rollbackMode,
            String sceneId,
            LocalDateTime rolledBackAt,
            int restoredContentLength) {
        ObjectNode root = toObjectNode(originalTraceRoot);
        ObjectNode rollbackNode = root.putObject("rollback");
        rollbackNode.put("mode", rollbackMode);
        rollbackNode.put("sceneId", sceneId);
        rollbackNode.put("rolledBackAt", rolledBackAt.toString());
        rollbackNode.put("restoredContentLength", restoredContentLength);
        rollbackNode.put("status", "rolled_back");
        return writeJson(root);
    }

    private AcceptanceSnapshot readAcceptanceSnapshot(JsonNode traceRoot) {
        JsonNode acceptanceNode = traceRoot.path("acceptance");
        if (acceptanceNode.isMissingNode() || acceptanceNode.isNull()) {
            return new AcceptanceSnapshot(null, null);
        }
        String beforeAccept = acceptanceNode.has("contentBeforeAccept")
                ? acceptanceNode.path("contentBeforeAccept").asText("")
                : null;
        String afterAccept = acceptanceNode.has("contentAfterAccept")
                ? acceptanceNode.path("contentAfterAccept").asText("")
                : null;
        return new AcceptanceSnapshot(beforeAccept, afterAccept);
    }

    private String buildRollbackMessage(boolean rollbackAll, List<String> rolledBackSceneIds, String unlockedSceneId, boolean noRemainingAcceptedScenes) {
        if (rollbackAll) {
            return "已撤回本章全部已接纳镜头，当前重新回到 " + unlockedSceneId + "。";
        }
        if (noRemainingAcceptedScenes) {
            return "已撤回 " + String.join("、", rolledBackSceneIds) + "，当前章节重新回到首个镜头。";
        }
        return "已撤回 " + String.join("、", rolledBackSceneIds) + "，当前可继续处理 " + unlockedSceneId + "。";
    }

    private String buildChapterFact(Chapter chapter) {
        String title = trimToEmpty(chapter.getTitle());
        String summary = firstNonBlank(chapter.getSummary(), truncateForFact(chapter.getContent(), 80));
        if (!StringUtils.hasText(summary) && !StringUtils.hasText(title)) {
            return "";
        }
        if (!StringUtils.hasText(title)) {
            return summary;
        }
        if (!StringUtils.hasText(summary)) {
            return "已发生章节：" + title;
        }
        return title + "：" + summary;
    }

    private boolean isEarlierChapter(Chapter candidate, Chapter current) {
        int candidateOrder = candidate.getOrderNum() == null ? Integer.MAX_VALUE : candidate.getOrderNum();
        int currentOrder = current.getOrderNum() == null ? Integer.MAX_VALUE : current.getOrderNum();
        if (candidateOrder != currentOrder) {
            return candidateOrder < currentOrder;
        }
        if (candidate.getCreateTime() != null && current.getCreateTime() != null) {
            return candidate.getCreateTime().isBefore(current.getCreateTime());
        }
        return candidate.getId() < current.getId();
    }

    private String resolveEntryPoint(JsonNode traceRoot) {
        return text(traceRoot.path("orchestration").path("entryPoint"));
    }

    private String resolveSceneId(AIWritingRecord record, JsonNode traceRoot) {
        String sceneId = text(traceRoot.path("orchestration").path("sceneId"));
        if (StringUtils.hasText(sceneId)) {
            return sceneId;
        }
        if (!StringUtils.hasText(record.getUserInstruction())) {
            return "";
        }
        for (String line : record.getUserInstruction().split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("【镜头ID】")) {
                return trimmed.substring("【镜头ID】".length()).trim();
            }
        }
        int marker = record.getUserInstruction().indexOf("scene-");
        if (marker < 0) {
            return "";
        }
        int endIndex = marker + "scene-".length();
        while (endIndex < record.getUserInstruction().length()
                && Character.isDigit(record.getUserInstruction().charAt(endIndex))) {
            endIndex++;
        }
        return endIndex > marker + "scene-".length()
                ? record.getUserInstruction().substring(marker, endIndex).trim()
                : "";
    }

    private JsonNode readJson(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode node = objectMapper.readTree(rawJson);
            return node == null ? objectMapper.createObjectNode() : node;
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private ObjectNode toObjectNode(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            return objectNode.deepCopy();
        }
        return objectMapper.createObjectNode();
    }

    private String writeJson(ObjectNode root) {
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private String text(JsonNode node) {
        return node == null ? "" : node.asText("").trim();
    }

    private String normalizeStatus(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeContentValue(String value) {
        return value == null ? "" : value;
    }

    private String scenePendingLoop(String sceneId) {
        return "scene:" + sceneId + ":pending";
    }

    private List<String> mergeDistinct(List<String> first, List<String> second) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(sanitizeDistinct(first));
        merged.addAll(sanitizeDistinct(second));
        return List.copyOf(merged);
    }

    private List<String> sanitizeDistinct(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> sanitized = new LinkedHashSet<>();
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            sanitized.add(value.trim());
        }
        return List.copyOf(sanitized);
    }

    private String buildSummary(String content) {
        if (!StringUtils.hasText(content)) {
            return "章节正文已回退到当前镜头栈对应版本。";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return "";
        }
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return "";
    }

    private String truncateForFact(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private record AcceptanceSnapshot(String contentBeforeAccept, String contentAfterAccept) {
    }

    private record AcceptedSceneRecord(
            AIWritingRecord record,
            JsonNode traceRoot,
            String sceneId,
            SceneSkeletonItem scene,
            SceneExecutionState runtimeState,
            AcceptanceSnapshot acceptanceSnapshot) {
    }
}
