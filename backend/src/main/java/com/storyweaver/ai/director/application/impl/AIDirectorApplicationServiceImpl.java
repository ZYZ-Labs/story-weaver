package com.storyweaver.ai.director.application.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.storyweaver.ai.application.support.StructuredJsonSupport;
import com.storyweaver.ai.director.application.AIDirectorApplicationService;
import com.storyweaver.ai.director.application.DirectorDecisionPackAssembler;
import com.storyweaver.ai.director.application.DirectorModuleRegistry;
import com.storyweaver.ai.director.application.tool.DirectorToolExecutor;
import com.storyweaver.domain.dto.AIDirectorDecisionRequestDTO;
import com.storyweaver.domain.entity.AIDirectorDecision;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.Outline;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.vo.AIDirectorDecisionVO;
import com.storyweaver.repository.AIDirectorDecisionMapper;
import com.storyweaver.service.AIModelRoutingService;
import com.storyweaver.service.AIProviderService;
import com.storyweaver.service.AIWritingChatService;
import com.storyweaver.service.ChapterService;
import com.storyweaver.service.OutlineService;
import com.storyweaver.service.ProjectService;
import com.storyweaver.service.SystemConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AIDirectorApplicationServiceImpl implements AIDirectorApplicationService {

    private static final Set<String> SUPPORTED_STAGES = Set.of(
            "opening",
            "setup",
            "advancement",
            "turning",
            "convergence",
            "polish"
    );

    private static final Set<String> SUPPORTED_WRITING_MODES = Set.of(
            "draft",
            "continue",
            "expand",
            "rewrite",
            "polish"
    );

    private final ChapterService chapterService;
    private final ProjectService projectService;
    private final OutlineService outlineService;
    private final AIWritingChatService aiWritingChatService;
    private final AIModelRoutingService aiModelRoutingService;
    private final AIProviderService aiProviderService;
    private final SystemConfigService systemConfigService;
    private final AIDirectorDecisionMapper aiDirectorDecisionMapper;
    private final DirectorModuleRegistry directorModuleRegistry;
    private final DirectorDecisionPackAssembler directorDecisionPackAssembler;
    private final DirectorToolExecutor directorToolExecutor;
    private final StructuredJsonSupport structuredJsonSupport;
    private final ObjectMapper objectMapper;

    public AIDirectorApplicationServiceImpl(
            ChapterService chapterService,
            ProjectService projectService,
            OutlineService outlineService,
            AIWritingChatService aiWritingChatService,
            AIModelRoutingService aiModelRoutingService,
            AIProviderService aiProviderService,
            SystemConfigService systemConfigService,
            AIDirectorDecisionMapper aiDirectorDecisionMapper,
            DirectorModuleRegistry directorModuleRegistry,
            DirectorDecisionPackAssembler directorDecisionPackAssembler,
            DirectorToolExecutor directorToolExecutor,
            StructuredJsonSupport structuredJsonSupport,
            ObjectMapper objectMapper) {
        this.chapterService = chapterService;
        this.projectService = projectService;
        this.outlineService = outlineService;
        this.aiWritingChatService = aiWritingChatService;
        this.aiModelRoutingService = aiModelRoutingService;
        this.aiProviderService = aiProviderService;
        this.systemConfigService = systemConfigService;
        this.aiDirectorDecisionMapper = aiDirectorDecisionMapper;
        this.directorModuleRegistry = directorModuleRegistry;
        this.directorDecisionPackAssembler = directorDecisionPackAssembler;
        this.directorToolExecutor = directorToolExecutor;
        this.structuredJsonSupport = structuredJsonSupport;
        this.objectMapper = objectMapper;
    }

    @Override
    public AIDirectorDecisionVO decide(Long userId, AIDirectorDecisionRequestDTO requestDTO) {
        Chapter chapter = chapterService.getChapterWithAuth(requestDTO.getChapterId(), userId);
        if (chapter == null) {
            throw new IllegalArgumentException("章节不存在或无权访问");
        }

        String currentContent = normalizeText(
                StringUtils.hasText(requestDTO.getCurrentContent()) ? requestDTO.getCurrentContent() : chapter.getContent()
        );
        String entryPoint = StringUtils.hasText(requestDTO.getEntryPoint())
                ? requestDTO.getEntryPoint().trim()
                : "writing-center";
        String sourceType = StringUtils.hasText(requestDTO.getSourceType())
                ? requestDTO.getSourceType().trim()
                : "writing";
        String writingMode = normalizeWritingMode(requestDTO.getWritingType(), currentContent);

        Project project = chapter.getProjectId() == null ? null : projectService.getById(chapter.getProjectId());
        Outline outline = resolveOutline(chapter, userId);
        boolean hasBackgroundContext = aiWritingChatService.hasBackgroundContext(userId, chapter.getId());
        String heuristicStage = determineStage(writingMode, currentContent, outline);

        AIModelRoutingService.ResolvedModelSelection selection = aiModelRoutingService.resolve(
                requestDTO.getSelectedProviderId(),
                requestDTO.getSelectedModel(),
                "director"
        );

        DirectorDecisionPackAssembler.AssembledDecisionPack assembled;
        String finalStage;
        String finalWritingMode;
        String status;
        String errorMessage = null;

        if (!isDirectorEnabled()) {
            assembled = buildHeuristicDecision(chapter, outline, requestDTO, entryPoint, heuristicStage, writingMode, hasBackgroundContext);
            finalStage = heuristicStage;
            finalWritingMode = writingMode;
            status = "fallback";
            errorMessage = "当前总导决策层被关闭，已使用启发式 fallback 决策。";
        } else {
            try {
                ModelDecision modelDecision = buildModelDecision(
                        userId,
                        requestDTO,
                        chapter,
                        project,
                        outline,
                        selection,
                        heuristicStage,
                        writingMode,
                        hasBackgroundContext
                );
                assembled = directorDecisionPackAssembler.assembleResolvedDecision(
                        chapter,
                        entryPoint,
                        modelDecision.stage(),
                        modelDecision.writingMode(),
                        modelDecision.targetWordCount(),
                        modelDecision.selectedModules(),
                        modelDecision.requiredFacts(),
                        modelDecision.prohibitedMoves(),
                        modelDecision.writerHints(),
                        modelDecision.decisionSummary(),
                        modelDecision.toolTraces()
                );
                finalStage = modelDecision.stage();
                finalWritingMode = modelDecision.writingMode();
                status = "generated";
            } catch (Exception exception) {
                assembled = buildHeuristicDecision(chapter, outline, requestDTO, entryPoint, heuristicStage, writingMode, hasBackgroundContext);
                finalStage = heuristicStage;
                finalWritingMode = writingMode;
                status = "fallback";
                errorMessage = compactError(exception.getMessage());
            }
        }

        AIDirectorDecision decision = new AIDirectorDecision();
        decision.setProjectId(chapter.getProjectId());
        decision.setChapterId(chapter.getId());
        decision.setUserId(userId);
        decision.setSourceType(sourceType);
        decision.setEntryPoint(entryPoint);
        decision.setStage(finalStage);
        decision.setWritingMode(finalWritingMode);
        decision.setTargetWordCount(assembled.targetWordCount());
        decision.setSelectedModulesJson(assembled.selectedModulesJson());
        decision.setModuleWeightsJson(assembled.moduleWeightsJson());
        decision.setRequiredFactsJson(assembled.requiredFactsJson());
        decision.setProhibitedMovesJson(assembled.prohibitedMovesJson());
        decision.setDecisionPackJson(assembled.decisionPackJson());
        decision.setToolTraceJson(assembled.toolTraceJson());
        decision.setSelectedProviderId(selection.provider().getId());
        decision.setSelectedModel(selection.model());
        decision.setStatus(status);
        decision.setErrorMessage(errorMessage);
        aiDirectorDecisionMapper.insert(decision);
        return toVO(decision, assembled.decisionSummary());
    }

    @Override
    public AIDirectorDecisionVO getLatestDecision(Long userId, Long chapterId) {
        Chapter chapter = chapterService.getChapterWithAuth(chapterId, userId);
        if (chapter == null) {
            return null;
        }
        AIDirectorDecision decision = aiDirectorDecisionMapper.findLatestByChapterIdAndUserId(chapterId, userId);
        return decision == null ? null : toVO(decision, extractDecisionSummary(decision.getDecisionPackJson()));
    }

    private ModelDecision buildModelDecision(
            Long userId,
            AIDirectorDecisionRequestDTO requestDTO,
            Chapter chapter,
            Project project,
            Outline outline,
            AIModelRoutingService.ResolvedModelSelection selection,
            String heuristicStage,
            String writingMode,
            boolean hasBackgroundContext) {
        DirectorToolExecutor.DirectorToolContext toolContext = new DirectorToolExecutor.DirectorToolContext(
                userId,
                requestDTO,
                chapter,
                project,
                outline,
                hasBackgroundContext,
                selection.provider(),
                selection.model()
        );

        AIProviderService.ToolExecutionResult result = aiProviderService.generateTextWithTools(
                selection.provider(),
                selection.model(),
                buildDirectorSystemPrompt(),
                buildDirectorUserPrompt(chapter, project, outline, requestDTO, heuristicStage, writingMode, hasBackgroundContext),
                directorToolExecutor.listToolDefinitions(),
                1200,
                getConfiguredInt("ai.director.max_tool_calls", 4),
                toolRequest -> directorToolExecutor.execute(toolContext, toolRequest)
        );
        return parseModelDecision(
                result.finalText(),
                chapter,
                outline,
                heuristicStage,
                writingMode,
                hasBackgroundContext,
                result.toolCalls()
        );
    }

    private ModelDecision parseModelDecision(
            String rawResponse,
            Chapter chapter,
            Outline outline,
            String heuristicStage,
            String fallbackWritingMode,
            boolean hasBackgroundContext,
            List<AIProviderService.ToolCallTrace> toolTraces) {
        JsonNode root = structuredJsonSupport.readRoot(
                rawResponse,
                "总导层没有返回可用的决策结果",
                "总导层返回内容不是有效 JSON"
        );

        String stage = normalizeStage(structuredJsonSupport.readText(root, "stage", "chapterStage"), heuristicStage);
        String writingMode = normalizeReturnedWritingMode(
                structuredJsonSupport.readText(root, "writingMode", "mode"),
                fallbackWritingMode
        );
        DirectorDecisionPackAssembler.DecisionDefaults defaults = directorDecisionPackAssembler.createDefaults(
                chapter,
                outline,
                stage,
                writingMode,
                hasBackgroundContext
        );
        List<DirectorDecisionPackAssembler.DirectorModuleSelection> selectedModules = normalizeSelectedModules(
                root.path("selectedModules"),
                chapter,
                outline,
                stage,
                hasBackgroundContext
        );
        int targetWordCount = readPositiveInt(root, defaults.targetWordCount(), "targetWordCount", "targetWords");
        List<String> requiredFacts = mergeUnique(defaults.requiredFacts(), structuredJsonSupport.readList(root, "requiredFacts", "mustUseFacts"));
        List<String> prohibitedMoves = mergeUnique(defaults.prohibitedMoves(), structuredJsonSupport.readList(root, "prohibitedMoves", "mustAvoid"));
        List<String> writerHints = mergeUnique(structuredJsonSupport.readList(root, "writerHints", "hints"), defaults.writerHints());

        String decisionSummary = structuredJsonSupport.readText(root, "decisionSummary", "summary");
        if (!StringUtils.hasText(decisionSummary)) {
            decisionSummary = directorDecisionPackAssembler.buildDecisionSummary(stage, writingMode, selectedModules, hasBackgroundContext);
        }

        return new ModelDecision(
                stage,
                writingMode,
                targetWordCount,
                selectedModules,
                requiredFacts,
                prohibitedMoves,
                writerHints,
                decisionSummary,
                toolTraces == null ? List.of() : toolTraces
        );
    }

    private DirectorDecisionPackAssembler.AssembledDecisionPack buildHeuristicDecision(
            Chapter chapter,
            Outline outline,
            AIDirectorDecisionRequestDTO requestDTO,
            String entryPoint,
            String stage,
            String writingMode,
            boolean hasBackgroundContext) {
        List<DirectorDecisionPackAssembler.DirectorModuleSelection> selectedModules = selectModules(chapter, outline, stage, hasBackgroundContext);
        return directorDecisionPackAssembler.assemble(
                chapter,
                outline,
                requestDTO,
                entryPoint,
                stage,
                writingMode,
                selectedModules,
                hasBackgroundContext
        );
    }

    private String buildDirectorSystemPrompt() {
        return """
                你是小说章节写作的总导决策层。
                你的职责只有三件事：判断当前章节阶段、选择本轮需要的上下文模块、给写作层输出结构化 decision pack。
                优先通过工具获取事实，不要凭空补设定，不要直接写正文。
                只返回严格 JSON，不要解释，不要 Markdown。
                JSON 结构固定为：
                {"stage":"","writingMode":"","targetWordCount":1200,"decisionSummary":"","selectedModules":[{"module":"","weight":0.8,"topK":2,"fields":["..."]}],"requiredFacts":[],"prohibitedMoves":[],"writerHints":[]}
                """;
    }

    private String buildDirectorUserPrompt(
            Chapter chapter,
            Project project,
            Outline outline,
            AIDirectorDecisionRequestDTO requestDTO,
            String heuristicStage,
            String writingMode,
            boolean hasBackgroundContext) {
        StringBuilder builder = new StringBuilder();
        builder.append("请为当前章节生成一份 decision pack。\n");
        builder.append("项目名称：").append(project == null ? "未命名项目" : safe(project.getName(), "未命名项目")).append('\n');
        builder.append("章节标题：").append(safe(chapter.getTitle(), "未命名章节")).append('\n');
        builder.append("章节序号：").append(chapter.getOrderNum() == null ? 0 : chapter.getOrderNum()).append('\n');
        builder.append("请求写作模式：").append(writingMode).append('\n');
        builder.append("启发式阶段参考：").append(heuristicStage).append('\n');
        String currentContent = normalizeText(
                StringUtils.hasText(requestDTO.getCurrentContent()) ? requestDTO.getCurrentContent() : chapter.getContent()
        );
        builder.append("当前正文长度：").append(currentContent.length()).append('\n');
        builder.append("有无章节大纲：").append(outline == null ? "无" : "有").append('\n');
        builder.append("有无背景聊天：").append(hasBackgroundContext ? "有" : "无").append('\n');
        if (StringUtils.hasText(requestDTO.getUserInstruction())) {
            builder.append("用户补充要求：").append(requestDTO.getUserInstruction().trim()).append('\n');
        }
        builder.append("可选模块：chapter_snapshot, outline, plot, causality, world_setting, required_characters, character_inventory, knowledge, chat_background。\n");
        builder.append("请根据需要调用工具，再返回 JSON。chapter_snapshot 必须保留；selectedModules 只允许从上面的模块名中选择。");
        return builder.toString();
    }

    private Outline resolveOutline(Chapter chapter, Long userId) {
        if (chapter == null || chapter.getProjectId() == null) {
            return null;
        }
        return outlineService.getProjectOutlines(chapter.getProjectId(), userId).stream()
                .filter(item -> item.getChapterId() != null && item.getChapterId().equals(chapter.getId()))
                .findFirst()
                .orElse(null);
    }

    private List<DirectorDecisionPackAssembler.DirectorModuleSelection> selectModules(
            Chapter chapter,
            Outline outline,
            String stage,
            boolean hasBackgroundContext) {
        List<DirectorDecisionPackAssembler.DirectorModuleSelection> selected = new ArrayList<>();
        addModuleSelection(selected, "chapter_snapshot", stage, null, null, null);

        if (outline != null) {
            addModuleSelection(selected, "outline", stage, null, null, null);
        }
        if (List.of("opening", "setup", "advancement").contains(stage)) {
            addModuleSelection(selected, "world_setting", stage, null, null, null);
        }
        if (List.of("advancement", "turning", "convergence").contains(stage)) {
            addModuleSelection(selected, "plot", stage, null, null, null);
            addModuleSelection(selected, "causality", stage, null, null, null);
        }
        if (chapter.getRequiredCharacterNames() != null && !chapter.getRequiredCharacterNames().isEmpty()) {
            addModuleSelection(selected, "required_characters", stage, null, null, null);
        }
        if (chapter.getRequiredCharacterIds() != null && !chapter.getRequiredCharacterIds().isEmpty()) {
            addModuleSelection(selected, "character_inventory", stage, null, null, null);
        }
        if (hasBackgroundContext) {
            addModuleSelection(selected, "chat_background", stage, null, null, null);
        }
        if (!"polish".equals(stage)) {
            addModuleSelection(selected, "knowledge", stage, null, null, null);
        }

        int maxSelectedModules = getConfiguredInt("ai.director.max_selected_modules", 6);
        return selected.size() <= maxSelectedModules ? selected : new ArrayList<>(selected.subList(0, maxSelectedModules));
    }

    private List<DirectorDecisionPackAssembler.DirectorModuleSelection> normalizeSelectedModules(
            JsonNode selectedModulesNode,
            Chapter chapter,
            Outline outline,
            String stage,
            boolean hasBackgroundContext) {
        if (!selectedModulesNode.isArray() || selectedModulesNode.isEmpty()) {
            return selectModules(chapter, outline, stage, hasBackgroundContext);
        }

        Map<String, DirectorDecisionPackAssembler.DirectorModuleSelection> selected = new LinkedHashMap<>();
        for (JsonNode item : selectedModulesNode) {
            String moduleName = item.isTextual()
                    ? item.asText("").trim()
                    : structuredJsonSupport.readText(item, "module", "name");
            if (!StringUtils.hasText(moduleName) || selected.containsKey(moduleName)) {
                continue;
            }

            DirectorModuleRegistry.ModuleDefinition definition;
            try {
                definition = directorModuleRegistry.require(moduleName);
            } catch (Exception exception) {
                continue;
            }
            if (!definition.supportedStages().contains(stage)) {
                continue;
            }

            List<String> requestedFields = item.isTextual() ? List.of() : structuredJsonSupport.readList(item, "fields");
            List<String> resolvedFields = requestedFields.isEmpty()
                    ? definition.availableFields()
                    : definition.availableFields().stream()
                            .filter(requestedFields::contains)
                            .toList();
            if (resolvedFields.isEmpty()) {
                resolvedFields = definition.availableFields();
            }

            selected.put(moduleName, new DirectorDecisionPackAssembler.DirectorModuleSelection(
                    definition.moduleName(),
                    item.isTextual() ? definition.defaultWeight() : readPositiveDouble(item, definition.defaultWeight(), "weight", "priority"),
                    item.isTextual() ? definition.required() : structuredJsonSupport.readBoolean(item, definition.required(), "required"),
                    item.isTextual() ? definition.maxItems() : clampTopK(structuredJsonSupport.readInt(item, definition.maxItems(), "topK", "limit"), definition.maxItems()),
                    resolvedFields
            ));
        }

        if (selected.isEmpty()) {
            return selectModules(chapter, outline, stage, hasBackgroundContext);
        }
        if (!selected.containsKey("chapter_snapshot")) {
            DirectorModuleRegistry.ModuleDefinition definition = directorModuleRegistry.require("chapter_snapshot");
            Map<String, DirectorDecisionPackAssembler.DirectorModuleSelection> withRequired = new LinkedHashMap<>();
            withRequired.put("chapter_snapshot", new DirectorDecisionPackAssembler.DirectorModuleSelection(
                    definition.moduleName(),
                    definition.defaultWeight(),
                    definition.required(),
                    definition.maxItems(),
                    definition.availableFields()
            ));
            withRequired.putAll(selected);
            selected = withRequired;
        }

        List<DirectorDecisionPackAssembler.DirectorModuleSelection> result = new ArrayList<>(selected.values());
        int maxSelectedModules = getConfiguredInt("ai.director.max_selected_modules", 6);
        return result.size() <= maxSelectedModules ? result : new ArrayList<>(result.subList(0, maxSelectedModules));
    }

    private void addModuleSelection(
            List<DirectorDecisionPackAssembler.DirectorModuleSelection> selected,
            String moduleName,
            String stage,
            Double weightOverride,
            Integer topKOverride,
            List<String> fieldsOverride) {
        DirectorModuleRegistry.ModuleDefinition definition = directorModuleRegistry.require(moduleName);
        if (!definition.supportedStages().contains(stage)) {
            return;
        }
        List<String> fields = fieldsOverride == null || fieldsOverride.isEmpty()
                ? definition.availableFields()
                : definition.availableFields().stream().filter(fieldsOverride::contains).toList();
        if (fields.isEmpty()) {
            fields = definition.availableFields();
        }
        selected.add(new DirectorDecisionPackAssembler.DirectorModuleSelection(
                definition.moduleName(),
                weightOverride == null ? definition.defaultWeight() : weightOverride,
                definition.required(),
                topKOverride == null ? definition.maxItems() : clampTopK(topKOverride, definition.maxItems()),
                fields
        ));
    }

    private String determineStage(String writingMode, String currentContent, Outline outline) {
        if ("polish".equals(writingMode)) {
            return "polish";
        }

        int contentLength = currentContent == null ? 0 : currentContent.length();
        boolean hasTurningPoint = outline != null && StringUtils.hasText(outline.getTurningPoints());
        boolean hasExpectedEnding = outline != null && StringUtils.hasText(outline.getExpectedEnding());

        if (contentLength == 0) {
            return "opening";
        }
        if (contentLength < 500) {
            return "setup";
        }
        if (hasExpectedEnding && contentLength >= 2200) {
            return "convergence";
        }
        if (hasTurningPoint && contentLength >= 1200) {
            return "turning";
        }
        return "advancement";
    }

    private String normalizeWritingMode(String writingType, String currentContent) {
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

        return SUPPORTED_WRITING_MODES.contains(normalized)
                ? normalized
                : StringUtils.hasText(currentContent) ? "continue" : "draft";
    }

    private String normalizeReturnedWritingMode(String value, String fallback) {
        String normalized = StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
        return SUPPORTED_WRITING_MODES.contains(normalized) ? normalized : fallback;
    }

    private String normalizeStage(String value, String fallback) {
        String normalized = StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
        return SUPPORTED_STAGES.contains(normalized) ? normalized : fallback;
    }

    private boolean isDirectorEnabled() {
        String value = systemConfigService.getConfigValue("ai.director.enabled");
        return !StringUtils.hasText(value) || Boolean.parseBoolean(value.trim());
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

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private int clampTopK(int value, int maxItems) {
        return Math.max(1, Math.min(value, Math.max(1, maxItems)));
    }

    private int readPositiveInt(JsonNode root, int fallback, String... fieldNames) {
        int value = structuredJsonSupport.readInt(root, fallback, fieldNames);
        return value > 0 ? value : fallback;
    }

    private double readPositiveDouble(JsonNode root, double fallback, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = root.path(fieldName);
            if (node.isNumber()) {
                double value = node.asDouble(fallback);
                return value > 0 ? value : fallback;
            }
            if (!node.isMissingNode() && !node.isNull() && StringUtils.hasText(node.asText())) {
                try {
                    double value = Double.parseDouble(node.asText().trim());
                    return value > 0 ? value : fallback;
                } catch (NumberFormatException ignored) {
                    return fallback;
                }
            }
        }
        return fallback;
    }

    private List<String> mergeUnique(List<String> primary, List<String> secondary) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (primary != null) {
            primary.stream().filter(StringUtils::hasText).map(String::trim).forEach(values::add);
        }
        if (secondary != null) {
            secondary.stream().filter(StringUtils::hasText).map(String::trim).forEach(values::add);
        }
        return new ArrayList<>(values);
    }

    private String safe(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String compactError(String message) {
        if (!StringUtils.hasText(message)) {
            return "总导层生成失败，已退回启发式 fallback。";
        }
        String compact = message.replaceAll("\\s+", " ").trim();
        return compact.length() > 240 ? compact.substring(0, 240) + "..." : compact;
    }

    private AIDirectorDecisionVO toVO(AIDirectorDecision decision, String decisionSummary) {
        AIDirectorDecisionVO vo = new AIDirectorDecisionVO();
        vo.setId(decision.getId());
        vo.setProjectId(decision.getProjectId());
        vo.setChapterId(decision.getChapterId());
        vo.setSourceType(decision.getSourceType());
        vo.setEntryPoint(decision.getEntryPoint());
        vo.setStage(decision.getStage());
        vo.setWritingMode(decision.getWritingMode());
        vo.setTargetWordCount(decision.getTargetWordCount());
        vo.setDecisionSummary(decisionSummary);
        vo.setDecisionPack(readJson(decision.getDecisionPackJson()));
        vo.setSelectedProviderId(decision.getSelectedProviderId());
        vo.setSelectedModel(decision.getSelectedModel());
        vo.setStatus(decision.getStatus());
        vo.setErrorMessage(decision.getErrorMessage());
        vo.setCreateTime(decision.getCreateTime());
        return vo;
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

    private String extractDecisionSummary(String decisionPackJson) {
        JsonNode decisionPack = readJson(decisionPackJson);
        JsonNode summaryNode = decisionPack.path("decisionSummary");
        return summaryNode.isMissingNode() || summaryNode.isNull() ? "" : summaryNode.asText("");
    }

    private record ModelDecision(
            String stage,
            String writingMode,
            Integer targetWordCount,
            List<DirectorDecisionPackAssembler.DirectorModuleSelection> selectedModules,
            List<String> requiredFacts,
            List<String> prohibitedMoves,
            List<String> writerHints,
            String decisionSummary,
            List<AIProviderService.ToolCallTrace> toolTraces) {
    }
}
