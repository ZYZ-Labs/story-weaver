package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.dto.AIWritingRequestDTO;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.domain.entity.AIWritingRecord;
import com.storyweaver.domain.entity.Causality;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.KnowledgeDocument;
import com.storyweaver.domain.entity.Outline;
import com.storyweaver.domain.entity.Plot;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.vo.AIWritingResponseVO;
import com.storyweaver.domain.vo.AIWritingStreamEventVO;
import com.storyweaver.domain.vo.WorldSettingVO;
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
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class AIWritingServiceImpl extends ServiceImpl<AIWritingRecordMapper, AIWritingRecord> implements AIWritingService {

    private static final int MAX_CONTEXT_ITEMS = 4;

    private final ChapterService chapterService;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final AIProviderService aiProviderService;
    private final ProjectService projectService;
    private final SystemConfigService systemConfigService;
    private final OutlineService outlineService;
    private final PlotService plotService;
    private final CausalityService causalityService;
    private final WorldSettingService worldSettingService;
    private final AIModelRoutingService aiModelRoutingService;
    private final AIWritingChatService aiWritingChatService;

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
            AIModelRoutingService aiModelRoutingService,
            AIWritingChatService aiWritingChatService) {
        this.chapterService = chapterService;
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.aiProviderService = aiProviderService;
        this.projectService = projectService;
        this.systemConfigService = systemConfigService;
        this.outlineService = outlineService;
        this.plotService = plotService;
        this.causalityService = causalityService;
        this.worldSettingService = worldSettingService;
        this.aiModelRoutingService = aiModelRoutingService;
        this.aiWritingChatService = aiWritingChatService;
    }

    @Override
    public AIWritingResponseVO generateContent(Long userId, AIWritingRequestDTO requestDTO) {
        PreparedGenerationContext context = prepareGeneration(userId, requestDTO);
        WorkflowResult workflowResult = runWorkflow(context, null, false);
        return persistGeneratedRecord(context, workflowResult.content());
    }

    @Override
    public void streamContent(Long userId, AIWritingRequestDTO requestDTO, Consumer<AIWritingStreamEventVO> eventConsumer) {
        PreparedGenerationContext context = prepareGeneration(userId, requestDTO);

        eventConsumer.accept(AIWritingStreamEventVO.meta(
                context.writingType(),
                context.provider().getId(),
                context.selectedModel(),
                context.maxTokens()
        ));

        WorkflowResult workflowResult = runWorkflow(context, eventConsumer, true);
        AIWritingResponseVO response = persistGeneratedRecord(context, workflowResult.content());
        eventConsumer.accept(AIWritingStreamEventVO.complete(response));
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
    public AIWritingResponseVO acceptGeneratedContent(Long id) {
        AIWritingRecord record = getById(id);
        if (record == null || Integer.valueOf(1).equals(record.getDeleted())) {
            return null;
        }

        Chapter chapter = chapterService.getById(record.getChapterId());
        if (chapter != null) {
            if ("continue".equals(record.getWritingType())) {
                String currentContent = chapter.getContent() == null ? "" : chapter.getContent();
                String newContent = currentContent.isBlank()
                        ? record.getGeneratedContent()
                        : currentContent + "\n\n" + record.getGeneratedContent();
                chapter.setContent(newContent);
            } else {
                chapter.setContent(record.getGeneratedContent());
            }
            chapter.setWordCount(chapter.getContent() == null ? 0 : chapter.getContent().length());
            chapterService.updateById(chapter);
            syncKnowledgeDocument(chapter, record);
        }

        record.setStatus("accepted");
        updateById(record);
        return convertToVO(record);
    }

    @Override
    public void rejectGeneratedContent(Long id) {
        AIWritingRecord record = getById(id);
        if (record != null && !Integer.valueOf(1).equals(record.getDeleted())) {
            record.setStatus("rejected");
            updateById(record);
        }
    }

    private PreparedGenerationContext prepareGeneration(Long userId, AIWritingRequestDTO requestDTO) {
        Chapter chapter = chapterService.getChapterWithAuth(requestDTO.getChapterId(), userId);
        if (chapter == null) {
            throw new IllegalArgumentException("章节不存在或无权访问");
        }

        String currentContent = normalizeText(
                StringUtils.hasText(requestDTO.getCurrentContent()) ? requestDTO.getCurrentContent() : chapter.getContent()
        );
        String writingType = normalizeWritingType(requestDTO.getWritingType(), currentContent);
        String userInstruction = normalizeText(requestDTO.getUserInstruction());
        AIModelRoutingService.ResolvedModelSelection selection = aiModelRoutingService.resolve(
                requestDTO.getSelectedProviderId(),
                requestDTO.getSelectedModel(),
                requestDTO.getEntryPoint()
        );
        AIProvider provider = selection.provider();
        String selectedModel = selection.model();
        String promptSnapshot = resolvePromptSnapshot(requestDTO.getPromptSnapshot(), writingType);
        Project project = chapter.getProjectId() == null ? null : projectService.getById(chapter.getProjectId());
        WritingContextBundle contextBundle = buildContextBundle(userId, project, chapter, userInstruction);

        return new PreparedGenerationContext(
                chapter,
                currentContent,
                writingType,
                userInstruction,
                provider,
                selectedModel,
                promptSnapshot,
                buildSystemPrompt(promptSnapshot),
                buildUserPrompt(project, chapter, contextBundle, currentContent, writingType, userInstruction),
                normalizeMaxTokens(requestDTO.getMaxTokens())
        );
    }

    private WritingContextBundle buildContextBundle(Long userId, Project project, Chapter chapter, String userInstruction) {
        if (project == null || chapter.getProjectId() == null) {
            return WritingContextBundle.empty();
        }

        List<Outline> outlines = outlineService.getProjectOutlines(project.getId(), userId);
        Outline currentOutline = outlines.stream()
                .filter(item -> item.getChapterId() != null && item.getChapterId().equals(chapter.getId()))
                .findFirst()
                .orElse(null);

        Set<Long> relatedPlotIds = new LinkedHashSet<>();
        Set<Long> relatedCausalityIds = new LinkedHashSet<>();
        if (currentOutline != null) {
            relatedPlotIds.addAll(currentOutline.getRelatedPlotIdList() == null ? List.of() : currentOutline.getRelatedPlotIdList());
            relatedCausalityIds.addAll(currentOutline.getRelatedCausalityIdList() == null ? List.of() : currentOutline.getRelatedCausalityIdList());
        }

        List<Plot> allPlots = plotService.getProjectPlots(project.getId());
        List<Plot> relevantPlots = allPlots.stream()
                .filter(item -> item.getChapterId() != null && item.getChapterId().equals(chapter.getId()) || relatedPlotIds.contains(item.getId()))
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

        List<WorldSettingVO> worldSettings = worldSettingService.getWorldSettingsByProjectId(project.getId()).stream()
                .limit(MAX_CONTEXT_ITEMS)
                .toList();

        String retrievalQuery = String.join(" ",
                safe(chapter.getTitle(), ""),
                userInstruction,
                currentOutline == null ? "" : safe(currentOutline.getSummary(), currentOutline.getTitle()));
        List<KnowledgeDocument> knowledgeDocuments = StringUtils.hasText(retrievalQuery)
                ? knowledgeDocumentService.queryDocuments(project.getId(), userId, retrievalQuery).stream().limit(3).toList()
                : List.of();
        String chatBackground = aiWritingChatService.buildBackgroundContext(userId, chapter.getId());

        return new WritingContextBundle(
                currentOutline,
                relevantPlots,
                relevantCausalities,
                worldSettings,
                knowledgeDocuments,
                chapter.getRequiredCharacterNames() == null ? List.of() : chapter.getRequiredCharacterNames(),
                chatBackground
        );
    }

    private AIWritingResponseVO persistGeneratedRecord(PreparedGenerationContext context, String generatedContent) {
        String normalizedContent = generatedContent == null ? "" : generatedContent.trim();
        if (!StringUtils.hasText(normalizedContent)) {
            throw new IllegalStateException("模型没有返回可用的正文内容，请稍后重试");
        }

        AIWritingRecord record = new AIWritingRecord();
        record.setChapterId(context.chapter().getId());
        record.setOriginalContent(context.currentContent());
        record.setGeneratedContent(normalizedContent);
        record.setWritingType(context.writingType());
        record.setUserInstruction(context.userInstruction());
        record.setSelectedProviderId(context.provider().getId());
        record.setSelectedModel(context.selectedModel());
        record.setPromptSnapshot(context.promptSnapshot());
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

    private Integer normalizeMaxTokens(Integer maxTokens) {
        return maxTokens != null && maxTokens > 0 ? maxTokens : null;
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
        if (!contextBundle.requiredCharacters().isEmpty()) {
            builder.append("本章必须出现人物：").append(String.join("、", contextBundle.requiredCharacters())).append('\n');
        }
        builder.append(resolveWritingIntent(writingType));

        appendOutlineSection(builder, contextBundle.currentOutline());
        appendPlotSection(builder, contextBundle.plots());
        appendCausalitySection(builder, contextBundle.causalities());
        appendWorldSettingSection(builder, contextBundle.worldSettings());
        appendKnowledgeSection(builder, contextBundle.knowledgeDocuments());
        appendChatBackgroundSection(builder, contextBundle.chatBackground());

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

    private void appendOutlineSection(StringBuilder builder, Outline outline) {
        if (outline == null) {
            return;
        }
        builder.append("\n【章节大纲】\n");
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
            if (StringUtils.hasText(plot.getResolutions())) {
                builder.append("｜预期解法：").append(limit(plot.getResolutions(), 80));
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
            builder.append('\n');
        }
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

    private void appendChatBackgroundSection(StringBuilder builder, String chatBackground) {
        if (!StringUtils.hasText(chatBackground)) {
            return;
        }
        builder.append("\n【聊天背景信息】\n")
                .append(chatBackground.trim())
                .append('\n');
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
            emitStage(eventConsumer, "plan", "started", "Building chapter plan");
            plan = aiProviderService.generateText(
                    context.provider(),
                    context.selectedModel(),
                    """
                    You are a Chinese fiction planning assistant.
                    Produce a short, actionable chapter plan in Chinese.
                    Do not write the final prose.
                    """,
                    buildPlanningPrompt(context),
                    null,
                    800
            );
            emitLog(eventConsumer, "plan", limit(plan, 280));
            emitStage(eventConsumer, "plan", "completed", "Chapter plan ready");
        }

        emitStage(eventConsumer, "write", "started", "Generating chapter prose");
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
        emitStage(eventConsumer, "write", "completed", "Draft prose ready");

        if (settings.maxCheckRounds() <= 0) {
            return new WorkflowResult(content);
        }

        emitStage(eventConsumer, "check", "started", "Checking continuity and constraints");
        String checkReport = aiProviderService.generateText(
                context.provider(),
                context.selectedModel(),
                """
                You are a Chinese fiction reviewer.
                Check whether the chapter follows the provided context.
                Return the result using this exact format:
                RESULT: PASS 或 REVISE
                SUMMARY: one short sentence in Chinese
                ISSUES:
                - issue 1
                - issue 2
                """,
                buildCheckPrompt(context, plan, content),
                null,
                700
        );
        WritingCheckResult checkResult = parseCheckResult(checkReport);
        emitLog(eventConsumer, "check", checkResult.summary());
        emitStage(eventConsumer, "check", "completed", checkResult.requiresRevision() ? "Revision suggested" : "Check passed");

        if (!checkResult.requiresRevision() || settings.maxRevisionRounds() <= 0) {
            return new WorkflowResult(content);
        }

        emitStage(eventConsumer, "revise", "started", "Revising prose from review");
        String revisedContent = aiProviderService.generateText(
                context.provider(),
                context.selectedModel(),
                context.systemPrompt(),
                buildRevisionPrompt(context, plan, content, checkResult.report()),
                null,
                context.maxTokens()
        );
        emitLog(eventConsumer, "revise", "Auto revision completed");
        if (eventConsumer != null) {
            eventConsumer.accept(AIWritingStreamEventVO.replace(revisedContent));
        }
        emitStage(eventConsumer, "revise", "completed", "Revision applied");
        return new WorkflowResult(revisedContent);
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
                Based on the following writing context, produce a short chapter plan in Chinese.
                Include:
                1. Chapter goal
                2. Conflict progression
                3. Suggested scene order
                4. Character and setting constraints

                %s
                """.formatted(context.userPrompt());
    }

    private String buildWriterPrompt(PreparedGenerationContext context, String plan) {
        if (!StringUtils.hasText(plan)) {
            return context.userPrompt();
        }
        return context.userPrompt() + "\n\n【写作计划】\n" + plan.trim() + "\n";
    }

    private String buildCheckPrompt(PreparedGenerationContext context, String plan, String content) {
        StringBuilder builder = new StringBuilder();
        builder.append("Check whether this chapter prose follows the context and constraints.\n");
        if (StringUtils.hasText(plan)) {
            builder.append("[Plan]\n").append(plan.trim()).append("\n\n");
        }
        builder.append("[Context]\n").append(context.userPrompt()).append("\n\n");
        builder.append("[Draft]\n").append(content == null ? "" : content.trim()).append('\n');
        return builder.toString();
    }

    private String buildRevisionPrompt(PreparedGenerationContext context, String plan, String content, String checkReport) {
        StringBuilder builder = new StringBuilder();
        builder.append("Revise the following chapter prose based on the review notes.\n");
        builder.append("Requirements:\n");
        builder.append("1. Output full revised prose only.\n");
        builder.append("2. Keep correct plot facts and voice.\n");
        builder.append("3. Prioritize continuity, setting consistency, and chapter goal alignment.\n\n");
        if (StringUtils.hasText(plan)) {
            builder.append("[Plan]\n").append(plan.trim()).append("\n\n");
        }
        builder.append("[Context]\n").append(context.userPrompt()).append("\n\n");
        builder.append("[Review]\n").append(checkReport.trim()).append("\n\n");
        builder.append("[Original Draft]\n").append(content == null ? "" : content.trim());
        return builder.toString();
    }

    private WritingCheckResult parseCheckResult(String checkReport) {
        String normalized = normalizeText(checkReport);
        boolean requiresRevision = normalized.toUpperCase(Locale.ROOT).contains("RESULT: REVISE")
                || normalized.toLowerCase(Locale.ROOT).contains("revise")
                || normalized.contains("修订");
        String summary = normalized.lines()
                .filter(line -> line.toUpperCase(Locale.ROOT).startsWith("SUMMARY:"))
                .findFirst()
                .map(line -> line.substring("SUMMARY:".length()).trim())
                .orElse(limit(normalized, 220));
        return new WritingCheckResult(requiresRevision, summary, normalized);
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
            return "Accepted AI writing synchronized to RAG.";
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
        BeanUtils.copyProperties(record, vo);
        return vo;
    }

    private record PreparedGenerationContext(
            Chapter chapter,
            String currentContent,
            String writingType,
            String userInstruction,
            AIProvider provider,
            String selectedModel,
            String promptSnapshot,
            String systemPrompt,
            String userPrompt,
            Integer maxTokens) {
    }

    private record WritingContextBundle(
            Outline currentOutline,
            List<Plot> plots,
            List<Causality> causalities,
            List<WorldSettingVO> worldSettings,
            List<KnowledgeDocument> knowledgeDocuments,
            List<String> requiredCharacters,
            String chatBackground) {

        private static WritingContextBundle empty() {
            return new WritingContextBundle(null, List.of(), List.of(), List.of(), List.of(), List.of(), "");
        }
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
            String report) {
    }
}
