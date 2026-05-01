package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.storyweaver.ai.director.application.AIDirectorApplicationService;
import com.storyweaver.domain.dto.AIDirectorDecisionRequestDTO;
import com.storyweaver.domain.dto.AIWritingRequestDTO;
import com.storyweaver.exception.SceneWorkflowConflictException;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.domain.entity.AIWritingRecord;
import com.storyweaver.domain.entity.Causality;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.KnowledgeDocument;
import com.storyweaver.domain.entity.Outline;
import com.storyweaver.domain.entity.Plot;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.vo.AIWritingChatParticipationVO;
import com.storyweaver.domain.vo.AIDirectorDecisionVO;
import com.storyweaver.domain.vo.AIWritingRollbackResponseVO;
import com.storyweaver.domain.vo.AIWritingResponseVO;
import com.storyweaver.domain.vo.AIWritingStreamEventVO;
import com.storyweaver.domain.vo.WorldSettingVO;
import com.storyweaver.item.domain.entity.CharacterInventoryItem;
import com.storyweaver.item.domain.entity.ItemDefinition;
import com.storyweaver.item.infrastructure.persistence.mapper.CharacterInventoryItemMapper;
import com.storyweaver.item.infrastructure.persistence.mapper.ItemMapper;
import com.storyweaver.repository.AIWritingRecordMapper;
import com.storyweaver.service.AIModelRoutingService;
import com.storyweaver.service.AIProviderService;
import com.storyweaver.service.AIWritingChatService;
import com.storyweaver.service.AIWritingService;
import com.storyweaver.service.CausalityService;
import com.storyweaver.service.ChapterService;
import com.storyweaver.service.KnowledgeDocumentService;
import com.storyweaver.service.OutlineService;
import com.storyweaver.service.PlotService;
import com.storyweaver.service.ProjectService;
import com.storyweaver.service.SystemConfigService;
import com.storyweaver.service.WorldSettingService;
import com.storyweaver.story.generation.ChapterAnchorBundle;
import com.storyweaver.story.generation.GenerationReadinessService;
import com.storyweaver.story.generation.GenerationReadinessVO;
import com.storyweaver.story.generation.ReaderRevealConstraint;
import com.storyweaver.story.generation.StructuredCreationSuggestion;
import com.storyweaver.story.generation.StructuredCreationSuggestionService;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.story.generation.orchestration.SceneExecutionWriteService;
import com.storyweaver.story.generation.orchestration.SceneSkeletonItem;
import com.storyweaver.story.generation.orchestration.StorySessionContextAssembler;
import com.storyweaver.story.generation.orchestration.ChapterNarrativeRuntimeModeService;
import com.storyweaver.story.generation.orchestration.impl.AIContinuityStateService;
import com.storyweaver.story.generation.orchestration.impl.SceneContinuitySupport;
import com.storyweaver.story.generation.orchestration.impl.ChapterSceneWorkflowGuardService;
import com.storyweaver.storyunit.session.SceneContinuityState;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
public class AIWritingServiceImpl extends ServiceImpl<AIWritingRecordMapper, AIWritingRecord> implements AIWritingService {

    private static final int MAX_CONTEXT_ITEMS = 4;
    private static final long STREAM_HEARTBEAT_INTERVAL_MS = 10_000L;

    private final ChapterService chapterService;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final AIProviderService aiProviderService;
    private final ProjectService projectService;
    private final SystemConfigService systemConfigService;
    private final OutlineService outlineService;
    private final PlotService plotService;
    private final CausalityService causalityService;
    private final WorldSettingService worldSettingService;
    private final CharacterInventoryItemMapper characterInventoryItemMapper;
    private final ItemMapper itemMapper;
    private final AIModelRoutingService aiModelRoutingService;
    private final AIWritingChatService aiWritingChatService;
    private final AIDirectorApplicationService aiDirectorApplicationService;
    private final GenerationReadinessService generationReadinessService;
    private final StructuredCreationSuggestionService structuredCreationSuggestionService;
    private final StorySessionContextAssembler storySessionContextAssembler;
    private final SceneExecutionWriteService sceneExecutionWriteService;
    private final ChapterSceneWorkflowGuardService chapterSceneWorkflowGuardService;
    private final ChapterNarrativeRuntimeModeService chapterNarrativeRuntimeModeService;
    private final ChapterWorkspaceAcceptedSceneRollbackService chapterWorkspaceAcceptedSceneRollbackService;
    private final AIContinuityStateService aiContinuityStateService;
    private final ObjectMapper objectMapper;

