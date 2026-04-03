package com.storyweaver.item.web.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ItemGenerationResultResponse {

    private String providerName;

    private String modelName;

    private List<GeneratedItemResponse> items = new ArrayList<>();

    @Data
    public static class GeneratedItemResponse {
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
        private Integer suggestedQuantity;
    }
}
