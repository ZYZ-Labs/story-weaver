package com.storyweaver.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.domain.dto.NameSuggestionRequestDTO;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.vo.NameSuggestionVO;
import com.storyweaver.service.AIProviderService;
import com.storyweaver.service.NameSuggestionService;
import com.storyweaver.service.ProjectService;
import com.storyweaver.service.SystemConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class NameSuggestionServiceImpl implements NameSuggestionService {

    private static final int DEFAULT_COUNT = 5;
    private static final int MAX_COUNT = 8;
    private static final Pattern ORDER_PREFIX = Pattern.compile("^\\s*[-*•]?[\\d一二三四五六七八九十]+[.、)）:]?\\s*");

    private final ProjectService projectService;
    private final SystemConfigService systemConfigService;
    private final AIProviderService aiProviderService;
    private final ObjectMapper objectMapper;

    public NameSuggestionServiceImpl(
            ProjectService projectService,
            SystemConfigService systemConfigService,
            AIProviderService aiProviderService,
            ObjectMapper objectMapper) {
        this.projectService = projectService;
        this.systemConfigService = systemConfigService;
        this.aiProviderService = aiProviderService;
        this.objectMapper = objectMapper;
    }

    @Override
    public NameSuggestionVO generateSuggestions(Long projectId, Long userId, NameSuggestionRequestDTO requestDTO) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            throw new IllegalArgumentException("项目不存在或无权访问");
        }

        Project project = projectService.getById(projectId);
        if (project == null || Integer.valueOf(1).equals(project.getDeleted())) {
            throw new IllegalArgumentException("项目不存在或无权访问");
        }

        String entityType = normalizeEntityType(requestDTO.getEntityType());
        int count = resolveCount(requestDTO.getCount());
        AIProvider provider = resolveProvider();
        String modelName = resolveModelName(provider);

        String systemPrompt = buildSystemPrompt(entityType);
        String userPrompt = buildUserPrompt(project, requestDTO, entityType, count);

        String rawResponse = aiProviderService.generateText(
                provider,
                modelName,
                systemPrompt,
                userPrompt,
                0.45,
                320
        );

        List<String> suggestions = extractSuggestions(rawResponse, count);
        if (suggestions.isEmpty()) {
            throw new IllegalStateException("模型没有返回可用名称，请稍后重试");
        }

        return new NameSuggestionVO(suggestions, provider.getName(), modelName);
    }

    private String normalizeEntityType(String entityType) {
        String normalized = entityType == null ? "" : entityType.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "chapter", "character" -> normalized;
            default -> throw new IllegalArgumentException("暂不支持该类型的命名生成");
        };
    }

    private int resolveCount(Integer count) {
        if (count == null || count <= 0) {
            return DEFAULT_COUNT;
        }
        return Math.min(count, MAX_COUNT);
    }

    private AIProvider resolveProvider() {
        Long providerId = parseLong(systemConfigService.getConfigValue("naming_ai_provider_id"));
        if (providerId != null) {
            AIProvider provider = aiProviderService.getById(providerId);
            if (provider != null && !Integer.valueOf(1).equals(provider.getDeleted()) && Integer.valueOf(1).equals(provider.getEnabled())) {
                return provider;
            }
        }

        Long defaultProviderId = parseLong(systemConfigService.getConfigValue("default_ai_provider_id"));
        if (defaultProviderId != null) {
            AIProvider provider = aiProviderService.getById(defaultProviderId);
            if (provider != null && !Integer.valueOf(1).equals(provider.getDeleted()) && Integer.valueOf(1).equals(provider.getEnabled())) {
                return provider;
            }
        }

        return aiProviderService.listProviders().stream()
                .filter(item -> Integer.valueOf(1).equals(item.getEnabled()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("当前没有可用的模型服务，请先在模型服务页启用一个 Provider"));
    }

    private String resolveModelName(AIProvider provider) {
        String configured = systemConfigService.getConfigValue("naming_ai_model");
        if (StringUtils.hasText(configured)) {
            return configured.trim();
        }
        if (StringUtils.hasText(provider.getModelName())) {
            return provider.getModelName().trim();
        }
        throw new IllegalStateException("命名模型未配置，请先在系统设置中指定命名模型");
    }

    private String buildSystemPrompt(String entityType) {
        String promptKey = "chapter".equals(entityType) ? "prompt.naming.chapter" : "prompt.naming.character";
        String customPrompt = systemConfigService.getConfigValue(promptKey);
        String objectLabel = "chapter".equals(entityType) ? "章节标题" : "人物名称";

        return """
                你是一名擅长中文长篇小说命名的编辑助手。
                现在需要为%s生成多个可选名称。
                风格要求：%s
                输出要求：
                1. 只返回严格 JSON，不要解释，不要 Markdown。
                2. 格式必须是 {"suggestions":["名称1","名称2","名称3"]}。
                3. 每个名称都要简洁、易读、适合小说语境。
                """.formatted(objectLabel, StringUtils.hasText(customPrompt) ? customPrompt.trim() : "符合项目风格，避免太长和太口号化");
    }

    private String buildUserPrompt(Project project, NameSuggestionRequestDTO requestDTO, String entityType, int count) {
        String objectLabel = "chapter".equals(entityType) ? "章节标题" : "人物名称";
        StringBuilder builder = new StringBuilder();
        builder.append("请生成 ").append(count).append(" 个").append(objectLabel).append("候选。\n");
        builder.append("项目名：").append(safe(project.getName())).append('\n');
        if (StringUtils.hasText(project.getGenre())) {
            builder.append("项目题材：").append(project.getGenre().trim()).append('\n');
        }
        if (StringUtils.hasText(project.getDescription())) {
            builder.append("项目简介：").append(project.getDescription().trim()).append('\n');
        }
        if (StringUtils.hasText(requestDTO.getBrief())) {
            builder.append("命名依据：").append(requestDTO.getBrief().trim()).append('\n');
        }
        if (StringUtils.hasText(requestDTO.getExtraRequirements())) {
            builder.append("额外要求：").append(requestDTO.getExtraRequirements().trim()).append('\n');
        }
        builder.append("请只输出 JSON。");
        return builder.toString();
    }

    private List<String> extractSuggestions(String rawResponse, int count) {
        Set<String> suggestions = new LinkedHashSet<>();

        addJsonSuggestions(suggestions, rawResponse);
        if (suggestions.isEmpty()) {
            addPlainTextSuggestions(suggestions, rawResponse);
        }

        return suggestions.stream().limit(count).toList();
    }

    private void addJsonSuggestions(Set<String> suggestions, String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            return;
        }

        String normalized = stripCodeFence(rawResponse);
        try {
            JsonNode root = objectMapper.readTree(normalized);
            if (root.isObject() && root.path("suggestions").isArray()) {
                for (JsonNode item : root.path("suggestions")) {
                    addSuggestion(suggestions, item.asText());
                }
                return;
            }

            if (root.isArray()) {
                for (JsonNode item : root) {
                    addSuggestion(suggestions, item.asText());
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void addPlainTextSuggestions(Set<String> suggestions, String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            return;
        }

        String normalized = stripCodeFence(rawResponse)
                .replace("\r", "\n")
                .replace("，", "\n")
                .replace(",", "\n")
                .replace("；", "\n")
                .replace(";", "\n");

        for (String line : normalized.split("\n")) {
            addSuggestion(suggestions, ORDER_PREFIX.matcher(line).replaceFirst(""));
        }
    }

    private String stripCodeFence(String value) {
        return value
                .replace("```json", "")
                .replace("```JSON", "")
                .replace("```", "")
                .trim();
    }

    private void addSuggestion(Set<String> suggestions, String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return;
        }

        String normalized = candidate.trim()
                .replace("\"", "")
                .replace("“", "")
                .replace("”", "")
                .replace("'", "")
                .replace("【", "")
                .replace("】", "")
                .trim();

        if (!StringUtils.hasText(normalized) || normalized.length() > 30) {
            return;
        }
        if (normalized.startsWith("{") || normalized.startsWith("[")) {
            return;
        }
        suggestions.add(normalized);
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

    private String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "未命名项目";
    }
}
