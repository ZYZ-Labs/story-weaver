package com.storyweaver.storyunit.context.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.ProjectCharacterLink;
import com.storyweaver.item.domain.entity.CharacterInventoryItem;
import com.storyweaver.item.domain.entity.ItemDefinition;
import com.storyweaver.item.infrastructure.persistence.mapper.CharacterInventoryItemMapper;
import com.storyweaver.item.infrastructure.persistence.mapper.ItemMapper;
import com.storyweaver.repository.CharacterMapper;
import com.storyweaver.repository.ProjectCharacterMapper;
import com.storyweaver.storyunit.context.CharacterRuntimeStateQueryService;
import com.storyweaver.storyunit.context.CharacterRuntimeStateView;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DefaultCharacterRuntimeStateQueryService implements CharacterRuntimeStateQueryService {

    private final CharacterMapper characterMapper;
    private final ProjectCharacterMapper projectCharacterMapper;
    private final CharacterInventoryItemMapper characterInventoryItemMapper;
    private final ItemMapper itemMapper;

    public DefaultCharacterRuntimeStateQueryService(
            CharacterMapper characterMapper,
            ProjectCharacterMapper projectCharacterMapper,
            CharacterInventoryItemMapper characterInventoryItemMapper,
            ItemMapper itemMapper) {
        this.characterMapper = characterMapper;
        this.projectCharacterMapper = projectCharacterMapper;
        this.characterInventoryItemMapper = characterInventoryItemMapper;
        this.itemMapper = itemMapper;
    }

    @Override
    public Optional<CharacterRuntimeStateView> getCharacterRuntimeState(Long projectId, Long characterId) {
        if (projectId == null || characterId == null) {
            return Optional.empty();
        }
        Character character = characterMapper.selectById(characterId);
        if (character == null || Integer.valueOf(1).equals(character.getDeleted())) {
            return Optional.empty();
        }
        ProjectCharacterLink projectLink = projectCharacterMapper.selectOne(new LambdaQueryWrapper<ProjectCharacterLink>()
                .eq(ProjectCharacterLink::getProjectId, projectId)
                .eq(ProjectCharacterLink::getCharacterId, characterId)
                .last("LIMIT 1"));
        if (projectLink == null) {
            return Optional.empty();
        }

        List<String> inventoryNames = loadInventoryNames(projectId, characterId);
        List<String> stateTags = ContextViewSupport.sanitizeDistinct(Arrays.asList(
                projectLink.getRoleType(),
                projectLink.getProjectRole(),
                character.getActiveStage(),
                Integer.valueOf(1).equals(character.getIsRetired()) ? "已退场" : null
        ));

        return Optional.of(new CharacterRuntimeStateView(
                projectId,
                characterId,
                character.getName(),
                "",
                character.getActiveStage(),
                ContextViewSupport.firstNonBlank(character.getCoreGoal(), character.getGrowthArc(), character.getDescription()),
                inventoryNames,
                List.of(),
                stateTags
        ));
    }

    private List<String> loadInventoryNames(Long projectId, Long characterId) {
        List<CharacterInventoryItem> inventoryItems = characterInventoryItemMapper.selectList(new LambdaQueryWrapper<CharacterInventoryItem>()
                .eq(CharacterInventoryItem::getProjectId, projectId)
                .eq(CharacterInventoryItem::getCharacterId, characterId)
                .eq(CharacterInventoryItem::getDeleted, 0)
                .orderByDesc(CharacterInventoryItem::getEquipped)
                .orderByAsc(CharacterInventoryItem::getSortOrder)
                .orderByDesc(CharacterInventoryItem::getUpdateTime));
        if (inventoryItems.isEmpty()) {
            return List.of();
        }

        List<Long> itemIds = inventoryItems.stream()
                .map(CharacterInventoryItem::getItemId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, ItemDefinition> itemMap = itemIds.isEmpty()
                ? Map.of()
                : itemMapper.selectBatchIds(itemIds).stream()
                .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item), Map::putAll);

        return ContextViewSupport.sanitizeDistinct(inventoryItems.stream()
                .map(item -> ContextViewSupport.firstNonBlank(item.getCustomName(), itemMap.containsKey(item.getItemId()) ? itemMap.get(item.getItemId()).getName() : ""))
                .toList());
    }
}
