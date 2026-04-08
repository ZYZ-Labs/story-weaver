package com.storyweaver.ai.director.application.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.domain.dto.AIDirectorDecisionRequestDTO;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.domain.entity.Causality;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.KnowledgeDocument;
import com.storyweaver.domain.entity.Outline;
import com.storyweaver.domain.entity.Plot;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.vo.AIWritingChatParticipationVO;
import com.storyweaver.domain.vo.WorldSettingVO;
import com.storyweaver.item.domain.entity.CharacterInventoryItem;
import com.storyweaver.item.domain.entity.ItemDefinition;
import com.storyweaver.item.infrastructure.persistence.mapper.CharacterInventoryItemMapper;
import com.storyweaver.item.infrastructure.persistence.mapper.ItemMapper;
import com.storyweaver.service.AIProviderService;
import com.storyweaver.service.AIWritingChatService;
import com.storyweaver.service.CausalityService;
import com.storyweaver.service.CharacterService;
import com.storyweaver.service.KnowledgeDocumentService;
import com.storyweaver.service.PlotService;
import com.storyweaver.service.WorldSettingService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DirectorToolExecutor {

    private static final int DEFAULT_TOP_K = 3;
    private static final int MAX_TOOL_ITEMS = 6;
    private static final int MAX_INVENTORY_ITEMS_PER_CHARACTER = 4;

    private final ObjectMapper objectMapper;
    private final PlotService plotService;
    private final CausalityService causalityService;
    private final WorldSettingService worldSettingService;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final CharacterService characterService;
    private final AIWritingChatService aiWritingChatService;
    private final CharacterInventoryItemMapper characterInventoryItemMapper;
    private final ItemMapper itemMapper;
    private final List<DirectorToolDefinition> definitions;

    public DirectorToolExecutor(
            ObjectMapper objectMapper,
            PlotService plotService,
            CausalityService causalityService,
            WorldSettingService worldSettingService,
            KnowledgeDocumentService knowledgeDocumentService,
            CharacterService characterService,
            AIWritingChatService aiWritingChatService,
            CharacterInventoryItemMapper characterInventoryItemMapper,
            ItemMapper itemMapper) {
        this.objectMapper = objectMapper;
        this.plotService = plotService;
        this.causalityService = causalityService;
        this.worldSettingService = worldSettingService;
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.characterService = characterService;
        this.aiWritingChatService = aiWritingChatService;
        this.characterInventoryItemMapper = characterInventoryItemMapper;
        this.itemMapper = itemMapper;
        this.definitions = List.of(
                new DirectorToolDefinition(
                        "getChapterSnapshot",
                        "读取当前章节标题、字数、必出人物和正文摘要。",
                        """
                        {
                          "type":"object",
                          "properties":{
                            "chapterId":{"type":"integer"},
                            "includeCurrentContent":{"type":"boolean"}
                          },
                          "required":["chapterId"],
                          "additionalProperties":false
                        }
                        """
                ),
                new DirectorToolDefinition(
                        "getOutlineContext",
                        "读取当前章节关联的大纲摘要、目标、冲突和转折。",
                        """
                        {
                          "type":"object",
                          "properties":{
                            "projectId":{"type":"integer"},
                            "chapterId":{"type":"integer"}
                          },
                          "required":["projectId","chapterId"],
                          "additionalProperties":false
                        }
                        """
                ),
                new DirectorToolDefinition(
                        "getPlotCandidates",
                        "读取与当前章节相关的剧情节点，可按 query 或 topK 限制返回。",
                        """
                        {
                          "type":"object",
                          "properties":{
                            "projectId":{"type":"integer"},
                            "chapterId":{"type":"integer"},
                            "query":{"type":"string"},
                            "topK":{"type":"integer","minimum":1,"maximum":6}
                          },
                          "required":["projectId","chapterId"],
                          "additionalProperties":false
                        }
                        """
                ),
                new DirectorToolDefinition(
                        "getCausalityCandidates",
                        "读取与当前章节相关的因果链，可按 query 或 topK 限制返回。",
                        """
                        {
                          "type":"object",
                          "properties":{
                            "projectId":{"type":"integer"},
                            "query":{"type":"string"},
                            "topK":{"type":"integer","minimum":1,"maximum":6}
                          },
                          "required":["projectId"],
                          "additionalProperties":false
                        }
                        """
                ),
                new DirectorToolDefinition(
                        "getWorldSettingFacts",
                        "读取当前项目的世界观设定摘要。",
                        """
                        {
                          "type":"object",
                          "properties":{
                            "projectId":{"type":"integer"},
                            "topK":{"type":"integer","minimum":1,"maximum":6}
                          },
                          "required":["projectId"],
                          "additionalProperties":false
                        }
                        """
                ),
                new DirectorToolDefinition(
                        "getRequiredCharacterState",
                        "读取本章必出人物的状态、设定和项目内角色定位。",
                        """
                        {
                          "type":"object",
                          "properties":{
                            "projectId":{"type":"integer"},
                            "chapterId":{"type":"integer"},
                            "topK":{"type":"integer","minimum":1,"maximum":6}
                          },
                          "required":["projectId","chapterId"],
                          "additionalProperties":false
                        }
                        """
                ),
                new DirectorToolDefinition(
                        "getRequiredCharacterInventory",
                        "读取本章必出人物的背包摘要。",
                        """
                        {
                          "type":"object",
                          "properties":{
                            "projectId":{"type":"integer"},
                            "chapterId":{"type":"integer"},
                            "topK":{"type":"integer","minimum":1,"maximum":6}
                          },
                          "required":["projectId","chapterId"],
                          "additionalProperties":false
                        }
                        """
                ),
                new DirectorToolDefinition(
                        "searchKnowledgeDocuments",
                        "检索与当前章节最相关的知识片段。",
                        """
                        {
                          "type":"object",
                          "properties":{
                            "projectId":{"type":"integer"},
                            "queryText":{"type":"string"},
                            "topK":{"type":"integer","minimum":1,"maximum":6}
                          },
                          "required":["projectId"],
                          "additionalProperties":false
                        }
                        """
                ),
                new DirectorToolDefinition(
                        "getChatBackgroundSummary",
                        "读取当前章节背景聊天中稳定可复用的设定与硬约束摘要。",
                        """
                        {
                          "type":"object",
                          "properties":{
                            "chapterId":{"type":"integer"}
                          },
                          "required":["chapterId"],
                          "additionalProperties":false
                        }
                        """
                )
        );
    }

    public List<AIProviderService.ToolDefinition> listToolDefinitions() {
        return definitions.stream()
                .map(DirectorToolDefinition::toProviderDefinition)
                .toList();
    }

    public String execute(DirectorToolContext context, AIProviderService.ToolCallRequest request) {
        JsonNode arguments = readArguments(request.argumentsJson());
        Object result = switch (request.name()) {
            case "getChapterSnapshot" -> getChapterSnapshot(context, arguments);
            case "getOutlineContext" -> getOutlineContext(context, arguments);
            case "getPlotCandidates" -> getPlotCandidates(context, arguments);
            case "getCausalityCandidates" -> getCausalityCandidates(context, arguments);
            case "getWorldSettingFacts" -> getWorldSettingFacts(context, arguments);
            case "getRequiredCharacterState" -> getRequiredCharacterState(context, arguments);
            case "getRequiredCharacterInventory" -> getRequiredCharacterInventory(context, arguments);
            case "searchKnowledgeDocuments" -> searchKnowledgeDocuments(context, arguments);
            case "getChatBackgroundSummary" -> getChatBackgroundSummary(context, arguments);
            default -> Map.of("error", "unknown_tool", "name", request.name());
        };
        return writeJson(result);
    }

    private Object getChapterSnapshot(DirectorToolContext context, JsonNode arguments) {
        Chapter chapter = context.chapter();
        String currentContent = normalizeText(
                StringUtils.hasText(context.requestDTO().getCurrentContent())
                        ? context.requestDTO().getCurrentContent()
                        : chapter.getContent()
        );
        boolean includeCurrentContent = readBoolean(arguments, false, "includeCurrentContent");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chapterId", chapter.getId());
        payload.put("projectId", chapter.getProjectId());
        payload.put("title", safe(chapter.getTitle(), "未命名章节"));
        payload.put("orderNum", chapter.getOrderNum());
        payload.put("wordCount", chapter.getWordCount() == null ? currentContent.length() : chapter.getWordCount());
        payload.put("requiredCharacterIds", chapter.getRequiredCharacterIds() == null ? List.of() : chapter.getRequiredCharacterIds());
        payload.put("requiredCharacterNames", chapter.getRequiredCharacterNames() == null ? List.of() : chapter.getRequiredCharacterNames());
        payload.put("currentContentLength", currentContent.length());
        payload.put("currentContentSummary", summarize(currentContent, includeCurrentContent ? 900 : 280));
        payload.put("userInstruction", normalizeText(context.requestDTO().getUserInstruction()));
        payload.put("writingType", normalizeText(context.requestDTO().getWritingType()));
        return payload;
    }

    private Object getOutlineContext(DirectorToolContext context, JsonNode arguments) {
        Outline outline = context.outline();
        if (outline == null) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("projectId", context.project() == null ? null : context.project().getId());
            payload.put("chapterId", context.chapter().getId());
            payload.put("available", false);
            payload.put("message", "当前章节暂无关联大纲");
            return payload;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectId", outline.getProjectId());
        payload.put("chapterId", outline.getChapterId());
        payload.put("outlineId", outline.getId());
        payload.put("title", safe(outline.getTitle(), "未命名大纲"));
        payload.put("summary", summarize(outline.getSummary(), 240));
        payload.put("stageGoal", normalizeText(outline.getStageGoal()));
        payload.put("keyConflict", normalizeText(outline.getKeyConflict()));
        payload.put("turningPoints", normalizeText(outline.getTurningPoints()));
        payload.put("expectedEnding", normalizeText(outline.getExpectedEnding()));
        payload.put("focusCharacterNames", outline.getFocusCharacterNames() == null ? List.of() : outline.getFocusCharacterNames());
        payload.put("relatedPlotIds", outline.getRelatedPlotIdList() == null ? List.of() : outline.getRelatedPlotIdList());
        payload.put("relatedCausalityIds", outline.getRelatedCausalityIdList() == null ? List.of() : outline.getRelatedCausalityIdList());
        payload.put("available", true);
        return payload;
    }

    private Object getPlotCandidates(DirectorToolContext context, JsonNode arguments) {
        if (context.project() == null) {
            return Map.of("plots", List.of(), "available", false);
        }

        int topK = readTopK(arguments);
        String query = readText(arguments, "query", "queryText");
        Set<Long> relatedPlotIds = context.outline() == null || context.outline().getRelatedPlotIdList() == null
                ? Set.of()
                : new LinkedHashSet<>(context.outline().getRelatedPlotIdList());

        List<Plot> plots;
        if (StringUtils.hasText(query)) {
            plots = plotService.searchPlots(context.project().getId(), query.trim());
        } else {
            List<Plot> chapterPlots = plotService.getChapterPlots(context.chapter().getId());
            List<Plot> projectPlots = plotService.getProjectPlots(context.project().getId());
            LinkedHashMap<Long, Plot> merged = new LinkedHashMap<>();
            for (Plot plot : projectPlots) {
                if (plot == null || plot.getId() == null) {
                    continue;
                }
                if ((plot.getChapterId() != null && plot.getChapterId().equals(context.chapter().getId()))
                        || relatedPlotIds.contains(plot.getId())) {
                    merged.put(plot.getId(), plot);
                }
            }
            if (merged.isEmpty()) {
                for (Plot plot : chapterPlots) {
                    if (plot != null && plot.getId() != null) {
                        merged.put(plot.getId(), plot);
                    }
                }
            }
            if (merged.isEmpty()) {
                for (Plot plot : projectPlots) {
                    if (plot != null && plot.getId() != null) {
                        merged.put(plot.getId(), plot);
                    }
                }
            }
            plots = new ArrayList<>(merged.values());
        }

        List<Map<String, Object>> payload = plots.stream()
                .filter(Objects::nonNull)
                .limit(topK)
                .map(plot -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", plot.getId());
                    item.put("title", safe(plot.getTitle(), "未命名剧情"));
                    item.put("description", summarize(plot.getDescription(), 160));
                    item.put("content", summarize(plot.getContent(), 220));
                    item.put("conflicts", summarize(plot.getConflicts(), 120));
                    item.put("resolutions", summarize(plot.getResolutions(), 120));
                    item.put("characters", normalizeText(plot.getCharacters()));
                    item.put("locations", normalizeText(plot.getLocations()));
                    item.put("timeline", normalizeText(plot.getTimeline()));
                    item.put("plotType", plot.getPlotType());
                    return item;
                })
                .toList();

        return Map.of(
                "projectId", context.project().getId(),
                "chapterId", context.chapter().getId(),
                "query", query,
                "plots", payload
        );
    }

    private Object getCausalityCandidates(DirectorToolContext context, JsonNode arguments) {
        if (context.project() == null) {
            return Map.of("causalities", List.of(), "available", false);
        }

        int topK = readTopK(arguments);
        String query = readText(arguments, "query", "queryText");
        List<Causality> all = causalityService.getProjectCausalities(context.project().getId(), context.userId());
        Set<Long> relatedCausalityIds = context.outline() == null || context.outline().getRelatedCausalityIdList() == null
                ? Set.of()
                : new LinkedHashSet<>(context.outline().getRelatedCausalityIdList());

        List<Causality> filtered = all.stream()
                .filter(Objects::nonNull)
                .filter(item -> {
                    if (StringUtils.hasText(query)) {
                        String haystack = String.join("\n",
                                safe(item.getName(), ""),
                                safe(item.getRelationship(), ""),
                                safe(item.getDescription(), ""),
                                safe(item.getConditions(), ""));
                        return haystack.contains(query.trim());
                    }
                    return relatedCausalityIds.isEmpty() || relatedCausalityIds.contains(item.getId());
                })
                .limit(topK)
                .toList();
        if (filtered.isEmpty() && !StringUtils.hasText(query) && !all.isEmpty()) {
            filtered = all.stream().limit(topK).toList();
        }

        List<Map<String, Object>> payload = filtered.stream()
                .map(item -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("id", item.getId());
                    result.put("name", safe(item.getName(), safe(item.getRelationship(), "未命名因果")));
                    result.put("relationship", normalizeText(item.getRelationship()));
                    result.put("description", summarize(item.getDescription(), 180));
                    result.put("conditions", summarize(item.getConditions(), 120));
                    result.put("strength", item.getStrength());
                    result.put("causeType", normalizeText(item.getCauseType()));
                    result.put("effectType", normalizeText(item.getEffectType()));
                    return result;
                })
                .toList();

        return Map.of(
                "projectId", context.project().getId(),
                "query", query,
                "causalities", payload
        );
    }

    private Object getWorldSettingFacts(DirectorToolContext context, JsonNode arguments) {
        if (context.project() == null) {
            return Map.of("worldSettings", List.of(), "available", false);
        }

        int topK = readTopK(arguments);
        List<Map<String, Object>> payload = worldSettingService.getWorldSettingsByProjectId(context.project().getId()).stream()
                .limit(topK)
                .map(item -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("id", item.getId());
                    result.put("name", safe(item.getName(), safe(item.getTitle(), "未命名设定")));
                    result.put("title", normalizeText(item.getTitle()));
                    result.put("category", normalizeText(item.getCategory()));
                    result.put("description", summarize(
                            StringUtils.hasText(item.getDescription()) ? item.getDescription() : item.getContent(),
                            180
                    ));
                    result.put("associationCount", item.getAssociationCount());
                    return result;
                })
                .toList();
        return Map.of(
                "projectId", context.project().getId(),
                "worldSettings", payload
        );
    }

    private Object getRequiredCharacterState(DirectorToolContext context, JsonNode arguments) {
        if (context.project() == null) {
            return Map.of("characters", List.of(), "available", false);
        }

        int topK = readTopK(arguments);
        List<Character> characters = characterService.getProjectCharacters(context.project().getId(), context.userId()).stream()
                .filter(item -> isRequiredCharacter(context.chapter(), item))
                .limit(topK)
                .toList();

        List<Map<String, Object>> payload = characters.stream()
                .map(item -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("id", item.getId());
                    result.put("name", safe(item.getName(), "未命名角色"));
                    result.put("description", summarize(item.getDescription(), 180));
                    result.put("attributes", parseJsonOrText(item.getAttributes()));
                    result.put("projectRole", normalizeText(item.getProjectRole()));
                    result.put("inventoryItemCount", item.getInventoryItemCount());
                    result.put("equippedItemCount", item.getEquippedItemCount());
                    result.put("rareItemCount", item.getRareItemCount());
                    return result;
                })
                .toList();

        return Map.of(
                "projectId", context.project().getId(),
                "chapterId", context.chapter().getId(),
                "characters", payload
        );
    }

    private Object getRequiredCharacterInventory(DirectorToolContext context, JsonNode arguments) {
        if (context.project() == null || context.chapter().getRequiredCharacterIds() == null || context.chapter().getRequiredCharacterIds().isEmpty()) {
            return Map.of("inventories", List.of(), "available", false);
        }

        int topK = readTopK(arguments);
        List<Long> characterIds = context.chapter().getRequiredCharacterIds().stream()
                .filter(Objects::nonNull)
                .limit(topK)
                .toList();
        if (characterIds.isEmpty()) {
            return Map.of("inventories", List.of(), "available", false);
        }

        List<CharacterInventoryItem> inventoryItems = characterInventoryItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CharacterInventoryItem>()
                        .eq(CharacterInventoryItem::getProjectId, context.project().getId())
                        .in(CharacterInventoryItem::getCharacterId, characterIds)
                        .eq(CharacterInventoryItem::getDeleted, 0)
                        .orderByDesc(CharacterInventoryItem::getEquipped)
                        .orderByAsc(CharacterInventoryItem::getSortOrder)
                        .orderByDesc(CharacterInventoryItem::getUpdateTime)
        );

        Map<Long, ItemDefinition> itemMap = inventoryItems.stream()
                .map(CharacterInventoryItem::getItemId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.collectingAndThen(Collectors.toList(), itemIds -> itemIds.isEmpty()
                        ? Map.of()
                        : itemMapper.selectBatchIds(itemIds).stream()
                                .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                                .collect(Collectors.toMap(ItemDefinition::getId, item -> item, (left, right) -> left, LinkedHashMap::new))));

        Map<Long, String> characterNames = buildRequiredCharacterNameMap(context.chapter());
        Map<Long, List<Map<String, Object>>> itemsByCharacter = new LinkedHashMap<>();
        for (CharacterInventoryItem inventoryItem : inventoryItems) {
            if (inventoryItem.getCharacterId() == null) {
                continue;
            }
            List<Map<String, Object>> items = itemsByCharacter.computeIfAbsent(inventoryItem.getCharacterId(), key -> new ArrayList<>());
            if (items.size() >= MAX_INVENTORY_ITEMS_PER_CHARACTER) {
                continue;
            }
            ItemDefinition item = itemMap.get(inventoryItem.getItemId());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("itemId", inventoryItem.getItemId());
            result.put("name", StringUtils.hasText(inventoryItem.getCustomName())
                    ? inventoryItem.getCustomName().trim()
                    : item == null ? "物品#" + inventoryItem.getItemId() : safe(item.getName(), "物品#" + inventoryItem.getItemId()));
            result.put("quantity", inventoryItem.getQuantity() == null ? 1 : Math.max(1, inventoryItem.getQuantity()));
            result.put("equipped", Integer.valueOf(1).equals(inventoryItem.getEquipped()));
            result.put("durability", inventoryItem.getDurability());
            result.put("description", summarize(
                    StringUtils.hasText(inventoryItem.getNotes())
                            ? inventoryItem.getNotes()
                            : item == null ? "" : item.getDescription(),
                    120
            ));
            result.put("category", item == null ? "" : normalizeText(item.getCategory()));
            result.put("rarity", item == null ? "" : normalizeText(item.getRarity()));
            items.add(result);
        }

        List<Map<String, Object>> inventories = new ArrayList<>();
        for (Long characterId : characterIds) {
            List<Map<String, Object>> items = itemsByCharacter.getOrDefault(characterId, List.of());
            if (items.isEmpty()) {
                continue;
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("characterId", characterId);
            result.put("characterName", characterNames.getOrDefault(characterId, "角色#" + characterId));
            result.put("items", items);
            inventories.add(result);
        }

        return Map.of(
                "projectId", context.project().getId(),
                "chapterId", context.chapter().getId(),
                "inventories", inventories
        );
    }

    private Object searchKnowledgeDocuments(DirectorToolContext context, JsonNode arguments) {
        if (context.project() == null) {
            return Map.of("documents", List.of(), "available", false);
        }

        int topK = readTopK(arguments);
        String queryText = readText(arguments, "queryText", "query");
        if (!StringUtils.hasText(queryText)) {
            queryText = buildKnowledgeQuery(context);
        }
        if (!StringUtils.hasText(queryText)) {
            return Map.of(
                    "projectId", context.project().getId(),
                    "queryText", "",
                    "documents", List.of()
            );
        }

        List<Map<String, Object>> payload = knowledgeDocumentService.queryDocuments(context.project().getId(), context.userId(), queryText).stream()
                .limit(topK)
                .map(document -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("id", document.getId());
                    result.put("title", safe(document.getTitle(), "未命名知识"));
                    result.put("summary", summarize(
                            StringUtils.hasText(document.getSummary()) ? document.getSummary() : document.getContentText(),
                            180
                    ));
                    result.put("sourceType", normalizeText(document.getSourceType()));
                    result.put("status", normalizeText(document.getStatus()));
                    return result;
                })
                .toList();

        return Map.of(
                "projectId", context.project().getId(),
                "queryText", queryText,
                "documents", payload
        );
    }

    private Object getChatBackgroundSummary(DirectorToolContext context, JsonNode arguments) {
        if (!context.hasBackgroundContext()) {
            return Map.of(
                    "chapterId", context.chapter().getId(),
                    "available", false,
                    "worldFacts", List.of(),
                    "characterConstraints", List.of(),
                    "plotGuidance", List.of(),
                    "writingPreferences", List.of(),
                    "hardConstraints", List.of()
            );
        }

        AIWritingChatParticipationVO participation = aiWritingChatService.buildParticipationContext(
                context.userId(),
                context.chapter().getId(),
                context.provider(),
                context.model()
        );
        return Map.of(
                "chapterId", context.chapter().getId(),
                "available", participation.hasContent(),
                "worldFacts", safeList(participation.getWorldFacts()),
                "characterConstraints", safeList(participation.getCharacterConstraints()),
                "plotGuidance", safeList(participation.getPlotGuidance()),
                "writingPreferences", safeList(participation.getWritingPreferences()),
                "hardConstraints", safeList(participation.getHardConstraints())
        );
    }

    private JsonNode readArguments(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            return root == null ? objectMapper.createObjectNode() : root;
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{\"error\":\"tool_result_serialization_failed\"}";
        }
    }

    private boolean isRequiredCharacter(Chapter chapter, Character character) {
        if (chapter == null || character == null) {
            return false;
        }
        List<Long> requiredIds = chapter.getRequiredCharacterIds() == null ? List.of() : chapter.getRequiredCharacterIds();
        if (character.getId() != null && requiredIds.contains(character.getId())) {
            return true;
        }
        List<String> requiredNames = chapter.getRequiredCharacterNames() == null ? List.of() : chapter.getRequiredCharacterNames();
        return StringUtils.hasText(character.getName()) && requiredNames.contains(character.getName().trim());
    }

    private Map<Long, String> buildRequiredCharacterNameMap(Chapter chapter) {
        Map<Long, String> names = new LinkedHashMap<>();
        List<Long> ids = chapter.getRequiredCharacterIds() == null ? List.of() : chapter.getRequiredCharacterIds();
        List<String> requiredNames = chapter.getRequiredCharacterNames() == null ? List.of() : chapter.getRequiredCharacterNames();
        for (int index = 0; index < ids.size(); index++) {
            Long characterId = ids.get(index);
            if (characterId == null) {
                continue;
            }
            String name = index < requiredNames.size() ? requiredNames.get(index) : null;
            if (StringUtils.hasText(name)) {
                names.put(characterId, name.trim());
            }
        }
        return names;
    }

    private String buildKnowledgeQuery(DirectorToolContext context) {
        List<String> parts = new ArrayList<>();
        parts.add(normalizeText(context.chapter().getTitle()));
        parts.add(normalizeText(context.requestDTO().getUserInstruction()));
        if (context.outline() != null) {
            parts.add(normalizeText(context.outline().getSummary()));
            parts.add(normalizeText(context.outline().getStageGoal()));
        }
        return parts.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
    }

    private Object parseJsonOrText(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return Map.of();
        }
        try {
            JsonNode node = objectMapper.readTree(rawJson);
            if (node != null && (node.isObject() || node.isArray())) {
                return node;
            }
        } catch (Exception ignored) {
        }
        return rawJson.trim();
    }

    private List<String> safeList(List<String> items) {
        return items == null ? List.of() : items.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }

    private int readTopK(JsonNode arguments) {
        return Math.min(MAX_TOOL_ITEMS, Math.max(1, readInt(arguments, DEFAULT_TOP_K, "topK", "limit")));
    }

    private String summarize(String text, int maxLength) {
        String normalized = normalizeText(text);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String safe(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private int readInt(JsonNode node, int fallback, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode valueNode = node.path(fieldName);
            if (valueNode.isInt() || valueNode.isLong()) {
                return valueNode.asInt();
            }
            if (!valueNode.isMissingNode() && !valueNode.isNull() && StringUtils.hasText(valueNode.asText())) {
                try {
                    return Integer.parseInt(valueNode.asText().trim());
                } catch (NumberFormatException ignored) {
                    return fallback;
                }
            }
        }
        return fallback;
    }

    private boolean readBoolean(JsonNode node, boolean fallback, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode valueNode = node.path(fieldName);
            if (valueNode.isBoolean()) {
                return valueNode.asBoolean();
            }
            if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                String value = valueNode.asText("").trim().toLowerCase();
                if ("true".equals(value) || "1".equals(value) || "yes".equals(value)) {
                    return true;
                }
                if ("false".equals(value) || "0".equals(value) || "no".equals(value)) {
                    return false;
                }
            }
        }
        return fallback;
    }

    private String readText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode valueNode = node.path(fieldName);
            if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                String value = valueNode.asText("").trim();
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
        }
        return "";
    }

    public record DirectorToolContext(
            Long userId,
            AIDirectorDecisionRequestDTO requestDTO,
            Chapter chapter,
            Project project,
            Outline outline,
            boolean hasBackgroundContext,
            AIProvider provider,
            String model) {
    }
}