    public AIWritingServiceImpl(
            ChapterService chapterService,
            KnowledgeDocumentService knowledgeDocumentService,
            AIProviderService aiProviderService,
            ProjectService projectService,
            SystemConfigService systemConfigService,
            OutlineService outlineService,
            PlotService plotService,
            CausalityService causalityService,
            WorldSettingService worldSettingService,
            CharacterInventoryItemMapper characterInventoryItemMapper,
            ItemMapper itemMapper,
            AIModelRoutingService aiModelRoutingService,
            AIWritingChatService aiWritingChatService,
            AIDirectorApplicationService aiDirectorApplicationService,
            GenerationReadinessService generationReadinessService,
            StructuredCreationSuggestionService structuredCreationSuggestionService,
            StorySessionContextAssembler storySessionContextAssembler,
            SceneExecutionWriteService sceneExecutionWriteService,
            ChapterSceneWorkflowGuardService chapterSceneWorkflowGuardService,
            ChapterNarrativeRuntimeModeService chapterNarrativeRuntimeModeService,
            ChapterWorkspaceAcceptedSceneRollbackService chapterWorkspaceAcceptedSceneRollbackService,
            AIContinuityStateService aiContinuityStateService,
            ObjectMapper objectMapper) {
        this.chapterService = chapterService;
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.aiProviderService = aiProviderService;
        this.projectService = projectService;
        this.systemConfigService = systemConfigService;
        this.outlineService = outlineService;
        this.plotService = plotService;
        this.causalityService = causalityService;
        this.worldSettingService = worldSettingService;
        this.characterInventoryItemMapper = characterInventoryItemMapper;
        this.itemMapper = itemMapper;
        this.aiModelRoutingService = aiModelRoutingService;
        this.aiWritingChatService = aiWritingChatService;
        this.aiDirectorApplicationService = aiDirectorApplicationService;
        this.generationReadinessService = generationReadinessService;
        this.structuredCreationSuggestionService = structuredCreationSuggestionService;
        this.storySessionContextAssembler = storySessionContextAssembler;
        this.sceneExecutionWriteService = sceneExecutionWriteService;
        this.chapterSceneWorkflowGuardService = chapterSceneWorkflowGuardService;
        this.chapterNarrativeRuntimeModeService = chapterNarrativeRuntimeModeService;
        this.chapterWorkspaceAcceptedSceneRollbackService = chapterWorkspaceAcceptedSceneRollbackService;
        this.aiContinuityStateService = aiContinuityStateService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AIWritingResponseVO generateContent(Long userId, AIWritingRequestDTO requestDTO) {
        PreparedGenerationContext context = prepareGeneration(userId, requestDTO, null);
        WorkflowResult workflowResult = runWorkflow(context, null, false);
        return persistGeneratedRecord(context, workflowResult.content());
    }

    @Override
    public void streamContent(Long userId, AIWritingRequestDTO requestDTO, Consumer<AIWritingStreamEventVO> eventConsumer) {
        emitStage(eventConsumer, "prepare", "started", "已收到生成请求，正在整理章节上下文");
        PreparedGenerationContext context = prepareGeneration(userId, requestDTO, eventConsumer);

        eventConsumer.accept(AIWritingStreamEventVO.meta(
                context.writingType(),
                context.provider().getId(),
                context.selectedModel(),
                context.maxTokens()
        ));
        emitStage(eventConsumer, "prepare", "completed", "章节上下文准备完成，开始生成正文");

        WorkflowResult workflowResult = runWorkflow(context, eventConsumer, true);
        AIWritingResponseVO response = persistGeneratedRecord(context, workflowResult.content());
        eventConsumer.accept(AIWritingStreamEventVO.complete(toStreamCompleteRecord(response)));
    }

    @Override
    public List<AIWritingResponseVO> getRecordsByChapterId(Long chapterId) {
        LambdaQueryWrapper<AIWritingRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AIWritingRecord::getChapterId, chapterId)
                .eq(AIWritingRecord::getDeleted, 0)
                .orderByDesc(AIWritingRecord::getCreateTime);

        return list(queryWrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AIWritingResponseVO> getRecordsByProjectId(Long projectId) {
        return baseMapper.findByProjectId(projectId).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public AIWritingResponseVO getRecordById(Long id) {
        AIWritingRecord record = getById(id);
        if (record == null || Integer.valueOf(1).equals(record.getDeleted())) {
            return null;
        }
        return convertToVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AIWritingResponseVO acceptGeneratedContent(Long id) {
        AIWritingRecord record = getById(id);
        if (record == null || Integer.valueOf(1).equals(record.getDeleted())) {
            return null;
        }
        if ("accepted".equalsIgnoreCase(record.getStatus())
                || "rejected".equalsIgnoreCase(record.getStatus())
                || "rolled_back".equalsIgnoreCase(record.getStatus())) {
            throw new SceneWorkflowConflictException("该草稿已处理，不能重复接纳。");
        }

        Chapter chapter = chapterService.getById(record.getChapterId());
        JsonNode traceRoot = readJson(record.getGenerationTraceJson());
        String entryPoint = resolveOrchestrationEntryPoint(traceRoot);
        String sceneId = resolveOrchestrationSceneId(record, traceRoot);
        Long projectId = chapter == null ? null : chapter.getProjectId();
        ChapterSceneWorkflowGuardService.SceneWorkflowState workflowState = null;
        if (isChapterWorkspaceSceneDraftEntryPoint(entryPoint)) {
            if (chapter == null || projectId == null) {
                throw new SceneWorkflowConflictException("章节工作区镜头草稿缺少有效的章节或项目信息，无法完成接纳。");
            }
            chapterNarrativeRuntimeModeService.assertSceneMode(chapter, "在章节工作区接纳 scene 草稿");
            workflowState = chapterSceneWorkflowGuardService.assertCurrentUnlockedScene(
                    projectId,
                    record.getChapterId(),
                    sceneId,
                    "接纳当前镜头"
                );
        }
        if (chapter != null) {
            String contentBeforeAccept = chapter.getContent() == null ? "" : chapter.getContent();
            String newContent;
            if ("continue".equals(record.getWritingType())) {
                newContent = contentBeforeAccept.isBlank()
                        ? record.getGeneratedContent()
                        : contentBeforeAccept + "\n\n" + record.getGeneratedContent();
            } else {
                newContent = record.getGeneratedContent();
            }
            chapter.setContent(newContent);
            chapter.setWordCount(chapter.getContent() == null ? 0 : chapter.getContent().length());
            chapterService.updateById(chapter);
            record.setGenerationTraceJson(updateGenerationTraceWithAcceptance(
                    traceRoot,
                    entryPoint,
                    sceneId,
                    contentBeforeAccept,
                    newContent
            ));
            syncKnowledgeDocument(chapter, record);
        }
        if (workflowState != null && projectId != null) {
            writeAcceptedSceneRuntime(projectId, chapter.getId(), sceneId, workflowState, record);
        }

        record.setStatus("accepted");
        updateById(record);
        return convertToVO(record);
    }

    @Override
    public AIWritingResponseVO rejectGeneratedContent(Long id) {
        AIWritingRecord record = getById(id);
        if (record != null && !Integer.valueOf(1).equals(record.getDeleted())) {
            record.setStatus("rejected");
            updateById(record);
            return convertToVO(record);
        }
        return null;
    }

    @Override
    public AIWritingRollbackResponseVO rollbackLatestAcceptedScene(Long chapterId) {
        Chapter chapter = chapterService.getById(chapterId);
        if (chapter != null) {
            chapterNarrativeRuntimeModeService.assertSceneMode(chapter, "撤回已接纳 scene");
        }
        return chapterWorkspaceAcceptedSceneRollbackService.rollbackLatestAcceptedScene(chapterId);
    }

    @Override
    public AIWritingRollbackResponseVO rollbackAllAcceptedScenes(Long chapterId) {
        Chapter chapter = chapterService.getById(chapterId);
        if (chapter != null) {
            chapterNarrativeRuntimeModeService.assertSceneMode(chapter, "撤回已接纳 scene");
        }
        return chapterWorkspaceAcceptedSceneRollbackService.rollbackAllAcceptedScenes(chapterId);
    }

    private void writeAcceptedSceneRuntime(
            Long projectId,
            Long chapterId,
            String sceneId,
            ChapterSceneWorkflowGuardService.SceneWorkflowState workflowState,
            AIWritingRecord record) {
        if (!StringUtils.hasText(sceneId)) {
            throw new SceneWorkflowConflictException("当前镜头草稿缺少 sceneId，无法完成接纳。");
        }
        SceneSkeletonItem currentScene = workflowState.findScene(sceneId)
                .orElseThrow(() -> new SceneWorkflowConflictException("当前镜头 " + sceneId + " 不存在于章节骨架中。"));
        SceneSkeletonItem nextScene = workflowState.nextScene(sceneId).orElse(null);
        storySessionContextAssembler.assemble(projectId, chapterId, sceneId)
                .ifPresentOrElse(
                        contextPacket -> sceneExecutionWriteService.writeAccepted(
                                contextPacket,
                                currentScene,
                                nextScene,
                                record.getId(),
                                record.getGeneratedContent()
                        ),
                        () -> {
                            throw new SceneWorkflowConflictException("当前镜头上下文装配失败，无法完成接纳写回。");
                        }
                );
    }

    private String updateGenerationTraceWithAcceptance(
            JsonNode traceRoot,
            String entryPoint,
            String sceneId,
            String contentBeforeAccept,
            String contentAfterAccept) {
        ObjectNode root = traceRoot instanceof ObjectNode objectNode
                ? objectNode.deepCopy()
                : objectMapper.createObjectNode();
        ObjectNode acceptanceNode = root.putObject("acceptance");
        acceptanceNode.put("entryPoint", entryPoint == null ? "" : entryPoint.trim());
        acceptanceNode.put("sceneId", sceneId == null ? "" : sceneId.trim());
        acceptanceNode.put("contentBeforeAccept", contentBeforeAccept == null ? "" : contentBeforeAccept);
        acceptanceNode.put("contentAfterAccept", contentAfterAccept == null ? "" : contentAfterAccept);
        acceptanceNode.put("acceptedAt", LocalDateTime.now().toString());
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String resolveRequestedSceneId(AIWritingRequestDTO requestDTO, String userInstruction) {
        if (StringUtils.hasText(requestDTO.getSceneId())) {
            return requestDTO.getSceneId().trim();
        }
        return extractSceneIdFromInstruction(userInstruction);
    }

    private String resolveOrchestrationEntryPoint(JsonNode traceRoot) {
        return traceRoot.path("orchestration").path("entryPoint").asText("").trim();
    }

    private String resolveOrchestrationSceneId(AIWritingRecord record, JsonNode traceRoot) {
        String sceneId = traceRoot.path("orchestration").path("sceneId").asText("").trim();
        if (StringUtils.hasText(sceneId)) {
            return sceneId;
        }
        return extractSceneIdFromInstruction(record.getUserInstruction());
    }

    private String extractSceneIdFromInstruction(String userInstruction) {
        if (!StringUtils.hasText(userInstruction)) {
            return "";
        }
        for (String line : userInstruction.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("【镜头ID】")) {
                return trimmed.substring("【镜头ID】".length()).trim();
            }
        }
        int marker = userInstruction.indexOf("scene-");
        if (marker < 0) {
            return "";
        }
        int endIndex = marker + "scene-".length();
        while (endIndex < userInstruction.length() && Character.isDigit(userInstruction.charAt(endIndex))) {
            endIndex++;
        }
        return endIndex > marker + "scene-".length() ? userInstruction.substring(marker, endIndex).trim() : "";
    }

    private boolean isChapterWorkspaceSceneDraftEntryPoint(String entryPoint) {
        return "phase8.chapter-workspace.scene-draft".equals(entryPoint == null ? "" : entryPoint.trim());
    }

    private PreparedGenerationContext prepareGeneration(
            Long userId,
            AIWritingRequestDTO requestDTO,
            Consumer<AIWritingStreamEventVO> eventConsumer) {
        Chapter chapter = chapterService.getChapterWithAuth(requestDTO.getChapterId(), userId);
        if (chapter == null) {
            throw new IllegalArgumentException("章节不存在或无权访问");
        }

        String currentContent = normalizeText(
                StringUtils.hasText(requestDTO.getCurrentContent()) ? requestDTO.getCurrentContent() : chapter.getContent()
        );
        String writingType = normalizeWritingType(requestDTO.getWritingType(), currentContent);
        String userInstruction = normalizeText(requestDTO.getUserInstruction());
        String entryPoint = normalizeText(requestDTO.getEntryPoint());
        String sceneId = resolveRequestedSceneId(requestDTO, userInstruction);
        SceneDraftConstraintBundle sceneDraftConstraintBundle = null;
        if (isChapterWorkspaceSceneDraftEntryPoint(entryPoint)) {
            if (chapter.getProjectId() == null) {
                throw new SceneWorkflowConflictException("当前章节缺少项目归属，无法按镜头顺序生成。");
            }
            chapterNarrativeRuntimeModeService.assertSceneMode(chapter, "在章节工作区生成 scene 草稿");
            ChapterSceneWorkflowGuardService.SceneWorkflowState workflowState = chapterSceneWorkflowGuardService.assertCurrentUnlockedScene(
                    chapter.getProjectId(),
                    chapter.getId(),
                    sceneId,
                    "生成当前镜头草稿"
            );
            sceneDraftConstraintBundle = buildSceneDraftConstraintBundle(
                    chapter.getProjectId(),
                    chapter.getId(),
                    sceneId,
                    workflowState
            );
        }
        AIModelRoutingService.ResolvedModelSelection selection = aiModelRoutingService.resolve(
                requestDTO.getSelectedProviderId(),
                requestDTO.getSelectedModel(),
                entryPoint
        );
        AIProvider provider = selection.provider();
        String selectedModel = selection.model();
        String promptSnapshot = resolvePromptSnapshot(requestDTO.getPromptSnapshot(), writingType);
        Project project = chapter.getProjectId() == null ? null : projectService.getById(chapter.getProjectId());
        GenerationReadinessVO readiness = generationReadinessService.evaluate(userId, chapter.getId());
        ChapterAnchorBundle anchorBundle = readiness.getResolvedAnchors();
        ReaderRevealConstraint readerRevealConstraint = buildReaderRevealConstraint(
                chapter,
                currentContent,
                writingType,
                anchorBundle
        );
        emitGenerationReadinessLogs(eventConsumer, readiness);
        emitAnchorSnapshotLog(eventConsumer, anchorBundle);
        enforceReadinessBeforeGeneration(readiness, chapter, writingType, currentContent);
        boolean exposeDirectorDebug = getConfiguredBoolean("ai.director.debug_expose_decision", false);
        if (exposeDirectorDebug) {
            emitStage(eventConsumer, "director", "started", "正在生成总导决策并选择本轮上下文模块");
        }
        AIDirectorDecisionVO directorDecision = aiDirectorApplicationService.decide(
                userId,
                buildDirectorDecisionRequest(
                        requestDTO,
                        writingType,
                        currentContent,
                        userInstruction,
                        readerRevealConstraint
                )
        );
        if (exposeDirectorDebug) {
            emitStage(eventConsumer, "director", "completed", resolveDirectorStageMessage(directorDecision));
            emitDirectorDecisionLogs(eventConsumer, directorDecision);
        }
        WritingContextBundle contextBundle = buildContextBundle(
                userId,
                project,
                chapter,
                anchorBundle,
                userInstruction,
                provider,
                selectedModel,
                eventConsumer
        );

        return new PreparedGenerationContext(
                userId,
                chapter,
                currentContent,
                writingType,
                userInstruction,
                provider,
                selectedModel,
                promptSnapshot,
                entryPoint,
                sceneId,
                buildSystemPrompt(promptSnapshot),
                buildUserPrompt(
                        project,
                        chapter,
                        contextBundle,
                        anchorBundle,
                        readiness,
                        readerRevealConstraint,
                        directorDecision,
                        sceneDraftConstraintBundle,
                        currentContent,
                        writingType,
                        userInstruction
                ),
                contextBundle,
                directorDecision,
                readiness,
                anchorBundle,
                readerRevealConstraint,
                sceneDraftConstraintBundle,
                resolveMaxTokens(requestDTO.getMaxTokens(), chapter, currentContent, writingType)
        );
    }

    private AIDirectorDecisionRequestDTO buildDirectorDecisionRequest(
            AIWritingRequestDTO requestDTO,
            String writingType,
            String currentContent,
            String userInstruction,
            ReaderRevealConstraint readerRevealConstraint) {
        AIDirectorDecisionRequestDTO directorRequest = new AIDirectorDecisionRequestDTO();
        directorRequest.setChapterId(requestDTO.getChapterId());
        directorRequest.setCurrentContent(currentContent);
        directorRequest.setUserInstruction(userInstruction);
        directorRequest.setWritingType(writingType);
        directorRequest.setEntryPoint(requestDTO.getEntryPoint());
        directorRequest.setSourceType("writing");
        directorRequest.setForceRefresh(false);
        if (readerRevealConstraint != null) {
            directorRequest.setOpeningMode(readerRevealConstraint.getOpeningMode());
            directorRequest.setReaderRevealGoals(readerRevealConstraint.getRevealTargets());
            directorRequest.setForbiddenReaderAssumptions(readerRevealConstraint.getForbiddenAssumptions());
        }
        return directorRequest;
    }

    private ReaderRevealConstraint buildReaderRevealConstraint(
            Chapter chapter,
            String currentContent,
            String writingType,
            ChapterAnchorBundle anchorBundle) {
        ReaderRevealConstraint constraint = new ReaderRevealConstraint();
        constraint.setChapterId(chapter.getId());
        constraint.setOpeningMode(StringUtils.hasText(currentContent) ? "chapter_continue" : "cold_open");

        if (StringUtils.hasText(currentContent)) {
            constraint.addReaderKnownFact("当前章节已经存在已写出的正文内容，只能默认承接已经写出来的事实。");
        }

        if (StringUtils.hasText(anchorBundle == null ? null : anchorBundle.getChapterSummary())) {
            constraint.addRevealTarget("本章首先要让读者理解：" + preview(anchorBundle.getChapterSummary(), 100));
        }
        if (StringUtils.hasText(anchorBundle == null ? null : anchorBundle.getMainPovCharacterName())) {
            constraint.addRevealTarget("先明确当前主要跟随的人物是 " + anchorBundle.getMainPovCharacterName().trim() + "。");
        }
        if (chapter.getOrderNum() != null && chapter.getOrderNum() == 1) {
            constraint.addRevealTarget("这是第一章，必须先完成世界、人物状态和触发事件的初始定向。");
        } else if (!StringUtils.hasText(currentContent)) {
            constraint.addRevealTarget("即使是续写项目的新章节，也要先重新交代当前场景和人物状态。");
        }
        if ("draft".equals(writingType) && !StringUtils.hasText(currentContent)) {
            constraint.addRevealTarget("前 20%-30% 需要交代当前场景、人物状态和本章触发点。");
        }

        constraint.addForbiddenAssumption("不要把项目设定、大纲、剧情或因果直接当成读者已经知道的前情。");
        constraint.addForbiddenAssumption("只有正文里已经揭晓过的事实，才能按读者已知来承接。");
        if (!StringUtils.hasText(currentContent)) {
            constraint.addForbiddenAssumption("当前章节正文为空，不要像从章节中段直接切入。");
        }
        return constraint;
    }

    private WritingContextBundle buildContextBundle(
            Long userId,
            Project project,
            Chapter chapter,
            ChapterAnchorBundle anchorBundle,
            String userInstruction,
            AIProvider provider,
            String selectedModel,
            Consumer<AIWritingStreamEventVO> eventConsumer) {
        List<String> requiredCharacters = anchorBundle != null
                && anchorBundle.getRequiredCharacterNames() != null
                && !anchorBundle.getRequiredCharacterNames().isEmpty()
                ? anchorBundle.getRequiredCharacterNames()
                : chapter.getRequiredCharacterNames() == null ? List.of() : chapter.getRequiredCharacterNames();
        List<CharacterInventorySummary> characterInventories = buildRequiredCharacterInventories(chapter, anchorBundle);
        AIWritingChatParticipationVO chatParticipation = buildChatParticipationContext(
                userId,
                chapter,
                provider,
                selectedModel,
                eventConsumer
        );
        if (project == null || chapter.getProjectId() == null) {
            return WritingContextBundle.empty(requiredCharacters, chatParticipation);
        }

        List<Outline> outlines = outlineService.getProjectOutlines(project.getId(), userId);
        Outline currentOutline = resolveCurrentOutline(chapter, outlines);

        Set<Long> relatedPlotIds = new LinkedHashSet<>();
        Set<Long> relatedCausalityIds = new LinkedHashSet<>();
        Set<Long> relatedWorldSettingIds = new LinkedHashSet<>();
        if (currentOutline != null) {
            relatedPlotIds.addAll(currentOutline.getRelatedPlotIdList() == null ? List.of() : currentOutline.getRelatedPlotIdList());
            relatedCausalityIds.addAll(currentOutline.getRelatedCausalityIdList() == null ? List.of() : currentOutline.getRelatedCausalityIdList());
            relatedWorldSettingIds.addAll(currentOutline.getRelatedWorldSettingIdList() == null ? List.of() : currentOutline.getRelatedWorldSettingIdList());
        }
        relatedPlotIds.addAll(chapter.getStoryBeatIds() == null ? List.of() : chapter.getStoryBeatIds());
        if (anchorBundle != null) {
            relatedPlotIds.addAll(anchorBundle.getStoryBeatIds() == null ? List.of() : anchorBundle.getStoryBeatIds());
            relatedWorldSettingIds.addAll(anchorBundle.getRelatedWorldSettingIds() == null ? List.of() : anchorBundle.getRelatedWorldSettingIds());
        }

        List<Plot> allPlots = plotService.getProjectPlots(project.getId());
        Map<Long, Plot> plotMap = allPlots.stream()
                .filter(item -> item != null && item.getId() != null)
                .collect(Collectors.toMap(Plot::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        LinkedHashMap<Long, Plot> relevantPlotMap = new LinkedHashMap<>();
        for (Long plotId : relatedPlotIds) {
            Plot plot = plotMap.get(plotId);
            if (plot != null) {
                relevantPlotMap.put(plotId, plot);
            }
        }
        for (Plot plot : allPlots) {
            if (plot == null || plot.getId() == null) {
                continue;
            }
            if (plot.getChapterId() != null && plot.getChapterId().equals(chapter.getId())) {
                relevantPlotMap.put(plot.getId(), plot);
            }
        }
        List<Plot> relevantPlots = relevantPlotMap.values().stream()
                .limit(MAX_CONTEXT_ITEMS)
                .collect(Collectors.toCollection(ArrayList::new));
        if (relevantPlots.isEmpty()) {
            relevantPlots = allPlots.stream().limit(MAX_CONTEXT_ITEMS).toList();
        }

        List<Causality> allCausalities = causalityService.getProjectCausalities(project.getId(), userId);
        List<Causality> relevantCausalities = allCausalities.stream()
                .filter(item -> relatedCausalityIds.contains(item.getId()))
                .limit(MAX_CONTEXT_ITEMS)
                .collect(Collectors.toCollection(ArrayList::new));
        if (relevantCausalities.isEmpty()) {
            relevantCausalities = allCausalities.stream().limit(MAX_CONTEXT_ITEMS).toList();
        }

        List<WorldSettingVO> allWorldSettings = worldSettingService.getWorldSettingsByProjectId(project.getId());
        Map<Long, WorldSettingVO> worldSettingMap = allWorldSettings.stream()
                .filter(item -> item != null && item.getId() != null)
                .collect(Collectors.toMap(WorldSettingVO::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        LinkedHashMap<Long, WorldSettingVO> relevantWorldSettingMap = new LinkedHashMap<>();
        for (Long worldSettingId : relatedWorldSettingIds) {
            WorldSettingVO worldSetting = worldSettingMap.get(worldSettingId);
            if (worldSetting != null) {
                relevantWorldSettingMap.put(worldSettingId, worldSetting);
            }
        }
        if (relevantWorldSettingMap.isEmpty()) {
            for (WorldSettingVO item : allWorldSettings) {
                if (item != null && item.getId() != null) {
                    relevantWorldSettingMap.put(item.getId(), item);
                }
                if (relevantWorldSettingMap.size() >= MAX_CONTEXT_ITEMS) {
                    break;
                }
            }
        }
        List<WorldSettingVO> worldSettings = new ArrayList<>(relevantWorldSettingMap.values());

        String retrievalQuery = String.join(" ",
                safe(chapter.getTitle(), ""),
                safe(chapter.getSummary(), ""),
                userInstruction,
                chapter.getStoryBeatTitles() == null ? "" : String.join(" ", chapter.getStoryBeatTitles()),
                anchorBundle == null || anchorBundle.getStoryBeatTitles() == null ? "" : String.join(" ", anchorBundle.getStoryBeatTitles()),
                currentOutline == null ? "" : safe(currentOutline.getSummary(), currentOutline.getTitle()),
                currentOutline == null ? "" : safe(currentOutline.getStageGoal(), ""));
        List<KnowledgeDocument> knowledgeDocuments = StringUtils.hasText(retrievalQuery)
                ? knowledgeDocumentService.queryDocuments(project.getId(), userId, retrievalQuery).stream().limit(3).toList()
                : List.of();
        return new WritingContextBundle(
                currentOutline,
                relevantPlots,
                relevantCausalities,
                worldSettings,
                knowledgeDocuments,
                characterInventories,
                requiredCharacters,
                chatParticipation
        );
    }

    private List<CharacterInventorySummary> buildRequiredCharacterInventories(Chapter chapter, ChapterAnchorBundle anchorBundle) {
        if (chapter == null
                || chapter.getProjectId() == null
                || anchorBundle == null
                || anchorBundle.getRequiredCharacterIds() == null
                || anchorBundle.getRequiredCharacterIds().isEmpty()) {
            return List.of();
        }

        List<Long> characterIds = anchorBundle.getRequiredCharacterIds().stream()
                .filter(Objects::nonNull)
                .toList();
        if (characterIds.isEmpty()) {
            return List.of();
        }

        List<CharacterInventoryItem> inventoryItems = characterInventoryItemMapper.selectList(
                new LambdaQueryWrapper<CharacterInventoryItem>()
                        .eq(CharacterInventoryItem::getProjectId, chapter.getProjectId())
                        .in(CharacterInventoryItem::getCharacterId, characterIds)
                        .eq(CharacterInventoryItem::getDeleted, 0)
                        .orderByDesc(CharacterInventoryItem::getEquipped)
                        .orderByAsc(CharacterInventoryItem::getSortOrder)
                        .orderByDesc(CharacterInventoryItem::getUpdateTime)
        );
        if (inventoryItems.isEmpty()) {
            return List.of();
        }

        List<Long> itemIds = inventoryItems.stream()
                .map(CharacterInventoryItem::getItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ItemDefinition> itemMap = itemIds.isEmpty()
                ? Collections.emptyMap()
                : itemMapper.selectBatchIds(itemIds).stream()
                        .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                        .collect(Collectors.toMap(ItemDefinition::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        Map<Long, String> characterNameMap = buildRequiredCharacterNameMap(anchorBundle);
        Map<Long, List<String>> itemSummariesByCharacter = new LinkedHashMap<>();
        for (CharacterInventoryItem inventoryItem : inventoryItems) {
            Long characterId = inventoryItem.getCharacterId();
            if (characterId == null) {
                continue;
            }
            List<String> itemSummaries = itemSummariesByCharacter.computeIfAbsent(characterId, key -> new ArrayList<>());
            if (itemSummaries.size() >= MAX_CONTEXT_ITEMS) {
                continue;
            }
            String summary = buildInventoryItemSummary(inventoryItem, itemMap.get(inventoryItem.getItemId()));
            if (StringUtils.hasText(summary)) {
                itemSummaries.add(summary);
            }
        }

        List<CharacterInventorySummary> result = new ArrayList<>();
        for (Long characterId : characterIds) {
            List<String> itemSummaries = itemSummariesByCharacter.getOrDefault(characterId, List.of());
            if (itemSummaries.isEmpty()) {
                continue;
            }
            result.add(new CharacterInventorySummary(
                    characterNameMap.getOrDefault(characterId, "角色#" + characterId),
                    itemSummaries
            ));
        }
        return result;
    }

    private Map<Long, String> buildRequiredCharacterNameMap(ChapterAnchorBundle anchorBundle) {
        Map<Long, String> nameMap = new LinkedHashMap<>();
        List<Long> requiredCharacterIds = anchorBundle == null || anchorBundle.getRequiredCharacterIds() == null
                ? List.of()
                : anchorBundle.getRequiredCharacterIds();
        List<String> requiredCharacterNames = anchorBundle == null || anchorBundle.getRequiredCharacterNames() == null
                ? List.of()
                : anchorBundle.getRequiredCharacterNames();

        for (int index = 0; index < requiredCharacterIds.size(); index++) {
            Long characterId = requiredCharacterIds.get(index);
            if (characterId == null) {
                continue;
            }
            String characterName = index < requiredCharacterNames.size() ? requiredCharacterNames.get(index) : null;
            if (StringUtils.hasText(characterName)) {
                nameMap.put(characterId, characterName.trim());
            }
        }
        return nameMap;
    }

    private String buildInventoryItemSummary(CharacterInventoryItem inventoryItem, ItemDefinition item) {
        if (inventoryItem == null) {
            return "";
        }

        String itemName = StringUtils.hasText(inventoryItem.getCustomName())
                ? inventoryItem.getCustomName().trim()
                : item != null && StringUtils.hasText(item.getName())
                        ? item.getName().trim()
                        : "物品#" + inventoryItem.getItemId();

        int quantity = inventoryItem.getQuantity() == null ? 1 : Math.max(1, inventoryItem.getQuantity());
        List<String> details = new ArrayList<>();
        if (Integer.valueOf(1).equals(inventoryItem.getEquipped())) {
            details.add("已装备");
        }
        if (inventoryItem.getDurability() != null && inventoryItem.getDurability() >= 0 && inventoryItem.getDurability() < 100) {
            details.add("耐久" + inventoryItem.getDurability());
        }

        String note = StringUtils.hasText(inventoryItem.getNotes())
                ? inventoryItem.getNotes()
                : item == null ? null : item.getDescription();
        if (StringUtils.hasText(note)) {
            details.add(limit(note, 36));
        }

        return details.isEmpty()
                ? itemName + " x" + quantity
                : itemName + " x" + quantity + "（" + String.join("；", details) + "）";
    }

    private AIWritingChatParticipationVO buildChatParticipationContext(
            Long userId,
            Chapter chapter,
            AIProvider provider,
            String selectedModel,
            Consumer<AIWritingStreamEventVO> eventConsumer) {
        if (!aiWritingChatService.hasBackgroundContext(userId, chapter.getId())) {
            return AIWritingChatParticipationVO.empty();
        }

        emitStage(eventConsumer, "context", "started", "正在整理背景讨论并合并到创作上下文");
        AIWritingChatParticipationVO participation = executeWithHeartbeat(
                eventConsumer,
                "context",
                "背景讨论仍在整理中，请稍候",
                () -> aiWritingChatService.buildParticipationContext(
                        userId,
                        chapter.getId(),
                        provider,
                        selectedModel
                )
        );
        if (participation.hasContent()) {
            emitLog(eventConsumer, "context", buildChatParticipationLog(participation));
            emitStage(eventConsumer, "context", "completed", "背景讨论已并入世界观、人物约束和创作偏好");
            return participation;
        }

        emitStage(eventConsumer, "context", "completed", "本轮未提炼出需要并入的背景讨论");
        return participation;
    }

    private String buildChatParticipationLog(AIWritingChatParticipationVO participation) {
        List<String> parts = new ArrayList<>();
        if (!participation.getWorldFacts().isEmpty()) {
            parts.add("世界观补充 " + participation.getWorldFacts().size() + " 条");
        }
        if (!participation.getCharacterConstraints().isEmpty()) {
            parts.add("人物约束 " + participation.getCharacterConstraints().size() + " 条");
        }
        if (!participation.getPlotGuidance().isEmpty()) {
            parts.add("剧情推进 " + participation.getPlotGuidance().size() + " 条");
        }
        if (!participation.getWritingPreferences().isEmpty()) {
            parts.add("写作偏好 " + participation.getWritingPreferences().size() + " 条");
        }
        if (!participation.getHardConstraints().isEmpty()) {
            parts.add("硬性约束 " + participation.getHardConstraints().size() + " 条");
        }
        return parts.isEmpty()
                ? "背景讨论已检查，但没有提炼出稳定的写作约束"
                : "已整理背景讨论：" + String.join("，", parts);
    }

    private void emitGenerationReadinessLogs(Consumer<AIWritingStreamEventVO> eventConsumer, GenerationReadinessVO readiness) {
        if (eventConsumer == null || readiness == null) {
            return;
        }

        emitLog(eventConsumer, "prepare", "生成就绪度：" + readiness.getStatus() + " / " + readiness.getScore());
        if (readiness.getBlockingIssues() != null && !readiness.getBlockingIssues().isEmpty()) {
            emitLog(eventConsumer, "prepare", "阻塞项：" + limit(String.join("；", readiness.getBlockingIssues()), 260));
        }
        if (readiness.getWarnings() != null && !readiness.getWarnings().isEmpty()) {
            emitLog(eventConsumer, "prepare", "预警：" + limit(String.join("；", readiness.getWarnings()), 260));
        }
    }

    private void emitAnchorSnapshotLog(Consumer<AIWritingStreamEventVO> eventConsumer, ChapterAnchorBundle anchorBundle) {
        if (eventConsumer == null || anchorBundle == null) {
            return;
        }
        String summary = buildAnchorSummary(anchorBundle);
        if (StringUtils.hasText(summary)) {
            emitLog(eventConsumer, "prepare", "锚点快照：" + limit(summary, 260));
        }
    }

    private String resolveDirectorStageMessage(AIDirectorDecisionVO directorDecision) {
        if (directorDecision == null) {
            return "总导决策未返回可用结果";
        }
        if ("fallback".equalsIgnoreCase(directorDecision.getStatus())) {
            return "总导已回退到启发式决策，本轮仍继续走生成流水线";
        }
        return "总导决策已生成并写入本轮创作上下文";
    }

    private void emitDirectorDecisionLogs(Consumer<AIWritingStreamEventVO> eventConsumer, AIDirectorDecisionVO directorDecision) {
        if (eventConsumer == null || directorDecision == null) {
            return;
        }

        emitLog(eventConsumer, "director", buildDirectorOverviewLog(directorDecision));
        if (StringUtils.hasText(directorDecision.getDecisionSummary())) {
            emitLog(eventConsumer, "director", "决策摘要：" + limit(directorDecision.getDecisionSummary(), 220));
        }

        String moduleLog = buildDirectorModuleLog(directorDecision);
        if (StringUtils.hasText(moduleLog)) {
            emitLog(eventConsumer, "director", moduleLog);
        }

        emitDirectorConstraintLog(eventConsumer, "硬约束", readDecisionPackStrings(directorDecision, "requiredFacts"));
        emitDirectorConstraintLog(eventConsumer, "禁止事项", readDecisionPackStrings(directorDecision, "prohibitedMoves"));
        emitDirectorConstraintLog(eventConsumer, "写作提示", readDecisionPackStrings(directorDecision, "writerHints"));

        String toolTraceLog = buildDirectorToolTraceLog(directorDecision);
        if (StringUtils.hasText(toolTraceLog)) {
            emitLog(eventConsumer, "director", toolTraceLog);
        }

        if (StringUtils.hasText(directorDecision.getErrorMessage())) {
            emitLog(eventConsumer, "director", "附加信息：" + limit(directorDecision.getErrorMessage(), 220));
        }
    }

    private String buildDirectorOverviewLog(AIDirectorDecisionVO directorDecision) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(directorDecision.getStage())) {
            parts.add("stage=" + directorDecision.getStage().trim());
        }
        if (StringUtils.hasText(directorDecision.getWritingMode())) {
            parts.add("mode=" + directorDecision.getWritingMode().trim());
        }
        if (directorDecision.getTargetWordCount() != null && directorDecision.getTargetWordCount() > 0) {
            parts.add("target=" + directorDecision.getTargetWordCount());
        }
        if (StringUtils.hasText(directorDecision.getStatus())) {
            parts.add("status=" + directorDecision.getStatus().trim());
        }
        if (StringUtils.hasText(directorDecision.getSelectedModel())) {
            parts.add("model=" + directorDecision.getSelectedModel().trim());
        }
        return parts.isEmpty()
                ? "总导已完成本轮决策。"
                : "总导概览：" + String.join(" | ", parts);
    }

    private String buildDirectorModuleLog(AIDirectorDecisionVO directorDecision) {
        if (directorDecision.getDecisionPack() == null || directorDecision.getDecisionPack().isNull()) {
            return "";
        }
        JsonNode selectedModules = directorDecision.getDecisionPack().path("selectedModules");
        if (!selectedModules.isArray() || selectedModules.isEmpty()) {
            return "";
        }

        List<String> modules = new ArrayList<>();
        for (JsonNode module : selectedModules) {
            String moduleName = module.path("module").asText("").trim();
            if (!StringUtils.hasText(moduleName)) {
                continue;
            }

            List<String> details = new ArrayList<>();
            if (module.path("required").asBoolean(false)) {
                details.add("required");
            }
            if (module.hasNonNull("weight")) {
                details.add("w=" + module.path("weight").asDouble());
            }
            if (module.hasNonNull("topK")) {
                details.add("topK=" + module.path("topK").asInt());
            }
            modules.add(details.isEmpty() ? moduleName : moduleName + "(" + String.join(",", details) + ")");
        }

        return modules.isEmpty() ? "" : "已选模块：" + limit(String.join("、", modules), 260);
    }

    private void emitDirectorConstraintLog(
            Consumer<AIWritingStreamEventVO> eventConsumer,
            String label,
            List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        emitLog(eventConsumer, "director", label + "：" + limit(String.join("；", items), 240));
    }

    private String buildDirectorToolTraceLog(AIDirectorDecisionVO directorDecision) {
        if (directorDecision.getToolTrace() == null || directorDecision.getToolTrace().isNull()) {
            return "";
        }
        JsonNode toolTrace = directorDecision.getToolTrace();
        if (!toolTrace.isArray() || toolTrace.isEmpty()) {
            return "";
        }

        Set<String> toolNames = new LinkedHashSet<>();
        for (JsonNode trace : toolTrace) {
            String toolName = trace.path("name").asText("").trim();
            if (StringUtils.hasText(toolName)) {
                toolNames.add(toolName);
            }
        }

        return toolNames.isEmpty() ? "" : "工具调用：" + String.join("、", toolNames);
    }

    private AIWritingResponseVO persistGeneratedRecord(PreparedGenerationContext context, String generatedContent) {
        String normalizedContent = generatedContent == null ? "" : generatedContent.trim();
        if (!StringUtils.hasText(normalizedContent)) {
            throw new IllegalStateException("模型没有返回可用的正文内容，请稍后重试");
        }
        List<StructuredCreationSuggestion> creationSuggestions = structuredCreationSuggestionService.suggestFromText(
                context.userId(),
                context.chapter().getProjectId(),
                context.chapter().getId(),
                normalizedContent
        );

        AIWritingRecord record = new AIWritingRecord();
        record.setChapterId(context.chapter().getId());
        record.setOriginalContent(context.currentContent());
        record.setGeneratedContent(normalizedContent);
        record.setWritingType(context.writingType());
        record.setUserInstruction(context.userInstruction());
        record.setSelectedProviderId(context.provider().getId());
        record.setSelectedModel(context.selectedModel());
        record.setPromptSnapshot(context.promptSnapshot());
        record.setDirectorDecisionId(context.directorDecision() == null ? null : context.directorDecision().getId());
        record.setGenerationTraceJson(buildGenerationTraceJson(context, creationSuggestions));
        record.setStatus("draft");

        save(record);
        return convertToVO(record);
    }

    private AIProvider resolveProvider(Long selectedProviderId) {
        if (selectedProviderId != null) {
            AIProvider provider = aiProviderService.getById(selectedProviderId);
            if (provider != null
                    && !Integer.valueOf(1).equals(provider.getDeleted())
                    && Integer.valueOf(1).equals(provider.getEnabled())) {
                return provider;
            }
        }

        Long configuredProviderId = parseLong(systemConfigService.getConfigValue("default_ai_provider_id"));
        if (configuredProviderId != null) {
            AIProvider provider = aiProviderService.getById(configuredProviderId);
            if (provider != null
                    && !Integer.valueOf(1).equals(provider.getDeleted())
                    && Integer.valueOf(1).equals(provider.getEnabled())) {
                return provider;
            }
        }

        return aiProviderService.listProviders().stream()
                .filter(item -> Integer.valueOf(1).equals(item.getEnabled()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("当前没有可用的默认模型服务，请先在模型服务页启用一个 Provider"));
    }

    private String resolveModelName(AIProvider provider, String selectedModel) {
        if (StringUtils.hasText(selectedModel)) {
            return selectedModel.trim();
        }

        if (provider != null && StringUtils.hasText(provider.getModelName())) {
            return provider.getModelName().trim();
        }

        String configuredModel = systemConfigService.getConfigValue("default_ai_model");
        if (StringUtils.hasText(configuredModel)) {
            return configuredModel.trim();
        }

        throw new IllegalStateException("当前默认模型尚未配置，请先在系统设置里指定默认模型");
    }

    private String resolvePromptSnapshot(String requestPromptSnapshot, String writingType) {
        if (StringUtils.hasText(requestPromptSnapshot)) {
            return requestPromptSnapshot.trim();
        }

        String configuredPrompt = systemConfigService.getConfigValue(resolvePromptKey(writingType));
        if (StringUtils.hasText(configuredPrompt)) {
            return configuredPrompt.trim();
        }

        return getDefaultPromptTemplate(writingType);
    }

    private Integer resolveMaxTokens(
            Integer requestedMaxTokens,
            Chapter chapter,
            String currentContent,
            String writingType) {
        Integer normalizedRequested = requestedMaxTokens != null && requestedMaxTokens > 0 ? requestedMaxTokens : null;
        boolean emptyChapterStart = !StringUtils.hasText(currentContent);
        boolean firstChapter = chapter != null && chapter.getOrderNum() != null && chapter.getOrderNum() == 1;

        int resolved = normalizedRequested != null
                ? normalizedRequested
                : defaultMaxTokensForContext(emptyChapterStart, firstChapter, writingType);

        if (emptyChapterStart) {
            resolved = Math.max(resolved, firstChapter ? 1600 : 1200);
        } else if ("rewrite".equals(writingType) || "expand".equals(writingType)) {
            resolved = Math.max(resolved, 1000);
        } else if ("continue".equals(writingType)) {
            resolved = Math.max(resolved, 800);
        }

        return Math.min(resolved, 2600);
    }

    private int defaultMaxTokensForContext(boolean emptyChapterStart, boolean firstChapter, String writingType) {
        if (emptyChapterStart) {
            return firstChapter ? 1600 : 1200;
        }
        return switch (writingType) {
            case "rewrite", "expand" -> 1100;
            case "continue" -> 900;
            case "polish" -> 700;
            case "draft" -> 1000;
            default -> 900;
        };
    }

    private String buildSystemPrompt(String promptSnapshot) {
        return """
                你是一名中文长篇小说写作助手。
                请只输出可直接使用的小说正文，不要解释，不要标题，不要 Markdown 标记，不要列提纲。
                %s
                """.formatted(promptSnapshot);
    }

    private String buildUserPrompt(
            Project project,
            Chapter chapter,
            WritingContextBundle contextBundle,
            ChapterAnchorBundle anchorBundle,
            GenerationReadinessVO readiness,
            ReaderRevealConstraint readerRevealConstraint,
            AIDirectorDecisionVO directorDecision,
            SceneDraftConstraintBundle sceneDraftConstraintBundle,
            String currentContent,
            String writingType,
            String userInstruction) {
        StringBuilder builder = new StringBuilder();
        builder.append("请基于以下上下文处理当前章节正文。\n");

        if (project != null) {
            builder.append("【项目信息】\n");
            builder.append("项目名称：").append(safe(project.getName(), "未命名项目")).append('\n');
            if (StringUtils.hasText(project.getGenre())) {
                builder.append("题材类型：").append(project.getGenre().trim()).append('\n');
            }
            if (StringUtils.hasText(project.getDescription())) {
                builder.append("项目简介：").append(project.getDescription().trim()).append('\n');
            }
            if (StringUtils.hasText(project.getTags())) {
                builder.append("项目标签：").append(project.getTags().trim()).append('\n');
            }
            builder.append('\n');
        }

        builder.append("【当前章节】\n");
        builder.append("章节标题：").append(safe(chapter.getTitle(), "未命名章节")).append('\n');
        if (chapter.getOrderNum() != null) {
            builder.append("章节顺序：第 ").append(chapter.getOrderNum()).append(" 章\n");
        }
        if (StringUtils.hasText(chapter.getSummary())) {
            builder.append("章节摘要：").append(limit(chapter.getSummary(), 180)).append('\n');
        }
        if (StringUtils.hasText(chapter.getChapterStatus())) {
            builder.append("章节状态：").append(chapter.getChapterStatus().trim()).append('\n');
        }
        if (StringUtils.hasText(chapter.getOutlineTitle())) {
            builder.append("绑定大纲：").append(chapter.getOutlineTitle().trim()).append('\n');
        }
        String mainPovName = StringUtils.hasText(chapter.getMainPovCharacterName())
                ? chapter.getMainPovCharacterName().trim()
                : anchorBundle == null ? "" : safe(anchorBundle.getMainPovCharacterName(), "");
        if (StringUtils.hasText(mainPovName)) {
            builder.append("主 POV 人物：").append(mainPovName).append('\n');
        }
        List<String> storyBeatTitles = chapter.getStoryBeatTitles() != null && !chapter.getStoryBeatTitles().isEmpty()
                ? chapter.getStoryBeatTitles()
                : anchorBundle == null ? List.of() : anchorBundle.getStoryBeatTitles();
        if (storyBeatTitles != null && !storyBeatTitles.isEmpty()) {
            builder.append("剧情节拍：").append(String.join("、", storyBeatTitles)).append('\n');
        }
        if (!contextBundle.requiredCharacters().isEmpty()) {
            builder.append("本章必须出现人物：").append(String.join("、", contextBundle.requiredCharacters())).append('\n');
        }
        appendInlineConstraint(builder, "背景补充人物约束", contextBundle.chatParticipation().getCharacterConstraints());
        appendInlineConstraint(builder, "背景补充硬性约束", contextBundle.chatParticipation().getHardConstraints());
        appendAnchorSnapshotSection(builder, anchorBundle, readiness);
        appendReaderRevealSection(builder, readerRevealConstraint);
        appendSceneDraftConstraintSection(builder, sceneDraftConstraintBundle);
        builder.append(resolveWritingIntent(writingType));
        appendDirectorDecisionSection(builder, directorDecision);

        if (shouldUseModule(directorDecision, "character_inventory")) {
            appendCharacterInventorySection(builder, contextBundle.characterInventories());
        }
        if (shouldUseModule(directorDecision, "outline")) {
            appendOutlineSection(builder, contextBundle.currentOutline());
        }
        if (shouldUseModule(directorDecision, "chat_background")) {
            appendBackgroundPlotGuidanceSection(builder, contextBundle.chatParticipation());
        }
        if (shouldUseModule(directorDecision, "plot")) {
            appendPlotSection(builder, contextBundle.plots());
        }
        if (shouldUseModule(directorDecision, "causality")) {
            appendCausalitySection(builder, contextBundle.causalities());
        }
        if (shouldUseModule(directorDecision, "world_setting")) {
            appendWorldSettingSection(builder, contextBundle.worldSettings());
        }
        if (shouldUseModule(directorDecision, "chat_background")) {
            appendBackgroundWorldSettingSection(builder, contextBundle.chatParticipation());
            appendWritingPreferenceSection(builder, contextBundle.chatParticipation());
        }
        if (shouldUseModule(directorDecision, "knowledge")) {
            appendKnowledgeSection(builder, contextBundle.knowledgeDocuments());
        }

        builder.append("\n【当前正文】\n");
        if (StringUtils.hasText(currentContent)) {
            builder.append(currentContent.trim()).append('\n');
        } else {
            builder.append("（当前章节还没有正文，请先拟生成一版可继续扩写的草稿）\n");
        }

        if (StringUtils.hasText(userInstruction)) {
            builder.append("\n【补充要求】\n").append(userInstruction.trim()).append('\n');
        }

        return builder.toString();
    }

    private SceneDraftConstraintBundle buildSceneDraftConstraintBundle(
            Long projectId,
            Long chapterId,
            String sceneId,
            ChapterSceneWorkflowGuardService.SceneWorkflowState workflowState) {
        SceneSkeletonItem currentScene = workflowState.findScene(sceneId)
                .orElseThrow(() -> new SceneWorkflowConflictException("当前镜头 " + sceneId + " 不存在于章节骨架中。"));
        SceneSkeletonItem nextScene = workflowState.nextScene(sceneId).orElse(null);
        StorySessionContextPacket contextPacket = storySessionContextAssembler.assemble(projectId, chapterId, sceneId).orElse(null);
        SceneContinuityState continuityState = contextPacket == null
                ? new SceneContinuityState(
                "",
                "",
                "",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false,
                nextScene == null ? "" : nextScene.sceneId(),
                nextScene == null ? "" : nextScene.goal(),
                currentScene.stopCondition()
        )
                : SceneContinuitySupport.resolveContinuityState(
                contextPacket.previousSceneHandoff(),
                contextPacket.sceneBindingContext().resolvedSceneState(),
                contextPacket.existingSceneStates(),
                nextScene == null ? "" : nextScene.sceneId(),
                nextScene == null ? "" : nextScene.goal(),
                currentScene.stopCondition()
        );
        return new SceneDraftConstraintBundle(
                currentScene.sceneId(),
                currentScene.goal(),
                currentScene.readerReveal(),
                currentScene.mustUseAnchors(),
                currentScene.stopCondition(),
                nextScene == null ? "" : nextScene.sceneId(),
                nextScene == null ? "" : nextScene.goal(),
                continuityState
        );
    }

    private void appendSceneDraftConstraintSection(
            StringBuilder builder,
            SceneDraftConstraintBundle sceneDraftConstraintBundle) {
        if (builder == null || sceneDraftConstraintBundle == null) {
            return;
        }
        builder.append("\n【镜头顺序硬约束】\n");
        builder.append("当前镜头：").append(sceneDraftConstraintBundle.sceneId()).append('\n');
        if (StringUtils.hasText(sceneDraftConstraintBundle.goal())) {
            builder.append("本镜头目标：").append(sceneDraftConstraintBundle.goal()).append('\n');
        }
        appendInlineConstraint(builder, "本镜头应揭晓", sceneDraftConstraintBundle.readerReveal());
        appendInlineConstraint(builder, "本镜头必须使用锚点", sceneDraftConstraintBundle.mustUseAnchors());
        if (StringUtils.hasText(sceneDraftConstraintBundle.stopCondition())) {
            builder.append("本镜头停点：").append(sceneDraftConstraintBundle.stopCondition()).append('\n');
        }
        if (sceneDraftConstraintBundle.continuityState() != null && !sceneDraftConstraintBundle.continuityState().isEmpty()) {
            if (StringUtils.hasText(sceneDraftConstraintBundle.continuityState().summary())) {
                builder.append("上一镜头真实摘要：").append(sceneDraftConstraintBundle.continuityState().summary()).append('\n');
            }
            if (StringUtils.hasText(sceneDraftConstraintBundle.continuityState().handoffLine())) {
                builder.append("上一镜头真实交接：").append(sceneDraftConstraintBundle.continuityState().handoffLine()).append('\n');
            }
            appendInlineConstraint(builder, "必须继承事实", sceneDraftConstraintBundle.continuityState().carryForwardFacts());
            appendInlineConstraint(builder, "时间锚点", sceneDraftConstraintBundle.continuityState().timeAnchors());
            appendInlineConstraint(
                    builder,
                    "沿用人物称呼",
                    sceneDraftConstraintBundle.continuityState().counterpartNames().isEmpty()
                            ? sceneDraftConstraintBundle.continuityState().expectedNames()
                            : sceneDraftConstraintBundle.continuityState().counterpartNames()
            );
        }
        if (StringUtils.hasText(sceneDraftConstraintBundle.nextSceneId())
                && StringUtils.hasText(sceneDraftConstraintBundle.nextSceneGoal())) {
            builder.append("下一镜头入口预留：")
                    .append(sceneDraftConstraintBundle.nextSceneId())
                    .append(" 将转入 ")
                    .append(sceneDraftConstraintBundle.nextSceneGoal())
                    .append("。\n");
        }
        builder.append("顺序规则：只能完成当前镜头，禁止回写已接纳前缀，禁止提前展开下一镜头正文。\n");
    }

    private void appendAnchorSnapshotSection(
            StringBuilder builder,
            ChapterAnchorBundle anchorBundle,
            GenerationReadinessVO readiness) {
        if (anchorBundle == null) {
            return;
        }

        builder.append("\n【章节锚点快照】\n");
        if (anchorBundle.getChapterOutlineId() != null) {
            builder.append("章纲 ID：").append(anchorBundle.getChapterOutlineId()).append('\n');
        }
        if (anchorBundle.getVolumeOutlineId() != null) {
            builder.append("卷纲 ID：").append(anchorBundle.getVolumeOutlineId()).append('\n');
        }
        if (StringUtils.hasText(anchorBundle.getChapterSummary())) {
            builder.append("章节 brief：").append(limit(anchorBundle.getChapterSummary(), 180)).append('\n');
        }
        if (StringUtils.hasText(anchorBundle.getMainPovCharacterName())) {
            builder.append("稳定 POV：").append(anchorBundle.getMainPovCharacterName().trim()).append('\n');
        }
        if (anchorBundle.getRequiredCharacterNames() != null && !anchorBundle.getRequiredCharacterNames().isEmpty()) {
            builder.append("稳定必出人物：").append(String.join("、", anchorBundle.getRequiredCharacterNames())).append('\n');
        }
        if (anchorBundle.getStoryBeatTitles() != null && !anchorBundle.getStoryBeatTitles().isEmpty()) {
            builder.append("稳定剧情锚点：").append(String.join("、", anchorBundle.getStoryBeatTitles())).append('\n');
        }
        if (readiness != null) {
            builder.append("生成就绪度：").append(readiness.getStatus()).append(" / ").append(readiness.getScore()).append('\n');
            appendInlineConstraint(builder, "生成前阻塞项", readiness.getBlockingIssues());
            appendInlineConstraint(builder, "生成前警告", readiness.getWarnings());
        }
    }

    private void appendReaderRevealSection(StringBuilder builder, ReaderRevealConstraint readerRevealConstraint) {
        if (readerRevealConstraint == null) {
            return;
        }

        builder.append("\n【读者揭晓边界】\n");
        if (StringUtils.hasText(readerRevealConstraint.getOpeningMode())) {
            builder.append("开场模式：").append(readerRevealConstraint.getOpeningMode()).append('\n');
        }
        appendInlineConstraint(builder, "已揭晓事实", readerRevealConstraint.getReaderKnownFacts());
        appendInlineConstraint(builder, "本轮应揭晓", readerRevealConstraint.getRevealTargets());
        appendInlineConstraint(builder, "禁止默认前情", readerRevealConstraint.getForbiddenAssumptions());
    }

    private void appendDirectorDecisionSection(StringBuilder builder, AIDirectorDecisionVO directorDecision) {
        if (directorDecision == null || directorDecision.getDecisionPack() == null || directorDecision.getDecisionPack().isNull()) {
            return;
        }
        builder.append("\n【总导决策】\n");
        if (StringUtils.hasText(directorDecision.getStage())) {
            builder.append("章节阶段：").append(directorDecision.getStage()).append('\n');
        }
        if (StringUtils.hasText(directorDecision.getWritingMode())) {
            builder.append("本轮模式：").append(directorDecision.getWritingMode()).append('\n');
        }
        if (StringUtils.hasText(directorDecision.getDecisionSummary())) {
            builder.append("决策摘要：").append(directorDecision.getDecisionSummary().trim()).append('\n');
        }
        appendInlineConstraint(builder, "总导硬约束", readDecisionPackStrings(directorDecision, "requiredFacts"));
        appendInlineConstraint(builder, "总导禁止事项", readDecisionPackStrings(directorDecision, "prohibitedMoves"));
        appendInlineConstraint(builder, "总导写作提示", readDecisionPackStrings(directorDecision, "writerHints"));
    }

    private boolean shouldUseModule(AIDirectorDecisionVO directorDecision, String moduleName) {
        if (directorDecision == null || directorDecision.getDecisionPack() == null || directorDecision.getDecisionPack().isNull()) {
            return true;
        }
        JsonNode selectedModules = directorDecision.getDecisionPack().path("selectedModules");
        if (!selectedModules.isArray() || selectedModules.isEmpty()) {
            return true;
        }
        for (JsonNode module : selectedModules) {
            if (moduleName.equals(module.path("module").asText())) {
                return true;
            }
        }
        return false;
    }

    private List<String> readDecisionPackStrings(AIDirectorDecisionVO directorDecision, String fieldName) {
        if (directorDecision == null || directorDecision.getDecisionPack() == null || directorDecision.getDecisionPack().isNull()) {
            return List.of();
        }
        JsonNode arrayNode = directorDecision.getDecisionPack().path(fieldName);
        if (!arrayNode.isArray() || arrayNode.isEmpty()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            String value = item.asText("").trim();
            if (StringUtils.hasText(value)) {
                values.add(value);
            }
        }
        return values;
    }

    private void appendOutlineSection(StringBuilder builder, Outline outline) {
        if (outline == null) {
            return;
        }
        builder.append("\n【章节大纲】\n");
        if (StringUtils.hasText(outline.getOutlineType())) {
            builder.append("大纲类型：").append(outline.getOutlineType().trim()).append('\n');
        }
        if (StringUtils.hasText(outline.getTitle())) {
            builder.append("大纲标题：").append(outline.getTitle().trim()).append('\n');
        }
        if (StringUtils.hasText(outline.getSummary())) {
            builder.append("大纲摘要：").append(limit(outline.getSummary(), 220)).append('\n');
        }
        if (StringUtils.hasText(outline.getStageGoal())) {
            builder.append("本章目标：").append(limit(outline.getStageGoal(), 160)).append('\n');
        }
        if (StringUtils.hasText(outline.getKeyConflict())) {
            builder.append("核心冲突：").append(limit(outline.getKeyConflict(), 160)).append('\n');
        }
        if (StringUtils.hasText(outline.getTurningPoints())) {
            builder.append("关键转折：").append(limit(outline.getTurningPoints(), 180)).append('\n');
        }
        if (StringUtils.hasText(outline.getExpectedEnding())) {
            builder.append("收束方向：").append(limit(outline.getExpectedEnding(), 160)).append('\n');
        }
        if (outline.getFocusCharacterNames() != null && !outline.getFocusCharacterNames().isEmpty()) {
            builder.append("聚焦人物：").append(String.join("、", outline.getFocusCharacterNames())).append('\n');
        }
        if (outline.getRelatedWorldSettingNames() != null && !outline.getRelatedWorldSettingNames().isEmpty()) {
            builder.append("关联世界观：").append(String.join("、", outline.getRelatedWorldSettingNames())).append('\n');
        }
    }

    private void appendCharacterInventorySection(StringBuilder builder, List<CharacterInventorySummary> characterInventories) {
        if (characterInventories == null || characterInventories.isEmpty()) {
            return;
        }
        builder.append("\n【章节人物背包】\n");
        for (int index = 0; index < characterInventories.size(); index++) {
            CharacterInventorySummary inventory = characterInventories.get(index);
            builder.append(index + 1)
                    .append(". ")
                    .append(inventory.characterName())
                    .append("：")
                    .append(String.join("；", inventory.itemSummaries()))
                    .append('\n');
        }
    }

    private void appendBackgroundPlotGuidanceSection(StringBuilder builder, AIWritingChatParticipationVO chatParticipation) {
        if (chatParticipation == null || chatParticipation.getPlotGuidance().isEmpty()) {
            return;
        }
        builder.append("\n【背景补充推进】\n");
        for (int index = 0; index < chatParticipation.getPlotGuidance().size(); index++) {
            builder.append(index + 1)
                    .append(". ")
                    .append(chatParticipation.getPlotGuidance().get(index))
                    .append('\n');
        }
    }

    private void appendPlotSection(StringBuilder builder, List<Plot> plots) {
        if (plots == null || plots.isEmpty()) {
            return;
        }
        builder.append("\n【相关剧情节点】\n");
        for (int index = 0; index < Math.min(plots.size(), MAX_CONTEXT_ITEMS); index++) {
            Plot plot = plots.get(index);
            builder.append(index + 1).append(". ");
            builder.append(safe(plot.getTitle(), "未命名剧情"));
            if (StringUtils.hasText(plot.getDescription())) {
                builder.append("｜").append(limit(plot.getDescription(), 120));
            }
            if (StringUtils.hasText(plot.getConflicts())) {
                builder.append("｜冲突：").append(limit(plot.getConflicts(), 80));
            }
            if (StringUtils.hasText(plot.getStoryBeatType())) {
                builder.append("｜节拍：").append(plot.getStoryBeatType().trim());
            }
            if (StringUtils.hasText(plot.getStoryFunction())) {
                builder.append("｜功能：").append(plot.getStoryFunction().trim());
            }
            if (StringUtils.hasText(plot.getEventResult()) || StringUtils.hasText(plot.getResolutions())) {
                builder.append("｜事件结果：").append(limit(
                        StringUtils.hasText(plot.getEventResult()) ? plot.getEventResult() : plot.getResolutions(),
                        80
                ));
            }
            builder.append('\n');
        }
    }

    private void appendCausalitySection(StringBuilder builder, List<Causality> causalities) {
        if (causalities == null || causalities.isEmpty()) {
            return;
        }
        builder.append("\n【相关因果链】\n");
        for (int index = 0; index < Math.min(causalities.size(), MAX_CONTEXT_ITEMS); index++) {
            Causality item = causalities.get(index);
            builder.append(index + 1).append(". ");
            builder.append(safe(item.getName(), safe(item.getRelationship(), "未命名因果")));
            if (StringUtils.hasText(item.getDescription())) {
                builder.append("｜").append(limit(item.getDescription(), 120));
            }
            if (StringUtils.hasText(item.getConditions())) {
                builder.append("｜触发条件：").append(limit(item.getConditions(), 80));
            }
            if (StringUtils.hasText(item.getCausalType())) {
                builder.append("｜因果类型：").append(item.getCausalType().trim());
            }
            if (StringUtils.hasText(item.getTriggerMode())) {
                builder.append("｜触发模式：").append(item.getTriggerMode().trim());
            }
            if (StringUtils.hasText(item.getPayoffStatus())) {
                builder.append("｜回收状态：").append(item.getPayoffStatus().trim());
            }
            builder.append('\n');
        }
    }

    private Outline resolveCurrentOutline(Chapter chapter, List<Outline> outlines) {
        if (chapter == null || outlines == null || outlines.isEmpty()) {
            return null;
        }
        if (chapter.getOutlineId() != null) {
            Outline explicitOutline = outlines.stream()
                    .filter(item -> item != null && chapter.getOutlineId().equals(item.getId()))
                    .findFirst()
                    .orElse(null);
            if (explicitOutline != null) {
                return explicitOutline;
            }
        }
        return outlines.stream()
                .filter(item -> item != null && item.getChapterId() != null && item.getChapterId().equals(chapter.getId()))
                .findFirst()
                .orElse(null);
    }

    private void appendWorldSettingSection(StringBuilder builder, List<WorldSettingVO> worldSettings) {
        if (worldSettings == null || worldSettings.isEmpty()) {
            return;
        }
        builder.append("\n【世界观上下文】\n");
        for (int index = 0; index < Math.min(worldSettings.size(), MAX_CONTEXT_ITEMS); index++) {
            WorldSettingVO item = worldSettings.get(index);
            builder.append(index + 1).append(". ");
            builder.append(safe(item.getName(), item.getTitle() == null ? "未命名设定" : item.getTitle()));
            if (StringUtils.hasText(item.getCategory())) {
                builder.append("（").append(item.getCategory().trim()).append("）");
            }
            String description = StringUtils.hasText(item.getDescription()) ? item.getDescription() : item.getContent();
            if (StringUtils.hasText(description)) {
                builder.append("｜").append(limit(description, 120));
            }
            builder.append('\n');
        }
    }

    private void appendKnowledgeSection(StringBuilder builder, List<KnowledgeDocument> knowledgeDocuments) {
        if (knowledgeDocuments == null || knowledgeDocuments.isEmpty()) {
            return;
        }
        builder.append("\n【知识片段】\n");
        for (int index = 0; index < knowledgeDocuments.size(); index++) {
            KnowledgeDocument document = knowledgeDocuments.get(index);
            builder.append(index + 1).append(". ");
            builder.append(safe(document.getTitle(), "未命名知识"));
            String summary = StringUtils.hasText(document.getSummary()) ? document.getSummary() : document.getContentText();
            if (StringUtils.hasText(summary)) {
                builder.append("｜").append(limit(summary, 120));
            }
            builder.append('\n');
        }
    }

    private void appendBackgroundWorldSettingSection(StringBuilder builder, AIWritingChatParticipationVO chatParticipation) {
        if (chatParticipation == null || chatParticipation.getWorldFacts().isEmpty()) {
            return;
        }
        builder.append("\n【背景补充设定】\n");
        for (int index = 0; index < chatParticipation.getWorldFacts().size(); index++) {
            builder.append(index + 1)
                    .append(". ")
                    .append(chatParticipation.getWorldFacts().get(index))
                    .append('\n');
        }
    }

    private void appendWritingPreferenceSection(StringBuilder builder, AIWritingChatParticipationVO chatParticipation) {
        if (chatParticipation == null || chatParticipation.getWritingPreferences().isEmpty()) {
            return;
        }
        builder.append("\n【创作偏好】\n");
        for (int index = 0; index < chatParticipation.getWritingPreferences().size(); index++) {
            builder.append(index + 1)
                    .append(". ")
                    .append(chatParticipation.getWritingPreferences().get(index))
                    .append('\n');
        }
    }

    private void appendInlineConstraint(StringBuilder builder, String label, List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        builder.append(label)
                .append("：")
                .append(String.join("；", items))
                .append('\n');
    }

    private void appendEndingGuardrails(StringBuilder builder, PreparedGenerationContext context) {
        if (builder == null || context == null) {
            return;
        }
        builder.append("\n【本轮收束要求】\n");
        if (isEmptyChapterStart(context)) {
            builder.append("1. 当前是空章起稿，优先完成一个完整可停住的开场片段，不要把范围铺得过宽。\n");
            if (isFirstChapter(context.chapter())) {
                builder.append("2. 第一章先完成现实场景、人物状态和触发事件的读者定向，再自然停住。\n");
            } else {
                builder.append("2. 先完成当前场景、人物状态和本章触发点，再自然停住。\n");
            }
            builder.append("3. 如果接近篇幅上限，优先收束当前动作或心理段落，不要在结尾新开场景。\n");
            builder.append("4. 宁可早一点停在完整句号上，也不要把句子停在半截。\n");
            return;
        }
        builder.append("1. 本轮只推进当前已开启的场景或冲突，不要在结尾突然切去新场景。\n");
        builder.append("2. 如果接近篇幅上限，优先把当前段落写完整并停在自然句号上。\n");
    }

    private void appendRevisionGuardrails(StringBuilder builder, PreparedGenerationContext context) {
        if (builder == null || context == null) {
            return;
        }
        builder.append("4. 如果正文结尾不完整，优先重写最后一到两段，确保自然收束。\n");
        builder.append("5. 如果接近篇幅上限，宁可缩小范围，也不要把句子停在半截。\n");
        if (isEmptyChapterStart(context)) {
            builder.append("6. 当前是空章起稿，请把章节停在一个完整可继续扩写的开场节点上。\n");
        }
    }

    private String resolveWritingIntent(String writingType) {
        return switch (writingType) {
            case "draft" -> "任务要求：当前正文为空，请先拟生成一版可继续扩写的章节初稿。\n";
            case "continue" -> "任务要求：请承接现有正文自然续写，不要重复已经写过的句子。\n";
            case "expand" -> "任务要求：请在不偏离既有剧情事实的前提下扩写，输出更完整的章节版本。\n";
            case "rewrite" -> "任务要求：保留核心剧情意图，输出重写后的完整章节版本。\n";
            case "polish" -> "任务要求：保留剧情事实，对当前正文做语言润色与节奏优化。\n";
            default -> "任务要求：输出可直接放入章节中的小说正文。\n";
        };
    }

    private WorkflowResult runWorkflow(
            PreparedGenerationContext context,
            Consumer<AIWritingStreamEventVO> eventConsumer,
            boolean streamWriteStage) {
        WorkflowSettings settings = loadWorkflowSettings();
        String plan = "";
        String content;

        if (settings.maxPlanRounds() > 0) {
            emitStage(eventConsumer, "plan", "started", "正在整理本章写作计划");
            plan = executeWithHeartbeat(
                    eventConsumer,
                    "plan",
                    "写作计划仍在生成中，请稍候",
                    () -> aiProviderService.generateText(
                            context.provider(),
                            context.selectedModel(),
                            """
                            你是一名中文小说章节规划助手。
                            请输出一份简短、可执行的中文写作计划。
                            不要直接产出最终正文。
                            """,
                            buildPlanningPrompt(context),
                            null,
                            800
                    )
            );
            emitLog(eventConsumer, "plan", limit(plan, 280));
            emitStage(eventConsumer, "plan", "completed", "本章计划已生成");
        }

        emitStage(eventConsumer, "write", "started", "正在生成章节正文");
        if (streamWriteStage) {
            StringBuilder builder = new StringBuilder();
            aiProviderService.streamText(
                    context.provider(),
                    context.selectedModel(),
                    context.systemPrompt(),
                    buildWriterPrompt(context, plan),
                    null,
                    context.maxTokens(),
                    delta -> {
                        if (!StringUtils.hasText(delta)) {
                            return;
                        }
                        builder.append(delta);
                        eventConsumer.accept(AIWritingStreamEventVO.chunk(delta));
                    }
            );
            content = builder.toString();
        } else {
            content = aiProviderService.generateText(
                    context.provider(),
                    context.selectedModel(),
                    context.systemPrompt(),
                    buildWriterPrompt(context, plan),
                    null,
                    context.maxTokens()
            );
        }
        emitStage(eventConsumer, "write", "completed", "正文初稿已生成");
        List<String> hardIssuesAfterWrite = detectHardConstraintIssues(context, content);

        if (settings.maxCheckRounds() <= 0) {
            return new WorkflowResult(finalizeContentWithHardConstraintRepair(
                    context,
                    eventConsumer,
                    content,
                    hardIssuesAfterWrite
            ));
        }

        emitStage(eventConsumer, "check", "started", "正在检查连贯性与约束条件");
        String planForCheck = plan;
        String contentForCheck = content;
        String checkReport = executeWithHeartbeat(
                eventConsumer,
                "check",
                "一致性审校仍在进行中，请稍候",
                () -> aiProviderService.generateText(
                        context.provider(),
                        context.selectedModel(),
                        """
                        你是一名中文小说审校助手。
                        请检查章节正文是否遵循给定上下文和约束。
                        如果正文开头像从章节中段直接切入、没有完成必要的读者定向，必须判为修订。
                        如果正文结尾明显半句截断、场景收束未完成或停在不自然的位置，必须判为修订。
                        请严格按照以下格式返回：
                        结论：通过 或 修订
                        摘要：一句中文简述
                        问题：
                        - 问题 1
                        - 问题 2
                        """,
                        buildCheckPrompt(context, planForCheck, contentForCheck),
                        null,
                        700
                )
        );
        WritingCheckResult checkResult = mergeHardConstraintIssues(
                parseCheckResult(checkReport),
                hardIssuesAfterWrite
        );
        emitLog(eventConsumer, "check", checkResult.summary());
        emitStage(eventConsumer, "check", "completed", checkResult.requiresRevision() ? "建议进行修订" : "检查通过");

        if (!checkResult.requiresRevision() || settings.maxRevisionRounds() <= 0) {
            return new WorkflowResult(finalizeContentWithHardConstraintRepair(
                    context,
                    eventConsumer,
                    content,
                    checkResult.hardIssues()
            ));
        }

        emitStage(eventConsumer, "revise", "started", "正在根据审校意见修订正文");
        String planForRevision = plan;
        String contentForRevision = content;
        String revisedContent = executeWithHeartbeat(
                eventConsumer,
                "revise",
                "正在根据审校结果修订正文，请稍候",
                () -> aiProviderService.generateText(
                        context.provider(),
                        context.selectedModel(),
                        context.systemPrompt(),
                        buildRevisionPrompt(context, planForRevision, contentForRevision, checkResult.report()),
                        null,
                        context.maxTokens()
                )
        );
        emitLog(eventConsumer, "revise", "已根据审校意见自动完成修订");
        if (eventConsumer != null) {
            eventConsumer.accept(AIWritingStreamEventVO.replace(revisedContent));
        }
        emitStage(eventConsumer, "revise", "completed", "修订结果已应用");
        return new WorkflowResult(finalizeContentWithHardConstraintRepair(
                context,
                eventConsumer,
                revisedContent,
                detectHardConstraintIssues(context, revisedContent)
        ));
    }

    private WorkflowSettings loadWorkflowSettings() {
        return new WorkflowSettings(
                getConfiguredInt("ai.workflow.max_plan_rounds", 1),
                getConfiguredInt("ai.workflow.max_check_rounds", 1),
                getConfiguredInt("ai.workflow.max_revision_rounds", 1)
        );
    }

    private String buildPlanningPrompt(PreparedGenerationContext context) {
        return """
                请根据以下写作上下文，生成一份简短的中文章节计划。
                需要包含：
                1. 本章目标
                2. 冲突推进方式
                3. 建议的场景顺序
                4. 人物与设定约束

                %s
                """.formatted(context.userPrompt());
    }

    private String buildWriterPrompt(PreparedGenerationContext context, String plan) {
        StringBuilder builder = new StringBuilder(context.userPrompt());
        appendEndingGuardrails(builder, context);
        if (StringUtils.hasText(plan)) {
            builder.append("\n\n【写作计划】\n").append(plan.trim()).append("\n");
        }
        return builder.toString();
    }

    private String buildCheckPrompt(PreparedGenerationContext context, String plan, String content) {
        StringBuilder builder = new StringBuilder();
        builder.append("请检查这段章节正文是否遵循上下文与约束条件。\n");
        if (StringUtils.hasText(plan)) {
            builder.append("【写作计划】\n").append(plan.trim()).append("\n\n");
        }
        builder.append("【上下文】\n").append(context.userPrompt()).append("\n\n");
        builder.append("【正文草稿】\n").append(content == null ? "" : content.trim()).append('\n');
        return builder.toString();
    }

    private String buildRevisionPrompt(PreparedGenerationContext context, String plan, String content, String checkReport) {
        StringBuilder builder = new StringBuilder();
        builder.append("请根据以下审校意见修订章节正文。\n");
        builder.append("要求：\n");
        builder.append("1. 只输出完整修订后的正文。\n");
        builder.append("2. 保留正确的剧情事实和叙事口吻。\n");
        builder.append("3. 优先修复连贯性、设定一致性和章节目标偏差。\n");
        appendRevisionGuardrails(builder, context);
        builder.append('\n');
        if (StringUtils.hasText(plan)) {
            builder.append("【写作计划】\n").append(plan.trim()).append("\n\n");
        }
        builder.append("【上下文】\n").append(context.userPrompt()).append("\n\n");
        builder.append("【审校意见】\n").append(checkReport.trim()).append("\n\n");
        builder.append("【原始草稿】\n").append(content == null ? "" : content.trim());
        return builder.toString();
    }

    private WritingCheckResult parseCheckResult(String checkReport) {
        String normalized = normalizeText(checkReport);
        String upperCaseNormalized = normalized.toUpperCase(Locale.ROOT);
        boolean requiresRevision = upperCaseNormalized.contains("RESULT: REVISE")
                || upperCaseNormalized.contains("结论：修订".toUpperCase(Locale.ROOT))
                || upperCaseNormalized.contains("结论: 修订".toUpperCase(Locale.ROOT))
                || normalized.toLowerCase(Locale.ROOT).contains("revise")
                || normalized.contains("修订");
        String summary = normalized.lines()
                .filter(line -> line.toUpperCase(Locale.ROOT).startsWith("SUMMARY:")
                        || line.startsWith("摘要：")
                        || line.startsWith("摘要:"))
                .findFirst()
                .map(line -> {
                    if (line.toUpperCase(Locale.ROOT).startsWith("SUMMARY:")) {
                        return line.substring("SUMMARY:".length()).trim();
                    }
                    int splitIndex = line.indexOf('：');
                    if (splitIndex < 0) {
                        splitIndex = line.indexOf(':');
                    }
                    return splitIndex >= 0 ? line.substring(splitIndex + 1).trim() : line.trim();
                })
                .orElse(limit(normalized, 220));
        return new WritingCheckResult(requiresRevision, summary, normalized, List.of());
    }

    private void enforceReadinessBeforeGeneration(
            GenerationReadinessVO readiness,
            Chapter chapter,
            String writingType,
            String currentContent) {
        if (readiness == null || !readiness.isBlocked()) {
            return;
        }
        if (allowsBlockedReadiness(writingType, currentContent)) {
            return;
        }
        throw new IllegalStateException(buildBlockedReadinessMessage(chapter, readiness));
    }

    private boolean allowsBlockedReadiness(String writingType, String currentContent) {
        return StringUtils.hasText(currentContent) && "polish".equals(writingType);
    }

    private String buildBlockedReadinessMessage(Chapter chapter, GenerationReadinessVO readiness) {
        String chapterLabel = chapter != null && StringUtils.hasText(chapter.getTitle())
                ? "《" + chapter.getTitle().trim() + "》"
                : "当前章节";
        List<String> blockingIssues = readiness == null || readiness.getBlockingIssues() == null
                ? List.of()
                : readiness.getBlockingIssues().stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeIssueFragment)
                .limit(4)
                .toList();
        String issueText = blockingIssues.isEmpty()
                ? "缺少必要的生成锚点"
                : String.join("；", blockingIssues);
        return chapterLabel + " 尚未满足生成前置条件：" + issueText + "。请先补齐章节摘要、POV 或人物锚点后再生成。";
    }

    private WritingCheckResult mergeHardConstraintIssues(WritingCheckResult baseResult, List<String> hardIssues) {
        if (hardIssues == null || hardIssues.isEmpty()) {
            return baseResult;
        }
        String summary = StringUtils.hasText(baseResult.summary())
                ? baseResult.summary() + "；存在必须修复的硬性问题"
                : "检测到必须修复的硬性问题";
        StringBuilder reportBuilder = new StringBuilder();
        if (StringUtils.hasText(baseResult.report())) {
            reportBuilder.append(baseResult.report().trim()).append('\n');
        }
        reportBuilder.append("硬性问题：\n");
        for (String issue : hardIssues) {
            reportBuilder.append("- ").append(issue).append('\n');
        }
        return new WritingCheckResult(
                true,
                summary,
                reportBuilder.toString().trim(),
                List.copyOf(hardIssues)
        );
    }

    private List<String> detectHardConstraintIssues(PreparedGenerationContext context, String content) {
        List<String> issues = new ArrayList<>();
        if (!StringUtils.hasText(content)) {
            issues.add("生成结果为空，未产出可用正文。");
            return issues;
        }
        if (context != null && context.sceneDraftConstraintBundle() != null) {
            issues.addAll(aiContinuityStateService.inspectGeneratedScene(
                    context.provider(),
                    context.selectedModel(),
                    context.sceneDraftConstraintBundle().continuityState(),
                    content,
                    context.sceneDraftConstraintBundle().goal(),
                    context.sceneDraftConstraintBundle().stopCondition(),
                    context.sceneDraftConstraintBundle().nextSceneId(),
                    context.sceneDraftConstraintBundle().nextSceneGoal()
            ));
        }
        if (hasIncompleteEnding(content)) {
            issues.add("正文结尾停在未完成句子、缺少自然收束，不能直接作为可提交结果。");
        }
        return issues;
    }

    private boolean hasIncompleteEnding(String content) {
        String normalized = normalizeText(content);
        if (!StringUtils.hasText(normalized)) {
            return true;
        }
        String candidate = trimTrailingClosers(normalized);
        if (!StringUtils.hasText(candidate)) {
            return true;
        }
        char lastChar = candidate.charAt(candidate.length() - 1);
        return !isTerminalPunctuation(lastChar);
    }

    private String trimTrailingClosers(String value) {
        int end = value.length();
        while (end > 0 && isTrailingCloser(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end).trim();
    }

    private boolean isTrailingCloser(char value) {
        return "\"'”’）》】』」〉〕）]}】".indexOf(value) >= 0;
    }

    private boolean isTerminalPunctuation(char value) {
        return "。！？!?…".indexOf(value) >= 0;
    }

    private void failOnHardConstraintIssues(List<String> hardIssues) {
        if (hardIssues == null || hardIssues.isEmpty()) {
            return;
        }
        throw new IllegalStateException(buildHardConstraintFailureMessage(hardIssues));
    }

    private String buildHardConstraintFailureMessage(List<String> hardIssues) {
        List<String> normalizedIssues = hardIssues == null
                ? List.of()
                : hardIssues.stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeIssueFragment)
                .toList();
        return "生成结果未通过硬性完整性检查：" + String.join("；", normalizedIssues) + "。请重试或调整本章锚点后再生成。";
    }

    private String finalizeContentWithHardConstraintRepair(
            PreparedGenerationContext context,
            Consumer<AIWritingStreamEventVO> eventConsumer,
            String content,
            List<String> hardIssues) {
        String stabilizedContent = stabilizeEndingIfNeeded(context, eventConsumer, content, hardIssues);
        List<String> remainingHardIssues = detectHardConstraintIssues(context, stabilizedContent);
        String repairedContent = repairHardConstraintIssuesIfNeeded(
                context,
                eventConsumer,
                stabilizedContent,
                remainingHardIssues
        );
        failOnHardConstraintIssues(detectHardConstraintIssues(context, repairedContent));
        return repairedContent;
    }

    private String stabilizeEndingIfNeeded(
            PreparedGenerationContext context,
            Consumer<AIWritingStreamEventVO> eventConsumer,
            String content,
            List<String> hardIssues) {
        if (!containsIncompleteEndingIssue(hardIssues) || !StringUtils.hasText(content)) {
            return content;
        }

        emitStage(eventConsumer, "repair", "started", "检测到结尾未自然收束，正在定向修补尾段");
        String stablePrefix = extractStablePrefixForEndingRepair(content);
        String brokenTail = content.substring(Math.min(stablePrefix.length(), content.length())).trim();
        String repairedTail = executeWithHeartbeat(
                eventConsumer,
                "repair",
                "正在补足结尾收束，请稍候",
                () -> aiProviderService.generateText(
                        context.provider(),
                        context.selectedModel(),
                        """
                        你是一名中文小说尾段修补助手。
                        请只输出需要接在已有正文后面的新增尾段，不要重复前文，不要解释。
                        你的目标是把当前场景自然收住，而不是继续扩展新情节。
                        """,
                        buildEndingRepairPrompt(context, stablePrefix, brokenTail),
                        null,
                        resolveEndingRepairMaxTokens(context.maxTokens())
                )
        );
        String stabilized = mergeEndingRepair(stablePrefix, repairedTail);
        emitLog(eventConsumer, "repair", "已根据当前场景补足结尾收束");
        if (eventConsumer != null && !Objects.equals(stabilized, content)) {
            eventConsumer.accept(AIWritingStreamEventVO.replace(stabilized));
        }
        emitStage(eventConsumer, "repair", "completed", "结尾修补已完成");
        return stabilized;
    }

    private String repairHardConstraintIssuesIfNeeded(
            PreparedGenerationContext context,
            Consumer<AIWritingStreamEventVO> eventConsumer,
            String content,
            List<String> hardIssues) {
        if (!StringUtils.hasText(content) || hardIssues == null || hardIssues.isEmpty()) {
            return content;
        }
        List<String> actionableIssues = hardIssues.stream()
                .filter(StringUtils::hasText)
                .filter(issue -> !issue.contains("结尾") && !issue.contains("收束"))
                .toList();
        if (actionableIssues.isEmpty()) {
            return content;
        }

        emitStage(eventConsumer, "repair", "started", "检测到镜头连续性硬问题，正在定向修订当前镜头");
        String repairedContent = executeWithHeartbeat(
                eventConsumer,
                "repair",
                "正在校正人物称呼、时间线和镜头停点，请稍候",
                () -> aiProviderService.generateText(
                        context.provider(),
                        context.selectedModel(),
                        """
                        你是一名中文小说连续性修订助手。
                        请只输出修订后的完整正文，不要解释。
                        你必须优先修复人物称呼漂移、时间线冲突和越过下一镜头停点的问题。
                        如果原稿已经提前写到下一镜头，请保留当前镜头需要的部分，并删回到当前镜头停点前自然收束。
                        """,
                        buildHardConstraintRepairPrompt(context, content, actionableIssues),
                        null,
                        context.maxTokens()
                )
        );
        emitLog(eventConsumer, "repair", "已根据硬性完整性问题重写当前镜头。");
        if (eventConsumer != null && !Objects.equals(repairedContent, content)) {
            eventConsumer.accept(AIWritingStreamEventVO.replace(repairedContent));
        }
        emitStage(eventConsumer, "repair", "completed", "镜头连续性定向修订已完成");
        return repairedContent;
    }

    private String buildHardConstraintRepairPrompt(
            PreparedGenerationContext context,
            String content,
            List<String> hardIssues) {
        StringBuilder builder = new StringBuilder();
        builder.append("请针对以下硬性问题修订当前镜头正文。\n");
        builder.append("要求：\n");
        builder.append("1. 只输出完整修订后的正文。\n");
        builder.append("2. 必须沿用上一镜头已确认的人物称呼和会话对象，不要改成其他人。\n");
        builder.append("3. 只能完成当前镜头，不能把正文推进到下一镜头。\n");
        builder.append("4. 如果正文已经提前写到下一镜头，请删回到当前镜头停点前，并自然收束。\n");
        builder.append("5. 保留已经正确的剧情事实、文风和情绪基调。\n\n");
        builder.append("【必须修复的问题】\n");
        for (String issue : hardIssues) {
            builder.append("- ").append(issue.trim()).append('\n');
        }
        builder.append('\n');
        builder.append("【上下文】\n").append(context.userPrompt()).append("\n\n");
        builder.append("【待修订正文】\n").append(content.trim());
        return builder.toString();
    }

    private boolean containsIncompleteEndingIssue(List<String> hardIssues) {
        if (hardIssues == null || hardIssues.isEmpty()) {
            return false;
        }
        return hardIssues.stream()
                .filter(StringUtils::hasText)
                .anyMatch(issue -> issue.contains("结尾") || issue.contains("收束"));
    }

    private String buildEndingRepairPrompt(
            PreparedGenerationContext context,
            String stablePrefix,
            String brokenTail) {
        StringBuilder builder = new StringBuilder();
        builder.append("请根据以下正文补足结尾，只输出新增尾段。\n");
        builder.append("要求：\n");
        builder.append("1. 不要重复前文。\n");
        builder.append("2. 不要引入新人物、新场景、新任务。\n");
        builder.append("3. 优先把当前动作、心理或场景写完整，并自然停住。\n");
        builder.append("4. 新增尾段控制在 120 到 260 字左右。\n");
        builder.append("5. 最后一句必须完整收束，不能停在半句。\n\n");
        if (StringUtils.hasText(context.userInstruction())) {
            builder.append("【补充要求】\n").append(context.userInstruction().trim()).append("\n\n");
        }
        builder.append("【已保留正文】\n").append(stablePrefix.trim()).append("\n\n");
        if (StringUtils.hasText(brokenTail)) {
            builder.append("【被截断的原始尾部，仅供参考】\n").append(brokenTail.trim()).append("\n\n");
        }
        builder.append("请直接输出新增尾段。");
        return builder.toString();
    }

    private int resolveEndingRepairMaxTokens(Integer contextMaxTokens) {
        if (contextMaxTokens == null || contextMaxTokens <= 0) {
            return 360;
        }
        return Math.min(Math.max(contextMaxTokens / 3, 260), 480);
    }

    private String extractStablePrefixForEndingRepair(String content) {
        String normalized = normalizeText(content);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }

        int lastTerminalBoundary = findLastTerminalBoundary(normalized);
        if (lastTerminalBoundary > 0 && lastTerminalBoundary < normalized.length() - 1) {
            return normalized.substring(0, lastTerminalBoundary + 1).trim();
        }

        int lastParagraphBreak = normalized.lastIndexOf("\n\n");
        if (lastParagraphBreak > 0) {
            return normalized.substring(0, lastParagraphBreak).trim();
        }
        return normalized;
    }

    private int findLastTerminalBoundary(String value) {
        if (!StringUtils.hasText(value)) {
            return -1;
        }
        int searchFrom = Math.max(0, value.length() - 500);
        for (int index = value.length() - 1; index >= searchFrom; index--) {
            if (isTerminalPunctuation(value.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    private String mergeEndingRepair(String stablePrefix, String repairedTail) {
        String prefix = normalizeText(stablePrefix);
        String tail = normalizeText(repairedTail);
        if (!StringUtils.hasText(prefix)) {
            return tail;
        }
        if (!StringUtils.hasText(tail)) {
            return prefix;
        }
        String mergedTail = tail;
        int overlapLength = Math.min(prefix.length(), Math.min(mergedTail.length(), 40));
        for (int length = overlapLength; length >= 12; length--) {
            String prefixSuffix = prefix.substring(prefix.length() - length);
            if (mergedTail.startsWith(prefixSuffix)) {
                mergedTail = mergedTail.substring(length).trim();
                break;
            }
        }
        if (!StringUtils.hasText(mergedTail)) {
            return prefix;
        }
        return prefix + "\n\n" + mergedTail;
    }

    private boolean isEmptyChapterStart(PreparedGenerationContext context) {
        return context != null && !StringUtils.hasText(context.currentContent());
    }

    private boolean isFirstChapter(Chapter chapter) {
        return chapter != null && chapter.getOrderNum() != null && chapter.getOrderNum() == 1;
    }

    private String normalizeIssueFragment(String issue) {
        if (!StringUtils.hasText(issue)) {
            return "";
        }
        String normalized = issue.trim();
        while (normalized.endsWith("。")
                || normalized.endsWith("；")
                || normalized.endsWith(";")
                || normalized.endsWith("，")
                || normalized.endsWith(",")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private void emitStage(Consumer<AIWritingStreamEventVO> eventConsumer, String stage, String status, String message) {
        if (eventConsumer != null) {
            eventConsumer.accept(AIWritingStreamEventVO.stage(stage, status, message));
        }
    }

    private void emitLog(Consumer<AIWritingStreamEventVO> eventConsumer, String stage, String message) {
        if (eventConsumer != null && StringUtils.hasText(message)) {
            eventConsumer.accept(AIWritingStreamEventVO.log(stage, message));
        }
    }

    private <T> T executeWithHeartbeat(
            Consumer<AIWritingStreamEventVO> eventConsumer,
            String stage,
            String heartbeatMessage,
            StreamTask<T> task) {
        if (eventConsumer == null) {
            return task.run();
        }

        AtomicReference<T> resultReference = new AtomicReference<>();
        AtomicReference<Throwable> failureReference = new AtomicReference<>();
        Thread worker = Thread.startVirtualThread(() -> {
            try {
                resultReference.set(task.run());
            } catch (Throwable throwable) {
                failureReference.set(throwable);
            }
        });

        while (worker.isAlive()) {
            try {
                worker.join(STREAM_HEARTBEAT_INTERVAL_MS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("流式生成任务被中断", exception);
            }
            if (worker.isAlive()) {
                emitLog(eventConsumer, stage, heartbeatMessage);
            }
        }

        Throwable failure = failureReference.get();
        if (failure == null) {
            return resultReference.get();
        }
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        throw new IllegalStateException(failure.getMessage(), failure);
    }

    private void syncKnowledgeDocument(Chapter chapter, AIWritingRecord record) {
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
        document.setSummary(buildSummary(record.getGeneratedContent()));
        document.setStatus("indexed");

        if (document.getId() == null) {
            knowledgeDocumentService.save(document);
        } else {
            knowledgeDocumentService.updateById(document);
        }
    }

    private String buildSummary(String content) {
        if (!StringUtils.hasText(content)) {
            return "已采纳的 AI 正文已同步到知识检索。";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160);
    }

    private String normalizeWritingType(String writingType, String currentContent) {
        String normalized = StringUtils.hasText(writingType)
                ? writingType.trim().toLowerCase(Locale.ROOT)
                : "";

        if (!StringUtils.hasText(currentContent)) {
            if (!StringUtils.hasText(normalized)
                    || "continue".equals(normalized)
                    || "expand".equals(normalized)) {
                return "draft";
            }
        }

        return switch (normalized) {
            case "draft", "continue", "expand", "rewrite", "polish" -> normalized;
            default -> StringUtils.hasText(currentContent) ? "continue" : "draft";
        };
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private int getConfiguredInt(String key, int fallback) {
        String value = systemConfigService.getConfigValue(key);
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private boolean getConfiguredBoolean(String key, boolean fallback) {
        String value = systemConfigService.getConfigValue(key);
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "1", "true", "yes", "on" -> true;
            case "0", "false", "no", "off" -> false;
            default -> fallback;
        };
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String resolvePromptKey(String writingType) {
        return switch (writingType) {
            case "draft" -> "prompt.draft";
            case "continue" -> "prompt.continue";
            case "expand" -> "prompt.expand";
            case "rewrite" -> "prompt.rewrite";
            case "polish" -> "prompt.polish";
            default -> "prompt.continue";
        };
    }

    private String getDefaultPromptTemplate(String writingType) {
        return switch (writingType) {
            case "draft" -> "先根据章节标题、大纲和上下文拟生成一段可继续扩写的小说正文初稿，优先搭好场景、人物状态和冲突起点。";
            case "continue" -> "延续当前章节的叙事节奏，保持人物口吻与设定一致，优先推进当前冲突。";
            case "expand" -> "在不偏离原意的前提下补足细节、动作、环境与情绪描写，让场景更饱满。";
            case "rewrite" -> "保留关键信息与剧情目标，重写表达方式，提升节奏、清晰度和戏剧性。";
            case "polish" -> "在不改变剧情事实的前提下润色语言，让句子更自然、流畅、有画面感。";
            default -> "输出可直接用于章节正文的中文小说内容。";
        };
    }

    private String safe(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }

    private AIWritingResponseVO convertToVO(AIWritingRecord record) {
        AIWritingResponseVO vo = new AIWritingResponseVO();
        BeanUtils.copyProperties(record, vo, "generationTrace");
        vo.setGenerationTrace(readJson(record.getGenerationTraceJson()));
        return vo;
    }

    private AIWritingResponseVO toStreamCompleteRecord(AIWritingResponseVO source) {
        AIWritingResponseVO target = new AIWritingResponseVO();
        target.setId(source.getId());
        target.setChapterId(source.getChapterId());
        target.setGeneratedContent(null);
        target.setWritingType(source.getWritingType());
        target.setUserInstruction(null);
        target.setSelectedProviderId(source.getSelectedProviderId());
        target.setSelectedModel(source.getSelectedModel());
        target.setPromptSnapshot(null);
        target.setDirectorDecisionId(source.getDirectorDecisionId());
        target.setGenerationTrace(source.getGenerationTrace());
        target.setStatus(source.getStatus());
        target.setCreateTime(source.getCreateTime());
        return target;
    }

    private String buildGenerationTraceJson(
            PreparedGenerationContext context,
            List<StructuredCreationSuggestion> creationSuggestions) {
        Map<String, Object> root = new LinkedHashMap<>();

        GenerationReadinessVO readiness = context.readiness();
        if (readiness != null) {
            Map<String, Object> readinessTrace = new LinkedHashMap<>();
            readinessTrace.put("score", readiness.getScore());
            readinessTrace.put("status", readiness.getStatus());
            readinessTrace.put("blockingIssues", readiness.getBlockingIssues());
            readinessTrace.put("warnings", readiness.getWarnings());
            readinessTrace.put("recommendedModules", readiness.getRecommendedModules());
            root.put("readiness", readinessTrace);
        }

        ChapterAnchorBundle anchorBundle = context.anchorBundle();
        if (anchorBundle != null) {
            Map<String, Object> anchorTrace = new LinkedHashMap<>();
            anchorTrace.put("chapterOutlineId", anchorBundle.getChapterOutlineId());
            anchorTrace.put("volumeOutlineId", anchorBundle.getVolumeOutlineId());
            anchorTrace.put("mainPovCharacterId", anchorBundle.getMainPovCharacterId());
            anchorTrace.put("mainPovCharacterName", anchorBundle.getMainPovCharacterName());
            anchorTrace.put("requiredCharacterNames", anchorBundle.getRequiredCharacterNames());
            anchorTrace.put("storyBeatTitles", anchorBundle.getStoryBeatTitles());
            anchorTrace.put("relatedWorldSettingNames", anchorBundle.getRelatedWorldSettingNames());
            anchorTrace.put("chapterSummary", anchorBundle.getChapterSummary());
            anchorTrace.put("anchorSources", anchorBundle.getAnchorSources());
            anchorTrace.put("anchorSummary", buildAnchorSummary(anchorBundle));
            root.put("anchors", anchorTrace);
        }

        ReaderRevealConstraint readerRevealConstraint = context.readerRevealConstraint();
        if (readerRevealConstraint != null) {
            Map<String, Object> readerRevealTrace = new LinkedHashMap<>();
            readerRevealTrace.put("openingMode", readerRevealConstraint.getOpeningMode());
            readerRevealTrace.put("readerKnownFacts", readerRevealConstraint.getReaderKnownFacts());
            readerRevealTrace.put("revealTargets", readerRevealConstraint.getRevealTargets());
            readerRevealTrace.put("forbiddenAssumptions", readerRevealConstraint.getForbiddenAssumptions());
            root.put("readerReveal", readerRevealTrace);
        }

        AIDirectorDecisionVO directorDecision = context.directorDecision();
        if (directorDecision != null) {
            Map<String, Object> directorTrace = new LinkedHashMap<>();
            directorTrace.put("decisionId", directorDecision.getId());
            directorTrace.put("status", directorDecision.getStatus());
            directorTrace.put("mode", directorDecision.getMode());
            directorTrace.put("model", directorDecision.getSelectedModel());
            directorTrace.put("decisionSummary", directorDecision.getDecisionSummary());
            directorTrace.put("selectedAnchorSummary", directorDecision.getSelectedAnchorSummary());
            root.put("director", directorTrace);
        }

        Map<String, Object> orchestrationTrace = new LinkedHashMap<>();
        orchestrationTrace.put("entryPoint", context.entryPoint());
        orchestrationTrace.put("sceneId", context.sceneId());
        root.put("orchestration", orchestrationTrace);

        WritingContextBundle contextBundle = context.contextBundle();
        AIWritingChatParticipationVO participation = contextBundle == null
                ? AIWritingChatParticipationVO.empty()
                : contextBundle.chatParticipation();
        Map<String, Object> summaryTrace = new LinkedHashMap<>();
        summaryTrace.put("promptSnapshotPreview", preview(context.promptSnapshot(), 180));
        summaryTrace.put("userInstructionPreview", preview(context.userInstruction(), 180));
        Map<String, Object> participationTrace = new LinkedHashMap<>();
        participationTrace.put("active", participation != null && participation.hasContent());
        participationTrace.put("worldFactsCount", participation == null ? 0 : participation.getWorldFacts().size());
        participationTrace.put("characterConstraintsCount", participation == null ? 0 : participation.getCharacterConstraints().size());
        participationTrace.put("plotGuidanceCount", participation == null ? 0 : participation.getPlotGuidance().size());
        participationTrace.put("writingPreferencesCount", participation == null ? 0 : participation.getWritingPreferences().size());
        participationTrace.put("hardConstraintsCount", participation == null ? 0 : participation.getHardConstraints().size());
        summaryTrace.put("chatParticipation", participationTrace);
        root.put("summaryTrace", summaryTrace);

        if (creationSuggestions != null && !creationSuggestions.isEmpty()) {
            root.put("creationSuggestions", creationSuggestions);
        }

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private JsonNode readJson(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return NullNode.getInstance();
        }
        try {
            return objectMapper.readTree(rawJson);
        } catch (Exception exception) {
            return NullNode.getInstance();
        }
    }

    private String buildAnchorSummary(ChapterAnchorBundle anchorBundle) {
        if (anchorBundle == null) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(anchorBundle.getMainPovCharacterName())) {
            parts.add("POV " + anchorBundle.getMainPovCharacterName().trim());
        }
        if (anchorBundle.getRequiredCharacterNames() != null && !anchorBundle.getRequiredCharacterNames().isEmpty()) {
            parts.add("人物 " + String.join("、", anchorBundle.getRequiredCharacterNames()));
        }
        if (anchorBundle.getStoryBeatTitles() != null && !anchorBundle.getStoryBeatTitles().isEmpty()) {
            parts.add("节拍 " + String.join("、", anchorBundle.getStoryBeatTitles()));
        }
        if (StringUtils.hasText(anchorBundle.getChapterSummary())) {
            parts.add("brief " + preview(anchorBundle.getChapterSummary(), 80));
        }
        return String.join(" | ", parts);
    }

    private String preview(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) + "..." : normalized;
    }

    private record PreparedGenerationContext(
            Long userId,
            Chapter chapter,
            String currentContent,
            String writingType,
            String userInstruction,
            AIProvider provider,
            String selectedModel,
            String promptSnapshot,
            String entryPoint,
            String sceneId,
            String systemPrompt,
            String userPrompt,
            WritingContextBundle contextBundle,
            AIDirectorDecisionVO directorDecision,
            GenerationReadinessVO readiness,
            ChapterAnchorBundle anchorBundle,
            ReaderRevealConstraint readerRevealConstraint,
            SceneDraftConstraintBundle sceneDraftConstraintBundle,
            Integer maxTokens) {
    }

    private record SceneDraftConstraintBundle(
            String sceneId,
            String goal,
            List<String> readerReveal,
            List<String> mustUseAnchors,
            String stopCondition,
            String nextSceneId,
            String nextSceneGoal,
            SceneContinuityState continuityState) {
    }

    private record WritingContextBundle(
            Outline currentOutline,
            List<Plot> plots,
            List<Causality> causalities,
            List<WorldSettingVO> worldSettings,
            List<KnowledgeDocument> knowledgeDocuments,
            List<CharacterInventorySummary> characterInventories,
            List<String> requiredCharacters,
            AIWritingChatParticipationVO chatParticipation) {

        private static WritingContextBundle empty(List<String> requiredCharacters, AIWritingChatParticipationVO chatParticipation) {
            return new WritingContextBundle(
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    requiredCharacters == null ? List.of() : requiredCharacters,
                    chatParticipation == null ? AIWritingChatParticipationVO.empty() : chatParticipation
            );
        }
    }

    private record CharacterInventorySummary(
            String characterName,
            List<String> itemSummaries) {
    }

    private record WorkflowResult(String content) {
    }

    private record WorkflowSettings(
            int maxPlanRounds,
            int maxCheckRounds,
            int maxRevisionRounds) {
    }

    private record WritingCheckResult(
            boolean requiresRevision,
            String summary,
            String report,
            List<String> hardIssues) {
    }

    @FunctionalInterface
    private interface StreamTask<T> {
        T run();
    }
}
