package com.storyweaver.item.web.request;

import lombok.Data;

@Data
public class ItemRequest {

    private String name;

    private String description;

    private String category;

    private String rarity;

    private Boolean stackable;

    private Integer maxStack;

    private Boolean usable;

    private Boolean equippable;

    private String slotType;

    private Integer itemValue;

    private Integer weight;

    private String attributesJson;

    private String effectJson;

    private String tags;

    private String sourceType;
}
