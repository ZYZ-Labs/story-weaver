package com.storyweaver.item.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.storyweaver.ai.application.support.StructuredJsonSupport;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.vo.WorldSettingVO;
import com.storyweaver.item.domain.support.ItemCatalogRules;
import com.storyweaver.item.web.request.ItemGenerationRequest;
import com.storyweaver.item.web.response.ItemGenerationResultResponse;
import com.storyweaver.repository.ProjectCharacterMapper;
import com.storyweaver.service.AIModelRoutingService;
import com.storyweaver.service.AIProviderService;
import com.storyweaver.service.CharacterService;
import com.storyweaver.service.ProjectService;
import com.storyweaver.service.SystemConfigService;
import com.storyweaver.service.WorldSettingService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ItemGenerationApplicationService {

    private final ProjectService projectService;
    private final CharacterService characterService;
    private final WorldSettingService worldSettingService;
    private final SystemConfigService systemConfigService;
    private final AIProviderService aiProviderService;
    private final AIModelRoutingService aiModelRoutingService;
    private final StructuredJsonSupport structuredJsonSupport;
    private final ProjectCharacterMapper projectCharacterMapper;

    public ItemGenerationApplicationService(
            ProjectService projectService,
            CharacterService characterService,
            WorldSettingService worldSettingService,
            SystemConfigService systemConfigService,
            AIProviderService aiProviderService,
            AIModelRoutingService aiModelRoutingService,
            StructuredJsonSupport structuredJsonSupport,
            ProjectCharacterMapper projectCharacterMapper) {
        this.projectService = projectService;
        this.characterService = characterService;
        this.worldSettingService = worldSettingService;
        this.systemConfigService = systemConfigService;
        this.aiProviderService = aiProviderService;
        this.aiModelRoutingService = aiModelRoutingService;
        this.structuredJsonSupport = structuredJsonSupport;
        this.projectCharacterMapper = projectCharacterMapper;
    }

    public ItemGenerationResultResponse generate(Long projectId, Long characterId, Long userId, ItemGenerationRequest request) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            throw new IllegalArgumentException("项目不存在或无权访问");
        }

        Project project = projectService.getById(projectId);
        if (project == null || Integer.valueOf(1).equals(project.getDeleted())) {
            throw new IllegalArgumentException("项目不存在或无权访问");
        }

        Character character = null;
        if (characterId != null) {
            character = characterService.getCharacterWithAuth(characterId, userId);
            if (character == null) {
                throw new IllegalArgumentException("角色不存在或无权访问");
            }
            boolean linked = projectCharacterMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.storyweaver.domain.entity.ProjectCharacterLink>()
                    .eq(com.storyweaver.domain.entity.ProjectCharacterLink::getProjectId, projectId)
                    .eq(com.storyweaver.domain.entity.ProjectCharacterLink::getCharacterId, characterId)) > 0;
            if (!linked) {
                throw new IllegalArgumentException("角色未关联到当前项目");
            }
        }

        AIModelRoutingService.ResolvedModelSelection selection = aiModelRoutingService.resolve(
                request.getSelectedProviderId(),
                request.getSelectedModel(),
                "item"
        );
        AIProvider provider = selection.provider();
        String model = selection.model();

        String rawResponse = aiProviderService.generateText(
                provider,
                model,
                buildSystemPrompt(),
                buildUserPrompt(project, character, request),
                0.45,
                1200
        );

        ItemGenerationResultResponse result = parseResponse(rawResponse);
        result.setProviderName(provider.getName());
        result.setModelName(model);
        return result;
    }

    private String buildSystemPrompt() {
        String customPrompt = systemConfigService.getConfigValue("prompt.item_generation");
        return """
                你是一名中文小说与游戏道具设计助手。
                你的任务是输出可直接进入项目物品库的结构化物品列表。
                风格要求：%s
                输出要求：
                1. 只返回严格 JSON，不要解释，不要 Markdown。
                2. 顶层结构必须是 {"items":[...]}。
                3. items 中每个物品至少包含：
                   {"name":"","description":"","category":"prop","rarity":"common","stackable":false,"maxStack":1,"usable":false,"equippable":false,"slotType":"misc","itemValue":0,"weight":0,"attributes":{},"effect":{},"tags":[],"suggestedQuantity":1}
                4. category 只能是 prop, consumable, equipment, material, quest。
                5. rarity 只能是 common, uncommon, rare, epic, legendary, artifact。
                6. slotType 只能是 weapon, head, body, accessory, offhand, consumable, misc。
                7. attributes 和 effect 必须是 JSON object；tags 必须是字符串数组。
                8. 物品风格要适合中文长篇创作场景，具备叙事用途，不要生成纯数值堆砌内容。
                """.formatted(
                StringUtils.hasText(customPrompt)
                        ? customPrompt.trim()
                        : "优先生成可用于剧情推进、战斗、探索、治疗或任务推进的物品，名称与说明要有叙事感"
        );
    }

    private String buildUserPrompt(Project project, Character character, ItemGenerationRequest request) {
        List<WorldSettingVO> worldSettings = worldSettingService.getWorldSettingsByProjectId(project.getId()).stream()
                .limit(4)
                .toList();

        StringBuilder builder = new StringBuilder();
        builder.append("请为当前项目生成一组结构化物品。\n");
        builder.append("项目名称：").append(safe(project.getName(), "未命名项目")).append('\n');
        if (StringUtils.hasText(project.getGenre())) {
            builder.append("项目题材：").append(project.getGenre().trim()).append('\n');
        }
        if (StringUtils.hasText(project.getDescription())) {
            builder.append("项目简介：").append(project.getDescription().trim()).append('\n');
        }
        if (!worldSettings.isEmpty()) {
            builder.append("世界观摘要：\n");
            for (WorldSettingVO item : worldSettings) {
                builder.append("- ")
                        .append(safe(item.getName(), safe(item.getTitle(), "未命名设定")))
                        .append("：")
                        .append(safe(item.getDescription(), safe(item.getContent(), "暂无说明")))
                        .append('\n');
            }
        }
        if (character != null) {
            builder.append("角色名称：").append(safe(character.getName(), "未命名角色")).append('\n');
            if (StringUtils.hasText(character.getDescription())) {
                builder.append("角色描述：").append(character.getDescription().trim()).append('\n');
            }
            if (StringUtils.hasText(character.getAttributes())) {
                builder.append("角色属性 JSON：").append(character.getAttributes().trim()).append('\n');
            }
        }
        builder.append("期望分类：")
                .append(StringUtils.hasText(request.getCategory()) ? ItemCatalogRules.normalizeCategory(request.getCategory()) : "mixed")
                .append('\n');
        builder.append("期望数量：").append(request.getCount() == null ? 3 : Math.max(1, request.getCount())).append('\n');
        builder.append("核心需求：").append(safe(request.getPrompt(), "请生成适合当前项目的道具、药品、装备等物品。")).append('\n');
        if (StringUtils.hasText(request.getConstraints())) {
            builder.append("附加约束：").append(request.getConstraints().trim()).append('\n');
        }
        builder.append("请只返回 JSON。");
        return builder.toString();
    }

    private ItemGenerationResultResponse parseResponse(String rawResponse) {
        JsonNode root = structuredJsonSupport.readRoot(
                rawResponse,
                "模型没有返回可用的物品内容",
                "物品生成结果无法解析，请稍后重试"
        );
        JsonNode itemsNode = root.isArray() ? root : root.path("items");
        if (!itemsNode.isArray() || itemsNode.isEmpty()) {
            throw new IllegalStateException("物品生成结果为空，请调整提示词后重试");
        }

        ItemGenerationResultResponse result = new ItemGenerationResultResponse();
        for (JsonNode itemNode : itemsNode) {
            ItemGenerationResultResponse.GeneratedItemResponse item = new ItemGenerationResultResponse.GeneratedItemResponse();
            item.setName(structuredJsonSupport.readText(itemNode, "name", "名称"));
            if (!StringUtils.hasText(item.getName())) {
                continue;
            }
            item.setDescription(structuredJsonSupport.readText(itemNode, "description", "描述"));
            item.setCategory(ItemCatalogRules.normalizeCategory(structuredJsonSupport.readText(itemNode, "category", "类型", "分类")));
            item.setRarity(ItemCatalogRules.normalizeRarity(structuredJsonSupport.readText(itemNode, "rarity", "稀有度")));
            item.setStackable(structuredJsonSupport.readBoolean(itemNode, false, "stackable", "可堆叠"));
            item.setMaxStack(item.isStackable()
                    ? Math.max(2, structuredJsonSupport.readInt(itemNode, 20, "maxStack", "最大堆叠"))
                    : 1);
            item.setUsable(structuredJsonSupport.readBoolean(itemNode, "consumable".equals(item.getCategory()), "usable", "可使用"));
            item.setEquippable(structuredJsonSupport.readBoolean(itemNode, "equipment".equals(item.getCategory()), "equippable", "可装备"));
            item.setSlotType(ItemCatalogRules.normalizeSlotType(structuredJsonSupport.readText(itemNode, "slotType", "部位", "slot")));
            item.setItemValue(Math.max(0, structuredJsonSupport.readInt(itemNode, 0, "itemValue", "value", "价值")));
            item.setWeight(Math.max(0, structuredJsonSupport.readInt(itemNode, 0, "weight", "重量")));
            item.setAttributesJson(structuredJsonSupport.normalizeJsonObject(itemNode, "attributes", "attributesJson", "属性"));
            item.setEffectJson(structuredJsonSupport.normalizeJsonObject(itemNode, "effect", "effectJson", "效果"));
            item.setTags(ItemCatalogRules.normalizeTags(String.join(", ", structuredJsonSupport.readList(itemNode, "tags", "标签"))));
            item.setSourceType("ai");
            item.setSuggestedQuantity(Math.max(1, structuredJsonSupport.readInt(itemNode, 1, "suggestedQuantity", "quantity", "建议数量")));
            result.getItems().add(item);
        }

        if (result.getItems().isEmpty()) {
            throw new IllegalStateException("物品生成结果为空，请调整提示词后重试");
        }
        return result;
    }

    private String safe(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
