package com.storyweaver.item.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.ProjectCharacterLink;
import com.storyweaver.item.domain.entity.CharacterInventoryItem;
import com.storyweaver.item.domain.entity.ItemDefinition;
import com.storyweaver.item.infrastructure.persistence.mapper.CharacterInventoryItemMapper;
import com.storyweaver.item.infrastructure.persistence.mapper.ItemMapper;
import com.storyweaver.item.web.request.InventoryItemRequest;
import com.storyweaver.repository.ProjectCharacterMapper;
import com.storyweaver.service.CharacterService;
import com.storyweaver.service.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class InventoryApplicationService {

    private final CharacterInventoryItemMapper inventoryItemMapper;
    private final ItemMapper itemMapper;
    private final ProjectService projectService;
    private final CharacterService characterService;
    private final ProjectCharacterMapper projectCharacterMapper;

    public InventoryApplicationService(
            CharacterInventoryItemMapper inventoryItemMapper,
            ItemMapper itemMapper,
            ProjectService projectService,
            CharacterService characterService,
            ProjectCharacterMapper projectCharacterMapper) {
        this.inventoryItemMapper = inventoryItemMapper;
        this.itemMapper = itemMapper;
        this.projectService = projectService;
        this.characterService = characterService;
        this.projectCharacterMapper = projectCharacterMapper;
    }

    public List<CharacterInventoryItem> listInventory(Long projectId, Long characterId, Long userId) {
        requireProjectCharacter(projectId, characterId, userId);
        return inventoryItemMapper.selectList(new LambdaQueryWrapper<CharacterInventoryItem>()
                .eq(CharacterInventoryItem::getProjectId, projectId)
                .eq(CharacterInventoryItem::getCharacterId, characterId)
                .eq(CharacterInventoryItem::getDeleted, 0)
                .orderByDesc(CharacterInventoryItem::getEquipped)
                .orderByAsc(CharacterInventoryItem::getSortOrder)
                .orderByDesc(CharacterInventoryItem::getUpdateTime));
    }

    @Transactional
    public CharacterInventoryItem addInventoryItem(Long projectId, Long characterId, Long userId, InventoryItemRequest request) {
        requireProjectCharacter(projectId, characterId, userId);
        ItemDefinition item = requireProjectItem(projectId, request.getItemId());

        if (Integer.valueOf(1).equals(item.getStackable())) {
            CharacterInventoryItem existing = inventoryItemMapper.selectOne(new LambdaQueryWrapper<CharacterInventoryItem>()
                    .eq(CharacterInventoryItem::getProjectId, projectId)
                    .eq(CharacterInventoryItem::getCharacterId, characterId)
                    .eq(CharacterInventoryItem::getItemId, item.getId())
                    .eq(CharacterInventoryItem::getDeleted, 0)
                    .last("LIMIT 1"));
            if (existing != null) {
                int maxStack = Math.max(1, item.getMaxStack() == null ? 99 : item.getMaxStack());
                int currentQuantity = existing.getQuantity() == null ? 1 : Math.max(1, existing.getQuantity());
                existing.setQuantity(Math.min(maxStack, currentQuantity + normalizeQuantity(request.getQuantity(), item)));
                if (request.getEquipped() != null) {
                    existing.setEquipped(Boolean.TRUE.equals(request.getEquipped()) && Integer.valueOf(1).equals(item.getEquippable()) ? 1 : 0);
                }
                if (request.getDurability() != null) {
                    existing.setDurability(Math.max(0, request.getDurability()));
                }
                if (request.getNotes() != null) {
                    existing.setNotes(trimToNull(request.getNotes()));
                }
                inventoryItemMapper.updateById(existing);
                return inventoryItemMapper.selectById(existing.getId());
            }
        }

        CharacterInventoryItem inventoryItem = new CharacterInventoryItem();
        inventoryItem.setProjectId(projectId);
        inventoryItem.setCharacterId(characterId);
        inventoryItem.setItemId(item.getId());
        inventoryItem.setQuantity(normalizeQuantity(request.getQuantity(), item));
        inventoryItem.setEquipped(Boolean.TRUE.equals(request.getEquipped()) && Integer.valueOf(1).equals(item.getEquippable()) ? 1 : 0);
        inventoryItem.setDurability(request.getDurability() == null ? 100 : Math.max(0, request.getDurability()));
        inventoryItem.setCustomName(trimToNull(request.getCustomName()));
        inventoryItem.setNotes(trimToNull(request.getNotes()));
        inventoryItem.setSortOrder(resolveSortOrder(projectId, characterId, request.getSortOrder()));
        inventoryItemMapper.insert(inventoryItem);
        return inventoryItemMapper.selectById(inventoryItem.getId());
    }

    @Transactional
    public CharacterInventoryItem updateInventoryItem(
            Long projectId,
            Long characterId,
            Long inventoryItemId,
            Long userId,
            InventoryItemRequest request) {
        requireProjectCharacter(projectId, characterId, userId);
        CharacterInventoryItem inventoryItem = requireInventoryItem(projectId, characterId, inventoryItemId);
        ItemDefinition item = requireProjectItem(projectId, inventoryItem.getItemId());

        if (request.getQuantity() != null) {
            inventoryItem.setQuantity(normalizeQuantity(request.getQuantity(), item));
        }
        if (request.getEquipped() != null) {
            inventoryItem.setEquipped(Boolean.TRUE.equals(request.getEquipped()) && Integer.valueOf(1).equals(item.getEquippable()) ? 1 : 0);
        }
        if (request.getDurability() != null) {
            inventoryItem.setDurability(Math.max(0, request.getDurability()));
        }
        if (request.getCustomName() != null) {
            inventoryItem.setCustomName(trimToNull(request.getCustomName()));
        }
        if (request.getNotes() != null) {
            inventoryItem.setNotes(trimToNull(request.getNotes()));
        }
        if (request.getSortOrder() != null) {
            inventoryItem.setSortOrder(Math.max(0, request.getSortOrder()));
        }

        inventoryItemMapper.updateById(inventoryItem);
        return inventoryItemMapper.selectById(inventoryItem.getId());
    }

    @Transactional
    public boolean deleteInventoryItem(Long projectId, Long characterId, Long inventoryItemId, Long userId) {
        requireProjectCharacter(projectId, characterId, userId);
        CharacterInventoryItem inventoryItem = requireInventoryItem(projectId, characterId, inventoryItemId);
        return inventoryItemMapper.deleteById(inventoryItem.getId()) > 0;
    }

    @Transactional
    public void deleteByItemId(Long itemId) {
        inventoryItemMapper.delete(new LambdaQueryWrapper<CharacterInventoryItem>()
                .eq(CharacterInventoryItem::getItemId, itemId)
                .eq(CharacterInventoryItem::getDeleted, 0));
    }

    @Transactional
    public void deleteByProjectCharacter(Long projectId, Long characterId) {
        inventoryItemMapper.delete(new LambdaQueryWrapper<CharacterInventoryItem>()
                .eq(CharacterInventoryItem::getProjectId, projectId)
                .eq(CharacterInventoryItem::getCharacterId, characterId)
                .eq(CharacterInventoryItem::getDeleted, 0));
    }

    private CharacterInventoryItem requireInventoryItem(Long projectId, Long characterId, Long inventoryItemId) {
        CharacterInventoryItem inventoryItem = inventoryItemMapper.selectById(inventoryItemId);
        if (inventoryItem == null
                || Integer.valueOf(1).equals(inventoryItem.getDeleted())
                || !projectId.equals(inventoryItem.getProjectId())
                || !characterId.equals(inventoryItem.getCharacterId())) {
            throw new IllegalArgumentException("背包条目不存在或不属于当前角色");
        }
        return inventoryItem;
    }

    private void requireProjectCharacter(Long projectId, Long characterId, Long userId) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            throw new IllegalArgumentException("项目不存在或无权访问");
        }

        Character character = characterService.getCharacterWithAuth(characterId, userId);
        if (character == null) {
            throw new IllegalArgumentException("角色不存在或无权访问");
        }

        ProjectCharacterLink link = projectCharacterMapper.selectOne(new LambdaQueryWrapper<ProjectCharacterLink>()
                .eq(ProjectCharacterLink::getProjectId, projectId)
                .eq(ProjectCharacterLink::getCharacterId, characterId)
                .last("LIMIT 1"));
        if (link == null) {
            throw new IllegalArgumentException("角色未关联到当前项目");
        }
    }

    private ItemDefinition requireProjectItem(Long projectId, Long itemId) {
        ItemDefinition item = itemMapper.selectById(itemId);
        if (item == null || Integer.valueOf(1).equals(item.getDeleted()) || !projectId.equals(item.getProjectId())) {
            throw new IllegalArgumentException("物品不存在或未归属当前项目");
        }
        return item;
    }

    private int normalizeQuantity(Integer requested, ItemDefinition item) {
        int quantity = requested == null ? 1 : Math.max(1, requested);
        if (!Integer.valueOf(1).equals(item.getStackable())) {
            return 1;
        }
        return Math.min(quantity, Math.max(1, item.getMaxStack() == null ? 99 : item.getMaxStack()));
    }

    private Integer resolveSortOrder(Long projectId, Long characterId, Integer requestedSortOrder) {
        if (requestedSortOrder != null) {
            return Math.max(0, requestedSortOrder);
        }

        CharacterInventoryItem last = inventoryItemMapper.selectOne(new LambdaQueryWrapper<CharacterInventoryItem>()
                .eq(CharacterInventoryItem::getProjectId, projectId)
                .eq(CharacterInventoryItem::getCharacterId, characterId)
                .eq(CharacterInventoryItem::getDeleted, 0)
                .orderByDesc(CharacterInventoryItem::getSortOrder)
                .last("LIMIT 1"));
        return last == null || last.getSortOrder() == null ? 0 : last.getSortOrder() + 1;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
