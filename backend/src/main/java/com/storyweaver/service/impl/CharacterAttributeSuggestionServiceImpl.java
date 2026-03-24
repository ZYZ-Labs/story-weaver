package com.storyweaver.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.domain.dto.CharacterAttributeSuggestionRequestDTO;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.vo.CharacterAttributeSuggestionVO;
import com.storyweaver.service.AIProviderService;
import com.storyweaver.service.CharacterAttributeSuggestionService;
import com.storyweaver.service.ProjectService;
import com.storyweaver.service.SystemConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CharacterAttributeSuggestionServiceImpl implements CharacterAttributeSuggestionService {

    private static final Set<String> SPLIT_SYMBOLS = Set.of("，", ",", "、", ";", "；", "/", "|", "\n");

    private final ProjectService projectService;
    private final SystemConfigService systemConfigService;
    private final AIProviderService aiProviderService;
    private final ObjectMapper objectMapper;

    public CharacterAttributeSuggestionServiceImpl(
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
    public CharacterAttributeSuggestionVO generateAttributes(
            Long projectId,
            Long userId,
            CharacterAttributeSuggestionRequestDTO requestDTO) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            throw new IllegalArgumentException("项目不存在或无权访问");
        }

        Project project = projectService.getById(projectId);
        if (project == null || Integer.valueOf(1).equals(project.getDeleted())) {
            throw new IllegalArgumentException("项目不存在或无权访问");
        }

        AIProvider provider = resolveProvider();
        String modelName = resolveModelName(provider);

        String rawResponse = aiProviderService.generateText(
                provider,
                modelName,
                buildSystemPrompt(),
                buildUserPrompt(project, requestDTO),
                0.35,
                640
        );

        CharacterAttributeSuggestionVO result = parseResponse(rawResponse);
        result.setProviderName(provider.getName());
        result.setModelName(modelName);
        return result;
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
                .orElseThrow(() -> new IllegalStateException("当前没有可用的模型服务，请先在模型服务页面启用一个 Provider"));
    }

    private String resolveModelName(AIProvider provider) {
        String configured = systemConfigService.getConfigValue("naming_ai_model");
        if (StringUtils.hasText(configured)) {
            return configured.trim();
        }
        if (StringUtils.hasText(provider.getModelName())) {
            return provider.getModelName().trim();
        }
        throw new IllegalStateException("轻量生成模型未配置，请先在系统设置中指定命名模型");
    }

    private String buildSystemPrompt() {
        String customPrompt = systemConfigService.getConfigValue("prompt.character_attributes");
        return """
                你是一名中文小说角色设计助手。
                请根据用户提供的项目风格和人物描述，输出一份适合小说创作使用的人物属性草案。
                风格要求：%s
                输出要求：
                1. 只返回严格 JSON，不要解释，不要 Markdown。
                2. JSON 顶层必须包含这些 key：
                {"age":"","gender":"","identity":"","camp":"","goal":"","background":"","appearance":"","traits":[],"talents":[],"skills":[],"weaknesses":[],"equipment":[],"tags":[],"relations":[],"notes":""}
                3. traits、talents、skills、weaknesses、equipment、tags、relations 必须是字符串数组。
                4. 没有把握的字段可以留空，但尽量给出能直接用于填表的内容。
                """.formatted(
                StringUtils.hasText(customPrompt)
                        ? customPrompt.trim()
                        : "贴合项目题材与人物气质，优先补齐技能、特性、天赋、弱点、装备和关系线索"
        );
    }

    private String buildUserPrompt(Project project, CharacterAttributeSuggestionRequestDTO requestDTO) {
        StringBuilder builder = new StringBuilder();
        builder.append("请为一个小说角色生成结构化属性草案。\n");
        builder.append("项目名称：").append(safe(project.getName())).append('\n');
        if (StringUtils.hasText(project.getGenre())) {
            builder.append("项目题材：").append(project.getGenre().trim()).append('\n');
        }
        if (StringUtils.hasText(project.getDescription())) {
            builder.append("项目简介：").append(project.getDescription().trim()).append('\n');
        }
        if (StringUtils.hasText(requestDTO.getName())) {
            builder.append("角色名称：").append(requestDTO.getName().trim()).append('\n');
        }
        if (StringUtils.hasText(requestDTO.getDescription())) {
            builder.append("角色描述：").append(requestDTO.getDescription().trim()).append('\n');
        } else {
            builder.append("角色描述：请结合当前项目风格，生成一个可供填写的基础人物设定。\n");
        }
        if (StringUtils.hasText(requestDTO.getExtraRequirements())) {
            builder.append("额外要求：").append(requestDTO.getExtraRequirements().trim()).append('\n');
        }
        builder.append("请只输出 JSON。");
        return builder.toString();
    }

    private CharacterAttributeSuggestionVO parseResponse(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            throw new IllegalStateException("模型没有返回可用的人物属性内容");
        }

        String normalized = rawResponse
                .replace("```json", "")
                .replace("```JSON", "")
                .replace("```", "")
                .trim();

        try {
            JsonNode root = objectMapper.readTree(normalized);
            CharacterAttributeSuggestionVO result = new CharacterAttributeSuggestionVO();
            result.setAge(readText(root, "age", "年龄"));
            result.setGender(readText(root, "gender", "性别"));
            result.setIdentity(readText(root, "identity", "身份", "职业", "角色定位"));
            result.setCamp(readText(root, "camp", "阵营"));
            result.setGoal(readText(root, "goal", "目标"));
            result.setBackground(readText(root, "background", "背景", "出身"));
            result.setAppearance(readText(root, "appearance", "外貌"));
            result.setTraits(readList(root, "traits", "特性", "性格", "性格特性"));
            result.setTalents(readList(root, "talents", "天赋"));
            result.setSkills(readList(root, "skills", "技能", "特长", "能力"));
            result.setWeaknesses(readList(root, "weaknesses", "弱点", "缺点"));
            result.setEquipment(readList(root, "equipment", "装备"));
            result.setTags(readList(root, "tags", "标签"));
            result.setRelations(readList(root, "relations", "关系", "人际关系"));
            result.setNotes(readText(root, "notes", "备注", "秘密", "补充"));
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("人物属性生成结果无法解析，请稍后重试");
        }
    }

    private String readText(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = root.path(fieldName);
            if (!node.isMissingNode() && !node.isNull()) {
                String value = node.asText("").trim();
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
        }
        return "";
    }

    private List<String> readList(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = root.path(fieldName);
            if (node.isArray()) {
                LinkedHashSet<String> values = new LinkedHashSet<>();
                for (JsonNode item : node) {
                    String value = item.asText("").trim();
                    if (StringUtils.hasText(value)) {
                        values.add(value);
                    }
                }
                return new ArrayList<>(values);
            }
            if (!node.isMissingNode() && !node.isNull() && StringUtils.hasText(node.asText(""))) {
                return splitValues(node.asText(""));
            }
        }
        return new ArrayList<>();
    }

    private List<String> splitValues(String value) {
        String normalized = value;
        for (String symbol : SPLIT_SYMBOLS) {
            normalized = normalized.replace(symbol, "\n");
        }

        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String item : normalized.split("\n")) {
            String candidate = item.trim();
            if (StringUtils.hasText(candidate)) {
                values.add(candidate);
            }
        }
        return new ArrayList<>(values);
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
