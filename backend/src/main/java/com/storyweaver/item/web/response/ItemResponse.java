package com.storyweaver.item.web.response;

import com.storyweaver.item.domain.entity.ItemDefinition;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ItemResponse {

    private Long id;

    private Long projectId;

    private Long ownerUserId;

    private String name;

    private String description;

    private String category;

    private String rarity;

    private boolean stackable;

    private Integer maxStack;

    private boolean usable;

    private boolean equippable;

    private String slotType;

    private Integer itemValue;

    private Integer weight;

    private String attributesJson;

    private String effectJson;

    private String tags;

    private String sourceType;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static ItemResponse from(ItemDefinition item) {
        ItemResponse response = new ItemResponse();
        response.setId(item.getId());
        response.setProjectId(item.getProjectId());
        response.setOwnerUserId(item.getOwnerUserId());
        response.setName(item.getName());
        response.setDescription(item.getDescription());
        response.setCategory(item.getCategory());
        response.setRarity(item.getRarity());
        response.setStackable(Integer.valueOf(1).equals(item.getStackable()));
        response.setMaxStack(item.getMaxStack());
        response.setUsable(Integer.valueOf(1).equals(item.getUsable()));
        response.setEquippable(Integer.valueOf(1).equals(item.getEquippable()));
        response.setSlotType(item.getSlotType());
        response.setItemValue(item.getItemValue());
        response.setWeight(item.getWeight());
        response.setAttributesJson(item.getAttributesJson());
        response.setEffectJson(item.getEffectJson());
        response.setTags(item.getTags());
        response.setSourceType(item.getSourceType());
        response.setCreateTime(item.getCreateTime());
        response.setUpdateTime(item.getUpdateTime());
        return response;
    }
}
