package com.storyweaver.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.storyweaver.ai.application.support.StructuredJsonSupport;
import com.storyweaver.domain.dto.CharacterAttributeSuggestionRequestDTO;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.vo.CharacterAttributeSuggestionVO;
import com.storyweaver.service.AIModelRoutingService;
import com.storyweaver.service.AIProviderService;
import com.storyweaver.service.CharacterAttributeSuggestionService;
import com.storyweaver.service.ProjectService;
import com.storyweaver.service.SystemConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CharacterAttributeSuggestionServiceImpl implements CharacterAttributeSuggestionService {

    private final ProjectService projectService;
    private final SystemConfigService systemConfigService;
    private final AIProviderService aiProviderService;
    private final AIModelRoutingService aiModelRoutingService;
    private final StructuredJsonSupport structuredJsonSupport;

    public CharacterAttributeSuggestionServiceImpl(
            ProjectService projectService,
            SystemConfigService systemConfigService,
            AIProviderService aiProviderService,
            AIModelRoutingService aiModelRoutingService,
            StructuredJsonSupport structuredJsonSupport) {
        this.projectService = projectService;
        this.systemConfigService = systemConfigService;
        this.aiProviderService = aiProviderService;
        this.aiModelRoutingService = aiModelRoutingService;
        this.structuredJsonSupport = structuredJsonSupport;
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

        AIModelRoutingService.ResolvedModelSelection selection = aiModelRoutingService.resolve(null, null, "naming");
        AIProvider provider = selection.provider();
        String modelName = selection.model();

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
        JsonNode root = structuredJsonSupport.readRoot(
                rawResponse,
                "模型没有返回可用的人物属性内容",
                "人物属性生成结果无法解析，请稍后重试"
        );
        CharacterAttributeSuggestionVO result = new CharacterAttributeSuggestionVO();
        result.setAge(structuredJsonSupport.readText(root, "age", "年龄"));
        result.setGender(structuredJsonSupport.readText(root, "gender", "性别"));
        result.setIdentity(structuredJsonSupport.readText(root, "identity", "身份", "职业", "角色定位"));
        result.setCamp(structuredJsonSupport.readText(root, "camp", "阵营"));
        result.setGoal(structuredJsonSupport.readText(root, "goal", "目标"));
        result.setBackground(structuredJsonSupport.readText(root, "background", "背景", "出身"));
        result.setAppearance(structuredJsonSupport.readText(root, "appearance", "外貌"));
        result.setTraits(structuredJsonSupport.readList(root, "traits", "特性", "性格", "性格特性"));
        result.setTalents(structuredJsonSupport.readList(root, "talents", "天赋"));
        result.setSkills(structuredJsonSupport.readList(root, "skills", "技能", "特长", "能力"));
        result.setWeaknesses(structuredJsonSupport.readList(root, "weaknesses", "弱点", "缺点"));
        result.setEquipment(structuredJsonSupport.readList(root, "equipment", "装备"));
        result.setTags(structuredJsonSupport.readList(root, "tags", "标签"));
        result.setRelations(structuredJsonSupport.readList(root, "relations", "关系", "人际关系"));
        result.setNotes(structuredJsonSupport.readText(root, "notes", "备注", "秘密", "补充"));
        return result;
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "未命名项目";
    }
}
