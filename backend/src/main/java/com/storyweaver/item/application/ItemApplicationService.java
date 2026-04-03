package com.storyweaver.item.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.item.domain.entity.ItemDefinition;
import com.storyweaver.item.domain.support.ItemCatalogRules;
import com.storyweaver.item.infrastructure.persistence.mapper.ItemMapper;
import com.storyweaver.item.web.request.ItemRequest;
import com.storyweaver.item.web.response.ItemGenerationResultResponse;
import com.storyweaver.service.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ItemApplicationService {

    private final ItemMapper itemMapper;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    public ItemApplicationService(ItemMapper itemMapper, ProjectService projectService, ObjectMapper objectMapper) {
        this.itemMapper = itemMapper;
        this.projectService = projectService;
        this.objectMapper = objectMapper;
    }

    public List<ItemDefinition> listProjectItems(Long projectId, Long userId) {
        requireProjectAccess(projectId, userId);
        return itemMapper.selectList(new LambdaQueryWrapper<ItemDefinition>()
                .eq(ItemDefinition::getProjectId, projectId)
                .eq(ItemDefinition::getDeleted, 0)
                .orderByDesc(ItemDefinition::getUpdateTime)
                .orderByDesc(ItemDefinition::getId));
    }

    @Transactional
    public ItemDefinition createItem(Long projectId, Long userId, ItemRequest request) {
        requireProjectAccess(projectId, userId);
        ItemDefinition item = new ItemDefinition();
        item.setProjectId(projectId);
        item.setOwnerUserId(userId);
        applyRequest(item, request, false);
        itemMapper.insert(item);
        return itemMapper.selectById(item.getId());
    }

    @Transactional
    public ItemDefinition updateItem(Long projectId, Long itemId, Long userId, ItemRequest request) {
        requireProjectAccess(projectId, userId);
        ItemDefinition item = requireItem(projectId, itemId);
        applyRequest(item, request, true);
        itemMapper.updateById(item);
        return itemMapper.selectById(itemId);
    }

    @Transactional
    public boolean deleteItem(Long projectId, Long itemId, Long userId) {
        requireProjectAccess(projectId, userId);
        ItemDefinition item = requireItem(projectId, itemId);
        return itemMapper.deleteById(item.getId()) > 0;
    }

    public ItemDefinition findReusableProjectItem(Long projectId, String name, String category) {
        return itemMapper.selectOne(new LambdaQueryWrapper<ItemDefinition>()
                .eq(ItemDefinition::getProjectId, projectId)
                .eq(ItemDefinition::getDeleted, 0)
                .eq(ItemDefinition::getName, name)
                .eq(ItemDefinition::getCategory, category)
                .last("LIMIT 1"));
    }

    public ItemDefinition getItemById(Long itemId) {
        ItemDefinition item = itemMapper.selectById(itemId);
        return item == null || Integer.valueOf(1).equals(item.getDeleted()) ? null : item;
    }

    @Transactional
    public ItemDefinition createGeneratedItem(Long projectId, Long userId, ItemGenerationResultResponse.GeneratedItemResponse generatedItem) {
        ItemDefinition existing = findReusableProjectItem(projectId, generatedItem.getName(), generatedItem.getCategory());
        if (existing != null) {
            return existing;
        }

        ItemRequest request = new ItemRequest();
        request.setName(generatedItem.getName());
        request.setDescription(generatedItem.getDescription());
        request.setCategory(generatedItem.getCategory());
        request.setRarity(generatedItem.getRarity());
        request.setStackable(generatedItem.isStackable());
        request.setMaxStack(generatedItem.getMaxStack());
        request.setUsable(generatedItem.isUsable());
        request.setEquippable(generatedItem.isEquippable());
        request.setSlotType(generatedItem.getSlotType());
        request.setItemValue(generatedItem.getItemValue());
        request.setWeight(generatedItem.getWeight());
        request.setAttributesJson(generatedItem.getAttributesJson());
        request.setEffectJson(generatedItem.getEffectJson());
        request.setTags(generatedItem.getTags());
        request.setSourceType(generatedItem.getSourceType());
        return createItem(projectId, userId, request);
    }

    private void applyRequest(ItemDefinition item, ItemRequest request, boolean allowPartial) {
        if (!allowPartial || StringUtils.hasText(request.getName())) {
            if (!StringUtils.hasText(request.getName())) {
                throw new IllegalArgumentException("物品名称不能为空");
            }
            item.setName(request.getName().trim());
        }

        if (!allowPartial || request.getDescription() != null) {
            item.setDescription(trimToNull(request.getDescription()));
        }
        if (!allowPartial || request.getCategory() != null) {
            item.setCategory(ItemCatalogRules.normalizeCategory(request.getCategory()));
        }
        if (!allowPartial || request.getRarity() != null) {
            item.setRarity(ItemCatalogRules.normalizeRarity(request.getRarity()));
        }
        if (!allowPartial || request.getStackable() != null) {
            item.setStackable(Boolean.TRUE.equals(request.getStackable()) ? 1 : 0);
        }

        int maxStack = request.getMaxStack() == null ? 1 : Math.max(1, request.getMaxStack());
        boolean stackable = request.getStackable() != null
                ? Boolean.TRUE.equals(request.getStackable())
                : Integer.valueOf(1).equals(item.getStackable());
        if (!allowPartial || request.getMaxStack() != null || request.getStackable() != null) {
            item.setMaxStack(stackable ? Math.max(2, maxStack) : 1);
        }

        if (!allowPartial || request.getUsable() != null) {
            item.setUsable(Boolean.TRUE.equals(request.getUsable()) ? 1 : 0);
        }
        if (!allowPartial || request.getEquippable() != null) {
            item.setEquippable(Boolean.TRUE.equals(request.getEquippable()) ? 1 : 0);
        }
        if (!allowPartial || request.getSlotType() != null) {
            item.setSlotType(ItemCatalogRules.normalizeSlotType(request.getSlotType()));
        }
        if (!allowPartial || request.getItemValue() != null) {
            item.setItemValue(request.getItemValue() == null ? 0 : Math.max(0, request.getItemValue()));
        }
        if (!allowPartial || request.getWeight() != null) {
            item.setWeight(request.getWeight() == null ? 0 : Math.max(0, request.getWeight()));
        }
        if (!allowPartial || request.getAttributesJson() != null) {
            item.setAttributesJson(normalizeJson(request.getAttributesJson(), "物品属性 JSON 格式不正确"));
        }
        if (!allowPartial || request.getEffectJson() != null) {
            item.setEffectJson(normalizeJson(request.getEffectJson(), "物品效果 JSON 格式不正确"));
        }
        if (!allowPartial || request.getTags() != null) {
            item.setTags(ItemCatalogRules.normalizeTags(request.getTags()));
        }
        if (!allowPartial || request.getSourceType() != null) {
            item.setSourceType(StringUtils.hasText(request.getSourceType()) ? request.getSourceType().trim() : "manual");
        }

        if (item.getCategory() == null) {
            item.setCategory("prop");
        }
        if (item.getRarity() == null) {
            item.setRarity("common");
        }
        if (item.getStackable() == null) {
            item.setStackable(0);
        }
        if (item.getUsable() == null) {
            item.setUsable(0);
        }
        if (item.getEquippable() == null) {
            item.setEquippable(0);
        }
        if (item.getMaxStack() == null) {
            item.setMaxStack(Integer.valueOf(1).equals(item.getStackable()) ? 20 : 1);
        }
        if (item.getItemValue() == null) {
            item.setItemValue(0);
        }
        if (item.getWeight() == null) {
            item.setWeight(0);
        }
        if (!StringUtils.hasText(item.getAttributesJson())) {
            item.setAttributesJson("{}");
        }
        if (!StringUtils.hasText(item.getEffectJson())) {
            item.setEffectJson("{}");
        }
        if (!StringUtils.hasText(item.getSourceType())) {
            item.setSourceType("manual");
        }
    }

    private String normalizeJson(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            return "{}";
        }
        try {
            objectMapper.readTree(value.trim());
            return value.trim();
        } catch (Exception exception) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private ItemDefinition requireItem(Long projectId, Long itemId) {
        ItemDefinition item = itemMapper.selectById(itemId);
        if (item == null || Integer.valueOf(1).equals(item.getDeleted()) || !projectId.equals(item.getProjectId())) {
            throw new IllegalArgumentException("物品不存在、未归属当前项目或已被删除");
        }
        return item;
    }

    private void requireProjectAccess(Long projectId, Long userId) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            throw new IllegalArgumentException("项目不存在或无权访问");
        }
    }
}
